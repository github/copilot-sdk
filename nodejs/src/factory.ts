/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type {
    FactoryGetRunProgressRequest,
    FactoryProgressPage,
    FactoryRunDetail,
    FactoryRunResult as WireFactoryRunResult,
    FactoryRunStatus,
    FactoryRunSummary,
} from "./generated/rpc.js";
import type { CopilotSession } from "./session.js";
import type { FactoryLimits, FactoryMeta } from "./types.js";

/**
 * The terminal envelope describing a factory run's outcome (status, result,
 * reason). Re-exported so consumers can name the type returned by
 * {@link SessionFactoryApi} methods.
 *
 * `result` is re-typed here rather than taken from the generated wire type. The
 * runtime returns any JSON value — including `null`, a string, a number, or an
 * array — but the schema models the field as an opaque node, which the
 * generator renders as an object. Narrowing the correction to this surface
 * keeps the `x-opaque-json` handling unchanged for every other consumer.
 */
export type FactoryRunResult = Omit<WireFactoryRunResult, "result"> & {
    /** Completed factory result. */
    result?: JsonValue;
};

export type {
    FactoryAgentSummary,
    FactoryPhaseStatus,
    FactoryPhaseObservation,
    FactoryProgressLine,
    FactoryProgressPage,
    FactoryRunDetail,
    FactoryRunStatus,
    FactoryRunSummary,
} from "./generated/rpc.js";

/**
 * Run statuses a factory run can no longer move away from.
 *
 * A run is either still in flight (`pending`, `running`) or settled into one of
 * these four. Terminal state is final: once written it is never reopened, so a
 * caller that observes one of these can stop watching the run.
 */
const FACTORY_TERMINAL_STATUSES: ReadonlySet<FactoryRunStatus> = new Set([
    "completed",
    "halted",
    "cancelled",
    "error",
]);

/**
 * Whether a factory run status is terminal.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export function isFactoryRunTerminal(status: FactoryRunStatus): boolean {
    return FACTORY_TERMINAL_STATUSES.has(status);
}

declare const factoryHandleBrand: unique symbol;

/** A value that can be represented losslessly on the SDK JSON wire. */
export type JsonValue =
    | null
    | boolean
    | number
    | string
    | JsonValue[]
    | { [key: string]: JsonValue };

/**
 * Conservative JSON shape language accepted for structured factory agent output.
 *
 * This is a best-effort structural guard used to decide whether a subagent's
 * structured output should be accepted or retried — **not** a full JSON Schema
 * validator. Only these keywords are honored: `type`, `required`, `enum`,
 * `const`, recursive `properties`/`items`, and `anyOf`/`oneOf`/`allOf`.
 *
 * Everything else is **ignored, not enforced**. In particular, string
 * constraints (`pattern`, `minLength`, `maxLength`, `format`), numeric ranges
 * (`minimum`, `maximum`), `additionalProperties`, and boolean (`true`/`false`)
 * schemas do not reject non-conforming output. `oneOf` is treated like `anyOf`
 * (at least one branch must match) rather than strict exactly-one. Author
 * schemas within this subset; do not rely on unsupported constraints for
 * correctness.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export type FactoryJsonSchema = { [key: string]: JsonValue };

/**
 * Options for one factory-scoped subagent call.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface FactoryAgentOptions {
    label?: string;
    schema?: FactoryJsonSchema;
    model?: string;
}

/**
 * Options for a durable factory step.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface FactoryStepOptions {
    /** Skip the journal and always invoke the producer. */
    volatile?: boolean;
}

