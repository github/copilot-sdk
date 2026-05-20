#!/bin/bash
# Test script for update-changelog.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UPDATE_SCRIPT="${SCRIPT_DIR}/update-changelog.sh"
TEST_DIR="/tmp/changelog-test-$$"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

passed=0
failed=0

# Setup test directory
mkdir -p "$TEST_DIR"

# Cleanup on exit
cleanup() {
    rm -rf "$TEST_DIR"
}
trap cleanup EXIT

# Helper function to run a test
run_test() {
    local test_name="$1"
    local test_func="$2"
    
    echo -n "Testing: $test_name ... "
    
    if $test_func; then
        echo -e "${GREEN}PASSED${NC}"
        passed=$((passed + 1))
    else
        echo -e "${RED}FAILED${NC}"
        failed=$((failed + 1))
    fi
}

# Test 1: Basic functionality - Replace Unreleased with version
test_basic_replace() {
    local test_file="${TEST_DIR}/test1.md"
    cat > "$test_file" << 'EOF'
# Changelog

## [Unreleased]

### Added
- New feature

## [1.0.0] - 2026-01-01

### Added
- Initial release

[1.0.0]: https://github.com/test/repo/releases/tag/1.0.0
EOF

    # Run the script
    CHANGELOG_FILE="$test_file" bash "$UPDATE_SCRIPT" 1.0.1 > /dev/null 2>&1
    
    # Verify the changes
    if grep -q "## \[Unreleased\]" "$test_file" && \
       grep -q "## \[1.0.1\] - $(date +%Y-%m-%d)" "$test_file" && \
       grep -q "\[Unreleased\]: https://github.com/test/repo/compare/v1.0.1...HEAD" "$test_file" && \
       grep -q "\[1.0.1\]: https://github.com/test/repo/compare/v1.0.0...v1.0.1" "$test_file"; then
        return 0
    else
        return 1
    fi
}

# Test 2: Handle CHANGELOG without Unreleased link
test_no_unreleased_link() {
    local test_file="${TEST_DIR}/test2.md"
    cat > "$test_file" << 'EOF'
# Changelog

## [Unreleased]

### Added
- New feature

## [1.0.0] - 2026-01-01

[1.0.0]: https://github.com/test/repo/releases/tag/1.0.0
EOF

    CHANGELOG_FILE="$test_file" bash "$UPDATE_SCRIPT" 1.0.1 > /dev/null 2>&1
    
    # Should add both Unreleased and version links
    if grep -q "\[Unreleased\]: https://github.com/test/repo/compare/v1.0.1...HEAD" "$test_file" && \
       grep -q "\[1.0.1\]: https://github.com/test/repo/compare/v1.0.0...v1.0.1" "$test_file"; then
        return 0
    else
        return 1
    fi
}

# Test 3: Preserve content structure
test_preserve_content() {
    local test_file="${TEST_DIR}/test3.md"
    cat > "$test_file" << 'EOF'
# Changelog

## [Unreleased]

### Added
- Feature A
- Feature B

### Fixed
- Bug fix

## [1.0.0] - 2026-01-01

[1.0.0]: https://github.com/test/repo/releases/tag/1.0.0
EOF

    CHANGELOG_FILE="$test_file" bash "$UPDATE_SCRIPT" 1.0.1 > /dev/null 2>&1
    
    # Verify content is preserved under the new version
    if grep -A 6 "## \[1.0.1\]" "$test_file" | grep -q "Feature A" && \
       grep -A 6 "## \[1.0.1\]" "$test_file" | grep -q "Bug fix"; then
        return 0
    else
        return 1
    fi
}

