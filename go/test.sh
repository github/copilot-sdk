#!/bin/bash
# Test script for Go SDK (when Go is available)

set -e

echo "=== Testing Go SDK ==="
echo

# Check prerequisites
if ! command -v go &> /dev/null; then
    echo "❌ Go is not installed. Please install Go 1.24 or later."
    echo "   Visit: https://golang.org/dl/"
    exit 1
fi

# Determine COPILOT_CLI_PATH
if [ -z "$COPILOT_CLI_PATH" ]; then
    # Try to find it relative to the SDK. As of CLI 1.0.64-1 the @github/copilot
    # package is a thin loader; the runnable index.js ships in the installed
    # platform package (e.g. @github/copilot-linux-x64).
    SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
    case "$(go env GOHOSTARCH)" in
        amd64) NPM_ARCH="x64" ;;
        arm64) NPM_ARCH="arm64" ;;
        *)
            echo "❌ Unsupported Go architecture: $(go env GOHOSTARCH)"
            exit 1
            ;;
    esac

    NPM_PLATFORMS=()
    case "$(go env GOHOSTOS)" in
        darwin) NPM_PLATFORMS=("darwin") ;;
        windows) NPM_PLATFORMS=("win32") ;;
        linux)
            if ldd --version 2>&1 | grep -qi musl; then
                NPM_PLATFORMS=("linuxmusl" "linux")
            else
                NPM_PLATFORMS=("linux" "linuxmusl")
            fi
            ;;
        *)
            echo "❌ Unsupported Go platform: $(go env GOHOSTOS)"
            exit 1
            ;;
    esac

    POTENTIAL_PATH=""
    TRIED_PACKAGES=()
    for NPM_PLATFORM in "${NPM_PLATFORMS[@]}"; do
        PACKAGE_NAME="copilot-${NPM_PLATFORM}-${NPM_ARCH}"
        TRIED_PACKAGES+=("@github/${PACKAGE_NAME}")
        CANDIDATE="$SCRIPT_DIR/../nodejs/node_modules/@github/${PACKAGE_NAME}/index.js"
        if [ -f "$CANDIDATE" ]; then
            POTENTIAL_PATH="$CANDIDATE"
            break
        fi
    done

    if [ -n "$POTENTIAL_PATH" ] && [ -f "$POTENTIAL_PATH" ]; then
        export COPILOT_CLI_PATH="$POTENTIAL_PATH"
        echo "📍 Auto-detected CLI path: $COPILOT_CLI_PATH"
    else
        echo "❌ COPILOT_CLI_PATH environment variable not set"
        echo "   Tried platform packages: ${TRIED_PACKAGES[*]}"
        echo "   Run: export COPILOT_CLI_PATH=/path/to/dist-cli/index.js"
        exit 1
    fi
fi

if [ ! -f "$COPILOT_CLI_PATH" ]; then
    echo "❌ CLI not found at: $COPILOT_CLI_PATH"
    exit 1
fi

echo "✅ Go version: $(go version)"
echo "✅ CLI path: $COPILOT_CLI_PATH"
echo

# Run Go tests
cd "$(dirname "$0")"

echo "=== Running Go SDK E2E Tests ==="
echo

go test -v ./... -race -timeout=20m

echo
echo "✅ All tests passed!"
