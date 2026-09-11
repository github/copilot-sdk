# Canary and unstable Node SDK releases

Canary releases remain an internal runtime-to-SDK channel. Unstable Node SDK
releases can either publish the selected SDK branch with its existing bundled
runtime or package exact runtime inputs supplied by `github/copilot-agent-runtime`.

## Entry points

Use `.github/workflows/publish.yml` for a direct unstable release of the
selected SDK branch as-is. The workflow packages its selected or bundled
runtime, publishes the nine Node SDK packages to public npm, then mirrors those
packages to the internal Azure feed. Direct unstable releases can run from a
non-main branch. They do not publish .NET, Rust, Python, Java, or Go releases,
and they do not create an SDK GitHub Release. The same workflow remains the
normal stable and prerelease publisher for all SDK languages.

The runtime workflow dispatches an SDK workflow at an explicit SDK ref. Each
handoff includes the exact runtime version, full source SHA, and source workflow
run ID.

The runtime workflow dispatches `.github/workflows/runtime-sdk.yml`. This
runtime-driven Node entry is separate from the direct unstable path.
`runtime-sdk.yml`
owns runtime acquisition, cross-platform tests, packaging, manifest retention,
optional internal publication, and public unstable npm publication.

The runtime dispatch includes these inputs:

- `channel`: `canary` or `unstable`
- `runtime_version`: Exact runtime package version
- `runtime_sha`: Lowercase, 40-character `github/copilot-agent-runtime` SHA
- `runtime_run_id`: Source runtime workflow run ID for provenance
- `mode`: `tests-only` or `publish` for canary; `publish` for unstable

Maintainers can dispatch `runtime-sdk.yml` directly with the same inputs. The
optional `version` input is available only for unstable and must be an unstable
SemVer base. The workflow appends its run ID and SDK SHA so each new
dispatch still creates a unique version. Unstable runs reject `tests-only`.

## Release gates

Both channels acquire all eight `@github/copilot-<platform>` packages from
GitHub Packages with the job-scoped `GITHUB_TOKEN`. The workflows validate npm
integrity, runtime version and SHA metadata, the exact package set, platform
metadata, repository metadata, and required runtime files.

The runtime-driven workflow runs runtime-backed Node SDK tests on Ubuntu,
macOS, and Windows. It then builds and verifies eight self-contained
`@github/copilot-sdk-<platform>` packages and the
`@github/copilot-sdk` umbrella package. The checked-in
`COPILOT_CLI_USE_NPM_PACKAGE` value remains `false`; runtime npm packages are
build inputs rather than published dependencies.

Both unstable entry points use the same version planner. A generated version is
`<target-core>-unstable.<workflow-run-id>.g<sdk-sha-7>`, where the target core
comes from the nearest eligible SDK release on the selected branch's
first-parent history. A stable baseline increments the patch; a prerelease
baseline retains its release core. An explicit unstable base uses
`<explicit-unstable-base>.<workflow-run-id>.g<sdk-sha-7>`. GitHub workflow run
IDs are repository-wide, so the two entry points cannot collide when their
per-workflow run numbers happen to match. Release eligibility is frozen at the
workflow creation time, so a same-run retry keeps its identity and each new
dispatch receives a new version.

The runtime-driven packaging job writes all nine tarballs and
`release-manifest.json` to one retained artifact. Publication jobs use that
artifact without rebuilding or recalculating its identity.

## Publication order

Canary `tests-only` runs stop after package verification. Canary `publish`
runs publish platform packages before the umbrella package to the Azure
`copilot-canary` feed, then perform a clean install and package version check.
No canary job has a public npm publication path.

Direct `publish.yml` unstable runs publish the platform packages and umbrella
package to public npm first, then mirror the same Node package set to Azure.

Runtime-driven unstable runs publish the retained platform tarballs and
umbrella tarball to Azure first. A clean internal install must start the exact
selected SDK package version before public publication begins. The strict
acquisition and package validation gates verify the embedded runtime identity.
The public job uses npm trusted publishing from `runtime-sdk.yml` and publishes
the same tarballs under the `unstable` dist-tag, with the umbrella package last.

The two entry points share concurrency locks for public npm and internal Azure
publication so they cannot race either set of `unstable` tags.

Both unstable paths validate all nine retained tarballs against local SHA-512
manifest values before publication. A successful `npm publish` completes a
package publication. A recognized immutable-version conflict means the package
was already published and also completes that package publication; output
feeds do not need to expose `dist.integrity`. Azure authentication allows the
workflow to add or advance its tag, but it refuses to rewind a tag that points
to a newer version. Public npm trusted publishing sets `unstable` during
publication. The workflow then verifies all nine `@unstable` resolutions. It
fails rather than attempting a separate public dist-tag mutation if any
resolution differs.

## Recovery

Use **Re-run failed jobs** on the original workflow run for normal recovery.
The workflow run ID and frozen version remain unchanged. Runtime-driven runs
also retain the package artifact. Do not rerun a successful packaging job
merely to recover a publication job.

The runtime run ID is retained as provenance only. Re-running the same SDK
workflow run retries its frozen SDK version and retained artifact. A new
workflow dispatch creates a new SDK release identity and version, even when it
uses the same runtime run, version, and SHA. This allows any number of SDK
releases to reuse the same immutable runtime packages.

## Registry setup

The Azure `copilot-canary` feed continues to use the `cicd` environment and
Azure workload identity. GitHub Packages acquisition uses the workflow
`GITHUB_TOKEN` with `packages: read`.

Before enabling runtime dispatch, publish the eight signed runtime package
coordinates to GitHub Packages and confirm that this repository can read all
eight with its workflow token.

Confirm npm trusted publisher configuration authorizes
both `.github/workflows/publish.yml` and `.github/workflows/runtime-sdk.yml` for
`@github/copilot-sdk` and all eight `@github/copilot-sdk-<platform>` package
names. The first identity publishes stable, prerelease, and direct unstable
versions; the second publishes runtime-driven unstable versions. Do not add an
npm token, workflow indirection, or a separate protected SDK publication
environment.
