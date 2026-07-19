# Agent Factories

Agent Factories are extension-authored, session-scoped workflows that coordinate subagents and durable steps. The API is experimental.

## Define and register a factory

Use `defineFactory` and pass the returned handle to `joinSession`:

```js
import { defineFactory, joinSession } from "@github/copilot-sdk/extension";

const reviewChanged = defineFactory({
    meta: {
        name: "review-changed",
        description: "Review changed files and verify the findings",
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

`defineFactory<TArgs, TResult>` accepts a `run(context)` function returning `Promise<TResult>`, where `TResult` is `JsonValue | void`. Objects, arrays, strings, numbers, booleans, and `null` are valid results. Returning `undefined` completes the factory with no result. Other non-JSON values are rejected.

## Factory context

The `run()` context provides:

- `ctx.runId`: Stable ID reused across resumed attempts.
- `ctx.args`: Invocation arguments.
- `ctx.agent(prompt, options?)`: Runs one factory-owned subagent. Options include `label`, `schema`, and `model`.
- `ctx.parallel(thunks)`: Runs thunks concurrently and returns `null` for a thunk that throws, except cooperative cancellation propagates.
- `ctx.pipeline(items, ...stages)`: Flows each item through every stage without a barrier between stages.
- `ctx.phase(title)`: Starts a named progress phase.
- `ctx.log(message)`: Appends a progress line.
- `ctx.step(key, producer, options?)`: Journals the producer's JSON result under a stable key so a resume replays it without re-running the producer. A journaled (default) producer must return a JSON-serializable value; `undefined` or a non-JSON value is rejected. Pass `{ volatile: true }` to bypass the journal and run the producer every time.
- `ctx.session`: The full session returned by `joinSession`.
- `ctx.signal`: Cooperative cancellation signal for extension work and subprocesses.
- `ctx.factory(...)`: Always rejects because nested factories are not supported.

Factory-owned subagents are intentionally hidden from `read_agent` and `write_agent`. Use the factory observability APIs instead.

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
const result = await session.factory.run("review-changed", {
    args: { files: ["src/a.ts"] },
    limits: { maxAiCredits: 3 },
});
```

The name overload is:

```ts
session.factory.run<TResult extends JsonValue | void>(
    name: string,
    options?: { args?: JsonValue; limits?: FactoryLimits },
): Promise<TResult>;
```

Resume by run ID without resending the name or arguments:

```ts
const result = await session.factory.resume(runId, {
    limits: { maxAiCredits: 6 },
});
```

The signature is:

```ts
session.factory.resume<TResult = JsonValue | void>(
    runId: string,
    options?: { limits?: FactoryLimits },
): Promise<TResult>;
```

Pre-execution resume failures throw `FactoryResumeError`. Its `code` is one of `not_found`, `non_resumable`, `already_active`, `reapproval_declined`, or `no_approval_provider`. A run that starts but does not complete successfully throws `FactoryRunError` with its terminal envelope.

The agent-facing `run_factory` tool has exactly two input branches:

```ts
{ name: string; args?: JsonValue; limits?: FactoryLimits }
{ resumeFromRunId: string; limits?: FactoryLimits }
```

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

- `listRuns()` returns summaries in durable creation order.
- `getRunDetail(runId)` returns phases, prompt-safe agent summaries, and the latest progress page.
- `getRunProgress(runId, options?)` pages progress forward, backward, by phase, or from the latest tail.

`getRun(runId)` reads the latest run envelope, and `cancel(runId)` cancels a run and returns its terminal envelope.

Listen for the ephemeral `factory.run_updated` event. Its `{ runId, revision }` payload is an invalidation signal. Re-read the desired API when a newer monotonic revision arrives.

Revisions cover durable lifecycle, accounting, phase, agent, and progress changes. Continuous read-time fields can change without a new revision. These include `observedAt`, active-time calculations, live counts, and a live agent's status or prompt-safe activity text. Factory prompts are never exposed by these APIs. A run is visible only through the session that owns it.
