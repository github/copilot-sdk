/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type {
    WorkflowGetRunProgressRequest,
    WorkflowListRunsRequest,
    WorkflowListRunsResult,
    WorkflowProgressPage,
    WorkflowRunDetail,
    WorkflowRunResult,
    WorkflowRunStatus,
    WorkflowRunSummary,
} from "./generated/rpc.js";
import type { ContextTier } from "./generated/session-events.js";
import type { CopilotSession } from "./session.js";
import type { JsonValue } from "./factory.js";

export type { WorkflowRunResult };
export type {
    WorkflowAgentSummary,
    WorkflowPhaseStatus,
    WorkflowPhaseObservation,
    WorkflowProgressLine,
    WorkflowProgressPage,
    WorkflowRunDetail,
    WorkflowRunStatus,
    WorkflowRunSummary,
} from "./generated/rpc.js";

/**
 * Options for paging durable workflow runs.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export type WorkflowListRunsOptions = WorkflowListRunsRequest;

/**
 * A page of durable workflow runs and its paging metadata.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export type WorkflowRunsPage = WorkflowListRunsResult;

/**
 * Run statuses a workflow run can no longer move away from.
 *
 * A run is either still in flight (`pending`, `running`) or its current attempt
 * has settled into one of these states. A paused run can later start a new
 * attempt under the same run ID, but callers waiting on the current attempt can
 * stop watching once they observe it.
 */
const WORKFLOW_TERMINAL_STATUSES: ReadonlySet<WorkflowRunStatus> = new Set([
    "completed",
    "halted",
    "paused",
    "cancelled",
    "error",
]);

/**
 * Whether a workflow run status is terminal.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export function isWorkflowRunTerminal(status: WorkflowRunStatus): boolean {
    return WORKFLOW_TERMINAL_STATUSES.has(status);
}

declare const workflowHandleBrand: unique symbol;

/**
 * Conservative JSON shape language accepted by the Dynamic Workflows surface, for
 * both structured workflow agent output and a workflow's declared `argsSchema`.
 *
 * This is a best-effort structural guard — used to decide whether a subagent's
 * structured output should be accepted or retried, and whether a caller's
 * workflow `args` match the declared shape — **not** a full JSON Schema
 * validator. Only these keywords are honored: `type`, `required`, `enum`,
 * `const`, recursive `properties`/`items`, and `anyOf`/`oneOf`/`allOf`. A `type`
 * is one of `null`, `boolean`, `integer`, `number`, `string`, `array`, or
 * `object`, or a non-empty array of those (for example `["object", "null"]`).
 *
 * Everything else is **ignored, not enforced**. In particular, string
 * constraints (`pattern`, `minLength`, `maxLength`, `format`), numeric ranges
 * (`minimum`, `maximum`), and `additionalProperties` do not reject
 * non-conforming output. Boolean schemas are outside this accepted shape.
 * `oneOf` is treated like `anyOf` (at least one branch must match) rather than
 * strict exactly-one. Author schemas within this subset; do not rely on
 * unsupported constraints for correctness.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export type WorkflowJsonSchema = { [key: string]: JsonValue };

/**
 * Static resource ceilings declared by a workflow before it runs.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface WorkflowLimits {
    /** Maximum number of workflow subagents that may run concurrently. Must be positive when present. */
    maxConcurrentSubagents?: number;
    /** Maximum total number of workflow subagents that may be spawned. Must be positive when present. */
    maxTotalSubagents?: number;
    /** Maximum AI credits consumed by workflow subagents and descendants. This post-paid ceiling is soft. */
    maxAiCredits?: number;
    /**
     * Maximum accumulated active-execution time, in seconds. Active execution includes the entire extension body,
     * subprocess waits, queued-agent waits, and sleeps. The limit is armed from the remaining headroom when a run
     * resumes; time between attempts is not counted. Must be finite and positive when present.
     */
    timeoutSeconds?: number;
}

/**
 * Registration metadata for an extension-authored workflow.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface WorkflowMeta {
    /** Stable workflow name used for invocation. */
    name: string;
    /** Human-readable workflow description. */
    description: string;
    /** Display metadata for the progress phases the workflow may report. */
    phases: Array<{ title: string; detail?: string }>;
    /**
     * Optional declared shape of the arguments this workflow expects as `ctx.args`.
     *
     * The runtime records and validates this schema when the workflow contribution
     * is registered. Workflow bodies should still validate any semantic constraints
     * they depend on because the public `session.workflow.run(...)` API forwards
     * arguments directly.
     */
    argsSchema?: WorkflowJsonSchema;
    /** Optional resource ceilings presented before execution. */
    limits?: WorkflowLimits;
}

