#!/usr/bin/env bash
# Copyright (c) Microsoft Corporation. All rights reserved.
# Runs .NET SDK tests with optional backend filtering.

set -euo pipefail

usage() {
    cat <<'EOF'
Usage: run-dotnet-tests.sh [--help]

Runs the full .NET SDK test project. Environment variables:
  DOTNET_TEST_FILTER   Optional dotnet test filter (e.g. for a backend or transport).
  DOTNET_TEST_RUNTIME  Optional runtime identifier passed to dotnet test.
EOF
}

if (($# == 1)) && [[ "$1" == "--help" ]]; then
    usage
    exit 0
fi
if (($# != 0)); then
    usage >&2
    exit 2
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir/../../dotnet"

args=(
    test/GitHub.Copilot.SDK.Test.csproj
    --no-build
    -v n
    --blame-hang
    --blame-hang-timeout 10m
    --blame-hang-dump-type none
    --logger "trx;LogFilePrefix=test-results"
    --results-directory TestResults
    -p:RunAnalyzers=false
)
filter="${DOTNET_TEST_FILTER:-}"
runtime="${DOTNET_TEST_RUNTIME:-}"

if [[ -n "$filter" ]]; then
    args+=(--filter "$filter")
fi
if [[ -n "$runtime" ]]; then
    args+=(--runtime "$runtime")
fi

dotnet test "${args[@]}"
