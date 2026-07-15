/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { WorkflowRunResult } from "./generated/rpc.js";
import type { CopilotSession } from "./session.js";
import type { WorkflowMeta } from "./types.js";

declare const workflowHandleBrand: unique symbol;

/**
 * Conservative JSON shape language accepted for structured workflow agent output.
 *
 * Supports `type`, `required`, `enum`, `const`, recursive `properties`/`items`,
 * and `anyOf`/`oneOf`/`allOf`. Other JSON Schema keywords are ignored.
 */
export type WorkflowJsonSchema = Record<string, unknown>;

/** Options for one workflow-scoped subagent call. */
export interface WorkflowAgentOptions {
    label?: string;
    schema?: WorkflowJsonSchema;
    model?: string;
}

/** Options for a durable workflow step. */
export interface WorkflowStepOptions {
    /** Skip the journal and always invoke the producer. */
    volatile?: boolean;
}

/** One stage in a per-item workflow pipeline. */
export type WorkflowPipelineStage<TInput = unknown, TResult = unknown> = (
    previous: TInput,
    item: unknown,
    index: number
) => Promise<TResult> | TResult;

/** Context passed to an extension-authored workflow body. */
export interface WorkflowContext<TArgs = unknown> {
    /** Spawn and await one workflow-scoped subagent. */
    agent(prompt: string, options?: WorkflowAgentOptions): Promise<unknown>;
    /** Memoize an arbitrary producer under a stable author-supplied key. */
    step<TResult>(
        key: string,
        producer: () => Promise<TResult> | TResult,
        options?: WorkflowStepOptions
    ): Promise<TResult>;
    /** Run thunks concurrently, returning null for a thunk that throws. */
    parallel<TResult>(thunks: Array<() => Promise<TResult>>): Promise<Array<TResult | null>>;
    /** Run each item through every stage without barriers between stages. */
    pipeline(items: unknown[], ...stages: WorkflowPipelineStage[]): Promise<unknown[]>;
    /** Start a named workflow progress phase. */
    phase(title: string): void;
    /** Emit a workflow progress line. */
    log(message: string): void;
    /** Invoke another registered workflow as a child run. */
    workflow(name: string, args?: unknown): Promise<unknown>;
    /** Caller-supplied input, forwarded verbatim. */
    args: TArgs;
    /** The same full session instance returned by `joinSession`. */
    session: CopilotSession;
    /** Cooperative cancellation signal for the current workflow run. */
    signal: AbortSignal;
}

/** Definition accepted by {@link defineWorkflow}. */
export interface WorkflowDefinition<TArgs = unknown, TResult = unknown> {
    meta: WorkflowMeta;
    run(context: WorkflowContext<TArgs>): Promise<TResult>;
}

/** Opaque reusable reference to a defined workflow. */
export interface WorkflowHandle<TArgs = unknown, TResult = unknown> {
    readonly meta: WorkflowMeta;
    readonly [workflowHandleBrand]: {
        readonly args: TArgs;
        readonly result: TResult;
    };
}

/** Options for invoking a workflow. */
export interface RunOptions<TArgs = unknown> {
    /** Input surfaced as `context.args`. */
    args?: TArgs;
    /** Return once the approved run starts instead of awaiting completion. */
    background?: boolean;
    /** Prior run whose journal and progress should seed this run. */
    resumeFromRunId?: string;
}

/** Friendly workflow API exposed on a session. */
export interface SessionWorkflowApi {
    run<TArgs, TResult>(
        workflow: WorkflowHandle<TArgs, TResult>,
        options: RunOptions<TArgs> & { background: true }
    ): Promise<WorkflowRunResult>;
    run<TArgs, TResult>(
        workflow: WorkflowHandle<TArgs, TResult>,
        options?: RunOptions<TArgs> & { background?: false }
    ): Promise<TResult>;
    run<TArgs, TResult>(
        workflow: WorkflowHandle<TArgs, TResult>,
        options?: RunOptions<TArgs>
    ): Promise<TResult | WorkflowRunResult>;
    run(name: string, options: RunOptions & { background: true }): Promise<WorkflowRunResult>;
    run<TResult = unknown>(
        name: string,
        options?: RunOptions & { background?: false }
    ): Promise<TResult>;
    run<TResult = unknown>(
        name: string,
        options?: RunOptions
    ): Promise<TResult | WorkflowRunResult>;
}

/** Error thrown when a foreground workflow run does not complete successfully. */
export class WorkflowRunError extends Error {
    constructor(public readonly envelope: WorkflowRunResult) {
        super(
            envelope.error ??
                envelope.reason ??
                `Workflow run "${envelope.runId}" ended with status "${envelope.status}"`
        );
        this.name = "WorkflowRunError";
    }
}

interface StoredWorkflow {
    meta: WorkflowMeta;
    run(context: WorkflowContext<unknown>): Promise<unknown>;
}

const workflowDefinitions = new Map<string, StoredWorkflow>();
const workflowHandles = new WeakMap<object, StoredWorkflow>();

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

    if (limits.timeout !== undefined && (!Number.isFinite(limits.timeout) || limits.timeout <= 0)) {
        throw new Error('Workflow limit "timeout" must be a positive number of milliseconds');
    }
}

/**
 * Defines an extension-authored workflow and returns an opaque registration handle.
 */
export function defineWorkflow<TArgs = unknown, TResult = unknown>(
    definition: WorkflowDefinition<TArgs, TResult>
): WorkflowHandle<TArgs, TResult> {
    validateLimits(definition.meta);

    const stored: StoredWorkflow = {
        meta: definition.meta,
        run: definition.run as StoredWorkflow["run"],
    };
    const handle = Object.freeze({ meta: definition.meta }) as WorkflowHandle<TArgs, TResult>;

    workflowDefinitions.set(definition.meta.name, stored);
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
