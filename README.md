# SMS Share Automation

Simple automation for testing SMS Share forms with Slack notifications.

## Features

- ✅ Tests SMS Share forms across multiple URLs
- ✅ Sends results to Slack
- ✅ Runs automatically via cron/Launchd
- ✅ Headless mode (runs in background)

## Quick Start

### 1. Configure Slack (optional)

Put your Slack webhook in `.env` (file is gitignored):
```bash
SLACK_WEBHOOK_URL="https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
SLACK_USERNAME="SMS Share Test Bot"
```

### 2. Run tests and send report to Slack (one command)

**Windows (CMD)** — from this project folder:

```cmd
cd "c:\Users\vivek.rana\Downloads\Share-functionalty 4\Share-functionalty 2"
run
```

`run` uses `run.cmd`. It loads `.env` and runs all SMS Share tests. Slack is sent if `SLACK_WEBHOOK_URL` is set.

Optional:

```cmd
run -Dexec.args=nebraska
npm run run-script
```

`npm start` starts the daily scheduler only. Use `run` for an immediate test.

**macOS / Linux:**

```bash
./run.sh
```

This runs all tests and sends the summary to Slack if `SLACK_WEBHOOK_URL` is set in `.env`.

### 3. Schedule Daily Runs

**Option A: Node.js Scheduler (Recommended - Cross-platform)**
```bash
# Install Node.js dependencies
npm install

# Run the scheduler (runs daily at 4:00 PM)
npm start

# Or run in background
nohup npm start > scheduler.log 2>&1 &
```

The scheduler will:
- Run tests daily at 4:00 PM automatically
- Log all test runs to `logs/` directory
- Continue running until stopped (Ctrl+C)

**Configuration via Environment Variables:**
```bash
# Custom schedule (default: 0 16 * * * = 4:00 PM daily)
export CRON_SCHEDULE="0 16 * * *"

# Custom Slack webhook
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/YOUR/WEBHOOK/URL"

# Custom Slack channel and username
export SLACK_CHANNEL="#test-results"
export SLACK_USERNAME="Test Bot"

# Timezone (default: system timezone)
export TZ="America/New_York"

npm start
```

**Option B: Launchd (macOS)**
```bash
# Create Launchd plist
cat > ~/Library/LaunchAgents/com.sms.share.automation.plist <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.sms.share.automation</string>
    <key>ProgramArguments</key>
    <array>
        <string>/Users/Hardik-Patel/Desktop/SCRIPT/Share-functionalty/run_daily_automated.sh</string>
    </array>
    <key>StartCalendarInterval</key>
    <dict>
        <key>Hour</key>
        <integer>17</integer>
        <key>Minute</key>
        <integer>50</integer>
    </dict>
    <key>StandardOutPath</key>
    <string>/Users/Hardik-Patel/Desktop/SCRIPT/Share-functionalty/logs/launchd_stdout.log</string>
    <key>StandardErrorPath</key>
    <string>/Users/Hardik-Patel/Desktop/SCRIPT/Share-functionalty/logs/launchd_stderr.log</string>
</dict>
</plist>
EOF

# Load it
launchctl load ~/Library/LaunchAgents/com.sms.share.automation.plist
```

**Option C: Cron**
```bash
crontab -e
# Add:
50 17 * * * /bin/bash -c 'cd /Users/Hardik-Patel/Desktop/SCRIPT/Share-functionalty && ./run_daily_automated.sh' >> /Users/Hardik-Patel/Desktop/SCRIPT/Share-functionalty/logs/cron_stdout.log 2>> /Users/Hardik-Patel/Desktop/SCRIPT/Share-functionalty/logs/cron_stderr.log
```

## Files

- `scheduler.js` - Node.js cron scheduler (runs daily at 4:00 PM)
- `package.json` - Node.js dependencies and scripts
- `run.cmd` - **Windows one command**: `run` — all tests + Slack (uses .env)
- `run.sh` - **macOS/Linux one command**: run all tests + send report to Slack (uses .env)
- `run_tests.sh` - Alternative test runner (Slack only, uses java directly)
- `run_daily_automated.sh` - Wrapper for scheduled runs (with logging)
- `src/test/java/com/selenium/test/SMSShareTest.java` - Test code
- `src/main/java/com/selenium/utils/SlackService.java` - Slack integration

## Logs

All logs are saved to `logs/` directory:
- `test_YYYYMMDD_HHMMSS.log` - Individual test runs
- `daily_summary.log` - Summary of all runs
- `cron_stdout.log` / `cron_stderr.log` - Cron output
- `launchd_stdout.log` / `launchd_stderr.log` - Launchd output

## Requirements

- Java 11+
- Maven
- Node.js 18+ (for scheduler)
- Chrome browser
- Slack webhook URL

