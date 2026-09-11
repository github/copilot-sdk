# Runtime-driven SDK test branch runbook

> [!WARNING]
> The `mackinnonbuck-test-sdk-runtime-internal` branch is a publication test harness. Never merge it into `main`, never open a pull request for it, and never use its workflow for a production release.

This runbook manually exercises `.github/workflows/sdk-canary.yml` from its dedicated branch. The runtime workflow does not dispatch it. The runtime Actions database run ID is provenance only, so multiple new SDK workflow runs can reuse the same validated runtime handoff. Each new SDK workflow run creates a distinct release identity; rerunning the same SDK Actions run retains its identity.

## Inspect the remote safety boundary

Run this gate immediately before every dispatch. It reads the workflow from the remote test branch, not the local checkout, and fails unless runtime inputs use authenticated, read-only GitHub Packages access and SDK outputs use the exact Azure `copilot-canary` feed. It also rejects public writes, package write permission, public access, and npm trusted publication setup. A public npm read-only version lookup is allowed.

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

grep -F 'name: "TEST ONLY - Runtime-driven Node SDK"' "$REMOTE_YAML"
test "$(grep -Ec '^[[:space:]]*FEED_URL:' "$REMOTE_YAML")" -eq 1
grep -Fx '  FEED_URL: https://pkgs.dev.azure.com/devdiv/_packaging/copilot-canary/npm/registry/' "$REMOTE_YAML"
grep -F '//npm.pkg.github.com/:_authToken=${NODE_AUTH_TOKEN}' "$REMOTE_YAML"
grep -F -- '--registry https://npm.pkg.github.com' "$REMOTE_YAML"
grep -F 'node nodejs/scripts/npm-release.js publish-manifest' "$REMOTE_YAML"
grep -F '"https://pkgs.dev.azure.com/devdiv/_packaging/copilot-canary/npm/registry/" azure' "$REMOTE_YAML"
```

## Validate the runtime handoff

Set the five handoff fields and confirm the runtime run belongs to `github/copilot-agent-runtime`, completed successfully, and produced the exact lowercase source SHA. Confirm the GitHub Packages version and all eight platform artifacts match the runtime run evidence.

```bash
CHANNEL=canary
RUNTIME_VERSION='<exact-test-version>'
RUNTIME_SHA='<lowercase-full-runtime-test-branch-sha>'
RUNTIME_RUN_ID='34494827966'
MODE=tests-only

[[ "$RUNTIME_RUN_ID" =~ ^[1-9][0-9]*$ ]]
[[ "$RUNTIME_SHA" =~ ^[0-9a-f]{40}$ ]]
gh api "/repos/github/copilot-agent-runtime/actions/runs/$RUNTIME_RUN_ID" \
  --jq '{id,conclusion,event,head_branch,head_sha,name,url}'
test "$(gh api "/repos/github/copilot-agent-runtime/actions/runs/$RUNTIME_RUN_ID" --jq .id)" = "$RUNTIME_RUN_ID"
test "$(gh api "/repos/github/copilot-agent-runtime/actions/runs/$RUNTIME_RUN_ID" --jq .head_sha)" = "$RUNTIME_SHA"
test "$(gh api "/repos/github/copilot-agent-runtime/actions/runs/$RUNTIME_RUN_ID" --jq .conclusion)" = success
```

The accepted matrix is:

| Channel    | Mode                      | Runtime source  | Internal tag                |
| ---------- | ------------------------- | --------------- | --------------------------- |
| `canary`   | `tests-only` or `publish` | GitHub Packages | `runtime-sdk-canary-test`   |
| `unstable` | `publish` only            | GitHub Packages | `runtime-sdk-unstable-test` |

## Dispatch exactly

After the remote safety gate and handoff validation pass, dispatch all five and only five inputs:

```bash
gh workflow run sdk-canary.yml \
  --repo github/copilot-sdk \
  --ref mackinnonbuck-test-sdk-runtime-internal \
  --raw-field channel="$CHANNEL" \
  --raw-field runtime_version="$RUNTIME_VERSION" \
  --raw-field runtime_sha="$RUNTIME_SHA" \
  --raw-field runtime_run_id="$RUNTIME_RUN_ID" \
  --raw-field mode="$MODE"
```

Use `canary` with `tests-only` first. The same validated runtime inputs can be dispatched again to create another SDK workflow-run-derived identity. Use a handoff matching the selected channel for canary `publish` and unstable `publish`. In this test-only workflow, `publish` writes only to the isolated Azure test tags; there is no public npm publication path. Do not dispatch unstable `tests-only`.

## Exercise release identity

Use **Re-run failed jobs** on an SDK run to verify same-run recovery. Its `.test.<github.run_id>` package versions remain identical:

```bash
SDK_RUN_ID='<sdk-actions-run-id>'
gh run rerun "$SDK_RUN_ID" --repo github/copilot-sdk --failed
```

Run the exact dispatch command again with the same five values. Confirm it starts a new SDK workflow run with a different `.test.<github.run_id>` package version instead of mirroring the prior SDK run. The runtime run ID, version, and SHA remain unchanged as provenance.

## Retain evidence

Record the runtime run URL, each new SDK run URL and distinct frozen SDK version, the same-run retry result, three operating-system test results, nine-package manifest, SHA-512 values, internal tag resolutions, and cleanup results. Download `nodejs-runtime-test-<channel>-<version>` from each SDK run and verify its recorded workflow, runtime provenance, SDK ref, source SHA, and workflow run ID.

```bash
gh api "/repos/github/copilot-sdk/actions/runs/$SDK_RUN_ID/artifacts" \
  --jq '.artifacts[] | [.name, .expired, .archive_download_url] | @tsv'
gh run download "$SDK_RUN_ID" \
  --repo github/copilot-sdk \
  --name "nodejs-runtime-test-$CHANNEL-<sdk-version>" \
  --dir evidence/release
```

## Clean up

After capturing evidence, remove `runtime-sdk-canary-test` and `runtime-sdk-unstable-test` from all nine SDK package coordinates in the Azure `copilot-canary` feed. Remove test package versions only through the feed's approved package-retention process. Confirm production `canary` and `unstable` tags never changed, delete local evidence containing temporary Azure credentials, and retain the run URLs and manifests in the test record.
