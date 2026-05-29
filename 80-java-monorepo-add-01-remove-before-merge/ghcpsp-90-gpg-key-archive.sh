#!/usr/bin/env bash
set -euo pipefail

# Create an encrypted, signed handoff package for a GPG keypair.
#
# Usage:
#   ./ghcpsp-90-gpg-key-archive.sh <key-id-or-fingerprint> <recipient-key-id-or-email> [output-dir]
#
# Example:
#   ./ghcpsp-90-gpg-key-archive.sh 0123ABCD jane@example.com ./out

usage() {
  cat <<'EOF'
Usage:
  ghcpsp-90-gpg-key-archive.sh <key-id-or-fingerprint> <recipient-key-id-or-email> [output-dir]

Arguments:
  key-id-or-fingerprint      The secret key to export and hand off.
  recipient-key-id-or-email  Recipient key used to encrypt the bundle.
  output-dir                 Optional output directory (default: current directory).

Outputs:
  <prefix>.tar.gz                    Plain archive containing transfer files.
  <prefix>.tar.gz.asc                Encrypted + signed archive for transfer.

Notes:
  - Share the passphrase over a separate channel.
  - Keep the plain archive only as long as needed, then securely delete it.
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

if [[ $# -lt 2 || $# -gt 3 ]]; then
  usage >&2
  exit 1
fi

KEY_ID="$1"
RECIPIENT="$2"
OUTPUT_DIR="${3:-.}"

require_cmd gpg
require_cmd tar
require_cmd awk
require_cmd sed
require_cmd date

mkdir -p "$OUTPUT_DIR"

# Confirm key material is available locally.
if ! gpg --list-secret-keys "$KEY_ID" >/dev/null 2>&1; then
  echo "Error: no secret key found for: $KEY_ID" >&2
  exit 1
fi

# Confirm recipient key exists locally for encryption.
if ! gpg --list-keys "$RECIPIENT" >/dev/null 2>&1; then
  echo "Error: recipient public key not found locally: $RECIPIENT" >&2
  exit 1
fi

FPR="$(gpg --list-secret-keys --with-colons "$KEY_ID" | awk -F: '/^fpr:/ {print $10; exit}')"
if [[ -z "$FPR" ]]; then
  echo "Error: unable to determine fingerprint for key: $KEY_ID" >&2
  exit 1
fi

SHORT_FPR="${FPR: -16}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
PREFIX="gpg-key-handoff-${SHORT_FPR}-${STAMP}"

WORK_DIR="$(mktemp -d)"
cleanup() {
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

BUNDLE_DIR="$WORK_DIR/$PREFIX"
mkdir -p "$BUNDLE_DIR"

SECRET_ASC="$BUNDLE_DIR/secret-key.asc"
PUBLIC_ASC="$BUNDLE_DIR/public-key.asc"
FINGERPRINT_TXT="$BUNDLE_DIR/fingerprint.txt"
OWNERTRUST_TXT="$BUNDLE_DIR/ownertrust.txt"
NOTES_TXT="$BUNDLE_DIR/handoff-notes.txt"

# Export key material.
gpg --armor --export-secret-keys "$KEY_ID" > "$SECRET_ASC"
gpg --armor --export "$KEY_ID" > "$PUBLIC_ASC"
gpg --export-ownertrust > "$OWNERTRUST_TXT"

# Capture key identity details.
{
  echo "Primary fingerprint: $FPR"
  echo
  echo "Secret key listing:"
  gpg --list-secret-keys --keyid-format LONG "$KEY_ID"
  echo
  echo "Public key listing:"
  gpg --list-keys --keyid-format LONG "$KEY_ID"
} > "$FINGERPRINT_TXT"

# Try to locate an existing revocation certificate (GnuPG default layout).
REVOCATION_ASC="$BUNDLE_DIR/revocation.asc"
REVOCATION_SOURCE="${GNUPGHOME:-$HOME/.gnupg}/openpgp-revocs.d/${FPR}.rev"
if [[ -f "$REVOCATION_SOURCE" ]]; then
  cp "$REVOCATION_SOURCE" "$REVOCATION_ASC"
else
  {
    echo "Revocation certificate was not found at:"
    echo "  $REVOCATION_SOURCE"
    echo
    echo "If needed, generate one manually on a trusted host and add it to this bundle."
  } > "$BUNDLE_DIR/revocation-missing.txt"
fi

cat > "$NOTES_TXT" <<EOF
GPG Key Handoff Package

Created (UTC): $STAMP
Source key argument: $KEY_ID
Resolved fingerprint: $FPR
Recipient for encrypted transfer: $RECIPIENT

Bundle contents:
- secret-key.asc
- public-key.asc
- fingerprint.txt
- ownertrust.txt
- revocation.asc (if present) or revocation-missing.txt

Operational guidance:
1. Verify fingerprint out-of-band before importing.
2. Import secret and public keys.
3. Restore ownertrust if desired.
4. Store revocation certificate securely and offline.
5. Share passphrase through a separate channel.
EOF

PLAIN_ARCHIVE="$OUTPUT_DIR/$PREFIX.tar.gz"
SEALED_ARCHIVE="$OUTPUT_DIR/$PREFIX.tar.gz.asc"

tar -C "$WORK_DIR" -czf "$PLAIN_ARCHIVE" "$PREFIX"

# Encrypt and sign in one operation.
# - Encrypt to recipient so only recipient can decrypt.
# - Sign with the local default signing key for provenance.
gpg --armor --encrypt --sign --recipient "$RECIPIENT" --output "$SEALED_ARCHIVE" "$PLAIN_ARCHIVE"

echo "Created plain archive:   $PLAIN_ARCHIVE"
echo "Created sealed archive:  $SEALED_ARCHIVE"
echo
echo "Next step: transmit only the sealed archive and deliver the key passphrase separately."
