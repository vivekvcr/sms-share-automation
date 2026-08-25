#!/bin/bash

# Simple test runner with Slack notifications only

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Load .env if present (keeps webhook in one place; .env is gitignored)
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

# Slack Configuration (use .env or set here)
SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL:-}"
SLACK_CHANNEL="${SLACK_CHANNEL:-}"
SLACK_USERNAME="${SLACK_USERNAME:-SMS Share Test Bot}"

echo "=========================================="
echo "Running SMS Share Tests"
echo "=========================================="
echo "Slack: Enabled"
if [ ! -z "$SLACK_CHANNEL" ]; then
    echo "Slack Channel: $SLACK_CHANNEL"
fi
echo "=========================================="
echo ""

# Compile and build classpath
mvn clean compile test-compile -q
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q

# Run tests with Slack notification
CLASSPATH=$(cat cp.txt):target/classes:target/test-classes

JAVA_CMD="java -Dheadless=true \
     -Dslack.webhook.url=\"$SLACK_WEBHOOK_URL\""

if [ ! -z "$SLACK_CHANNEL" ]; then
    JAVA_CMD="$JAVA_CMD -Dslack.channel=\"$SLACK_CHANNEL\""
fi

if [ ! -z "$SLACK_USERNAME" ]; then
    JAVA_CMD="$JAVA_CMD -Dslack.username=\"$SLACK_USERNAME\""
fi

# Optional: pass one site name or URL to test only that one (e.g. connecticut, oklahoma, livewell)
JAVA_CMD="$JAVA_CMD -cp \"$CLASSPATH\" com.selenium.test.SMSShareTest"
if [ -n "$1" ]; then
    JAVA_CMD="$JAVA_CMD $1"
    echo "Single-site test: $1"
    echo ""
fi

# Execute the command
eval $JAVA_CMD

echo ""
echo "=========================================="
echo "Test execution completed!"
echo "Check your Slack channel for notifications"
echo "=========================================="
