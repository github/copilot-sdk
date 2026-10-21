<!-- Copyright (c) Microsoft Corporation. All rights reserved. -->

# Rust SDK releases

The [`github-copilot-sdk` crate](https://crates.io/crates/github-copilot-sdk)
is published alongside the Node.js, Python, Go, .NET, and Java SDKs. SDK and
runtime versions are numbered independently; each published crate pins the
runtime version it uses.

## Release channels

| Channel | Rust publication |
| --- | --- |
| Stable | Stable crate, released with all six SDKs and the CLI |
| Prerelease | Prerelease crate, released with all six SDKs and the CLI |
| Unstable | Development crate, released with all six SDKs and public runtime acquisition assets |

Find available crate versions on
[crates.io](https://crates.io/crates/github-copilot-sdk/versions).

## Runtime pins and release outputs

Published crates include `cli-version.txt` and `cli-version-in-process.txt`
with the exact runtime version, release location, and archive hashes.

Stable/prerelease acquisition uses
[`github/copilot-cli` releases](https://github.com/github/copilot-cli/releases).
Unstable acquisition uses the exact `runtime-<runtime-version>` release in
[`github/copilot-sdk`](https://github.com/github/copilot-sdk/releases).
Default consumer builds acquire both
the runtime platform package and standalone CLI archive without private-feed
credentials.

The `rust/v<SDK-version>` and `v<SDK-version>` tags identify the corresponding
public SDK source snapshot for stable, prerelease, and unstable versions.
Stable/prerelease versions also have a combined `v<SDK-version>` GitHub release.
Unstable source tags do not advance SDK `main` or create an SDK GitHub release
announcement. Their runtime assets remain available under the separate
`runtime-<runtime-version>` release.

## Cargo prerelease semantics

Cargo does not have npm distribution tags. Consumers opt into a prerelease
or unstable crate by explicitly requesting its version, for example:

```toml
github-copilot-sdk = "=1.0.0-unstable.123456.gabcdef0"
```

This is an illustrative version; select an available version from crates.io.
Stable requirements do not automatically select prereleases.

## Yanked versions

A crate version with a critical defect can be yanked. Yanking does not delete
the version or invalidate existing lockfiles, but Cargo excludes it from new
dependency resolutions. Consult the release notes and update affected
applications to a fixed version.
