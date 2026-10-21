#!/usr/bin/env bash
# --------------------------------------------------------------------------------------------
#  Copyright (c) Microsoft Corporation. All rights reserved.
# --------------------------------------------------------------------------------------------

# GitHub's Ubuntu runner source points at a mirror list whose Azure entry can
# stall even after apt falls back to archive.ubuntu.com for other requests.
# Build temporary source files so only this process uses the canonical archive;
# do not rewrite the runner's sources or alter local development machines.
APT_SOURCE_OPTIONS=()
APT_SOURCE_TEMP_DIR=""

cleanup_github_actions_apt_sources() {
    if [[ -n "$APT_SOURCE_TEMP_DIR" ]]; then
        rm -rf -- "$APT_SOURCE_TEMP_DIR"
    fi
}

rewrite_github_actions_apt_source() {
    local source_file="$1"
    local destination_file="$2"
    local mirror_list="$3"
    local line

    while IFS= read -r line || [[ -n "$line" ]]; do
        line="${line//http:\/\/azure.archive.ubuntu.com\/ubuntu/https:\/\/archive.ubuntu.com\/ubuntu}"
        line="${line//https:\/\/azure.archive.ubuntu.com\/ubuntu/https:\/\/archive.ubuntu.com\/ubuntu}"
        line="${line//mirror+file:$mirror_list/https:\/\/archive.ubuntu.com\/ubuntu}"
        printf '%s\n' "$line"
    done <"$source_file" >"$destination_file"
}

configure_github_actions_apt_sources() {
    local apt_etc_dir="${1:-/etc/apt}"
    local mirror_list="$apt_etc_dir/apt-mirrors.txt"
    local source_file
    local source_files=()
    local uses_azure_mirror=0

    APT_SOURCE_OPTIONS=()
    APT_SOURCE_TEMP_DIR=""

    if [[ "${GITHUB_ACTIONS:-}" != "true" ]]; then
        return
    fi

    if [[ -f "$apt_etc_dir/sources.list" ]]; then
        source_files+=("$apt_etc_dir/sources.list")
    fi

    shopt -s nullglob
    source_files+=("$apt_etc_dir/sources.list.d/"*.list)
    source_files+=("$apt_etc_dir/sources.list.d/"*.sources)
    shopt -u nullglob

    if [[ -f "$mirror_list" ]] && grep -q 'azure\.archive\.ubuntu\.com' "$mirror_list"; then
        uses_azure_mirror=1
    else
        for source_file in "${source_files[@]}"; do
            if grep -q 'azure\.archive\.ubuntu\.com' "$source_file"; then
                uses_azure_mirror=1
                break
            fi
        done
    fi

    if [[ "$uses_azure_mirror" -eq 0 || "${#source_files[@]}" -eq 0 ]]; then
        return
    fi

    APT_SOURCE_TEMP_DIR="$(mktemp -d)"
    mkdir "$APT_SOURCE_TEMP_DIR/sources.list.d"
    : >"$APT_SOURCE_TEMP_DIR/sources.list"

    for source_file in "${source_files[@]}"; do
        if [[ "$source_file" == "$apt_etc_dir/sources.list" ]]; then
            rewrite_github_actions_apt_source \
                "$source_file" "$APT_SOURCE_TEMP_DIR/sources.list" "$mirror_list"
        else
            rewrite_github_actions_apt_source \
                "$source_file" "$APT_SOURCE_TEMP_DIR/sources.list.d/${source_file##*/}" "$mirror_list"
        fi
    done

    if grep -ERq 'azure\.archive\.ubuntu\.com' "$APT_SOURCE_TEMP_DIR" \
        || grep -FRq "mirror+file:$mirror_list" "$APT_SOURCE_TEMP_DIR"; then
        echo "::error::Failed to replace the GitHub runner's Azure APT mirror in temporary sources."
        return 1
    fi

    APT_SOURCE_OPTIONS=(
        -o "Dir::Etc::sourcelist=$APT_SOURCE_TEMP_DIR/sources.list"
        -o "Dir::Etc::sourceparts=$APT_SOURCE_TEMP_DIR/sources.list.d"
    )

    echo "Using https://archive.ubuntu.com instead of the GitHub runner's Azure APT mirror."
}
