/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { FactoryRunResult } from "./generated/rpc.js";
import type { CopilotSession } from "./session.js";
import type { FactoryMeta } from "./types.js";

/**
 * The terminal envelope describing a factory run's outcome (status, result,
 * reason). Re-exported so consumers can name the type returned by
 * {@link SessionFactoryApi} methods and carried on {@link FactoryRunError}.
 */
export type { FactoryRunResult } from "./generated/rpc.js";

declare const factoryHandleBrand: unique symbol;

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
export type FactoryJsonSchema = Record<string, unknown>;

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
export interface FactoryContext<TArgs = unknown> {
    /** Spawn and await one factory-scoped subagent. */
    agent(prompt: string, options?: FactoryAgentOptions): Promise<unknown>;
    /** Memoize an arbitrary producer under a stable author-supplied key. */
    step<TResult>(
        key: string,
        producer: () => Promise<TResult> | TResult,
        options?: FactoryStepOptions
    ): Promise<TResult>;
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
    factory(name: string, args?: unknown): Promise<unknown>;
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
export interface FactoryDefinition<TArgs = unknown, TResult = unknown> {
    meta: FactoryMeta;
    run(context: FactoryContext<TArgs>): Promise<TResult>;
}

/**
 * Opaque reusable reference to a defined factory.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface FactoryHandle<TArgs = unknown, TResult = unknown> {
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
export interface RunOptions<TArgs = unknown> {
    /** Input surfaced as `context.args`. */
    args?: TArgs;
    /** Return once the approved run starts instead of awaiting completion. */
    background?: boolean;
    /** Prior run whose journal and progress should seed this run. */
    resumeFromRunId?: string;
}

/**
 * Friendly factory API exposed on a session.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface SessionFactoryApi {
    run(name: string, options: RunOptions & { background: true }): Promise<FactoryRunResult>;
    run<TResult = unknown>(
        name: string,
        options?: RunOptions & { background?: false }
    ): Promise<TResult>;
    run<TResult = unknown>(name: string, options?: RunOptions): Promise<TResult | FactoryRunResult>;
    run<TArgs, TResult>(
        factory: FactoryHandle<TArgs, TResult>,
        options: RunOptions<TArgs> & { background: true }
    ): Promise<FactoryRunResult>;
    run<TArgs, TResult>(
        factory: FactoryHandle<TArgs, TResult>,
        options?: RunOptions<TArgs> & { background?: false }
    ): Promise<TResult>;
    run<TArgs, TResult>(
        factory: FactoryHandle<TArgs, TResult>,
        options?: RunOptions<TArgs>
    ): Promise<TResult | FactoryRunResult>;
    /** Read the latest durable envelope for a factory run. */
    getRun(runId: string): Promise<FactoryRunResult>;
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

interface StoredFactory {
    meta: FactoryMeta;
    run(context: FactoryContext<unknown>): Promise<unknown>;
}

const factoryHandles = new WeakMap<object, StoredFactory>();

/**
 * Maximum accepted factory timeout in milliseconds (2^31-1). Node clamps
 * `setTimeout` delays above this to ~1ms, so a larger value would invert the
 * declared timeout into an immediate halt.
 */
const MAX_FACTORY_TIMEOUT_MS = 2_147_483_647;

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

    if (limits.timeout !== undefined && (!Number.isFinite(limits.timeout) || limits.timeout <= 0)) {
        throw new Error('Factory limit "timeout" must be a positive number of milliseconds');
    }
    // Node clamps setTimeout delays above 2^31-1 ms to ~1ms, which would make a
    // large timeout halt the run almost immediately. Reject it up front.
    if (limits.timeout !== undefined && limits.timeout > MAX_FACTORY_TIMEOUT_MS) {
        throw new Error(
            `Factory limit "timeout" must not exceed ${MAX_FACTORY_TIMEOUT_MS} milliseconds (~24.8 days)`
        );
    }
}

/**
 * Defines an extension-authored factory and returns an opaque registration handle.
 *
 * @experimental Part of the experimental Agent Factories surface and may
 * change or be removed in future SDK or CLI releases.
 */
export function defineFactory<TArgs = unknown, TResult = unknown>(
    definition: FactoryDefinition<TArgs, TResult>
): FactoryHandle<TArgs, TResult> {
    validateLimits(definition.meta);

    const stored: StoredFactory = {
        meta: definition.meta,
        run: definition.run as StoredFactory["run"],
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
