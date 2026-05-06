# Releasing `github-copilot-sdk`

This document describes how to cut a release of the `github-copilot-sdk` Rust crate
and publish it to [crates.io]. It is the operational counterpart to the
workflow files under `../.github/workflows/rust-*.yml` (which run the actual
mechanics).

If you are adding code to the SDK, you do not need to read this. This is for
maintainers cutting a release.

[crates.io]: https://crates.io/crates/github-copilot-sdk

---

## TL;DR

1. Land your changes on `main` using conventional-commit messages.
2. Trigger the **Rust SDK: Create Release PR** workflow manually
   (`workflow_dispatch`).
3. Review and merge the PR that release-plz opens.
4. The **Rust SDK: Publish Release** workflow runs automatically when that
   PR merges, publishes to crates.io, tags `rust-vX.Y.Z`, and creates a
   GitHub Release.

The first 0.1.0 publish requires a one-time `CARGO_REGISTRY_TOKEN` secret
setup — see [First-time setup](#first-time-setup) below.

---

## How releases are cut

The crate uses [release-plz] in a two-PR workflow. Both PRs run unattended
through GitHub Actions; the only manual step is reviewing and merging.

[release-plz]: https://release-plz.dev/

### Step 1 — `release-plz release-pr`

Workflow: `.github/workflows/rust-release-pr.yml` (`workflow_dispatch` only).

When you trigger it, release-plz:

- Reads conventional-commit history since the last `rust-vX.Y.Z` tag.
- Decides the next version (patch / minor / major) per SemVer rules.
- Bumps `rust/Cargo.toml`'s `version` field.
- Renames `## [Unreleased]` in `rust/CHANGELOG.md` to `## [X.Y.Z] -
  <date>` and prepends a fresh empty `## [Unreleased]` above it.
- Opens a PR with those changes.

Review the PR. The CHANGELOG entry is the one users see on crates.io and on
the GitHub Release page, so make sure it reads well. Edit the PR directly if
the auto-generated entry needs tweaking.

> **First-publish note.** The hand-curated 0.1.0 entry currently lives
> under `## [Unreleased]` so release-plz will rename it cleanly on the
> first run. If release-plz instead generates a *second* entry from
> conventional commits and prepends it above the curated one (depends on
> the configured `body` template), delete the auto-generated stub in the
> PR and keep the curated entry — you only want one 0.1.0 section.

### Step 2 — `release-plz release` (publish)

Workflow: `.github/workflows/rust-publish-release.yml` (auto-runs on push
to `main` when `rust/Cargo.toml`, `rust/Cargo.lock`, or `rust/release-plz.toml`
changes).

When the release-PR from step 1 merges, this workflow detects that
`rust/Cargo.toml`'s version is newer than the latest `rust-vX.Y.Z` tag and:

- Runs `cargo publish` to upload to crates.io.
- Creates a `rust-vX.Y.Z` git tag.
- Creates a GitHub Release with the CHANGELOG entry as the body.

The workflow is a no-op on non-release commits, so it's safe to run on every
push.

---

## First-time setup

Before the first 0.1.0 publish, complete this checklist exactly once:

1. **Reserve the crate name.** Have a maintainer with crates.io 2FA log in
   to crates.io and run `cargo publish` for an empty stub OR claim the name
   via the "New Crate" form. The owner account should be a service account
   (preferred) or a senior maintainer.
2. **Generate a scoped API token.** crates.io → Account Settings → API
   Tokens → New Token. Scope it to publish `github-copilot-sdk` *only* — do not
   issue an unscoped token.
3. **Add the secret.** GitHub repo Settings → Secrets and variables →
   Actions → New repository secret named `CARGO_REGISTRY_TOKEN`, value =
   the token from step 2.
4. **Rotation.** Rotate the token annually and whenever the maintainer set
   changes. There's no automated reminder for this — set a calendar event.

Until this checklist is complete, `cargo publish` in the workflow will fail.
That's intentional: it keeps accidental publishes from happening before the
repo is ready.

---

## Versioning policy

The crate follows [SemVer]. Pre-1.0 we treat **0.x.0** as breaking and
**0.x.y** as additive — same as the Rust ecosystem convention.

[SemVer]: https://semver.org/

Two CI checks defend the API surface:

- **`cargo semver-checks`** (`.github/workflows/rust-sdk-tests.yml`) —
  detects breaking changes against the latest *published* version on
  crates.io. Currently `continue-on-error: true` because there's no
  baseline yet. **Flip it to `false` after 0.1.0 ships** to make SemVer
  enforcement blocking.

For ad-hoc public-surface inspection, `cargo public-api -sss --features
derive,test-support` is handy — but the surface is not snapshotted in the
repo. The rendered docs on [docs.rs](https://docs.rs/github-copilot-sdk) are the
canonical reference; `cargo-semver-checks` is the gate.

For 0.x → 1.0, do an explicit API review pass (compare against the
language siblings under `../{nodejs,python,go,dotnet}/`),
remove anything `#[doc(hidden)]` you don't intend to keep public, and
write out the 1.0 commitment in the CHANGELOG.

---

## Public-disclosure gate

The Rust SDK release-prep work happens on `tclem/rust-sdk-release-prep`
and is held *unpushed* until product/comms gives explicit OK. Do not push
the branch, open a PR, or otherwise expose the work without that signal —
even if CI looks ready.

Ways to keep moving without pushing:

- Land work in local commits on the prep branch.
- Use `cargo publish --dry-run --allow-dirty` to validate package contents.
- Use `cargo public-api -sss --features derive,test-support` for ad-hoc
  surface inspection.

When the gate opens:

1. Push `tclem/rust-sdk-release-prep`.
2. Open a PR titled "Rust SDK: prepare for 0.1.0 release" (or similar).
3. Once it merges, trigger the **Rust SDK: Create Release PR** workflow and
   proceed with the publish flow above.

---

## Manual publish (emergency only)

If GitHub Actions is unavailable, a maintainer with crates.io credentials
can publish locally:

```sh
cd rust

# Verify the package contents first.
cargo publish --dry-run

# Publish for real.
cargo publish

# Tag and push.
git tag rust-v$(cargo metadata --no-deps --format-version=1 \
  | jq -r '.packages[] | select(.name=="github-copilot-sdk") | .version' | head -1)
git push origin --tags
```

Manual publishes skip the release-PR review step, so write the CHANGELOG
entry by hand before publishing and commit it on `main` first.

---

## Yanking a release

If a published version contains a critical bug (security, data loss, panic
on common input), yank it from crates.io to prevent new installs:

```sh
cargo yank --version X.Y.Z github-copilot-sdk
```

Yanking does *not* delete the version — existing `Cargo.lock` files keep
working — but it stops new resolutions from picking it. Follow up with a
patch release that fixes the bug, and add a note to the yanked version's
GitHub Release explaining why.

Reverse with `cargo yank --undo --version X.Y.Z github-copilot-sdk` if the yank
was a mistake.
