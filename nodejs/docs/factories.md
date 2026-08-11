# Agent Factories

Agent Factories are extension-authored, session-scoped workflows that coordinate subagents and durable steps. The API is experimental.

## Define and register a factory

Use `defineFactory` and pass the returned handle to `joinSession`:

```js
import { defineFactory, joinSession } from "@github/copilot-sdk/extension";

const reviewChanged = defineFactory({
    meta: {
        name: "review-changed",
        description:
            "Review changed files and verify the findings. " +
            "args: { files: string[] } — the paths to review.",
        phases: [{ title: "Review" }, { title: "Verify" }],
        limits: {
            maxConcurrentSubagents: 3,
            maxTotalSubagents: 10,
            timeoutSeconds: 90.5,
            maxAiCredits: 5,
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
        ctx.log(`Completed factory run ${ctx.runId}`);
        return report;
    },
});

const session = await joinSession({ factories: [reviewChanged] });
```

Factory metadata contains a stable `name`, a human-readable `description`, declared `phases`, and optional `limits`. Phase entries contain a `title` and optional `detail`.

There is no declared schema for `ctx.args`. The `run_factory` tool forwards `args` verbatim and its parameter is untyped, so **the `description` is the only thing telling an agent what arguments to supply** — state the expected shape there whenever a factory reads `ctx.args`, as the example above does. Arguments supplied by an extension calling `session.factory.run(...)` directly are typed through `defineFactory<TArgs>`, but that typing does not reach the model. A factory that reads `ctx.args` should validate it rather than assume a shape.

`defineFactory<TArgs, TResult>` accepts a `run(context)` function returning `Promise<TResult>`, where `TResult` is `JsonValue | void`. Objects, arrays, strings, numbers, booleans, and `null` are valid results. Returning `undefined` completes the factory with no result. Other non-JSON values are rejected.

## Factory context

The `run()` context provides:

