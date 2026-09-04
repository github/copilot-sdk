# Canary and unstable Node SDK releases

The SDK release workflows consume exact runtime platform packages produced by
`github/copilot-agent-runtime`. Canary releases remain internal. Unstable
releases publish the same self-contained Node SDK tarballs internally and then
to public npm.

## Runtime handoff

The runtime workflow dispatches an SDK workflow at an explicit SDK ref. Each
handoff includes the exact runtime version, full source SHA, and source workflow
run ID.

Canary dispatches `.github/workflows/sdk-canary.yml` with these inputs:

* `channel`: `canary`
* `runtime_version`: Exact Azure runtime package version
* `runtime_sha`: Lowercase, 40-character `github/copilot-agent-runtime` SHA
* `runtime_source`: `azure`
* `runtime_run_id`: Source runtime workflow run ID
* `mode`: `tests-only` or `internal`

Unstable dispatches `.github/workflows/publish.yml` with these inputs:

* `dist-tag`: `unstable`
* `runtime_version`: Exact signed GitHub Packages runtime version
* `runtime_sha`: Lowercase, 40-character `github/copilot-agent-runtime` SHA
* `runtime_source`: `github-packages`
* `runtime_run_id`: Source runtime workflow run ID

Maintainers can dispatch `publish.yml` directly with the same unstable inputs.
The optional `version` input must be an unstable SemVer. Do not reuse an
explicit version after an artifact has been built.

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
publishing from `publish.yml` and publishes the same tarballs under the
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

Use `resume_run_id` only when the original run cannot be resumed. Start a new
manual `publish.yml` run with `dist-tag=unstable` and the original SDK workflow
run ID. The recovery path downloads the original retained artifact, verifies
its manifest and all nine SHA-512 integrity values, and uses the recorded SDK
and runtime identities. It never rebuilds or substitutes packages.

## Registry setup

The Azure `copilot-canary` feed continues to use the `cicd` environment and
Azure workload identity. GitHub Packages acquisition uses the workflow
`GITHUB_TOKEN` with `packages: read`.

Before enabling unstable dispatch, publish the eight signed runtime package
coordinates once, set each GitHub Package to public visibility, and confirm
that this repository can read all eight with its workflow token. Public
visibility does not remove GitHub Packages npm authentication.

Confirm npm trusted publisher configuration authorizes
`.github/workflows/publish.yml` for `@github/copilot-sdk` and all eight
`@github/copilot-sdk-<platform>` package names. Do not add a separate protected
SDK publication environment.
