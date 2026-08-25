#!/bin/bash

# Daily Automated Test Runner
# This script runs the SMS Share tests and logs the output
# Designed to be run via cron or launchd

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Create logs directory if it doesn't exist
LOG_DIR="$SCRIPT_DIR/logs"
mkdir -p "$LOG_DIR"

# Log file with date
LOG_FILE="$LOG_DIR/test_$(date +%Y%m%d_%H%M%S).log"
SUMMARY_LOG="$LOG_DIR/daily_summary.log"

# Function to log messages
log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Start logging
log_message "=========================================="
log_message "Starting Daily Automated Test Run"
log_message "=========================================="
log_message "Script Directory: $SCRIPT_DIR"
log_message "Log File: $LOG_FILE"
log_message ""

# Run the test script and capture output
log_message "Executing test suite..."
log_message ""

# Run tests and capture both stdout and stderr
if ./run_tests.sh >> "$LOG_FILE" 2>&1; then
    EXIT_CODE=0
    STATUS="SUCCESS"
    log_message ""
    log_message "=========================================="
    log_message "✅ Test execution completed successfully"
    log_message "=========================================="
else
    EXIT_CODE=$?
    STATUS="FAILED"
    log_message ""
    log_message "=========================================="
    log_message "❌ Test execution failed with exit code: $EXIT_CODE"
    log_message "=========================================="
fi

# Log summary
log_message ""
log_message "Test Run Summary:"
log_message "  Date: $(date '+%Y-%m-%d %H:%M:%S')"
log_message "  Status: $STATUS"
log_message "  Exit Code: $EXIT_CODE"
log_message "  Log File: $LOG_FILE"
log_message ""

# Append summary to daily summary log
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Status: $STATUS | Exit Code: $EXIT_CODE | Log: $LOG_FILE" >> "$SUMMARY_LOG"

# Keep only last 30 days of logs (optional cleanup)
find "$LOG_DIR" -name "test_*.log" -type f -mtime +30 -delete 2>/dev/null

# Exit with the same code as the test
exit $EXIT_CODE