# Test 4: Error handling - no Unreleased section
test_no_unreleased_section() {
    local test_file="${TEST_DIR}/test4.md"
    cat > "$test_file" << 'EOF'
# Changelog

## [1.0.0] - 2026-01-01

[1.0.0]: https://github.com/test/repo/releases/tag/1.0.0
EOF

    # Should fail because there's no Unreleased section
    if ! CHANGELOG_FILE="$test_file" bash "$UPDATE_SCRIPT" 1.0.1 > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Test 5: Multiple version handling
test_multiple_versions() {
    local test_file="${TEST_DIR}/test5.md"
    cat > "$test_file" << 'EOF'
# Changelog

## [Unreleased]

### Added
- New feature

## [1.0.1] - 2026-02-01

## [1.0.0] - 2026-01-01

[1.0.1]: https://github.com/test/repo/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/test/repo/releases/tag/1.0.0
EOF

    CHANGELOG_FILE="$test_file" bash "$UPDATE_SCRIPT" 1.0.2 > /dev/null 2>&1
    
    # Verify the new version is added and links are updated
    if grep -q "## \[1.0.2\] - $(date +%Y-%m-%d)" "$test_file" && \
       grep -q "\[1.0.2\]: https://github.com/test/repo/compare/v1.0.1...v1.0.2" "$test_file"; then
        return 0
    else
        return 1
    fi
}

# Test 6: Beta-java version format (e.g., 1.0.0-beta-java.N)
test_beta_java_version() {
    local test_file="${TEST_DIR}/test6.md"
    cat > "$test_file" << 'EOF'
# Changelog

## [Unreleased]

### Added
- New feature

## [1.0.0-beta-java.1] - 2026-05-01

[Unreleased]: https://github.com/test/repo/compare/v1.0.0-beta-java.1...HEAD
[1.0.0-beta-java.1]: https://github.com/test/repo/compare/v0.3.0-java.2...v1.0.0-beta-java.1
[0.3.0-java.2]: https://github.com/test/repo/releases/tag/0.3.0-java.2
EOF

    CHANGELOG_FILE="$test_file" bash "$UPDATE_SCRIPT" 1.0.0-beta-java.2 > /dev/null 2>&1

    # The [Unreleased] link should now point to v1.0.0-beta-java.2
    # [1.0.0-beta-java.2] should compare from v1.0.0-beta-java.1
    if grep -q "\[Unreleased\]: https://github.com/test/repo/compare/v1.0.0-beta-java.2...HEAD" "$test_file" && \
       grep -q "\[1.0.0-beta-java.2\]: https://github.com/test/repo/compare/v1.0.0-beta-java.1...v1.0.0-beta-java.2" "$test_file"; then
        return 0
    else
        return 1
    fi
}

# Test 7: No duplicate [Unreleased] links when existing [Unreleased] link is present
test_no_duplicate_unreleased_links() {
    local test_file="${TEST_DIR}/test7.md"
    cat > "$test_file" << 'EOF'
# Changelog

## [Unreleased]

### Added
- New feature

## [1.0.0-beta-java.2] - 2026-05-08

## [1.0.0-beta-java.1] - 2026-05-05

[Unreleased]: https://github.com/test/repo/compare/v1.0.0-beta-java.2...HEAD
[1.0.0-beta-java.2]: https://github.com/test/repo/compare/v1.0.0-beta-java.1...v1.0.0-beta-java.2
[1.0.0-beta-java.1]: https://github.com/test/repo/compare/v0.3.0-java.2...v1.0.0-beta-java.1
[0.3.0-java.2]: https://github.com/test/repo/releases/tag/0.3.0-java.2
EOF

    CHANGELOG_FILE="$test_file" bash "$UPDATE_SCRIPT" 1.0.0-beta-java.3 > /dev/null 2>&1

    # Count [Unreleased] link definitions - there should be exactly one
    local unreleased_count
    unreleased_count=$(grep -c "^\[Unreleased\]:" "$test_file")
    if [ "$unreleased_count" -eq 1 ] && \
       grep -q "\[Unreleased\]: https://github.com/test/repo/compare/v1.0.0-beta-java.3...HEAD" "$test_file" && \
       grep -q "\[1.0.0-beta-java.3\]: https://github.com/test/repo/compare/v1.0.0-beta-java.2...v1.0.0-beta-java.3" "$test_file"; then
        return 0
    else
        return 1
    fi
}

# Run all tests
echo "Running CHANGELOG update script tests..."
echo ""

run_test "Basic functionality - Replace Unreleased with version" test_basic_replace
run_test "Handle CHANGELOG without Unreleased link" test_no_unreleased_link
run_test "Preserve content structure" test_preserve_content
run_test "Error handling - no Unreleased section" test_no_unreleased_section
run_test "Multiple version handling" test_multiple_versions
run_test "Beta-java version format (e.g., 1.0.0-beta-java.N)" test_beta_java_version
run_test "No duplicate [Unreleased] links when existing link is present" test_no_duplicate_unreleased_links

echo ""
echo "=========================================="
echo -e "Tests passed: ${GREEN}${passed}${NC}"
echo -e "Tests failed: ${RED}${failed}${NC}"
echo "=========================================="

if [ $failed -eq 0 ]; then
    exit 0
else
    exit 1
fi