/**
 * One stage in a per-item factory pipeline.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export type FactoryPipelineStage<TInput = unknown, TResult = unknown> = (
    previous: TInput,
    item: unknown,
    index: number
) => Promise<TResult> | TResult;

/**
 * Context passed to an extension-authored factory body.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface FactoryContext<TArgs extends JsonValue = JsonValue> {
    /** Stable identifier for the current factory run. */
    readonly runId: string;
    /** Spawn and await one factory-scoped subagent. */
    agent(prompt: string, options?: FactoryAgentOptions): Promise<unknown>;
    /** Memoize an arbitrary producer under a stable author-supplied key. */
    step(
        key: string,
        producer: () => Promise<JsonValue> | JsonValue,
        options?: FactoryStepOptions
    ): Promise<JsonValue>;
    /** Run thunks concurrently, returning null for a thunk that throws. */
    parallel<TResult>(
        thunks: Array<() => Promise<TResult> | TResult>
    ): Promise<Array<TResult | null>>;
    /** Run each item through every stage without barriers between stages. */
    pipeline(items: unknown[], ...stages: FactoryPipelineStage[]): Promise<unknown[]>;
    /** Start a named factory progress phase. */
    phase(title: string): void;
    /** Emit a factory progress line. */
    log(message: string): void;
    /** Reject because nested factories are not supported. */
    factory(name: string, args?: JsonValue): Promise<JsonValue | void>;
    /** Caller-supplied input, forwarded verbatim. */
    args: TArgs;
    /** The same full session instance returned by `joinSession`. */
    session: CopilotSession;
    /** Cooperative cancellation signal for the current factory run. */
    signal: AbortSignal;
}

/**
 * Definition accepted by {@link defineFactory}.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface FactoryDefinition<
    TArgs extends JsonValue = JsonValue,
    TResult extends JsonValue | void = JsonValue | void,
> {
    meta: FactoryMeta;
    run(context: FactoryContext<TArgs>): Promise<TResult>;
}

/**
 * Opaque reusable reference to a defined factory.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface FactoryHandle<
    TArgs extends JsonValue = JsonValue,
    TResult extends JsonValue | void = JsonValue | void,
> {
    readonly meta: FactoryMeta;
    readonly [factoryHandleBrand]: {
        readonly args: TArgs;
        readonly result: TResult;
    };
}

/**
 * Options for invoking a factory.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface RunOptions<TArgs extends JsonValue = JsonValue> {
    /** Input surfaced as `context.args`. */
    args?: TArgs;
    /** Optional per-invocation resource ceiling overrides. */
    limits?: FactoryLimits;
    /**
     * Prior run whose persisted identity, arguments, journal, and accounting should be resumed.
     *
     * @deprecated Use {@link SessionFactoryApi.resume} instead.
     */
    resumeFromRunId?: string;
}

/**
 * Options for resuming a factory run by ID.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface ResumeOptions {
    /** Optional per-invocation resource ceiling overrides. */
    limits?: FactoryLimits;
}

/**
 * Machine-readable pre-execution factory resume failure.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export type FactoryResumeErrorCode =
    | "not_found"
    | "non_resumable"
    | "already_active"
    | "reapproval_declined"
    | "no_approval_provider";

/**
 * Friendly factory API exposed on a session.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface SessionFactoryApi {
    /**
     * Run a registered factory and resolve with its run envelope.
     *
     * The envelope is returned for every outcome, including `error`, `halted`,
     * and `cancelled` — inspect `status` and read `result` only when the run
     * completed. A declined fresh run resolves with a terminal `cancelled`
     * envelope. Failures that occur before a run exists (such as an unknown
     * factory or an already-active session) still reject.
     */
    run(name: string, options?: RunOptions): Promise<FactoryRunResult>;
    run<TArgs extends JsonValue>(
        factory: FactoryHandle<TArgs, JsonValue | void>,
        options?: RunOptions<TArgs>
    ): Promise<FactoryRunResult>;
    /**
     * Resume a run from its persisted factory name, arguments, journal, and accounting.
     *
     * Resolves with the run envelope like {@link SessionFactoryApi.run}. A
     * pre-execution failure, including declined reapproval, rejects with
     * {@link FactoryResumeError}.
     */
    resume(runId: string, options?: ResumeOptions): Promise<FactoryRunResult>;
    /** Read the latest durable envelope for a factory run. */
    getRun(runId: string): Promise<FactoryRunResult>;
    /**
     * Wait for a run to settle and resolve with its terminal envelope.
     *
     * Resolves as soon as the run reaches `completed`, `error`, `halted`, or
     * `cancelled`, and resolves immediately when it has already settled. A
     * terminal envelope is final, so the resolved value never changes
     * afterwards.
     *
     * This watches the run's `factory.run_updated` invalidation events and
     * periodically re-reads the durable envelope so a missed event cannot
     * leave the wait hanging. Pass a `signal` to stop waiting; aborting rejects
     * and has no effect on the run itself, which keeps executing. Use
     * {@link SessionFactoryApi.cancel} to actually stop it.
     */
    waitForRun(runId: string, options?: { signal?: AbortSignal }): Promise<FactoryRunResult>;
    /** List this session's durable factory runs in creation order. */
    listRuns(): Promise<FactoryRunSummary[]>;
    /** Read durable phases, direct agents, and the latest progress tail for a run. */
    getRunDetail(runId: string): Promise<FactoryRunDetail>;
    /** Page durable progress forward, backward, or from the latest tail. */
    getRunProgress(
        runId: string,
        options?: Omit<FactoryGetRunProgressRequest, "runId">
    ): Promise<FactoryProgressPage>;
    /** Cancel a factory run and return its terminal envelope. */
    cancel(runId: string): Promise<FactoryRunResult>;
}

