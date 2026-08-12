#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
UPDATER="${SCRIPT_DIR}/update-documentation-versions.sh"
TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT

run_case() {
    local name=$1
    local old_version=$2
    local old_dev_version=$3
    local version=$4
    local dev_version=$5
    local old_jbang_version=$6
    local case_dir="${TEMP_DIR}/${name}"

    mkdir "$case_dir"
    printf '%s\n' \
        '<dependency>' \
        '    <artifactId>copilot-sdk-java</artifactId>' \
        "    <version>${old_version}</version>" \
        '</dependency>' \
        "implementation 'com.github:copilot-sdk-java:${old_version}'" \
        '<dependency>' \
        '    <artifactId>copilot-sdk-java</artifactId>' \
        "    <version>${old_dev_version}</version>" \
        '</dependency>' \
        "implementation 'com.github:copilot-sdk-java:${old_dev_version}'" \
        '<dependency>' \
        '    <artifactId>jna</artifactId>' \
        '    <version>5.19.1</version>' \
        '</dependency>' \
        > "${case_dir}/README.md"
    printf '%s\n' \
        "///usr/bin/env jbang \"\$0\" \"\$@\" ; exit \$?" \
        "//DEPS com.github:copilot-sdk-java:${old_jbang_version}" \
        > "${case_dir}/jbang-example.java"

    "$UPDATER" "$version" "$dev_version" "${case_dir}/README.md" "${case_dir}/jbang-example.java"

    grep -Fqx "    <version>${version}</version>" "${case_dir}/README.md"
    grep -Fqx "implementation 'com.github:copilot-sdk-java:${version}'" "${case_dir}/README.md"
    grep -Fqx "    <version>${dev_version}</version>" "${case_dir}/README.md"
    grep -Fqx "implementation 'com.github:copilot-sdk-java:${dev_version}'" "${case_dir}/README.md"
    grep -Fqx '    <version>5.19.1</version>' "${case_dir}/README.md"
    grep -Fqx "//DEPS com.github:copilot-sdk-java:${version}" "${case_dir}/jbang-example.java"

    if grep -Fq "$old_version" "${case_dir}/README.md" "${case_dir}/jbang-example.java" ||
        grep -Fq "$old_dev_version" "${case_dir}/README.md"; then
        echo "Stale version remained in ${name} test output" >&2
        exit 1
    fi
}

run_case stable 1.0.8 1.0.9-SNAPSHOT 1.0.9 1.0.10-SNAPSHOT "\${project.version}"
run_case preview 1.0.9-preview.2-01 1.0.10-preview.2-SNAPSHOT 1.0.10-preview.2 1.0.11-preview.2-SNAPSHOT 1.0.9-preview.2-01
