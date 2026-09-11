# Runtime-driven SDK test branch runbook

> [!WARNING]
> `mackinnonbuck-test-sdk-runtime-internal` is a non-mergeable publication test harness. Never merge it into `main` or use it for a production release.

This branch exposes `.github/workflows/sdk-canary.yml` as a manual, runtime-backed test entry point. It accepts only `dist-tag`, `mode`, and `runtime`. `dry-run` acquires, tests, packages, and verifies without mutating Azure. `publish` adds publication and clean-install verification using isolated Azure test tags. Neither mode can publish to public npm or create releases or source tags.

## Inspect the remote safety boundary

Run this gate immediately before every dispatch. It reads the workflow from the remote test branch and rejects obsolete inputs, direct releases, public writes, release/tag mutation, non-Azure output feeds, and workflow-level runtime registry overrides.

```bash
set -euo pipefail
BRANCH=mackinnonbuck-test-sdk-runtime-internal
REMOTE_YAML="$(mktemp)"
trap 'rm -f "$REMOTE_YAML"' EXIT

gh api \
  "/repos/github/copilot-sdk/contents/.github/workflows/sdk-canary.yml?ref=$BRANCH" \
  --jq .content |
  tr -d '\n' |
  base64 --decode > "$REMOTE_YAML"

npm exec --prefix nodejs -- tsx nodejs/scripts/sdk-canary-safety.ts "$REMOTE_YAML"
```

## Validate the runtime handoff

The runtime JSON must contain exactly three string properties:

```json
{
  "version": "<exact-runtime-semver>",
  "sha": "<lowercase-full-sha>",
  "run_id": "<positive-actions-run-id>"
}
```

Confirm the run belongs to `github/copilot-agent-runtime`, completed successfully, and produced the exact source SHA and GitHub Packages version supplied in the JSON. The runtime version must contain the selected `canary` or `unstable` prerelease identifier.

```bash
DIST_TAG=unstable
MODE=dry-run
RUNTIME_VERSION='<exact-runtime-semver>'
RUNTIME_SHA='<lowercase-full-runtime-sha>'
RUNTIME_RUN_ID='<runtime-actions-run-id>'
RUNTIME="$(jq -cn \
  --arg version "$RUNTIME_VERSION" \
  --arg sha "$RUNTIME_SHA" \
  --arg run_id "$RUNTIME_RUN_ID" \
  '{version:$version,sha:$sha,run_id:$run_id}')"

[[ "$RUNTIME_RUN_ID" =~ ^[1-9][0-9]*$ ]]
[[ "$RUNTIME_SHA" =~ ^[0-9a-f]{40}$ ]]
gh api "/repos/github/copilot-agent-runtime/actions/runs/$RUNTIME_RUN_ID" \
  --jq '{id,conclusion,event,head_branch,head_sha,name,url}'
test "$(gh api "/repos/github/copilot-agent-runtime/actions/runs/$RUNTIME_RUN_ID" --jq .id)" = "$RUNTIME_RUN_ID"
test "$(gh api "/repos/github/copilot-agent-runtime/actions/runs/$RUNTIME_RUN_ID" --jq .head_sha)" = "$RUNTIME_SHA"
test "$(gh api "/repos/github/copilot-agent-runtime/actions/runs/$RUNTIME_RUN_ID" --jq .conclusion)" = success
```

Both `canary` and `unstable` accept `dry-run` and `publish`. `publish` writes only to `runtime-sdk-canary-test` or `runtime-sdk-unstable-test` on the Azure `copilot-canary` feed.

## Dispatch exactly

```bash
gh workflow run sdk-canary.yml \
  --repo github/copilot-sdk \
  --ref mackinnonbuck-test-sdk-runtime-internal \
  --raw-field dist-tag="$DIST_TAG" \
  --raw-field mode="$MODE" \
  --raw-field runtime="$RUNTIME"
```

Use `dry-run` first. A new dispatch may reuse the same runtime JSON and creates a new SDK workflow-run-derived identity. A rerun of the same SDK Actions run retains its deterministic `.test.<github.run_id>` identity.

## Retain evidence

Record the runtime run URL, SDK run URL, exact acquired runtime, Linux/Windows/macOS test results, nine-package manifest and SHA-512 values, and—only for `publish`—the isolated Azure tag and clean-install result. Download the `nodejs-runtime-test-<dist-tag>-<version>` artifact and verify its workflow identity, SDK ref/SHA, runtime provenance, and package set.

## Clean up

After a `publish` test, remove `runtime-sdk-canary-test` and `runtime-sdk-unstable-test` from all nine SDK package coordinates using the Azure feed's approved retention process. Confirm production `canary` and `unstable` tags never changed, remove temporary credentials or downloaded evidence, and retain only the run URLs and manifests required by the test record.