/**
 * Error thrown when a factory cannot be resumed before execution begins.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export class FactoryResumeError extends Error {
    constructor(
        public readonly code: FactoryResumeErrorCode,
        message: string
    ) {
        super(message);
        this.name = "FactoryResumeError";
    }
}

interface StoredFactory {
    meta: FactoryMeta;
    run(context: FactoryContext): Promise<JsonValue | void>;
}

const factoryHandles = new WeakMap<object, StoredFactory>();

/** Maximum accepted factory timeout in seconds, derived from Node's maximum timer delay. */
const MAX_FACTORY_TIMEOUT_SECONDS = 2_147_483.647;
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

function validateLimits(meta: FactoryMeta): void {
    const limits = meta.limits;
    if (!limits) {
        return;
    }

    for (const field of ["maxConcurrentSubagents", "maxTotalSubagents"] as const) {
        const value = limits[field];
        if (value !== undefined && (!Number.isInteger(value) || value <= 0)) {
            throw new Error(`Factory limit "${field}" must be a positive integer`);
        }
    }

    if (
        limits.timeoutSeconds !== undefined &&
        (!Number.isFinite(limits.timeoutSeconds) || limits.timeoutSeconds <= 0)
    ) {
        throw new Error(
            'Factory limit "timeoutSeconds" must be a positive, finite number of seconds'
        );
    }
    if (
        limits.timeoutSeconds !== undefined &&
        limits.timeoutSeconds > MAX_FACTORY_TIMEOUT_SECONDS
    ) {
        throw new Error(
            `Factory limit "timeoutSeconds" must not exceed ${MAX_FACTORY_TIMEOUT_SECONDS} seconds`
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
                'Factory limit "maxAiCredits" must be a positive, finite number that rounds to a safe positive integer nano-AIU ceiling'
            );
        }
    }
}

function validatePhases(meta: FactoryMeta): void {
    const titles = new Set<string>();
    for (const phase of meta.phases) {
        if (phase.title.trim().length === 0) {
            throw new Error("Factory phase titles must not be empty");
        }
        if (titles.has(phase.title)) {
            throw new Error(`Factory phase title "${phase.title}" is declared more than once`);
        }
        titles.add(phase.title);
    }
}

/**
 * Defines an extension-authored factory and returns an opaque registration handle.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export function defineFactory<
    TArgs extends JsonValue = JsonValue,
    TResult extends JsonValue | void = JsonValue | void,
>(definition: FactoryDefinition<TArgs, TResult>): FactoryHandle<TArgs, TResult> {
    // Snapshot before validating so post-registration mutation of the caller's
    // object cannot slip past the authoring-boundary checks.
    const meta = deepFreeze(structuredClone(definition.meta));
    validateLimits(meta);
    validatePhases(meta);

    const stored: StoredFactory = {
        meta,
        run: definition.run,
    };
    const handle = Object.freeze({ meta }) as FactoryHandle<TArgs, TResult>;

    factoryHandles.set(handle, stored);
    return handle;
}

/** @internal */
export function getFactoryDefinition(handle: FactoryHandle): StoredFactory {
    const definition = factoryHandles.get(handle);
    if (!definition) {
        throw new Error("Invalid factory handle");
    }
    return definition;
}
