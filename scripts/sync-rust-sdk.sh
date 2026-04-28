#!/usr/bin/env bash
#
# Sync the Rust SDK between this monorepo (rust/) and the in-tree home in
# github/github-app (crates/copilot-sdk/) during the public release transition.
#
# Hand-written sources only — Cargo.toml, generated/, and monorepo-only
# infrastructure files (LICENSE, rust-toolchain.toml, Cargo.lock, .gitignore)
# are intentionally NOT synced. Generated types should be produced by running
# the codegen in each repo against its own pinned schemas, not copied across.
#
# Usage:
#   scripts/sync-rust-sdk.sh from-app [GITHUB_APP_DIR]   # github-app → monorepo
#   scripts/sync-rust-sdk.sh to-app   [GITHUB_APP_DIR]   # monorepo → github-app
#   scripts/sync-rust-sdk.sh diff     [GITHUB_APP_DIR]   # show what would change
#
# GITHUB_APP_DIR defaults to ../github-app relative to the monorepo root.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MONOREPO_RUST_DIR="$REPO_ROOT/rust"

direction="${1:-}"
github_app_dir="${2:-$REPO_ROOT/../github-app}"

if [[ -z "$direction" ]]; then
    echo "usage: $0 {from-app|to-app|diff} [GITHUB_APP_DIR]" >&2
    exit 1
fi

github_app_dir="$(cd "$github_app_dir" && pwd)"
APP_RUST_DIR="$github_app_dir/crates/copilot-sdk"

if [[ ! -d "$APP_RUST_DIR" ]]; then
    echo "error: $APP_RUST_DIR does not exist" >&2
    echo "       pass the github-app checkout path as the second argument" >&2
    exit 1
fi

# Files / directories to sync. These are the hand-written sources shared
# between both copies. Anything not listed here is owned independently by
# each repo (Cargo.toml differs, generated/ regenerates from different
# schema pins, LICENSE / rust-toolchain.toml / Cargo.lock / .gitignore
# only exist in the monorepo).
SYNC_PATHS=(
    src/
    tests/
    examples/
    build.rs
    README.md
)

# Inside src/, never overwrite generated/ — it's per-repo output of codegen.
RSYNC_FLAGS=(
    --archive
    --delete
    --exclude=generated/
    --exclude=target/
)

case "$direction" in
    from-app)
        src_root="$APP_RUST_DIR"
        dst_root="$MONOREPO_RUST_DIR"
        label="github-app → monorepo"
        ;;
    to-app)
        src_root="$MONOREPO_RUST_DIR"
        dst_root="$APP_RUST_DIR"
        label="monorepo → github-app"
        ;;
    diff)
        echo "Comparing hand-written sources (excluding generated/, Cargo.toml, infrastructure files):"
        echo "  monorepo:   $MONOREPO_RUST_DIR"
        echo "  github-app: $APP_RUST_DIR"
        echo
        rc=0
        for path in "${SYNC_PATHS[@]}"; do
            diff -r \
                --exclude=generated \
                --exclude=target \
                "$MONOREPO_RUST_DIR/$path" "$APP_RUST_DIR/$path" \
                || rc=$?
        done
        exit "$rc"
        ;;
    *)
        echo "error: unknown direction '$direction' (expected from-app|to-app|diff)" >&2
        exit 1
        ;;
esac

echo "Syncing $label"
echo "  source: $src_root"
echo "  dest:   $dst_root"
echo

for path in "${SYNC_PATHS[@]}"; do
    if [[ ! -e "$src_root/$path" ]]; then
        echo "  skip $path (not in source)"
        continue
    fi
    echo "  sync $path"
    rsync "${RSYNC_FLAGS[@]}" "$src_root/$path" "$dst_root/$path"
done

echo
echo "Done. Review changes with 'git status' and 'git diff' in the destination repo."
echo "Reminders:"
echo "  - Cargo.toml is intentionally NOT synced (each repo has its own metadata)."
echo "  - src/generated/ is NOT synced; regenerate via codegen in each repo."
echo "  - Run 'cargo test --features test-support' in the destination to verify."
