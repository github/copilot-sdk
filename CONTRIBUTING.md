# Contributing

Thanks for your interest in contributing!

This repository contains the Copilot SDK, a set of multi-language SDKs (Node/TypeScript, Python, Go, .NET, Java, and Rust) for building applications with the GitHub Copilot agent, maintained by the GitHub Copilot team.

Contributions to this project are [released](https://help.github.com/articles/github-terms-of-service/#6-contributions-under-repository-license) to the public under the [project's open source license](LICENSE).

Please note that this project is released with a [Contributor Code of Conduct](CODE_OF_CONDUCT.md). By participating in this project you agree to abide by its terms.

## Before You Submit a PR

**Please discuss any feature work with us before writing code.**

The team already has a committed product roadmap, and features must be maintained in sync across all supported languages. Pull requests that introduce features not previously aligned with the team are unlikely to be accepted, regardless of their quality or scope.

If you submit a PR, **be sure to link to an associated issue describing the bug or agreed feature**. No PRs without context :)

## What We're Looking For

We welcome:

- Bug fixes with clear reproduction steps
- Improvements to documentation
- Making the SDKs more idiomatic and nice to use for each supported language
- Bug reports and feature suggestions on [our issue tracker](https://github.com/github/copilot-sdk/issues) — especially for bugs with repro steps

We are generally **not** looking for:

- New features, capabilities, or UX changes that haven't been discussed and agreed with the team
- Refactors or architectural changes
- Integrations with external tools or services
- Additional documentation
- **SDKs for other languages** — if you want to create a Copilot SDK for another language, we'd love to hear from you and may offer to link to your SDK from our repo. However we do not plan to add further language-specific SDKs to this repo in the short term, since we need to retain our maintenance capacity for moving forwards quickly with the existing language set. For other languages, please consider running your own external project.

## Developing an SDK

Setup, build, and test instructions are maintained with each SDK:

- [Node.js/TypeScript](nodejs/README.md#development)
- [Python](python/README.md#development)
- [Go](go/README.md#development)
- [.NET](dotnet/README.md#development)
- [Rust](rust/README.md#development)
- [Java](java/README.md#development-setup)

### Testing an unreleased runtime API

The runtime's Rust contracts under `src/native/sdk-contract` produce both
`generated/api.schema.json` (RPC methods) and
`generated/session-events.schema.json` (event payloads). In a local checkout of
`github/copilot-agent-runtime`, build the runtime and emit these schemas:

```bash
pnpm run build
pnpm bazel build //src/native/schema-codegen:schema-codegen
bazel-bin/src/native/schema-codegen/schema-codegen emit \
  --api "$PWD/generated/api.schema.json" \
  --session-events "$PWD/generated/session-events.schema.json"
```

The SDK generators normally download schemas from the pinned CLI release. To
use the local schemas instead, pass the event-schema path followed by the
RPC-schema path. From this repository's `scripts/codegen` directory:

```bash
npm ci
for language in typescript csharp python go rust; do
  node --import tsx "$language.ts" \
    "$RUNTIME_ROOT/generated/session-events.schema.json" \
    "$RUNTIME_ROOT/generated/api.schema.json"
done
```

Set `RUNTIME_ROOT` to the absolute path of the runtime checkout. Java's generator
at `java/scripts/codegen/java.ts` reads these files from
`java/scripts/codegen/target/schemas` instead of accepting positional arguments;
stage the local schemas there before running it. Do not hand-edit generated
wrappers. Regenerating against a newer runtime
also includes any other contract changes since the SDK's pinned release.

Set `COPILOT_CLI_PATH` to the built runtime's `dist-cli/index.js` to run SDK E2Es
against that checkout rather than the packaged runtime. For example:

```bash
export COPILOT_CLI_PATH="$RUNTIME_ROOT/dist-cli/index.js"
# Supply GITHUB_TOKEN with Copilot access when recording new provider responses.
cd nodejs
npm test -- test/e2e/structured_output.e2e.test.ts
cd ../dotnet
dotnet test test/GitHub.Copilot.SDK.Test.csproj \
  --filter FullyQualifiedName~StructuredOutputE2ETests
```

The shared harness records real inference responses under `test/snapshots`.
Record new captures with `GITHUB_TOKEN` set and `GITHUB_ACTIONS` unset;
never author model responses by hand. Rerun with `GITHUB_ACTIONS=true` and real
provider credentials removed to require replay instead of forwarding cache
misses upstream. A draft targeting an unreleased runtime should document the
required runtime revision; update the pinned release only after it ships.
Pinned-schema CI can report drift in such a draft. Java codegen reports this
without automatically rewriting draft branches; automatic updates resume once
the pull request is ready for review.

For recording behind `HTTPS_PROXY`, Node versions that support environment
proxies (including Node 24.20) need `NODE_USE_ENV_PROXY=1` in the test runner's
environment. If the host proxy substitutes a protected credential, set
`GITHUB_TOKEN="$GH_TOKEN"` using its issued placeholder; do not print or persist
the credential. Keep localhost and loopback in `NO_PROXY`.

Equivalent cross-language E2Es should share snapshot names and prompts.
For example, Node's `typed_wait_returns_stop_hook_correction` and C#'s
`Typed_Wait_Returns_Stop_Hook_Correction` both use
`test/snapshots/structured_output/typed_wait_returns_stop_hook_correction.yaml`.
It was recorded once against real `gpt-4.1` inference, then replayed by both SDKs
against the local runtime. Both typed helpers select the corrected answer at
idle; there is no final-message flag.

## Submitting a Pull Request

1. Fork and clone the repository
1. Follow the development instructions for the SDK(s) you're modifying
1. Create a new branch: `git checkout -b my-branch-name`
1. Make your change, add tests, and run the documented checks
1. Push to your fork and [submit a pull request][pr]
1. Pat yourself on the back and wait for your pull request to be reviewed and merged.

Here are a few things you can do that will increase the likelihood of your pull request being accepted:

- Write tests.
- Keep your change as focused as possible. If there are multiple changes you would like to make that are not dependent upon each other, consider submitting them as separate pull requests.
- Write a [good commit message](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html).

## Resources

- [How to Contribute to Open Source](https://opensource.guide/how-to-contribute/)
- [Using Pull Requests](https://help.github.com/articles/about-pull-requests/)
- [GitHub Help](https://help.github.com)
