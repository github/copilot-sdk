/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { OrchestrationRunResult } from "./generated/rpc.js";
import type { CopilotSession } from "./session.js";
import type { OrchestrationMeta } from "./types.js";

declare const orchestrationHandleBrand: unique symbol;

/**
 * Conservative JSON shape language accepted for structured orchestration agent output.
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
 * @experimental Part of the experimental Agent Orchestrations surface and may
 * change or be removed in future SDK or CLI releases.
 */
export type OrchestrationJsonSchema = Record<string, unknown>;

/**
 * Options for one orchestration-scoped subagent call.
 *
 * @experimental Part of the experimental Agent Orchestrations surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface OrchestrationAgentOptions {
    label?: string;
    schema?: OrchestrationJsonSchema;
    model?: string;
}

/**
 * Options for a durable orchestration step.
 *
 * @experimental Part of the experimental Agent Orchestrations surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface OrchestrationStepOptions {
    /** Skip the journal and always invoke the producer. */
    volatile?: boolean;
}

/**
 * One stage in a per-item orchestration pipeline.
 *
 * @experimental Part of the experimental Agent Orchestrations surface and may
 * change or be removed in future SDK or CLI releases.
 */
export type OrchestrationPipelineStage<TInput = unknown, TResult = unknown> = (
    previous: TInput,
    item: unknown,
    index: number
) => Promise<TResult> | TResult;

/**
 * Context passed to an extension-authored orchestration body.
 *
 * @experimental Part of the experimental Agent Orchestrations surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface OrchestrationContext<TArgs = unknown> {
    /** Spawn and await one orchestration-scoped subagent. */
    agent(prompt: string, options?: OrchestrationAgentOptions): Promise<unknown>;
    /** Memoize an arbitrary producer under a stable author-supplied key. */
    step<TResult>(
        key: string,
        producer: () => Promise<TResult> | TResult,
        options?: OrchestrationStepOptions
    ): Promise<TResult>;
    /** Run thunks concurrently, returning null for a thunk that throws. */
    parallel<TResult>(
        thunks: Array<() => Promise<TResult> | TResult>
    ): Promise<Array<TResult | null>>;
    /** Run each item through every stage without barriers between stages. */
    pipeline(items: unknown[], ...stages: OrchestrationPipelineStage[]): Promise<unknown[]>;
    /** Start a named orchestration progress phase. */
    phase(title: string): void;
    /** Emit a orchestration progress line. */
    log(message: string): void;
    /** Reject because nested orchestrations are not supported. */
    orchestration(name: string, args?: unknown): Promise<unknown>;
    /** Caller-supplied input, forwarded verbatim. */
    args: TArgs;
    /** The same full session instance returned by `joinSession`. */
    session: CopilotSession;
    /** Cooperative cancellation signal for the current orchestration run. */
    signal: AbortSignal;
}

/**
 * Definition accepted by {@link defineOrchestration}.
 *
 * @experimental Part of the experimental Agent Orchestrations surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface OrchestrationDefinition<TArgs = unknown, TResult = unknown> {
    meta: OrchestrationMeta;
    run(context: OrchestrationContext<TArgs>): Promise<TResult>;
}

/**
 * Opaque reusable reference to a defined orchestration.
 *
 * @experimental Part of the experimental Agent Orchestrations surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface OrchestrationHandle<TArgs = unknown, TResult = unknown> {
    readonly meta: OrchestrationMeta;
    readonly [orchestrationHandleBrand]: {
        readonly args: TArgs;
        readonly result: TResult;
    };
}

/**
 * Options for invoking a orchestration.
 *
 * @experimental Part of the experimental Agent Orchestrations surface and may
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
 * Friendly orchestration API exposed on a session.
 *
 * @experimental Part of the experimental Agent Orchestrations surface and may
 * change or be removed in future SDK or CLI releases.
 */
export interface SessionOrchestrationApi {
    run(name: string, options: RunOptions & { background: true }): Promise<OrchestrationRunResult>;
    run<TResult = unknown>(
        name: string,
        options?: RunOptions & { background?: false }
    ): Promise<TResult>;
    run<TResult = unknown>(
        name: string,
        options?: RunOptions
    ): Promise<TResult | OrchestrationRunResult>;
    run<TArgs, TResult>(
        orchestration: OrchestrationHandle<TArgs, TResult>,
        options: RunOptions<TArgs> & { background: true }
    ): Promise<OrchestrationRunResult>;
    run<TArgs, TResult>(
        orchestration: OrchestrationHandle<TArgs, TResult>,
        options?: RunOptions<TArgs> & { background?: false }
    ): Promise<TResult>;
    run<TArgs, TResult>(
        orchestration: OrchestrationHandle<TArgs, TResult>,
        options?: RunOptions<TArgs>
    ): Promise<TResult | OrchestrationRunResult>;
    /** Read the latest durable envelope for a orchestration run. */
    getRun(runId: string): Promise<OrchestrationRunResult>;
    /** Cancel a orchestration run and return its terminal envelope. */
    cancel(runId: string): Promise<OrchestrationRunResult>;
}

/**
 * Error thrown when a foreground orchestration run does not complete successfully.
 *
 * @experimental Part of the experimental Agent Orchestrations surface and may
 * change or be removed in future SDK or CLI releases.
 */
export class OrchestrationRunError extends Error {
    constructor(public readonly envelope: OrchestrationRunResult) {
        super(
            envelope.error ??
                envelope.reason ??
                `Orchestration run "${envelope.runId}" ended with status "${envelope.status}"`
        );
        this.name = "OrchestrationRunError";
    }
}

interface StoredOrchestration {
    meta: OrchestrationMeta;
    run(context: OrchestrationContext<unknown>): Promise<unknown>;
}

const orchestrationHandles = new WeakMap<object, StoredOrchestration>();

function validateLimits(meta: OrchestrationMeta): void {
    const limits = meta.limits;
    if (!limits) {
        return;
    }

    for (const field of ["maxConcurrentSubagents", "maxTotalSubagents"] as const) {
        const value = limits[field];
        if (value !== undefined && (!Number.isInteger(value) || value <= 0)) {
            throw new Error(`Orchestration limit "${field}" must be a positive integer`);
        }
    }

    if (limits.timeout !== undefined && (!Number.isFinite(limits.timeout) || limits.timeout <= 0)) {
        throw new Error('Orchestration limit "timeout" must be a positive number of milliseconds');
    }
}

/**
 * Defines an extension-authored orchestration and returns an opaque registration handle.
 *
 * @experimental Part of the experimental Agent Orchestrations surface and may
 * change or be removed in future SDK or CLI releases.
 */
export function defineOrchestration<TArgs = unknown, TResult = unknown>(
    definition: OrchestrationDefinition<TArgs, TResult>
): OrchestrationHandle<TArgs, TResult> {
    validateLimits(definition.meta);

    const stored: StoredOrchestration = {
        meta: definition.meta,
        run: definition.run as StoredOrchestration["run"],
    };
    const handle = Object.freeze({ meta: definition.meta }) as OrchestrationHandle<TArgs, TResult>;

    orchestrationHandles.set(handle, stored);
    return handle;
}

/** @internal */
export function getOrchestrationDefinition(handle: OrchestrationHandle): StoredOrchestration {
    const definition = orchestrationHandles.get(handle);
    if (!definition) {
        throw new Error("Invalid orchestration handle");
    }
    return definition;
}
