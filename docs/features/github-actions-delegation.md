# Delegate agent work to GitHub Actions

Use a custom SDK tool to send isolated work to an allowlisted GitHub Actions workflow instead of running a Copilot CLI sub-agent locally. This pattern keeps the parent session local while the delegated worker runs on a GitHub-hosted runner.

> [!NOTE]
> The Copilot CLI runtime owns native sub-agent execution. A custom tool does not emit native `subagent.*` events or replace the runtime's `task` implementation.

## Architecture

The Node.js proof of concept uses this flow:

```text
Parent SDK session
    -> github_actions_delegate tool
    -> workflow_dispatch API
    -> GitHub Actions worker
    -> artifact or draft pull request
    -> tool result returned to the parent session
```

The dispatcher:

* Accepts a task, repository, ref, agent profile, and expected output.
* Rejects repositories, refs, and agent profiles that the host did not allowlist.
* Derives an opaque correlation ID and `agent/<correlation-id>` output branch from the SDK session and tool-call IDs.
* Reuses an existing correlated run when a tool call is retried.
* Reports queued, running, completed, failed, and cancelled lifecycle states.
* Cancels the workflow run when the SDK tool invocation is aborted.
* Returns the workflow URL, conclusion, artifacts, and matching pull requests.

See [`nodejs/samples/github-actions-client.ts`](../../nodejs/samples/github-actions-client.ts) for the dispatcher and [`nodejs/samples/github-actions-delegate.ts`](../../nodejs/samples/github-actions-delegate.ts) for the blocking tool.

## Configure the worker

The example worker is an [Agentic Workflow](https://github.github.com/gh-aw/) defined in [`.github/workflows/sdk-delegated-agent.md`](../../.github/workflows/sdk-delegated-agent.md). Its compiled `sdk-delegated-agent.lock.yml` file is the workflow that GitHub Actions executes.

The worker accepts only the declared `workflow_dispatch` inputs. It supports two profiles:

* `researcher`: Reads the repository and returns analysis without modifying files.
* `editor`: Changes only `nodejs/**` or `docs/**` and creates at most one draft pull request from an `agent/**` branch.

Adapt `allowed-files`, agent profiles, and the worker prompt before copying this pattern to another repository. Compile source changes with `gh aw compile sdk-delegated-agent`.

## Configure authentication

Provide a short-lived GitHub App installation token when possible. The token used by the SDK host needs only:

* Actions: Read and write, to dispatch, inspect, and cancel workflow runs.
* Contents: Read, to resolve the workflow and source ref.
* Pull requests: Read, to return a matching pull request URL.

Do not place a token in a task, workflow input, branch name, log message, or persisted delegate state. The sample reads `GITHUB_TOKEN` from the host environment and sends it only in the GitHub API authorization header.

## Run the blocking sample

Build the Node.js SDK, then configure the allowlisted target:

```shell
cd nodejs
npm run build
cd samples
npm install
GITHUB_TOKEN="..." \
GITHUB_ACTIONS_REPOSITORY="OWNER/REPOSITORY" \
GITHUB_ACTIONS_REF="BRANCH_WITH_WORKFLOW" \
npm run delegate -- "Review retry handling and return a concise report"
```

`GITHUB_ACTIONS_WORKFLOW` optionally selects another allowlisted workflow filename. It defaults to `sdk-delegated-agent.lock.yml`.

The tool handler waits for the remote run. This is suitable when the host process can remain alive for the workflow duration.

## Run the resumable sample

For long-running work, use the declaration-only tool in [`nodejs/samples/github-actions-delegate-resumable.ts`](../../nodejs/samples/github-actions-delegate-resumable.ts). The first command dispatches the workflow, persists the pending SDK request with mode `0600`, and stops the local runtime:

```shell
GITHUB_TOKEN="..." \
GITHUB_ACTIONS_REPOSITORY="OWNER/REPOSITORY" \
GITHUB_ACTIONS_REF="BRANCH_WITH_WORKFLOW" \
npm run delegate:resumable -- dispatch "Review retry handling"
```

After the workflow completes, resume the session and resolve its pending tool call:

```shell
GITHUB_TOKEN="..." \
GITHUB_ACTIONS_REPOSITORY="OWNER/REPOSITORY" \
GITHUB_ACTIONS_REF="BRANCH_WITH_WORKFLOW" \
npm run delegate:resumable -- complete
```

The sample stores state at `/tmp/copilot-sdk-github-actions-delegate.json`. Set `DELEGATE_STATE_FILE` to use durable encrypted storage in a production host. Treat the session ID and pending request ID as sensitive application state even though the file does not contain a GitHub token.

## Security boundaries

Keep these controls when adapting the proof of concept:

1. Use exact repository, workflow, ref, and agent-profile allowlists controlled by the host.
1. Pass structured task data. Never accept a command line or executable script as a workflow input.
1. Use an installation token with the minimum permissions and a short lifetime.
1. Restrict edits with the workflow's `allowed-files`, protected-file policy, draft pull requests, and environment approvals.
1. Keep each invocation on an isolated `agent/<correlation-id>` branch.
1. Set workflow and host-side timeouts, concurrency limits, and cancellation handling.
1. Persist the correlation ID, run ID, session ID, tool-call ID, and pending request ID in durable storage for retries.
1. Treat workflow artifacts and pull requests as untrusted output until validation succeeds.

## Native sub-agent compatibility

The parent prompt hides the built-in `task` tool and directs the model to `github_actions_delegate`. This preserves normal SDK tool-call behavior but not native sub-agent event semantics.

Overriding the built-in `task` tool with `overridesBuiltInTool: true` is possible but experimental because its schema and orchestration behavior belong to the Copilot CLI runtime. Full compatibility with `subagent.started`, `subagent.completed`, and `subagent.failed` requires a remote executor feature in the runtime rather than an SDK-only change.