/**
 * Options for one workflow-scoped subagent call.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface WorkflowAgentOptions {
    label?: string;
    schema?: WorkflowJsonSchema;
    model?: string;
    reasoningEffort?: string;
    contextTier?: ContextTier;
    agent?: string;
}

export const WORKFLOW_AGENT_OPTION_KEYS = [
    "label",
    "schema",
    "model",
    "reasoningEffort",
    "contextTier",
    "agent",
] as const;

/**
 * Options for a durable workflow step.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface WorkflowStepOptions {
    /** Skip the journal and always invoke the producer. */
    volatile?: boolean;
}

/**
 * Per-invocation workflow resource ceiling overrides.
 *
 * An omitted field preserves the existing/default ceiling, a number replaces
 * it, and `null` explicitly makes that dimension unlimited.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface WorkflowLimitOverrides {
    maxConcurrentSubagents?: number | null;
    maxTotalSubagents?: number | null;
    maxAiCredits?: number | null;
    timeoutSeconds?: number | null;
}

/**
 * One stage in a per-item workflow pipeline.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export type WorkflowPipelineStage<TInput = unknown, TResult = unknown> = (
    previous: TInput,
    item: unknown,
    index: number
) => Promise<TResult> | TResult;

/**
 * Context passed to an extension-authored workflow body.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface WorkflowContext<TArgs extends JsonValue = JsonValue> {
    /** Stable identifier for the current workflow run. */
    readonly runId: string;
    /** Spawn and await one workflow-scoped subagent. */
    agent(prompt: string, options?: WorkflowAgentOptions): Promise<unknown>;
    /** Memoize an arbitrary producer under a stable author-supplied key. */
    step(
        key: string,
        producer: () => Promise<JsonValue> | JsonValue,
        options?: WorkflowStepOptions
    ): Promise<JsonValue>;
    /**
     * Pause this run at a durable, one-shot checkpoint.
     *
     * The first attempt to reach a key pauses and aborts cooperatively. A
     * resumed attempt returns from the same key and continues.
     */
    pause(key: string): Promise<void>;
    /**
     * Run thunks concurrently and await all of them.
     *
     * A thunk that throws becomes `null` in the result array, so one failed
     * item does not lose the rest. Cancellation and hard runtime failures
     * (`ResponseError`, `ConnectionError`) are the exception: those propagate
     * and reject the whole call, because they mean the run itself is in
     * trouble rather than one item having failed.
     */
    parallel<TResult>(
        thunks: Array<() => Promise<TResult> | TResult>
    ): Promise<Array<TResult | null>>;
    /**
     * Run each item through every stage without barriers between stages.
     *
     * A stage that throws drops that item to `null` and skips its remaining
     * stages. As with {@link WorkflowContext.parallel}, cancellation and hard
     * runtime failures propagate instead of being recorded per item.
     */
    pipeline(items: unknown[], ...stages: WorkflowPipelineStage[]): Promise<unknown[]>;
    /** Start a named workflow progress phase. */
    phase(title: string): void;
    /** Emit a workflow progress line. */
    log(message: string): void;
    /** Reject because nested workflows are not supported. */
    workflow(name: string, args?: JsonValue): Promise<JsonValue | void>;
    /** Caller-supplied input, forwarded verbatim. */
    args: TArgs;
    /**
     * The session instance returned by `joinSession`. It refuses calls that
     * start, resume, or pause a workflow run.
     */
    session: CopilotSession;
    /** Cooperative cancellation signal for the current workflow run. */
    signal: AbortSignal;
}

/**
 * Definition accepted by {@link defineWorkflow}.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface WorkflowDefinition<
    TArgs extends JsonValue = JsonValue,
    TResult extends JsonValue | void = JsonValue | void,
> {
    meta: WorkflowMeta;
    run(context: WorkflowContext<TArgs>): Promise<TResult>;
}

/**
 * A deeply immutable view of a value.
 *
 * `defineWorkflow` deep-freezes the metadata it stores, so the handle's view of
 * it has to be readonly all the way down or `handle.meta.name = "..."` and
 * `handle.meta.phases.push(...)` would compile and then throw at runtime.
 */
type DeepReadonly<T> = T extends (infer U)[]
    ? readonly DeepReadonly<U>[]
    : T extends object
      ? { readonly [K in keyof T]: DeepReadonly<T[K]> }
      : T;

