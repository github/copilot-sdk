#!/usr/bin/env bash
# Exercise release bundling without the prepared CI runtime overriding it.
set -euo pipefail

case "${1:-default}" in
  default) features=bundled-runtime,test-support ;;
  inprocess) features=bundled-runtime,in-process,test-support ;;
  *) echo "Expected transport: default or inprocess" >&2; exit 1 ;;
esac

# Unset rather than empty: build.rs opts out when these variables are present.
unset COPILOT_SKIP_CLI_DOWNLOAD DOCS_RS
unset COPILOT_CLI_PATH COPILOT_LEGACY_CLI_PATH COPILOT_CLI_EXTRACT_DIR
unset COPILOT_RUNTIME_BINARY_PATH COPILOT_RUNTIME_LIBRARY_PATH
unset COPILOT_RUNTIME_HOST_COMMAND COPILOT_RUNTIME_PROVIDER_LIB
unset COPILOT_EXTENSION_SDK_PATH COPILOT_SDK_DEFAULT_CONNECTION

cargo test --locked --no-default-features --features "$features" \
  --test cli_resolution_test -- --test-threads=1 --nocapture
