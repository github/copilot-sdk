/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type {
    FactoryAgentSummary,
    FactoryGetRunProgressRequest,
    FactoryPhaseStatus,
    FactoryPhaseObservation,
    FactoryProgressLine,
    FactoryProgressPage,
    FactoryRunDetail,
    FactoryRunResult,
    FactoryRunSummary,
} from "./generated/rpc.js";
import type { CopilotSession } from "./session.js";
import type { FactoryLimits, FactoryMeta } from "./types.js";

/**
 * The terminal envelope describing a factory run's outcome (status, result,
 * reason). Re-exported so consumers can name the type returned by
 * {@link SessionFactoryApi} methods and carried on {@link FactoryRunError}.
 */
export type {
    FactoryAgentSummary,
    FactoryPhaseStatus,
    FactoryPhaseObservation,
    FactoryProgressLine,
    FactoryProgressPage,
    FactoryRunDetail,
    FactoryRunResult,
    FactoryRunSummary,
} from "./generated/rpc.js";

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

/** Machine-readable pre-execution factory resume failure. */
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
    run<TResult extends JsonValue | void = JsonValue | void>(
        name: string,
        options?: RunOptions
    ): Promise<TResult>;
    run<TArgs extends JsonValue, TResult extends JsonValue | void>(
        factory: FactoryHandle<TArgs, TResult>,
        options?: RunOptions<TArgs>
    ): Promise<TResult>;
    /**
     * Resume a run from its persisted factory name, arguments, journal, and accounting.
     *
     * `TResult` is an unchecked caller hint because a run ID does not carry its result type.
     */
    resume<TResult = JsonValue | void>(runId: string, options?: ResumeOptions): Promise<TResult>;
    /** Read the latest durable envelope for a factory run. */
    getRun(runId: string): Promise<FactoryRunResult>;
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
 * Error thrown when a foreground factory run does not complete successfully.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export class FactoryRunError extends Error {
    constructor(public readonly envelope: FactoryRunResult) {
        super(
            envelope.error ??
                envelope.reason ??
                `Factory run "${envelope.runId}" ended with status "${envelope.status}"`
        );
        this.name = "FactoryRunError";
    }
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
    validateLimits(definition.meta);
    validatePhases(definition.meta);

    const stored: StoredFactory = {
        meta: definition.meta,
        run: definition.run,
    };
    const handle = Object.freeze({ meta: definition.meta }) as FactoryHandle<TArgs, TResult>;

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