/**
 * Opaque reusable reference to a defined workflow.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface WorkflowHandle<
    TArgs extends JsonValue = JsonValue,
    TResult extends JsonValue | void = JsonValue | void,
> {
    readonly meta: DeepReadonly<WorkflowMeta>;
    readonly [workflowHandleBrand]: {
        readonly args: TArgs;
        readonly result: TResult;
    };
}

/**
 * Options for invoking a workflow.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface WorkflowRunOptions<TArgs extends JsonValue = JsonValue> {
    /** Input surfaced as `context.args`. */
    args?: TArgs;
    /** Optional per-invocation resource ceiling overrides. */
    limits?: WorkflowLimitOverrides;
    /** Whether to notify the originating session when the workflow completes. */
    notifyOnComplete?: boolean;
    /** Whether to emit workflow phase names to the session transcript. */
    logPhaseNames?: boolean;
}

/**
 * Options for resuming a workflow run by ID.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface WorkflowResumeOptions {
    /** Optional per-invocation resource ceiling overrides. */
    limits?: WorkflowLimitOverrides;
    /** Whether to notify the originating session when the workflow completes. */
    notifyOnComplete?: boolean;
    /** Whether to emit workflow phase names to the session transcript. */
    logPhaseNames?: boolean;
}

/**
 * Machine-readable pre-execution workflow resume failure.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export type WorkflowResumeErrorCode =
    | "not_found"
    | "non_resumable"
    | "workflow_run_not_resumable"
    | "already_active"
    | "workflow_already_running"
    | "workflow_limits_invalid"
    | "workflow_session_disposed"
    | "workflow_storage_unavailable"
    | "workflow_storage_corrupt";

/**
 * Friendly workflow API exposed on a session.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface SessionWorkflowApi {
    /**
     * Run a registered workflow and resolve with its run envelope.
     *
     * The envelope is returned for every outcome, including `error`, `halted`,
     * `paused`, and `cancelled` — inspect `status` and read `result` only when
     * the run completed. `paused` settles the current attempt, but the same
     * durable run can later resume under its existing run ID. SDK-initiated
     * runs do not request permission, so they have no declined outcome.
     * Failures that occur before a run exists (such as an unknown workflow or
     * attempting to start a run while the session is at its active top-level
     * run limit) still reject.
     */
    run(name: string, options?: WorkflowRunOptions): Promise<WorkflowRunResult>;
    run<TArgs extends JsonValue>(
        workflow: WorkflowHandle<TArgs, JsonValue | void>,
        options?: WorkflowRunOptions<TArgs>
    ): Promise<WorkflowRunResult>;
    /**
     * Resume a run from its persisted workflow name, arguments, journal, and accounting.
     *
     * Resolves with the run envelope like {@link SessionWorkflowApi.run}.
     * SDK-initiated resumes do not request permission. A pre-execution failure
     * with a documented resume code rejects with {@link WorkflowResumeError}.
     */
    resume(runId: string, options?: WorkflowResumeOptions): Promise<WorkflowRunResult>;
    /** Read the latest durable envelope for a workflow run. */
    getRun(runId: string): Promise<WorkflowRunResult>;
    /**
     * Wait for the current attempt to settle and resolve with its envelope.
     *
     * Resolves as soon as the run reaches `completed`, `error`, `halted`,
     * `paused`, or `cancelled`, and resolves immediately when the current
     * attempt has already settled. A `paused` envelope is an attempt-level
     * snapshot: resuming the same durable run can later change the envelope
     * returned by {@link SessionWorkflowApi.getRun}.
     *
     * This watches the runtime's `factory.run_updated` compatibility event and
     * periodically re-reads the durable envelope so a missed event cannot
     * leave the wait hanging. Pass a `signal` to stop waiting; aborting rejects
     * and has no effect on the run itself, which keeps executing. Use
     * {@link SessionWorkflowApi.cancel} to actually stop it.
     */
    waitForRun(runId: string, options?: { signal?: AbortSignal }): Promise<WorkflowRunResult>;
    /**
     * List the newest default page of this session's durable workflow runs.
     *
     * This backwards-compatible overload returns only the runs array. Pass
     * paging options to receive the full page, including its cursors and
     * truncation metadata.
     */
    listRuns(): Promise<WorkflowRunSummary[]>;
    /**
     * Page this session's durable workflow runs.
     *
     * `afterSeq` and `beforeSeq` are exclusive cursors. The result includes
     * `oldestSeq`, `newestSeq`, `hasMoreNewer`, and `omittedOlder` so callers
     * can continue paging without using the raw RPC client.
     */
    listRuns(options: WorkflowListRunsOptions): Promise<WorkflowRunsPage>;
    /** Read durable phases, direct agents, and the latest progress tail for a run. */
    getRunDetail(runId: string): Promise<WorkflowRunDetail>;
    /** Page durable progress forward, backward, or from the latest tail. */
    getRunProgress(
        runId: string,
        options?: Omit<WorkflowGetRunProgressRequest, "runId">
    ): Promise<WorkflowProgressPage>;
    /** Pause a running workflow attempt and return its `paused` envelope. */
    pause(runId: string): Promise<WorkflowRunResult>;
    /** Cancel a workflow run and return its terminal envelope. */
    cancel(runId: string): Promise<WorkflowRunResult>;
}

