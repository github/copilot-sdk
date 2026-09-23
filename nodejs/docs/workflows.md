# Dynamic Workflows

Dynamic Workflows are extension-authored, session-scoped workflows that coordinate subagents and durable steps. The API is experimental.

Use Dynamic Workflows for new extension-authored orchestration. Existing Agent Factory extensions remain supported during the transition, but one `joinSession` call must register either `workflows` or `factories`, never both.

## Define and register a workflow

Use `defineWorkflow` and pass the returned handle to `joinSession`:

```js
import { defineWorkflow, joinSession } from "@github/copilot-sdk/extension";

const reviewChanged = defineWorkflow({
    meta: {
        name: "review-changed",
        description:
            "Review changed files and verify the findings. " +
            "args: { files: string[] } — the paths to review.",
        phases: [{ title: "Review" }, { title: "Verify" }],
        argsSchema: {
            type: "object",
            required: ["files"],
            properties: {
                files: { type: "array", items: { type: "string" } },
            },
        },
    },
    run: async (ctx) => {
        ctx.phase("Review");
        const reviews = await ctx.parallel(
            ctx.args.files.map(
                (file) => () => ctx.agent(`Review ${file}`, { label: `Review ${file}` })
            )
        );

        ctx.phase("Verify");
        const report = await ctx.step("report", () => ({ reviews }));
        ctx.log(`Completed workflow run ${ctx.runId}`);
        return report;
    },
});

const session = await joinSession({ workflows: [reviewChanged] });
```

Workflow metadata contains a stable `name`, a human-readable `description`, declared `phases`, an optional `argsSchema`, and optional `limits`. Phase entries contain a `title` and optional `detail`.

## Declaring an argument shape

A workflow that reads `ctx.args` should declare `meta.argsSchema`, as the example above does. The schema records the workflow's expected input contract alongside its registration metadata.

Enforcement covers structure — types, required properties, and enum or const values. Finer constraints such as `minLength`, `pattern`, or `additionalProperties` are recorded in the declaration but not enforced. The accepted vocabulary is the `WorkflowJsonSchema` subset also used for subagent structured output: `type`, `required`, `enum`, `const`, recursive `properties`/`items`, and `anyOf`/`oneOf`/`allOf`. A `type` is one of `null`, `boolean`, `integer`, `number`, `string`, `array`, or `object`, or a non-empty array of those such as `["object", "null"]`. A declaration outside that subset is rejected at registration.

`argsSchema` is optional and backward compatible. A workflow that omits it behaves exactly as before, so state the expected shape in its `description`.

An extension calling `session.workflow.run(...)` is not validated against `argsSchema`; those arguments are typed through `defineWorkflow<TArgs>` instead. A workflow that reads `ctx.args` should still validate it rather than assume a shape because the declared subset does not enforce every constraint and JavaScript callers are not statically typed.

`defineWorkflow<TArgs, TResult>` accepts a `run(context)` function returning `Promise<TResult>`, where `TResult` is `JsonValue | void`. Objects, arrays, strings, numbers, booleans, and `null` are valid results. Returning `undefined` completes the workflow with no result. Other non-JSON values are rejected.

## Workflow context

The `run()` context provides:

