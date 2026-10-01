#!/usr/bin/env bash
# Copyright (c) Microsoft Corporation. All rights reserved.
#
# Sourced by the two snapshot entry points.

usage() {
  cat <<EOF
Usage: $(basename "${BASH_SOURCE[1]}") [--version VERSION] [--release-url URL] [--checksums FILE] [--output FILE]

Write this script's version/hash snapshot (by default in its Rust crate root).
  --version VERSION   Runtime version (default: ../nodejs/package.json copilotCliVersion).
  --release-url URL   Exact public release base URL (no trailing slash):
                      https://github.com/github/copilot-cli/releases/download/v<VERSION>
                      https://github.com/github/copilot-sdk/releases/download/runtime-<VERSION>
                      Default: the copilot-cli URL.
  --checksums FILE    Read local SHA256SUMS.txt instead of downloading it.
  --output FILE       Write to a selected-source staging file instead of this script's crate.
  --help              Show this help.

No environment variables or credentials are required. Local checksums let
packaging run before the public release exists; the snapshot still pins its
final public URL. Both snapshot scripts must use the same version and URL.
EOF
}

VERSION=""
RELEASE_URL=""
CHECKSUMS_FILE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --help|-h) usage; exit 0 ;;
    --version|--release-url|--checksums|--output)
      if [[ $# -lt 2 || -z "$2" || "$2" == --* ]]; then
        echo "error: $1 requires a value" >&2
        exit 1
      fi
      case "$1" in
        --version) VERSION="$2" ;;
        --release-url) RELEASE_URL="$2" ;;
        --checksums) CHECKSUMS_FILE="$2" ;;
        --output) OUTPUT="$2" ;;
      esac
      shift 2
      ;;
    *) echo "error: unknown argument: $1" >&2; usage >&2; exit 1 ;;
  esac
done

if [[ -z "${VERSION}" ]]; then
  PACKAGE_FILE="${REPO_ROOT}/nodejs/package.json"
  if [[ ! -f "${PACKAGE_FILE}" ]]; then
    echo "error: ${PACKAGE_FILE} not found" >&2
    exit 1
  fi
  VERSION="$(node -e 'console.log(require(process.argv[1]).copilotCliVersion ?? "")' "${PACKAGE_FILE}")"
fi
if [[ ! "${VERSION}" =~ ^[0-9][a-zA-Z0-9.+-]*$ ]]; then
  echo "error: invalid runtime version: ${VERSION}" >&2
  exit 1
fi

DEFAULT_RELEASE_URL="https://github.com/github/copilot-cli/releases/download/v${VERSION}"
RELEASE_URL="${RELEASE_URL:-${DEFAULT_RELEASE_URL}}"
if [[ "${RELEASE_URL}" != "${DEFAULT_RELEASE_URL}" &&
      "${RELEASE_URL}" != "https://github.com/github/copilot-sdk/releases/download/runtime-${VERSION}" ]]; then
  echo "error: --release-url must be the exact public copilot-cli/v<version> or copilot-sdk/runtime-<version> release URL" >&2
  exit 1
fi

if [[ -n "${CHECKSUMS_FILE}" ]]; then
  SHA256SUMS="$(cat "${CHECKSUMS_FILE}")"
else
  SHA256SUMS="$(curl --fail --silent --show-error --location --retry 3 "${RELEASE_URL}/SHA256SUMS.txt")"
fi
# Accept checksum files produced on Windows as well as Unix.
SHA256SUMS="${SHA256SUMS//$'\r'/}"

snapshot_hash() {
  local asset="$1" hash
  hash="$(printf '%s\n' "${SHA256SUMS}" | awk -v asset="${asset}" '$2 == asset || $2 == "*" asset { print $1 }')"
  if [[ ! "${hash}" =~ ^[a-fA-F0-9]{64}$ ]]; then
    echo "error: SHA256SUMS.txt must contain one valid SHA-256 for ${asset}" >&2
    return 1
  fi
  printf '%s\n' "${hash}" | tr '[:upper:]' '[:lower:]'
}
