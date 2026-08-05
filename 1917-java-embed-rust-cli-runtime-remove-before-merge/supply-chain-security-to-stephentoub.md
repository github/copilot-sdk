# Supply-chain security note for Stephen Toub

Hi Stephen,

As I was working on #1917, I was reminded of an SFI concern for which I forced the mitigation on my previous team (Copilot modernization, under Dmirty (now retired)). Specifically: the team was pulling artifacts directly from npmjs.org, rather from an internal AzDO pipeline that lists npmjs.org as an upstream.

I realize we're at GitHub, and its different here, but might we want to be SFI proactive here?

## What I found

Every non-Java SDK in the monorepo downloads `@github/copilot-{platform}` tarballs directly from `registry.npmjs.org` at build time:

- **.NET** — `dotnet/src/build/GitHub.Copilot.SDK.targets` (line 58): `DownloadFile` from `registry.npmjs.org` (configurable via `CopilotNpmRegistryUrl` property)
- **Rust** — `rust/build/in_process.rs` (line 86): Hardcoded `registry.npmjs.org` download in `build.rs`
- **Go** — `go/cmd/bundler/main.go` (line 38): Hardcoded `registry.npmjs.org` download URL
- **Python** — `python/copilot/_cli_version.py` (line 44): Hardcoded `_NPM_REGISTRY_BASE_URL = "https://registry.npmjs.org"`
- **Node.js** — `package-lock.json`: Standard npm resolution (expected for a JS project)

### Mitigations currently in place

- .NET is the only SDK that makes the registry URL configurable (`CopilotNpmRegistryUrl` property), so enterprise consumers can point at a private/mirrored registry.
- Rust verifies SHA-512 integrity after download.
- Go and Python have no registry override and no integrity verification visible in the code I examined.

I'm flagging this purely as a question — I don't know the team's existing threat model for this surface, and there may be context I'm missing. Happy to defer entirely to your judgment on whether this warrants action.

— Ed
