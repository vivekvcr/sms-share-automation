#!/bin/bash
#
# One command: run all SMS Share tests and send report to Slack.
# Uses .env for Slack webhook (SLACK_WEBHOOK_URL, SLACK_CHANNEL, SLACK_USERNAME).
#

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Load .env so Slack config is in environment (Java reads SLACK_WEBHOOK_URL etc.)
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

# Export for Maven/Java (inherited by exec:java)
export SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL:-}"
export SLACK_CHANNEL="${SLACK_CHANNEL:-}"
export SLACK_USERNAME="${SLACK_USERNAME:-SMS Share Test Bot}"

echo "=========================================="
echo "  SMS Share Tests + Slack Report"
echo "=========================================="
if [ -n "$SLACK_WEBHOOK_URL" ]; then
    echo "  Slack: enabled (report will be sent after run)"
else
    echo "  Slack: disabled (set SLACK_WEBHOOK_URL in .env to enable)"
fi
echo "=========================================="
echo ""

# Single command: compile and run; Java picks up Slack from env
mvn -q test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass="com.selenium.test.SMSShareTest"

echo ""
echo "Done. Check Slack for the report (if enabled)."