* `ctx.runId`: Stable ID reused across resumed attempts.
* `ctx.args`: Invocation arguments, forwarded verbatim. When the caller omits `args`, this is `{}` rather than `undefined`.
* `ctx.agent(prompt, options?)`: Runs one workflow-owned subagent. Options are exactly `label`, `schema`, `model`, `agent`, `reasoningEffort`, and `contextTier`. See [Subagent calls](#subagent-calls).
* `ctx.parallel(thunks)`: Runs thunks concurrently and awaits all of them (a barrier). A thunk that throws becomes `null` in the result array, so one failed item does not lose the rest. Cancellation and hard runtime failures (`ResponseError`, `ConnectionError`) are the exception — those propagate and reject the whole call, because they mean the run itself is in trouble rather than one item having failed. Handle them at run level; do not assume every failure arrives as a `null`. Rejects above 4096 items.
* `ctx.pipeline(items, ...stages)`: Flows each item through every stage without a barrier between stages, so one item can be in a later stage while another is still in an earlier one. Each stage is called as `(previous, item, index)`, where `previous` is the prior stage's result and `item` is the original input. A stage that throws drops that item to `null` and skips its remaining stages, with the same exception for cancellation and hard runtime failures. Rejects above 4096 items.
* `ctx.phase(title)`: Starts a named progress phase. This sets a single run-global value, so calling it from inside concurrent `parallel`/`pipeline` stages races. Call it at run-level transitions and distinguish concurrent work by `label` instead.
* `ctx.log(message)`: Appends a progress line. When a workflow bounds its own coverage (top-N, sampling), log what was dropped.
* `ctx.step(key, producer, options?)`: Journals the producer's JSON result under a stable key so a resume replays it without re-running the producer. A journaled (default) producer must return a JSON-serializable value; `undefined` or a non-JSON value is rejected. Pass `{ volatile: true }` to bypass the journal and run the producer every time.

  The key is the *sole* identity: neither the producer body nor its inputs contribute to it. A resume replays the cached value for a matching key even if the producer has since changed, so version the key (`"scan-v2"`) whenever its inputs or meaning change. Journaled producers are best-effort at-least-once and may run again across crashes or concurrent same-key callers, so keep side effects idempotent.
* `ctx.pause(key)`: Pauses at a durable, one-shot checkpoint. The first attempt records the checkpoint, pauses, and throws `AbortError` after cooperative cancellation. When the run resumes, the workflow starts again and the same checkpoint returns so execution can continue. Call it only from the main workflow flow, not inside `ctx.parallel()` or `ctx.pipeline()`.
* `ctx.session`: The session returned by `joinSession`. It refuses calls that start, resume, or pause a workflow run. Call `extensions_manage` with `operation: "guide"` to read more about the session APIs.
* `ctx.signal`: Cooperative cancellation signal for extension work and subprocesses.
* `ctx.workflow(...)`: Always rejects because nested workflows are not supported.

Workflow-owned subagents are intentionally hidden from `read_agent` and `write_agent`. Use the workflow observability APIs instead.

### Subagent calls

`ctx.agent(prompt, options?)` spawns one workflow-scoped subagent and awaits it. Without a schema it resolves to the subagent's final text. With `options.schema` it resolves to the parsed JSON value.

**Identical calls are memoized into one subagent.** Each call is journaled by its canonical prompt and options, including `label`. Two calls with the same prompt and the same options return one shared result — even when issued concurrently. To spawn N *independent* subagents, give each a unique `label` or vary the prompt:

```js
// One subagent, awaited five times — almost certainly not what you want.
await ctx.parallel([1, 2, 3, 4, 5].map(() => () => ctx.agent("Find a bug")));

// Five independent subagents.
await ctx.parallel(
    [1, 2, 3, 4, 5].map((i) => () => ctx.agent("Find a bug", { label: `finder:${i}` }))
);
```

**An ordinary failure resolves to `null` — it does not throw.** A subagent that errors, returns nothing, or (with a schema) produces output that still fails to parse or match after its one retry resolves `null`. Always guard the result before using it, including a bare `await ctx.agent(...)`:

```js
const finding = await ctx.agent(prompt, { label: "inspector" });
if (!finding) return { finding: null };
```

Cancellation and hard runtime failures — a reached limit, a durable-state failure — reject instead, aborting the run. When filtering results, prefer `v => v !== null` over `Boolean`, which also discards a valid `false`, `0`, or `""`.

**`schema` is a structural subset of JSON Schema, not a validator.** Honored: `type`, `required`, `enum`, `const`, recursive `properties`/`items`, and `anyOf`/`oneOf`/`allOf` — where `oneOf` is treated as `anyOf`, meaning at least one branch matches rather than exactly one. Ignored and *not* enforced: `additionalProperties`, `pattern`, `minLength`/`maxLength`, `format`, numeric ranges, and boolean schemas. Do not rely on an ignored keyword to constrain a result. A schema call retries once on a parse or match failure, so it may spawn twice, and both spawns count toward `maxTotalSubagents`.

### Choosing between pipeline and parallel

Prefer `pipeline` for multi-stage work. It has no barrier between stages, so each item advances as soon as its own prior stage finishes.

Reach for a barrier — `parallel` between stages — only when a stage genuinely needs every prior result at once: deduplicating or merging across the full set, an early exit based on the total, or a prompt that compares one result against the others. Needing to map, filter, or flatten is not a reason to use a barrier; do that inside a pipeline stage. Barrier latency is real: if the slowest of N subagents takes three times the fastest, a barrier wastes the rest of the pool's time.

## Resource limits

Limits may be declared in `meta.limits` and overridden per invocation. Every limit is optional and must be positive when present; an omitted limit leaves that dimension unbounded, except that an omitted `maxConcurrentSubagents` falls back to `maxTotalSubagents`, so a declared total cap also bounds concurrency.

Set a ceiling only from real knowledge of what the workflow costs, or because the user named one. A guessed ceiling does not make a run safer: it stops a healthy run partway with `workflow_limit_reached`, after that run has already spent credits. SDK-initiated `run` and `resume` do not request permission, so a caller that wants a ceiling must set it deliberately from a cost it already knows.

```js
// Only when the cost profile is known, or the user asked for this ceiling.
limits: { maxTotalSubagents: 10 },
```

- `maxConcurrentSubagents`: Positive integer concurrent-subagent cap. Additional subagents wait in a queue. Queueing applies backpressure and does not fail the run.
- `maxTotalSubagents`: Positive integer cumulative admission cap. An attempted subagent beyond the cap ends the attempt with failure kind `maxTotalSubagents`.
- `timeoutSeconds`: Positive finite number of seconds, including positive fractions, capped at `2_147_483.647`. It measures accumulated active-execution time across attempts, including the extension body, subprocess waits, queued-agent waits, and sleeps. Time between attempts is excluded. The timeout is soft because already-running work may take time to stop. Its failure kind is `timeoutSeconds`.
- `maxAiCredits`: Positive finite AI-credit budget for the whole run's workflow subagent subtree, including descendants. AI credits are GitHub Copilot's universal usage metric. This is a soft, post-paid ceiling, so completed or parallel turns can settle above it before the run stops. Accounting is fail-closed: an accounting failure stops a budgeted run rather than allowing untracked use. Its failure kind is `maxAiCredits`.

`maxTotalSubagents`, `timeoutSeconds`, and `maxAiCredits` use reject-and-retry semantics. A rejected attempt ends with run status `error` and `failure.type` set to `workflow_limit_reached`. The failed run keeps its ID, arguments, journal, and accounting. Resume the run with a raised limit when additional work is approved. Previously consumed resources still count.

## Run and resume

Run by registered name or handle:

```ts
const run = await session.workflow.run("review-changed", {
    args: { files: ["src/a.ts"] },
    limits: { maxAiCredits: 3 },
    notifyOnComplete: true,
    logPhaseNames: true,
});

if (run.status === "completed") {
    console.log(run.result);
} else {
    console.error(`run ${run.runId} ended as ${run.status}`, run.failure ?? run.error);
}
```

The name overload is:

```ts
session.workflow.run(
    name: string,
    options?: {
        args?: JsonValue;
        limits?: WorkflowLimitOverrides;
        notifyOnComplete?: boolean;
        logPhaseNames?: boolean;
    },
): Promise<WorkflowRunResult>;
```

Resume by run ID without resending the name or arguments:

```ts
const run = await session.workflow.resume(runId, {
    limits: { maxAiCredits: 6 },
    notifyOnComplete: true,
    logPhaseNames: true,
});
```

The signature is:

```ts
session.workflow.resume(
    runId: string,
    options?: {
        limits?: WorkflowLimitOverrides;
        notifyOnComplete?: boolean;
        logPhaseNames?: boolean;
    },
): Promise<WorkflowRunResult>;
```

Set `notifyOnComplete` to `true` for workflows that are likely to be invoked by an agent, so the originating session is notified when the workflow completes. Set it to `false` for workflows intended to be invoked programmatically, where the caller awaits the result directly. Set `logPhaseNames` to emit workflow phase names to the session transcript. Both options apply to new and resumed runs.

Both resolve with the run envelope (`WorkflowRunResult`) for **every** outcome—`completed`, `error`, `halted`, `paused`, and `cancelled` alike. Inspect `status` and read `result` only when the run completed; a limit breach carries a typed `failure`. A `paused` envelope means that the current attempt settled, not that the durable run is permanently finished. Resume the same run ID to start another attempt with its journal and accounting intact. SDK-initiated `run` and `resume` do not request permission, so they have no declined outcome. An SDK-initiated run is refused only when the session already has its maximum number of active top-level runs. Pre-execution resume failures throw `WorkflowResumeError`, whose `code` is one of `not_found`, `non_resumable`, `workflow_run_not_resumable`, `already_active`, `workflow_already_running`, `workflow_limits_invalid`, `workflow_session_disposed`, `workflow_storage_unavailable`, or `workflow_storage_corrupt`.

Pause a running attempt from outside its workflow body:

```ts
const paused = await session.workflow.pause(runId);
```

Inside a workflow body, use a durable checkpoint instead:

```ts
await ctx.step("prepare", prepareInput);
await ctx.pause("review-ready");
await ctx.agent("Review the prepared input");
```

The first attempt pauses at `"review-ready"` and ends through cooperative cancellation. On resume, the workflow starts from the beginning, reuses the journaled step, returns from the checkpoint, and continues.

## Observe a run

The calling session can inspect its own workflow runs:

```ts
const runs = await session.workflow.listRuns();
const runsPage = await session.workflow.listRuns({
    afterSeq,
    beforeSeq,
    limit,
});
const detail = await session.workflow.getRunDetail(runId);
const progressPage = await session.workflow.getRunProgress(runId, {
    phaseId,
    afterSeq,
    beforeSeq,
    limit,
});
```

- `listRuns()` returns only the runs array from the newest default page of this session's durable workflow runs. This overload preserves the original convenience API.
- `listRuns({ afterSeq, beforeSeq, limit })` returns the full page. Its `oldestSeq`, `newestSeq`, `hasMoreNewer`, and `omittedOlder` fields let callers continue paging without raw RPC calls.
- `getRunDetail(runId)` returns phases, prompt-safe agent summaries, and the latest progress page.
- `getRunProgress(runId, options?)` pages progress forward, backward, by phase, or from the latest tail.

`getRun(runId)` reads the latest run envelope. `pause(runId)` pauses a running attempt and returns its `paused` envelope. `cancel(runId)` cancels a run and returns its terminal envelope.

`waitForRun(runId, options?)` resolves with the current attempt's envelope once it settles into `completed`, `error`, `halted`, `paused`, or `cancelled`. It resolves immediately when the current attempt has already settled:

```ts
const settled = await session.workflow.waitForRun(runId);
if (settled.status === "completed") {
    console.log(settled.result);
}
```

It watches the runtime's `factory.run_updated` compatibility event and re-reads the durable envelope on each invalidation, collapsing a burst of events into a single in-flight read. A low-frequency periodic re-read runs alongside the subscription, so a dropped or missing invalidation degrades into a slightly late resolution rather than an unbounded wait. Pass a `signal` to stop waiting:

```ts
const controller = new AbortController();
setTimeout(() => controller.abort(), 30_000);
const settled = await session.workflow.waitForRun(runId, { signal: controller.signal });
```

Aborting rejects the wait and has no effect on the run, which keeps executing—use `pause(runId)` or `cancel(runId)` to stop it. The resolved object is a snapshot of that settled attempt. If its status is `paused`, a later resume updates the durable envelope under the same run ID. Call `getRun(runId)` to read the latest envelope. `isWorkflowRunTerminal(status)` exposes the same current-attempt settlement test for callers driving their own loop.

Listen for the ephemeral `factory.run_updated` compatibility event. Its `{ runId, revision }` payload is an invalidation signal. Re-read the desired API when a newer monotonic revision arrives.

Revisions cover durable lifecycle, accounting, phase, agent, and progress changes. Continuous read-time fields can change without a new revision. These include `observedAt`, active-time calculations, live counts, and a live agent's status or prompt-safe activity text. Workflow prompts are never exposed by these APIs. A run is visible only through the session that owns it.
