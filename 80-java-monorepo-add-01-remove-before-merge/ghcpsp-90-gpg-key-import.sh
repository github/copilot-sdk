#!/usr/bin/env bash
set -euo pipefail

# Import a GPG key handoff package created by ghcpsp-90-gpg-key-archive.sh.
#
# Usage:
#   ./ghcpsp-90-gpg-key-import.sh <sealed-archive.asc> [output-dir] [--import-ownertrust]
#
# Example:
#   ./ghcpsp-90-gpg-key-import.sh ./gpg-key-handoff-....tar.gz.asc ./out --import-ownertrust

usage() {
  cat <<'EOF'
Usage:
  ghcpsp-90-gpg-key-import.sh <sealed-archive.asc> [output-dir] [--import-ownertrust]

Arguments:
  sealed-archive.asc   Armored encrypted+signed archive produced by the sender.
  output-dir           Optional output directory for extracted files (default: ./recipient-import).
  --import-ownertrust  Optional: import ownertrust.txt from the bundle.

What this script does:
1. Decrypts and validates the signed archive with gpg.
2. Extracts bundle contents.
3. Verifies fingerprint metadata exists.
4. Imports public and secret keys.
5. Optionally imports ownertrust.

Important:
- Verify the reported fingerprint out-of-band before using the key.
- Key passphrase is required when the secret key is used, not necessarily at import.
EOF
}

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Error: required command not found: $cmd" >&2
    exit 1
  fi
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ $# -lt 1 || $# -gt 3 ]]; then
  usage >&2
  exit 1
fi

SEALED_ARCHIVE="$1"
OUTPUT_DIR="./recipient-import"
IMPORT_OWNERTRUST="false"

for arg in "${@:2}"; do
  case "$arg" in
    --import-ownertrust)
      IMPORT_OWNERTRUST="true"
      ;;
    *)
      OUTPUT_DIR="$arg"
      ;;
  esac
done

require_cmd gpg
require_cmd tar
require_cmd awk
require_cmd grep
require_cmd sed
require_cmd mkdir

if [[ ! -f "$SEALED_ARCHIVE" ]]; then
  echo "Error: archive not found: $SEALED_ARCHIVE" >&2
  exit 1
fi

umask 077
mkdir -p "$OUTPUT_DIR"

WORK_DIR="$(mktemp -d)"
cleanup() {
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

DECRYPTED_TAR="$WORK_DIR/handoff.tar.gz"
STATUS_LOG="$WORK_DIR/gpg-status.log"

# Decrypt and capture machine-readable GPG status for integrity checks.
if ! gpg --batch --status-fd=1 --decrypt --output "$DECRYPTED_TAR" "$SEALED_ARCHIVE" > "$STATUS_LOG"; then
  echo "Error: failed to decrypt/verify archive. Confirm recipient key and signer key are present." >&2
  exit 1
fi

if ! grep -q "^\[GNUPG:\] VALIDSIG " "$STATUS_LOG"; then
  echo "Error: archive decrypted but signature validity could not be confirmed." >&2
  echo "Status log: $STATUS_LOG" >&2
  exit 1
fi

TOP_DIR="$(tar -tzf "$DECRYPTED_TAR" | awk -F/ 'NR==1 {print $1}')"
if [[ -z "$TOP_DIR" ]]; then
  echo "Error: could not determine top-level bundle directory." >&2
  exit 1
fi

tar -xzf "$DECRYPTED_TAR" -C "$OUTPUT_DIR"
BUNDLE_DIR="$OUTPUT_DIR/$TOP_DIR"

SECRET_ASC="$BUNDLE_DIR/secret-key.asc"
PUBLIC_ASC="$BUNDLE_DIR/public-key.asc"
FINGERPRINT_TXT="$BUNDLE_DIR/fingerprint.txt"
OWNERTRUST_TXT="$BUNDLE_DIR/ownertrust.txt"

if [[ ! -f "$SECRET_ASC" || ! -f "$PUBLIC_ASC" || ! -f "$FINGERPRINT_TXT" ]]; then
  echo "Error: bundle is missing expected files (secret/public/fingerprint)." >&2
  exit 1
fi

EXPECTED_FPR="$(awk -F': ' '/^Primary fingerprint:/ {print $2; exit}' "$FINGERPRINT_TXT" | sed 's/[[:space:]]*$//')"
if [[ -z "$EXPECTED_FPR" ]]; then
  echo "Error: could not parse expected fingerprint from $FINGERPRINT_TXT" >&2
  exit 1
fi

echo "Expected fingerprint from bundle metadata: $EXPECTED_FPR"
echo "Verify this fingerprint out-of-band with the sender before trusting key usage."

# Import public first, then secret material.
gpg --import "$PUBLIC_ASC"
gpg --import "$SECRET_ASC"

if [[ "$IMPORT_OWNERTRUST" == "true" ]]; then
  if [[ -f "$OWNERTRUST_TXT" ]]; then
    gpg --import-ownertrust "$OWNERTRUST_TXT"
    echo "Ownertrust imported from bundle."
  else
    echo "Warning: --import-ownertrust requested, but ownertrust.txt was not found."
  fi
fi

IMPORTED_FPR="$(gpg --with-colons --list-secret-keys "$EXPECTED_FPR" | awk -F: '/^fpr:/ {print $10; exit}')"
if [[ "$IMPORTED_FPR" != "$EXPECTED_FPR" ]]; then
  echo "Error: imported key fingerprint does not match bundle metadata." >&2
  echo "Expected: $EXPECTED_FPR" >&2
  echo "Actual:   ${IMPORTED_FPR:-<none>}" >&2
  exit 1
fi

echo
echo "Import successful."
echo "Bundle extracted to: $BUNDLE_DIR"
echo "Imported fingerprint: $IMPORTED_FPR"
echo
echo "Recommended next steps:"
echo "1) Confirm fingerprint with sender through an independent channel."
echo "2) Store revocation certificate from the bundle in offline secure storage."
echo "3) Securely delete extracted secret-key material after operational handoff."
