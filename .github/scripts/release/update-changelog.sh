#!/bin/bash
set -e

# Script to update CHANGELOG.md during release process
# Usage: ./update-changelog.sh <version> [reference-impl-hash]
# Example: ./update-changelog.sh 1.0.8
# Example: ./update-changelog.sh 1.0.8 05e3c46c8c23130c9c064dc43d00ec78f7a75eab

if [ -z "$1" ]; then
    echo "Error: Version argument required"
    echo "Usage: $0 <version> [reference-impl-hash]"
    exit 1
fi

VERSION="$1"
REFERENCE_IMPL_HASH="${2:-}"
CHANGELOG_FILE="${CHANGELOG_FILE:-CHANGELOG.md}"
RELEASE_DATE=$(date +%Y-%m-%d)

echo "Updating CHANGELOG.md for version ${VERSION} (${RELEASE_DATE})"
if [ -n "$REFERENCE_IMPL_HASH" ]; then
    echo "  Reference implementation SDK sync: ${REFERENCE_IMPL_HASH:0:7}"
fi

# Check if CHANGELOG.md exists
if [ ! -f "$CHANGELOG_FILE" ]; then
    echo "Error: CHANGELOG.md not found"
    exit 1
fi

# Check if there's an [Unreleased] section
if ! grep -q "## \[Unreleased\]" "$CHANGELOG_FILE"; then
    echo "Error: No [Unreleased] section found in CHANGELOG.md"
    exit 1
fi

# Create a temporary file
TEMP_FILE=$(mktemp)

# Process the CHANGELOG
awk -v version="$VERSION" -v date="$RELEASE_DATE" -v REFERENCE_IMPL_HASH="$REFERENCE_IMPL_HASH" '
BEGIN {
    unreleased_found = 0
    content_found = 0
    links_section = 0
    first_version_link = ""
    repo_url = ""
    unreleased_link_handled = 0
}

# Track if we are in the links section at the bottom
/^\[/ {
    links_section = 1
}

# Capture the repository URL from the first version link
links_section && repo_url == "" && /^\[[0-9]+\.[0-9]+\.[0-9]+(-(beta-)?java(-preview)?\.[0-9]+)?\]:/ {
    match($0, /(https:\/\/github\.com\/[^\/]+\/[^\/]+)\//, arr)
    if (arr[1] != "") {
        repo_url = arr[1]
    }
}

# Replace [Unreleased] with the version and date
/^## \[Unreleased\]/ {
    if (!unreleased_found) {
        print "## [Unreleased]"
        print ""
        if (REFERENCE_IMPL_HASH != "") {
            short_hash = substr(REFERENCE_IMPL_HASH, 1, 7)
            print "> **Reference implementation sync:** [`github/copilot-sdk@" short_hash "`](https://github.com/github/copilot-sdk/commit/" REFERENCE_IMPL_HASH ")"
            print ""
        }
        print "## [" version "] - " date
        if (REFERENCE_IMPL_HASH != "") {
            print ""
            print "> **Reference implementation sync:** [`github/copilot-sdk@" short_hash "`](https://github.com/github/copilot-sdk/commit/" REFERENCE_IMPL_HASH ")"
        }
        unreleased_found = 1
        skip_old_reference_impl = 1
        next
    }
}

# Skip the old Reference implementation sync line and surrounding blank lines from the previous [Unreleased] section
skip_old_reference_impl && /^[[:space:]]*$/ { next }
skip_old_reference_impl && /^> \*\*Reference implementation sync:\*\*/ { next }
skip_old_reference_impl && !/^[[:space:]]*$/ && !/^> \*\*Reference implementation sync:\*\*/ { skip_old_reference_impl = 0 }

# Update existing [Unreleased] link if present (must be checked before the first-version-link block)
links_section && /^\[Unreleased\]:/ {
    # Get the previous version and repo URL from the existing link
    match($0, /(https:\/\/github\.com\/[^\/]+\/[^\/]+)\/compare\/v([0-9]+\.[0-9]+\.[0-9]+(-(beta-)?java(-preview)?\.[0-9]+)?)\.\.\.HEAD/, arr)
    if (arr[1] != "" && arr[2] != "") {
        print "[Unreleased]: " arr[1] "/compare/v" version "...HEAD"
        print "[" version "]: " arr[1] "/compare/v" arr[2] "...v" version
        unreleased_link_handled = 1
        next
    }
}

# Capture the first version link to get the previous version
# Only fires if the [Unreleased] link was not already handled above
links_section && first_version_link == "" && !unreleased_link_handled && /^\[[0-9]+\.[0-9]+\.[0-9]+(-(beta-)?java(-preview)?\.[0-9]+)?\]:/ {
    match($0, /\[([0-9]+\.[0-9]+\.[0-9]+(-(beta-)?java(-preview)?\.[0-9]+)?)\]:/, arr)
    if (arr[1] != "" && repo_url != "") {
        first_version_link = arr[1]
        # Insert Unreleased and new version links before first version link
        print "[Unreleased]: " repo_url "/compare/v" version "...HEAD"
        print "[" version "]: " repo_url "/compare/v" arr[1] "...v" version
    }
}

# Print all other lines unchanged
{ print }
' "$CHANGELOG_FILE" > "$TEMP_FILE"

# Replace the original file
mv "$TEMP_FILE" "$CHANGELOG_FILE"

echo "✓ CHANGELOG.md updated successfully"
echo "  - Added version ${VERSION} with date ${RELEASE_DATE}"
echo "  - Created new [Unreleased] section"
echo "  - Updated version comparison links"

