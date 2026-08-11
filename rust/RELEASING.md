# Releasing `github-copilot-sdk`

The Rust crate ships through the unified `publish.yml` workflow
alongside the other SDKs. There is no Rust-specific release workflow.

## TL;DR

1. Land your changes on `main`.
2. Trigger the **Publish SDK packages** workflow
   (`.github/workflows/publish.yml`) via `workflow_dispatch`.
3. Pick `dist-tag`:
   - `latest` — stable release (e.g. `1.0.0`).
   - `prerelease` — beta release (e.g. `1.0.0-beta.4`). Lands on
     crates.io as a prerelease; users must opt in with an explicit
     prerelease version requirement to install it.
   - `unstable` — skipped for Rust (Cargo doesn't have a clean
     equivalent of npm's `unstable` dist-tag).
4. For `latest` and `prerelease`, the workflow publishes all SDKs at
   the shared computed version, tags `rust/vX.Y.Z` for source
   traceability, and creates one combined `vX.Y.Z` GitHub Release.
   The `unstable` channel publishes only the Node.js SDK and does not
   create a GitHub Release.

## Version, tag, and release notes

- **Crate version:** the in-tree `rust/Cargo.toml` carries `0.0.0-dev`
  as a placeholder. CI overrides it at publish time with the version
  computed by `publish.yml` (or an explicit `version` workflow input).
- **Tag:** `rust/vX.Y.Z` (matches the `go/vX.Y.Z` style used elsewhere
  in this repo). The tag identifies the source used for that crate
  version.
- **Release notes:** generated for the combined `vX.Y.Z` GitHub
  Release. Write descriptive PR titles for changes that touch the Rust
  surface so they are represented accurately in the shared notes.

## Cargo prerelease semantics

`cargo add github-copilot-sdk` and `version = "1"` requirements skip
prereleases by default. Users who want to opt in to a beta must
write an explicit prerelease requirement:

```toml
github-copilot-sdk = "1.0.0-beta.4"
```

This matches Cargo's standard semver behavior and means a
prerelease-channel publish won't surprise stable users.

## Yanking a release

If a published version contains a critical bug, yank it from
crates.io to prevent new installs:

```sh
cargo yank --version X.Y.Z github-copilot-sdk
```

Yanking does *not* delete the version — existing `Cargo.lock` files
keep working — but it stops new resolutions from picking it. Follow
up with a patch release that fixes the bug, and update the combined
GitHub Release notes to explain why.

Reverse with `cargo yank --undo --version X.Y.Z github-copilot-sdk`
if the yank was a mistake.

## Manual publish (emergency only)

If GitHub Actions is unavailable, a maintainer with crates.io
credentials can publish locally:

```sh
cd rust

# Set the real version (replace X.Y.Z).
perl -i -pe 's/^version = ".*"$/version = "X.Y.Z"/' Cargo.toml

# Verify package contents.
cargo publish --dry-run

# Publish for real.
cargo publish

# Tag and push.
git tag rust/vX.Y.Z
git push origin rust/vX.Y.Z

# Restore the placeholder.
perl -i -pe 's/^version = ".*"$/version = "0.0.0-dev"/' Cargo.toml
```

Manual publishes skip the combined GitHub Release. Create or update the
matching `vX.Y.Z` release after pushing the tag.
