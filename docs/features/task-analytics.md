# Offline task analytics

`client.rpc.taskAnalytics.classifyTrajectory` runs the production classifiers on an archive without creating a conversation session. It does not execute tools, save history, or emit classification telemetry. Its metrics measure archived evidence; they are not new benchmark verdicts or proof that the agent's claims are true.

> [!WARNING]
> This API is experimental. It sends selected archive content to GitHub CAPI, independently of background telemetry consent, never to BYOK endpoints. Redact secrets and personal data before calling. Validation checks structure, not provenance or prompt-injection safety.

## Validate and select classifiers

Pass an ATIF-v1.7 document as one element of `events`, or pass individual ATIF steps. Native Copilot event objects are also accepted for existing archives; they are not a public format that external harnesses should synthesize.

<!-- docs-validate: hidden -->
```typescript
type TrajectoryEvents = Parameters<typeof client.rpc.taskAnalytics.classifyTrajectory>[0]["events"];
declare const atifDocument: TrajectoryEvents[number];

const events = [atifDocument];
const validation = await client.rpc.taskAnalytics.classifyTrajectory({
    events,
    validateOnly: true,
});
if (!validation.validation?.valid) {
    throw new Error(validation.validation?.errors.join("\n"));
}

const result = await client.rpc.taskAnalytics.classifyTrajectory({
    events,
    classifiers: ["task_taxonomy", "task_outcome"],
    maxConcurrentRequests: 2,
});
```
<!-- /docs-validate: hidden -->

```typescript
const events = [atifDocument];
const validation = await client.rpc.taskAnalytics.classifyTrajectory({
    events,
    validateOnly: true,
});
if (!validation.validation?.valid) {
    throw new Error(validation.validation?.errors.join("\n"));
}

const result = await client.rpc.taskAnalytics.classifyTrajectory({
    events,
    classifiers: ["task_taxonomy", "task_outcome"],
    maxConcurrentRequests: 2,
});
```

Validation also runs on ordinary classification requests. Invalid input returns `validation.valid: false`, diagnostics in `validation.errors`, and an empty `classifications` array, without authentication, model discovery, or inference. Archive repair is the caller's job; keep repairs auditable rather than silently filling in damaged content.

Omitting `classifiers` selects all eight. An empty list selects none. Selecting task/session classifiers or a classifier that reads task-opening turns also selects and returns the required `turn_splitting` outcomes.

| Classifier                 | Scope             |
| -------------------------- | ----------------- |
| `prompt_language`          | Prompt            |
| `user_message_sentiment`   | Prompt            |
| `task_completion_criteria` | Prompt            |
| `turn_splitting`           | Turn              |
| `requested_task_demand`    | Task-opening turn |
| `task_taxonomy`            | Task              |
| `task_outcome`             | Task              |
| `session_taxonomy`         | Session snapshot  |

## Retry failed units

Pass the unchanged archive and the previous result. Successful and declined rows are preserved, including their original model and interaction ID. Only failed or abandoned non-segmenting rows in the selected classifiers are rerun.

<!-- docs-validate: hidden -->
```typescript
declare const events: Parameters<typeof client.rpc.taskAnalytics.classifyTrajectory>[0]["events"];
declare const result: Awaited<ReturnType<typeof client.rpc.taskAnalytics.classifyTrajectory>>;

const retry = await client.rpc.taskAnalytics.classifyTrajectory({
    events,
    previousResult: result,
    classifiers: ["task_outcome"],
    maxConcurrentRequests: 1,
});
```
<!-- /docs-validate: hidden -->

```typescript
const retry = await client.rpc.taskAnalytics.classifyTrajectory({
    events,
    previousResult: result,
    classifiers: ["task_outcome"],
    maxConcurrentRequests: 1,
});
```

Retries reuse every original task boundary, including unresolved boundaries from failed segmentation. They do not pay to classify successful units again. Start a fresh analysis to improve segmentation or use changed classifier versions. A mismatched archive fingerprint, incompatible classifier version, or invalid previous result fails validation before inference. The fingerprint detects mismatches; it does not authenticate caller-supplied results.

Read each row's `status`: `success`, `declined`, `failed`, or `abandoned`. A decline means the classifier had no applicable evidence or no offered judge, not necessarily a service error. Do not retry every non-success outcome indiscriminately.

## Throttling and concurrency

HTTP inference failures include `httpStatus`, `retryable`, and, when supplied, `retryAfterSeconds`. The delay is a server-requested minimum and can be hours. Missing or malformed headers leave the delay absent; the API does not guess it from error text. Honor the largest applicable delay before scheduling retries, and do not automatically retry rows with `retryable: false`.

Archive inference does not use the provider's automatic retry loop. Each inference retains a 30-second ceiling, but the analysis has no shutdown-sized total deadline: healthy queued calls drain until completion or caller cancellation.

