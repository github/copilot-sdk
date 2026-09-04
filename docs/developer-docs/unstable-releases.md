# Canary and unstable Node SDK releases

The SDK release workflows consume exact runtime platform packages produced by
`github/copilot-agent-runtime`. Canary releases remain internal. Unstable
releases publish the same self-contained Node SDK tarballs internally and then
to public npm.

## Runtime handoff

The runtime workflow dispatches an SDK workflow at an explicit SDK ref. Each
handoff includes the exact runtime version, full source SHA, and source workflow
run ID.

The runtime workflow dispatches `.github/workflows/runtime-sdk.yml`. This
runtime-driven Node entry is separate from `publish.yml`, which remains the
manual stable and prerelease entry for all SDK languages. `runtime-sdk.yml`
invokes `runtime-backed-node-release.yml` for runtime acquisition,
cross-platform tests, packaging, manifest retention, recovery, and optional
internal publication. It alone contains public unstable npm publication.

The runtime dispatch includes these inputs:

* `channel`: `canary` or `unstable`
* `runtime_version`: Exact runtime package version
* `runtime_sha`: Lowercase, 40-character `github/copilot-agent-runtime` SHA
* `runtime_source`: `azure` for canary or `github-packages` for unstable
* `runtime_run_id`: Source runtime workflow run ID and receiver idempotency key
* `mode`: `tests-only` or `internal` for canary; `internal` for unstable

Maintainers can dispatch `runtime-sdk.yml` directly with the same inputs. The
optional `version` input is available only for unstable and must be an unstable
SemVer. Do not reuse an explicit version after an artifact has been built.

## Release gates

Both channels acquire all eight `@github/copilot-<platform>` packages with an
explicit registry argument. The workflows validate npm integrity, runtime
version and SHA metadata, platform metadata, repository metadata, and required
runtime files. Authentication configuration does not map the entire `@github`
scope to GitHub Packages.

The workflows run runtime-backed Node SDK tests on Ubuntu, macOS, and Windows.
They then build and verify eight self-contained
`@github/copilot-sdk-<platform>` packages and the
`@github/copilot-sdk` umbrella package. The checked-in
`COPILOT_CLI_USE_NPM_PACKAGE` value remains `false`; runtime npm packages are
build inputs rather than published dependencies.

An unstable run freezes a version from the nearest eligible SDK release on the
selected branch's first-parent history, the workflow run number, and the SDK
SHA. The packaging job writes all nine tarballs and `release-manifest.json` to
one retained artifact. Publication jobs use that artifact without rebuilding
or recalculating its identity.

## Publication order

Canary `tests-only` runs stop after package verification. Canary `internal`
runs publish platform packages before the umbrella package to the Azure
`copilot-canary` feed, then perform a clean install and runtime version check.
No canary job has a public npm publication path.

Every unstable run publishes the retained platform tarballs and umbrella
tarball to Azure first. A clean internal install must start the exact selected
runtime before public publication begins. The public job uses npm trusted
publishing from `runtime-sdk.yml` and publishes the same tarballs under the
`unstable` dist-tag, with the umbrella package last.

Before either publication, the workflow checks all nine package coordinates.
An existing package counts as complete only when registry integrity matches
the retained manifest. A mismatch fails the release. After all package
contents are present, the workflow updates the channel dist-tag.
Azure authentication allows the workflow to add or advance its tag, but it
refuses to rewind a tag that points to a newer version. Public npm trusted
publishing sets `unstable` as each missing package is published. The workflow
then verifies all nine `@unstable` resolutions. It fails rather than attempting
a separate public dist-tag mutation if any resolution differs.

## Recovery

Use **Re-run failed jobs** on the original workflow run for normal recovery.
The run number, frozen version, and retained artifact remain unchanged. Do not
rerun a successful packaging job merely to recover a publication job.

Each `runtime_run_id` is serialized and claimed by a 90-day marker artifact.
The marker records the canonical SDK run and complete runtime/input
provenance, but the runtime run ID is not part of the immutable release
identity. Exact duplicate dispatches wait for and mirror the canonical run.
If that run fails or is canceled, rerun the original run rather than
dispatching another release.

Use `resume_run_id` only when the canonical run cannot be rerun. Start a new
manual `runtime-sdk.yml` unstable run with the same dispatch tuple and the
canonical SDK workflow run ID. The workflow validates the marker and GitHub API
provenance, downloads the canonical retained artifact, verifies its manifest
and all nine SHA-512 integrity values, and uses the recorded identities. It
never rebuilds or substitutes packages.

## Registry setup

The Azure `copilot-canary` feed continues to use the `cicd` environment and
Azure workload identity. GitHub Packages acquisition uses the workflow
`GITHUB_TOKEN` with `packages: read`.

Before enabling unstable dispatch, publish the eight signed runtime package
coordinates once, set each GitHub Package to public visibility, and confirm
that this repository can read all eight with its workflow token. Public
visibility does not remove GitHub Packages npm authentication.

Confirm npm trusted publisher configuration authorizes
both `.github/workflows/publish.yml` and `.github/workflows/runtime-sdk.yml` for
`@github/copilot-sdk` and all eight `@github/copilot-sdk-<platform>` package
names. The first identity publishes stable and prerelease versions; the second
publishes unstable versions. Do not add an npm token, workflow indirection, or
a separate protected SDK publication environment.
