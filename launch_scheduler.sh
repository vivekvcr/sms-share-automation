#!/bin/bash
# Wrapper invoked by launchd at 7:00 PM IST daily.
# launchd handles the schedule via StartCalendarInterval in the plist —
# this script just sets up the environment and runs the tests.
#
# NOTE: For this to work, /bin/bash must have Full Disk Access:
#   System Settings > Privacy & Security > Full Disk Access > add /bin/bash

SCRIPT_DIR="/Users/hardikpatel/Desktop/script/Share-functionalty copy"
LIB_LOG_DIR="$HOME/Library/Logs/SMSShare"
mkdir -p "$LIB_LOG_DIR"

# Append all output to a dated log in ~/Library/Logs/SMSShare (always writable by launchd)
LOG_FILE="$LIB_LOG_DIR/run_$(date +%Y%m%d_%H%M%S).log"
exec >> "$LOG_FILE" 2>&1

echo "[$(date '+%Y-%m-%d %H:%M:%S')] launchd triggered run"

# Ensure required tools are on PATH
export PATH="/opt/homebrew/bin:/opt/homebrew/opt/openjdk@11/bin:/usr/local/bin:/usr/bin:/bin:$PATH"
export HOME="/Users/hardikpatel"
export TZ="Asia/Kolkata"

# Run the existing test runner
exec bash "$SCRIPT_DIR/run_daily_automated.sh"