`maxConcurrentRequests` is a positive per-analysis limit across all classifier lanes; its default is 4. Multiple concurrent analyses multiply that limit. Sixteen trajectories with a limit of 4 can make up to 64 classifier calls concurrently; this is not a service quota or a recommended load level. Bound both trajectory and inference concurrency, and back off on 429s.

Authentication and model-catalog discovery are awaited once before the analysis's first model-backed classifier. Local-only classification, validation, and fully reused retries do not need discovery. Discovery failures are reported rather than silently continuing with a cold catalog. This does not reserve inference capacity or guarantee that later requests will avoid throttling.

## Accepted archive evidence

Input array order is authoritative. The API does not sort timestamps, repair redaction damage, or reorder events. Include the beginning of the conversation; partial histories can change task boundaries and follow-up judgments.

ATIF documents accept `schema_version: "ATIF-v1.7"` or an omitted version with a `steps` array. Individual steps use `source: "user" | "agent" | "system"`. User `message` values may be strings or ATIF text-block arrays. The adapter consumes:

| Evidence                         | Fields                                                                                                                         |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| User/assistant text and identity | `message`, `step_id`, `extra.eventId`/`event_id`/`id`, `agent.model_name`, `model_name`                                        |
| Roles and excluded traffic       | `source`, `extra.agentId`/`agent_id`/`subagent_id`, `is_copied_context`, `extra.auxiliary`                                     |
| Tool calls and results           | `tool_calls[].function_name`, `arguments`, call IDs, `observation.results[].source_call_id`, `content`, success/error metadata |
| Changes and commands             | Tool telemetry/churn, written paths, patch summaries, command arguments, structured exit codes or the shell exit footer        |
| Completion                       | `task_complete` calls, accepted completion-decision metadata, step completion markers, `extra.fusion.committed`                |
| Delegated work                   | Embedded `subagent_trajectories` and referenced `trajectory_id` values                                                         |

ATIF assistant prose precedes its tool results. Uncommitted Fusion work retains executed tool/edit evidence but cannot claim task completion. Withdrawn completion tools do not end a task. Ordinary JSON returned by a tool is summary text, not a completion decision unless it carries the `taskCompletionDecision` envelope.

Native archives use the current runtime's event reader, not a separately versioned public event schema. Analytics consumes user and assistant messages, tool start/completion events, skills, errors, permission denials, task completion, aborts, and idle boundaries. Include `id`, `type`, and the original `data` fields: message `content`, role/agent/source metadata, tool call IDs/names/arguments, result status, tool telemetry, shell execution data, completion summaries, and idle outcome. Non-analytics event types are ignored. Unsupported structure and missing message content fail validation.

Native events with identical IDs and identical objects are read once. Conflicting copies fail validation; the API does not choose which copy is correct. ATIF step IDs must be unique within a document. Separate documents may reuse step IDs. Referenced embedded trajectories are visited once per document; copied context and auxiliary steps are excluded.

EOF closes unfinished work with `session_end`; it does not invent a successful idle. A recorded abort remains an interruption. Session snapshots use the prefix present when their task-opening turn was observed, so retries do not depend on inference scheduling.

## Limits and reproducibility

Requests use the JSON-RPC connection's 256 MiB retained-request budget, shared by concurrent requests and including envelope/accounting overhead. An oversized request can be rejected by transport before archive validation runs. This API has no chunked ingestion. Split independent conversations into separate requests rather than splitting one conversation at arbitrary events.

The adapter retains at most 4,000 UTF-16 units per user message, 2,000 per assistant message, 160 per shell command, and 512 paths per tool call. Classifiers apply additional evidence-packing limits. `textTruncated` reports text loss for that row's classifier; it does not promise that every tool output or argument was retained. The API is an analytics adapter, not a lossless ATIF conversion API.

The judge defaults to each classifier's production model policy, allowed-model policy, and offered CAPI catalog, not the archived model. Connection credentials, endpoint, and integration remain authoritative; this method does not switch an SDK caller to the CLI's integration identity. Allowed-model policy uses the server's working directory, never an archived path.

Set request `judge: { model: "gpt-5.6-luna-utility", reasoningEffort: "low" }` to pin an exact judge and optional effort. Explicitly choosing a billed model authorizes its costs. Catalog, allowed-model, effort, and prompt-compatibility checks still apply; an unusable judge fails rather than falling back. Completion criteria requires its compatible Luna judge. Retried successes keep their original judge; only rerun rows use the new configuration.

Each result includes an archive/adapter fingerprint and a fresh analysis `interactionId`. New inference uses that ID, never the archived conversation's interaction ID. Rows report classifier/taxonomy versions, resolved model, model-selection reason, reasoning effort, token usage when available, and their own interaction ID. Failed HTTP calls retain the resolved judge even when no response text completed. Local declines and calls abandoned before model resolution may have no model metadata. Keep the runtime and SDK versions alongside results; classifier fingerprints alone are not a complete configuration identity.
