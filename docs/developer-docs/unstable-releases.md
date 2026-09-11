# Canary and unstable Node SDK releases

Canary releases remain an internal runtime-to-SDK channel. Unstable Node SDK
releases can either publish the selected SDK branch with its existing bundled
runtime or package exact runtime inputs supplied by `github/copilot-agent-runtime`.
All production release jobs run in `.github/workflows/publish.yml`.

## Dispatch inputs

The workflow has one input surface for maintainer and automation dispatches:

* `dist-tag`: Required release channel: `latest`, `prerelease`, `unstable`, or
  `canary`. The default is `prerelease`.
* `version`: Optional direct SDK version. For direct unstable releases, this is
  an unstable SemVer base before workflow identity is added.
* `mode`: Required execution mode: `publish` or `dry-run`. The default is
  `publish`.
* `runtime`: Optional automation-only JSON object with exactly the string fields
  `version`, `sha`, and `run_id`.

The runtime workflow dispatches `publish.yml` at an explicit SDK ref. For
example:

```json
{"version":"1.0.83-5.unstable.123.gabcdef0","sha":"abcdef0123456789abcdef0123456789abcdef01","run_id":"34640000001"}
```

Runtime JSON is valid only for canary and unstable releases. Canary requires
runtime JSON; direct canary releases are rejected. The workflow also rejects a
direct `version` combined with runtime JSON. Runtime JSON must contain an exact
SemVer for the selected channel, a lowercase 40-character
`github/copilot-agent-runtime` SHA, and a positive canonical decimal workflow
run ID.

Dry-run mode is valid only for canary and unstable releases. Stable and
prerelease dry-runs are rejected because the existing Java release path does
not have a non-mutating build-only mode.

For a direct unstable release, select `dist-tag: unstable`, leave `runtime`
empty, and optionally provide `version`. The workflow packages the selected SDK
branch with its selected or bundled runtime. Direct unstable releases can run
from a non-main branch. They do not publish .NET, Rust, Python, Java, or Go
releases, and they do not create an SDK GitHub Release. The same workflow
remains the normal stable and prerelease publisher for all SDK languages.

## Release gates

Runtime-initiated releases acquire all eight
`@github/copilot-<platform>` packages from GitHub Packages with the job-scoped
`GITHUB_TOKEN`. The workflow validates npm integrity, runtime version and SHA
metadata, the exact package set, platform metadata, repository metadata, and
required runtime files.

The runtime jobs run runtime-backed Node SDK tests on Ubuntu, macOS, and
Windows. They then build and verify eight self-contained
`@github/copilot-sdk-<platform>` packages and the `@github/copilot-sdk` umbrella
package. The checked-in `COPILOT_CLI_USE_NPM_PACKAGE` value remains `false`;
runtime npm packages are build inputs rather than published dependencies.

Both unstable dispatch modes use the same version planner. A generated version
is `<target-core>-unstable.<workflow-run-id>.g<sdk-sha-7>`, where the target
core comes from the nearest eligible SDK release on the selected branch's
first-parent history. A stable baseline increments the patch; a prerelease
baseline retains its release core. A direct release with an explicit unstable
base uses `<explicit-unstable-base>.<workflow-run-id>.g<sdk-sha-7>`. GitHub
workflow run IDs are repository-wide, so the two modes cannot collide when
their per-workflow run numbers happen to match. Release eligibility is frozen
at workflow creation time, so a same-run retry keeps its identity and each new
dispatch receives a new version.

Canary versions use
`X.Y.(Z+1)-canary.<workflow-run-number>.g<sdk-sha-7>`, based on the newest
stable SDK release published before workflow creation.

The runtime packaging job writes all nine tarballs and
`release-manifest.json` to one retained artifact. Publication jobs use that
artifact without rebuilding or recalculating its identity.

## Publication order

Canary and runtime-backed unstable `dry-run` runs stop after package and local
manifest verification. Direct unstable `dry-run` runs build, pack, and verify
the same nine-package set without registry mutations. Dry-runs do not acquire
publication concurrency locks.

Canary `publish` runs publish platform packages before the umbrella package to
the Azure `copilot-canary` feed, then perform a clean install and package
version check. No canary job has a public npm publication path.

Direct unstable runs publish the platform packages and umbrella package to
public npm first, then mirror the same Node package set to Azure.

Runtime-initiated unstable runs publish the retained platform tarballs and
umbrella tarball to Azure first. A clean internal install must start the exact
selected SDK package version before public publication begins. The public job
runs directly in `publish.yml` so npm trusted publishing sees the configured
workflow identity. It publishes the same tarballs under the `unstable`
dist-tag, with the umbrella package last.

The two dispatch modes share concurrency locks for public npm and internal
Azure publication so they cannot race either set of `unstable` tags.

Both unstable modes validate all nine retained tarballs against local SHA-512
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
The workflow run ID and frozen version remain unchanged. Runtime-initiated runs
also retain the package artifact. Do not rerun a successful packaging job
merely to recover a publication job.

The runtime run ID is retained as provenance only. Re-running the same SDK
workflow run retries its frozen SDK version and retained artifact. A new
workflow dispatch creates a new SDK release identity and version, even when it
uses the same runtime run, version, and SHA.

## Registry setup

The Azure `copilot-canary` feed continues to use the `cicd` environment and
Azure workload identity. GitHub Packages acquisition uses the workflow
`GITHUB_TOKEN` with `packages: read`.

Before enabling runtime dispatch, publish the eight signed runtime package
coordinates to GitHub Packages and confirm that this repository can read all
eight with its workflow token.

Confirm npm trusted publisher configuration authorizes
`.github/workflows/publish.yml` for `@github/copilot-sdk` and all eight
`@github/copilot-sdk-<platform>` package names. This workflow publishes stable,
prerelease, direct unstable, and runtime-initiated unstable versions.