/**
 * Error thrown when a workflow cannot be resumed before execution begins.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export class WorkflowResumeError extends Error {
    constructor(
        public readonly code: WorkflowResumeErrorCode,
        message: string
    ) {
        super(message);
        this.name = "WorkflowResumeError";
    }
}

interface StoredWorkflow {
    meta: WorkflowMeta;
    run(context: WorkflowContext): Promise<JsonValue | void>;
}

const workflowHandles = new WeakMap<object, StoredWorkflow>();

/** Maximum accepted workflow timeout in seconds, derived from Node's maximum timer delay. */
const MAX_WORKFLOW_TIMEOUT_SECONDS = 2_147_483.647;
const NANO_AIU_PER_AIU = 1_000_000_000;

function deepFreeze<T>(value: T): T {
    if (value !== null && typeof value === "object" && !Object.isFrozen(value)) {
        Object.freeze(value);
        for (const nested of Object.values(value)) {
            deepFreeze(nested);
        }
    }
    return value;
}

function validateLimits(meta: WorkflowMeta): void {
    const limits = meta.limits;
    if (!limits) {
        return;
    }

    for (const field of ["maxConcurrentSubagents", "maxTotalSubagents"] as const) {
        const value = limits[field];
        if (value !== undefined && (!Number.isInteger(value) || value <= 0)) {
            throw new Error(`Workflow limit "${field}" must be a positive integer`);
        }
    }

    if (
        limits.timeoutSeconds !== undefined &&
        (!Number.isFinite(limits.timeoutSeconds) || limits.timeoutSeconds <= 0)
    ) {
        throw new Error(
            'Workflow limit "timeoutSeconds" must be a positive, finite number of seconds'
        );
    }
    if (
        limits.timeoutSeconds !== undefined &&
        limits.timeoutSeconds > MAX_WORKFLOW_TIMEOUT_SECONDS
    ) {
        throw new Error(
            `Workflow limit "timeoutSeconds" must not exceed ${MAX_WORKFLOW_TIMEOUT_SECONDS} seconds`
        );
    }

    if (limits.maxAiCredits !== undefined) {
        const maxNanoAiu = Math.round(limits.maxAiCredits * NANO_AIU_PER_AIU);
        if (
            !Number.isFinite(limits.maxAiCredits) ||
            limits.maxAiCredits <= 0 ||
            !Number.isSafeInteger(maxNanoAiu) ||
            maxNanoAiu < 1
        ) {
            throw new Error(
                'Workflow limit "maxAiCredits" must be a positive, finite number that rounds to a safe positive integer nano-AIU ceiling'
            );
        }
    }
}

function validatePhases(meta: WorkflowMeta): void {
    const titles = new Set<string>();
    for (const phase of meta.phases) {
        if (phase.title.trim().length === 0) {
            throw new Error("Workflow phase titles must not be empty");
        }
        if (titles.has(phase.title)) {
            throw new Error(`Workflow phase title "${phase.title}" is declared more than once`);
        }
        titles.add(phase.title);
    }
}

/**
 * Defines an extension-authored workflow and returns an opaque registration handle.
 *
 * @experimental Part of the experimental Dynamic Workflows surface and may
 * change or be removed in future SDK or CLI releases.
 */
export function defineWorkflow<
    TArgs extends JsonValue = JsonValue,
    TResult extends JsonValue | void = JsonValue | void,
>(definition: WorkflowDefinition<TArgs, TResult>): WorkflowHandle<TArgs, TResult> {
    // Snapshot before validating so post-registration mutation of the caller's
    // object cannot slip past the authoring-boundary checks.
    const meta = deepFreeze(structuredClone(definition.meta));
    validateLimits(meta);
    validatePhases(meta);

    const stored: StoredWorkflow = {
        meta,
        run: definition.run,
    };
    const handle = Object.freeze({ meta }) as unknown as WorkflowHandle<TArgs, TResult>;

    workflowHandles.set(handle, stored);
    return handle;
}

/** @internal */
export function getWorkflowDefinition(handle: WorkflowHandle): StoredWorkflow {
    const definition = workflowHandles.get(handle);
    if (!definition) {
        throw new Error("Invalid workflow handle");
    }
    return definition;
}
