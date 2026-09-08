# Runtime-driven SDK test branch runbook

> [!WARNING]
> The `mackinnonbuck-test-sdk-runtime-internal` branch is a publication test harness. Never merge it into `main`, never open a pull request for it, and never use its workflow for a production release.

This runbook manually exercises `.github/workflows/sdk-canary.yml` from its dedicated branch. The runtime workflow does not dispatch it. Every mode requires a fresh positive canonical runtime Actions database run ID, except an intentional exact duplicate or canonical SDK rerun test.

## Inspect the remote safety boundary

Run this gate immediately before every dispatch. It reads the workflow from the remote test branch, not the local checkout, and fails on public write paths, GitHub Packages access, package write permission, public access, or npm trusted publication setup. A public npm read-only version lookup is allowed.

```bash
set -euo pipefail
BRANCH=mackinnonbuck-test-sdk-runtime-internal
REMOTE_YAML="$(mktemp)"
NORMALIZED_YAML="$(mktemp)"
trap 'rm -f "$REMOTE_YAML" "$NORMALIZED_YAML"' EXIT

gh api \
  "/repos/github/copilot-sdk/contents/.github/workflows/sdk-canary.yml?ref=$BRANCH" \
  --jq .content |
  tr -d '\n' |
  base64 --decode > "$REMOTE_YAML"

perl -0pe 's/\\\r?\n[ \t]*/ /g' "$REMOTE_YAML" > "$NORMALIZED_YAML"
if grep -Ein \
  'publish-public:|npm\.pkg\.github\.com|packages:[[:space:]]*write|--access[[:space:]]+public|trusted[[:space:]-]*publish|npm[[:space:]]+publish|npm[[:space:]]+install[[:space:]]+--global[[:space:]]+npm@|(npm[[:space:]]+dist-tag|publish-manifest).*registry\.npmjs\.org|registry\.npmjs\.org.*(npm[[:space:]]+dist-tag|publish-manifest|_authToken)' \
  "$REMOTE_YAML" "$NORMALIZED_YAML"; then
  echo "Unsafe remote workflow; do not dispatch." >&2
  exit 1
fi

grep -F 'name: "TEST ONLY - Runtime-driven Node SDK"' "$REMOTE_YAML"
grep -F 'RUNTIME_SOURCE: azure' "$REMOTE_YAML"
grep -F 'runtime-sdk-canary-test' "$REMOTE_YAML"
grep -F 'runtime-sdk-unstable-test' "$REMOTE_YAML"
```

## Validate the runtime handoff

Set the five handoff fields and confirm the runtime run belongs to `github/copilot-agent-runtime`, completed successfully, and produced the exact lowercase source SHA. Confirm the Azure package version and all eight platform artifacts match the runtime run evidence.

```bash
CHANNEL=canary
RUNTIME_VERSION='<exact-test-version>'
RUNTIME_SHA='<lowercase-full-runtime-test-branch-sha>'
RUNTIME_RUN_ID='<positive-canonical-runtime-actions-run-id>'
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

| Channel    | Mode                       | Runtime source | Internal tag                |
| ---------- | -------------------------- | -------------- | --------------------------- |
| `canary`   | `tests-only` or `internal` | Azure          | `runtime-sdk-canary-test`   |
| `unstable` | `internal` only            | Azure          | `runtime-sdk-unstable-test` |

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

Use `canary` with `tests-only` first, then use fresh runtime run IDs for canary `internal` and unstable `internal`. Do not dispatch unstable `tests-only`.

## Exercise idempotency

Use **Re-run failed jobs** on the original canonical SDK run to verify same-run recovery. Its `.test.<github.run_id>` package versions remain identical:

```bash
CANONICAL_SDK_RUN_ID='<canonical-sdk-actions-run-id>'
gh run rerun "$CANONICAL_SDK_RUN_ID" --repo github/copilot-sdk --failed
```

Run the exact dispatch command again with the same five values to verify a duplicate mirrors `CANONICAL_SDK_RUN_ID` and does not mint another version. For the collision test, keep `RUNTIME_RUN_ID` unchanged, set `RUNTIME_SHA` to a different lowercase full SHA, rerun the exact command, and confirm the ledger rejects the tuple. Restore the validated SHA afterward. Do not reuse a runtime run ID to test a different mode.

## Retain evidence

Record the runtime run URL, canonical and duplicate SDK run URLs, frozen SDK version, three operating-system test results, nine-package manifest, SHA-512 values, internal tag resolutions, collision failure, and cleanup results. Download `sdk-runtime-test-dispatch-<runtime_run_id>` and `nodejs-runtime-test-<channel>-<version>` from the canonical SDK run and verify their recorded workflow, runtime, SDK ref, source SHA, and run IDs.

```bash
gh api "/repos/github/copilot-sdk/actions/runs/$CANONICAL_SDK_RUN_ID/artifacts" \
  --jq '.artifacts[] | [.name, .expired, .archive_download_url] | @tsv'
gh run download "$CANONICAL_SDK_RUN_ID" \
  --repo github/copilot-sdk \
  --name "sdk-runtime-test-dispatch-$RUNTIME_RUN_ID" \
  --dir evidence/marker
```

## Clean up

After capturing evidence, remove `runtime-sdk-canary-test` and `runtime-sdk-unstable-test` from all nine SDK package coordinates in the Azure `copilot-canary` feed. Remove test package versions only through the feed's approved package-retention process. Confirm production `canary` and `unstable` tags never changed, delete local evidence containing temporary Azure credentials, and retain the run URLs and manifests in the test record.