- `ctx.runId`: Stable ID reused across resumed attempts.
- `ctx.args`: Invocation arguments, forwarded verbatim. When the caller omits `args`, this is `{}` rather than `undefined`.
- `ctx.agent(prompt, options?)`: Runs one factory-owned subagent. Options are exactly `label`, `schema`, `model`, `agent`, `reasoningEffort`, and `contextTier`. See [Subagent calls](#subagent-calls).
- `ctx.parallel(thunks)`: Runs thunks concurrently and awaits all of them (a barrier). A thunk that throws becomes `null` in the result array, so one failed item does not lose the rest. Cancellation and hard runtime failures (`ResponseError`, `ConnectionError`) are the exception — those propagate and reject the whole call, because they mean the run itself is in trouble rather than one item having failed. Handle them at run level; do not assume every failure arrives as a `null`. Rejects above 4096 items.
- `ctx.pipeline(items, ...stages)`: Flows each item through every stage without a barrier between stages, so one item can be in a later stage while another is still in an earlier one. Each stage is called as `(previous, item, index)`, where `previous` is the prior stage's result and `item` is the original input. A stage that throws drops that item to `null` and skips its remaining stages, with the same exception for cancellation and hard runtime failures. Rejects above 4096 items.
- `ctx.phase(title)`: Starts a named progress phase. This sets a single run-global value, so calling it from inside concurrent `parallel`/`pipeline` stages races. Call it at run-level transitions and distinguish concurrent work by `label` instead.
- `ctx.log(message)`: Appends a progress line. When a factory bounds its own coverage (top-N, sampling), log what was dropped.
- `ctx.step(key, producer, options?)`: Journals the producer's JSON result under a stable key so a resume replays it without re-running the producer. A journaled (default) producer must return a JSON-serializable value; `undefined` or a non-JSON value is rejected. Pass `{ volatile: true }` to bypass the journal and run the producer every time.

  The key is the *sole* identity: neither the producer body nor its inputs contribute to it. A resume replays the cached value for a matching key even if the producer has since changed, so version the key (`"scan-v2"`) whenever its inputs or meaning change. Journaled producers are best-effort at-least-once and may run again across crashes or concurrent same-key callers, so keep side effects idempotent.
- `ctx.session`: The session returned by `joinSession`. It refuses calls that start or resume a factory run. Call `extensions_manage` with `operation: "guide"` to read more about the session APIs.
- `ctx.signal`: Cooperative cancellation signal for extension work and subprocesses.
- `ctx.factory(...)`: Always rejects because nested factories are not supported.

Factory-owned subagents are intentionally hidden from `read_agent` and `write_agent`. Use the factory observability APIs instead.

### Subagent calls

`ctx.agent(prompt, options?)` spawns one factory-scoped subagent and awaits it. Without a schema it resolves to the subagent's final text. With `options.schema` it resolves to the parsed JSON value.

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

See [factory-patterns.md](./factory-patterns.md) for composable orchestration patterns built on these primitives.

## Resource limits

Limits may be declared in `meta.limits` and overridden per invocation. All limits must be positive when present.

- `maxConcurrentSubagents`: Positive integer concurrent-subagent cap. Additional subagents wait in a queue. Queueing applies backpressure and does not fail the run.
- `maxTotalSubagents`: Positive integer cumulative admission cap. An attempted subagent beyond the cap ends the attempt with failure kind `maxTotalSubagents`.
- `timeoutSeconds`: Positive finite number of seconds, including positive fractions, capped at `2_147_483.647`. It measures accumulated active-execution time across attempts, including the extension body, subprocess waits, queued-agent waits, and sleeps. Time between attempts is excluded. The timeout is soft because already-running work may take time to stop. Its failure kind is `timeoutSeconds`.
- `maxAiCredits`: Positive finite AI-credit budget for the whole run's factory subagent subtree, including descendants. AI credits are GitHub Copilot's universal usage metric. This is a soft, post-paid ceiling, so completed or parallel turns can settle above it before the run stops. Accounting is fail-closed: an accounting failure stops a budgeted run rather than allowing untracked use. Its failure kind is `maxAiCredits`.

`maxTotalSubagents`, `timeoutSeconds`, and `maxAiCredits` use reject-and-retry semantics. A rejected attempt ends with run status `error` and `failure.type` set to `factory_limit_reached`. The failed run keeps its ID, arguments, journal, and accounting. Resume the run with a raised limit when additional work is approved. Previously consumed resources still count.

## Run and resume

Run by registered name or handle:

```ts
const run = await session.factory.run("review-changed", {
    args: { files: ["src/a.ts"] },
    limits: { maxAiCredits: 3 },
});

if (run.status === "completed") {
    console.log(run.result);
} else {
    console.error(`run ${run.runId} ended as ${run.status}`, run.failure ?? run.error);
}
```

The name overload is:

```ts
session.factory.run(
    name: string,
    options?: { args?: JsonValue; limits?: FactoryLimits },
): Promise<FactoryRunResult>;
```

Resume by run ID without resending the name or arguments:

```ts
const run = await session.factory.resume(runId, {
    limits: { maxAiCredits: 6 },
});
```

The signature is:

```ts
session.factory.resume(
    runId: string,
    options?: { limits?: FactoryLimits },
): Promise<FactoryRunResult>;
```

Both resolve with the run envelope (`FactoryRunResult`) for **every** outcome — `completed`, `error`, `halted`, and `cancelled` alike. Inspect `status` and read `result` only when the run completed; a limit breach carries a typed `failure`. SDK-initiated `run` and `resume` do not request permission, so they have no declined outcome. The model's `run_factory` tool requests permission before the durable row exists; declining it creates no run row. An SDK-initiated run is refused only when the session already has its maximum number of active top-level runs. Pre-execution resume failures throw `FactoryResumeError`, whose `code` is one of `not_found`, `non_resumable`, `already_active`, `factory_already_running`, `factory_limits_invalid`, `factory_session_disposed`, `factory_storage_unavailable`, or `factory_storage_corrupt`.

An agent that no longer has a prior run's ID in context can recover it with `factories_manage` and `operation: "runs"`, which lists the session's factory runs with their IDs and statuses. This matters for resume: a run that reached a limit keeps its journal, so resuming it replays completed work for free, while restarting it from scratch pays for that work twice.

The agent-facing `run_factory` tool has exactly two input branches:

```ts
{ name: string; args?: JsonValue; limits?: FactoryLimits }
{ resumeFromRunId: string; limits?: FactoryLimits }
```

## Authoring a factory from inside a session

The agent-facing `factories_manage` tool writes a factory into a session-scoped extension at runtime with `operation: "author"`. The rules above all apply, plus one constraint that does not affect an extension author.

**The `run` body is self-contained.** It is emitted verbatim into a generated module as a single async function expression. It closes over nothing: not the conversation that authored it, and not any authoring-time binding. Only its own locals, its `ctx` parameter, and standard Node and JavaScript globals are in scope, so every schema, constant, and helper must be defined *inside* the function. The generated module imports the SDK itself; the expression cannot add static `import` statements or use `require`. Load anything else with a dynamic `await import("...")` in the body.

```js
async ({ args, agent, phase }) => {
    // Defined inside — there is no outer scope to close over.
    const VERDICT = { type: "object", properties: { real: { type: "boolean" } }, required: ["real"] };

    phase("Inspect");
    const finding = await agent(`Name one likely bug in ${args.file ?? "the code"}.`, {
        label: "inspector",
    });
    if (!finding) return { finding: null, real: false };

    phase("Verify");
    const verdict = await agent(`Is this a real bug? Claim: ${finding}`, {
        label: "verifier",
        schema: VERDICT,
    });
    return { finding, real: verdict?.real === true };
};
```

Authoring registers the factory but does not run it. Invoke it afterwards with `run_factory`. Use `factories_manage` with `operation: "list"` to see the factories already registered in the session and `operation: "inspect"` to read one factory's description, phases, and limits before running it.

## Observe a run

The calling session can inspect its own factory runs:

```ts
const runs = await session.factory.listRuns();
const detail = await session.factory.getRunDetail(runId);
const page = await session.factory.getRunProgress(runId, {
    phaseId,
    afterSeq,
    beforeSeq,
    limit,
});
```

- `listRuns()` returns the newest default page of this session's durable factory runs.
- `getRunDetail(runId)` returns phases, prompt-safe agent summaries, and the latest progress page.
- `getRunProgress(runId, options?)` pages progress forward, backward, by phase, or from the latest tail.

`getRun(runId)` reads the latest run envelope, and `cancel(runId)` cancels a run and returns its terminal envelope.

`waitForRun(runId, options?)` resolves with the terminal envelope once the run settles into `completed`, `error`, `halted`, or `cancelled`, and resolves immediately when it has already settled:

```ts
const settled = await session.factory.waitForRun(runId);
if (settled.status === "completed") {
    console.log(settled.result);
}
```

It watches `factory.run_updated` and re-reads the durable envelope on each invalidation, collapsing a burst of events into a single in-flight read. A low-frequency periodic re-read runs alongside the subscription, so a dropped or missing invalidation degrades into a slightly late resolution rather than an unbounded wait. Pass a `signal` to stop waiting:

```ts
const controller = new AbortController();
setTimeout(() => controller.abort(), 30_000);
const settled = await session.factory.waitForRun(runId, { signal: controller.signal });
```

Aborting rejects the wait and has no effect on the run, which keeps executing — use `cancel(runId)` to actually stop it. Because a terminal envelope is final, the resolved value never changes afterwards. `isFactoryRunTerminal(status)` exposes the same terminal-status test for callers driving their own loop.

Listen for the ephemeral `factory.run_updated` event. Its `{ runId, revision }` payload is an invalidation signal. Re-read the desired API when a newer monotonic revision arrives.

Revisions cover durable lifecycle, accounting, phase, agent, and progress changes. Continuous read-time fields can change without a new revision. These include `observedAt`, active-time calculations, live counts, and a live agent's status or prompt-safe activity text. Factory prompts are never exposed by these APIs. A run is visible only through the session that owns it.
