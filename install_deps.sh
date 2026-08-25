#!/bin/bash
# Install Java 11+ and Maven for SMS Share Automation
# Run in Terminal: ./install_deps.sh
# You may be prompted for your Mac password (sudo).

set -e

echo "=========================================="
echo "Installing Java and Maven for SMS Share Tests"
echo "=========================================="
echo ""

# 1. Install Homebrew if not found
if ! command -v brew &>/dev/null; then
    echo "Installing Homebrew (you may be asked for your Mac password)..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    # Add Homebrew to PATH for this session
    if [ -f /opt/homebrew/bin/brew ]; then
        eval "$(/opt/homebrew/bin/brew shellenv)"
    elif [ -f /usr/local/bin/brew ]; then
        eval "$(/usr/local/bin/brew shellenv)"
    fi
    echo ""
else
    echo "Homebrew already installed."
    if [ -f /opt/homebrew/bin/brew ]; then
        eval "$(/opt/homebrew/bin/brew shellenv)"
    elif [ -f /usr/local/bin/brew ]; then
        eval "$(/usr/local/bin/brew shellenv)"
    fi
fi

# 2. Install OpenJDK 11
echo ""
echo "Installing Java (OpenJDK 11)..."
brew install openjdk@11
echo ""

# 3. Install Maven
echo "Installing Maven..."
brew install maven
echo ""

# 4. Add Java to PATH for current session and show instructions
echo "=========================================="
echo "Installation complete!"
echo "=========================================="
echo ""
echo "Add Java to your PATH (add to ~/.zshrc to make permanent):"
echo ""
if [ -d /opt/homebrew/opt/openjdk@11 ]; then
    echo '  export PATH="/opt/homebrew/opt/openjdk@11/bin:$PATH"'
elif [ -d /usr/local/opt/openjdk@11 ]; then
    echo '  export PATH="/usr/local/opt/openjdk@11/bin:$PATH"'
fi
echo ""
echo "Then run your test:"
echo "  cd $(dirname "$0")"
echo "  ./run_tests.sh connecticut"
echo ""
