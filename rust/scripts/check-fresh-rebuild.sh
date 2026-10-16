#!/usr/bin/env bash
#
# Fail when an unchanged rebuild reruns build.rs or recompiles the crate.
#
# Cargo checks `rerun-if-changed` paths only for local packages, such as a
# source checkout, a path dependency, or a vendored copy, and treats a missing
# path as always stale. Declaring an absent file therefore reruns build.rs and
# recompiles the crate and its dependents on every build, while registry and
# git consumers are unaffected. Checking this crate from its own directory
# exercises the same local-package behavior as a path dependency.
#
# Usage: scripts/check-fresh-rebuild.sh [cargo check options]
# Example: scripts/check-fresh-rebuild.sh --no-default-features --features test-support,derive

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}/.."

cargo check "$@"
if ! output="$(cargo check --verbose --color never "$@" 2>&1)"; then
  printf '%s\n' "${output}" >&2
  exit 1
fi
if ! grep -Eq '^ *Fresh github-copilot-sdk v' <<<"${output}"; then
  grep -E '^ *[A-Z][a-z]+ github-copilot-sdk v' <<<"${output}" >&2 || printf '%s\n' "${output}" >&2
  echo "error: an unchanged rebuild did not keep github-copilot-sdk fresh" >&2
  exit 1
fi
echo "An unchanged rebuild kept github-copilot-sdk fresh."
