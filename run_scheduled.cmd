@echo off
REM ===========================================================================
REM run_scheduled.cmd - unattended entry point for the SMS Share suite
REM ===========================================================================
REM
REM PURPOSE
REM   Runs the full 12-site suite and captures everything it prints to a
REM   timestamped log file. Intended to be launched by something that has no
REM   console attached (Windows Task Scheduler, a CI agent, a shortcut), where
REM   output would otherwise be lost.
REM
REM   This script has NO concept of time. It runs immediately, every time it is
REM   invoked. Any "run daily at 7 PM" behaviour comes from whatever calls it.
REM
REM HOW TO SCHEDULE IT
REM   No scheduler is registered by default. To run it daily at 19:00, create a
REM   Windows task (adjust the path if this folder moves):
REM
REM     $dir = "<full path to this folder>"
REM     Register-ScheduledTask -TaskName 'WIC SMS Share Daily Test' `
REM       -Action  (New-ScheduledTaskAction -Execute "cmd.exe" `
REM                   -Argument '/c "run_scheduled.cmd"' -WorkingDirectory $dir) `
REM       -Trigger (New-ScheduledTaskTrigger -Daily -At "19:00") `
REM       -Settings (New-ScheduledTaskSettingsSet -StartWhenAvailable `
REM                   -MultipleInstances IgnoreNew `
REM                   -ExecutionTimeLimit (New-TimeSpan -Hours 2))
REM
REM   Notes on those settings:
REM     StartWhenAvailable  - a run missed while the machine was off fires later
REM     IgnoreNew           - never start a second run while one is in progress
REM     ExecutionTimeLimit  - kill a wedged browser rather than hang forever
REM
REM   A task registered this way runs only while the user is logged on. Running
REM   it on a locked or signed-out machine requires storing the account password
REM   in the task, which must be done by hand in taskschd.msc.
REM
REM   The task stores an ABSOLUTE working directory, so moving or renaming this
REM   folder breaks the scheduled run silently. Re-register it after any move.
REM
REM CHAIN
REM   run_scheduled.cmd -> run.cmd -> bundled Maven (.tools\) -> SMSShareTest
REM   run.cmd is what loads .env, so the Slack webhook is picked up there, not
REM   here. Whichever webhook is uncommented in .env receives the report.
REM
REM EXIT CODE
REM   Propagates run.cmd's exit code, so a scheduler records a real pass/fail.
REM ===========================================================================

REM Anchor to this script's own folder. %~dp0 is the directory containing this
REM file (with a trailing backslash). Required because a scheduler may launch us
REM from anywhere, and run.cmd resolves .env and .tools\ relative to the cwd.
cd /d "%~dp0"

REM Log directory is gitignored and may not exist on a fresh clone.
if not exist "logs" md "logs"

REM Build a yyyyMMdd_HHmmss stamp so each run gets its own log.
REM
REM Deliberately NOT using cmd's %date%/%time%: their format follows the
REM machine's locale (dd-MM-yyyy here, MM/dd/yyyy elsewhere), so any substring
REM parsing of them breaks on a differently configured machine. Shelling out to
REM PowerShell costs ~200ms once per run and is locale-proof.
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set "TS=%%i"

set "LOG=logs\scheduled_%TS%.log"

REM Single > truncates: this is a new file per run, so it starts clean.
echo === SMS Share scheduled run started %TS% === > "%LOG%"

REM `call` (not a bare invocation) so control returns here afterwards - without
REM it, cmd transfers to run.cmd and never comes back to write the footer.
REM `2>&1` folds stderr into the log; the suite reports failures on stderr, so
REM omitting it would drop exactly the output worth keeping.
REM
REM While this is running, Windows leaves the log's reported FILE SIZE stale
REM (often showing only the header). The file is filling normally - read its
REM contents rather than trusting the size shown in Explorer or dir.
call "%~dp0run.cmd" >> "%LOG%" 2>&1

REM Capture immediately: ERRORLEVEL is clobbered by the next command that runs.
set "RC=%ERRORLEVEL%"

echo === finished (exit code %RC%) === >> "%LOG%"

exit /b %RC%
