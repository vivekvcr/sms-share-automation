# SMS Share Automation

Selenium suite that exercises the SMS Share form on 12 WIC/EBT sites and posts a
pass/fail report to Slack. Windows-only.

Each site runs the same nine steps — open page, share icon, SMS tab, country code
(India), phone number, terms checkbox, submit — then waits for the site's
"SMS sent successfully!" confirmation.

## Running it

From any folder (`run.cmd` sets its own working directory):

```powershell
& "<path to project folder>\run.cmd"
```

One site only — note the **quotes around the argument**. PowerShell splits an
unquoted `-Dexec.args=...` and Maven then rejects `.args=oklahoma` as a lifecycle
phase:

```powershell
& "...\run.cmd" "-Dexec.args=oklahoma"
```

Valid site arguments: `livewell`, `westvirginia`, `oklahoma`, `oregon`, `delaware`,
`indiana`, `indiana-breastfeeding` (alias `infographic`), `kansas`, `newjersey`,
`connecticut`, `nebraska`, `chickasaw`, or any full URL.

`run.cmd` loads `.env` and uses the bundled Maven in `.tools\`, so Maven does not
need to be on PATH.

## Scheduling it

**No scheduler is registered.** The suite is manual-only; nothing runs on its own.

`run_scheduled.cmd` exists for unattended runs — it wraps `run.cmd` and captures all
output to `logs\scheduled_<timestamp>.log` — but it has no notion of time and runs
immediately whenever invoked. Nothing in this repo schedules anything; that job
belongs to Windows Task Scheduler.

To set up a daily run at 19:00, from the project folder:

```powershell
Register-ScheduledTask -TaskName 'WIC SMS Share Daily Test' `
  -Action  (New-ScheduledTaskAction -Execute "cmd.exe" `
              -Argument '/c "run_scheduled.cmd"' -WorkingDirectory $PWD) `
  -Trigger (New-ScheduledTaskTrigger -Daily -At "19:00") `
  -Settings (New-ScheduledTaskSettingsSet -StartWhenAvailable `
              -MultipleInstances IgnoreNew `
              -ExecutionTimeLimit (New-TimeSpan -Hours 2))
```

Then manage it with:

```powershell
Start-ScheduledTask      -TaskName 'WIC SMS Share Daily Test'   # run now (silent; check the log)
Get-ScheduledTaskInfo    -TaskName 'WIC SMS Share Daily Test'   # last result / next run
Disable-ScheduledTask    -TaskName 'WIC SMS Share Daily Test'   # pause it
Unregister-ScheduledTask -TaskName 'WIC SMS Share Daily Test' -Confirm:$false
```

`LastTaskResult 267009` (`0x41301`) means "currently running", not an error. While a
log is being written its reported file size stays stale — read the contents, not the
size.

The task stores an **absolute** working directory, so moving or renaming the project
folder breaks the scheduled run silently. Re-register it after any move.

`LastTaskResult 267009` (`0x41301`) means "currently running", not an error. While a
log is being written its reported file size stays stale — read the contents, not the
size.

**Limitation:** the task runs only while the user is logged on. Running it on a
locked or signed-out machine requires storing the account password in the task,
which must be done by hand in `taskschd.msc`.

## Configuration

`.env` (gitignored) holds the Slack webhook and timezone. Two webhooks are kept
there — WLIQ and test — and **exactly one must be uncommented**. Slack is skipped
entirely if no webhook is set.

## Files

- `src/test/java/com/selenium/test/SMSShareTest.java` — all test logic, plus the
  per-site XPath map. Each site has its own selectors; these break whenever a site's
  markup changes, and are the usual cause of a failure.
- `src/main/java/com/selenium/utils/SlackService.java` — Slack report formatting.
- `run.cmd` — loads `.env`, runs the suite via the bundled Maven.
- `run_scheduled.cmd` — Task Scheduler entry point; wraps `run.cmd` with logging.
- `pom.xml` — Java 11 target, Selenium 4.15, JUnit 5.
- `.tools/` — vendored Maven 3.9.16.

## Logs

`logs\scheduled_<timestamp>.log` — one per scheduled run. Manual runs print to the
console and are not logged.

Note that the suite prints the first 50 characters of the Slack webhook URL to
stdout, so it lands in these files. They are gitignored, but avoid sharing them raw.

## Diagnosing failures

A failure reports which step it died on. Steps 2–8 failing means a **selector** is
stale — record the current flow in Selenium IDE, but verify against the live DOM
before trusting the recorded XPaths, which are often one wrapper level off.

Failing after step 9 is **not** a selector problem: the form submitted and the site
did not confirm. The wait loop watches for the sites' known rejection messages
("Monthly SMS limit reached.", "Failed to send SMS", and others) and reports the
actual text, so check that before touching any XPath.

Note that each full run sends 12 real SMS messages, so repeated runs consume quota.

## Requirements

- Java 17 (JDK 11+ target)
- Google Chrome (ChromeDriver is fetched automatically by WebDriverManager)
- No Maven install needed — bundled in `.tools\`
