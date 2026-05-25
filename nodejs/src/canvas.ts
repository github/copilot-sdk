/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type {
    CanvasJsonSchema,
    CanvasProviderCloseRequest,
    CanvasProviderInvokeActionRequest,
    CanvasProviderOpenRequest,
    CanvasProviderOpenResult,
} from "./generated/rpc.js";

export type { CanvasJsonSchema, CanvasHostContext } from "./generated/rpc.js";

/**
 * Extension-owned canvases declared via
 * `joinSession({ canvases: [createCanvas({...})] })`.
 *
 * The runtime sends provider callbacks as `canvas.open`, `canvas.close`, and
 * `canvas.invokeAction` JSON-RPC requests via the codegen client session API
 * pipeline. The SDK routes those requests by `canvasId` to the in-process
 * handlers bound by `createCanvas`. Re-opening with an existing `instanceId`
 * is how the host focuses an existing panel; reload is a renderer-only concern.
 */

/*
 * Wire-contract types re-exported from codegen. These are the single source of
 * truth — the Zod schemas in copilot-agent-runtime drive the JSON Schema which
 * drives the codegen which produces these types.
 */

/** Response returned from a canvas `open` handler. */
export type CanvasOpenResponse = CanvasProviderOpenResult;

/** Context handed to a canvas's `open` handler. */
export type CanvasOpenContext = CanvasProviderOpenRequest;

/** Context handed to a canvas action handler. */
export type CanvasActionContext = CanvasProviderInvokeActionRequest;

/** Context handed to a canvas's `onClose` handler. */
export type CanvasLifecycleContext = CanvasProviderCloseRequest;

/**
 * A single agent-callable action contributed by a canvas. The metadata
 * (`name`, `description`, `inputSchema`) is serialized over the wire on
 * `session.create` / `session.resume`; the `handler` closure is stripped
 * before the declaration is sent and dispatched in-process by the SDK.
 *
 * Names MUST NOT start with `canvas.` — that prefix is reserved for
 * lifecycle verbs.
 */
export interface CanvasAction {
    /** Action identifier, unique within the canvas. */
    name: string;
    /** Description shown to the model when picking an action. */
    description?: string;
    /** Optional JSON Schema for the action's `input` payload. */
    inputSchema?: CanvasJsonSchema;
    /** Required per-action dispatch handler. */
    handler: (ctx: CanvasActionContext) => Promise<unknown> | unknown;
}

/**
 * Declarative metadata for a single canvas, serialized over the wire on
 * `session.create` / `session.resume`.
 */
export interface CanvasDeclaration {
    /** Canvas id, unique within the declaring connection. */
    id: string;
    /** Human-readable label shown in discovery and host UI chrome. */
    displayName: string;
    /** Short, single-sentence description shown to the agent in canvas catalogs. */
    description: string;
    /** Optional JSON Schema for the `input` payload accepted by `canvas.open`. */
    inputSchema?: CanvasJsonSchema;
    /** Agent-invocable actions exposed via `invoke_canvas_action`. */
    actions?: Omit<CanvasAction, "handler">[];
}

/** Structured error returned from canvas handlers. */
export class CanvasError extends Error {
    constructor(
        public readonly code: string,
        message: string
    ) {
        super(message);
        this.name = "CanvasError";
    }

    /** Default error when an action is declared but no `handler` is wired. */
    static noHandler(): CanvasError {
        return new CanvasError(
            "canvas_action_no_handler",
            "No handler implemented for this canvas action"
        );
    }
}

/**
 * Options accepted by {@link createCanvas}. Combines the declarative
 * {@link CanvasDeclaration} fields with the in-process handler closures.
 */
export interface CanvasOptions {
    /** @see CanvasDeclaration.id */
    id: string;
    /** @see CanvasDeclaration.displayName */
    displayName: string;
    /** @see CanvasDeclaration.description */
    description: string;
    /** @see CanvasDeclaration.inputSchema */
    inputSchema?: CanvasJsonSchema;
    /**
     * Agent-invocable actions exposed via `invoke_canvas_action`. Each action
     * carries its own required `handler`; the action's wire metadata
     * (`name`, `description`, `inputSchema`) is what reaches the runtime.
     */
    actions?: CanvasAction[];

    /** Required. Open a new canvas instance. */
    open: (ctx: CanvasOpenContext) => Promise<CanvasOpenResponse> | CanvasOpenResponse;

    /**
     * Optional. Notified when a canvas instance is closed by the user, the
     * agent, or the host. Fire-and-forget: the return value is ignored and
     * errors are logged but not surfaced to the runtime.
     */
    onClose?: (ctx: CanvasLifecycleContext) => Promise<void> | void;
}

/** A registered canvas: declarative metadata + in-process handler closures.
 *
 * Node intentionally uses a per-canvas factory pattern (mirroring
 * {@link https://github.com/github/copilot-sdk | `DefineTool`}'s co-location
 * ergonomics) where other SDKs (Rust, Python, Go, .NET) expose a single
 * `CanvasHandler` per session that switches on `canvasId`. Both shapes target
 * the same JSON-RPC wire protocol; the divergence is API ergonomics only.
 */
export class Canvas {
    readonly declaration: CanvasDeclaration;
    readonly open: NonNullable<CanvasOptions["open"]>;
    readonly onClose?: CanvasOptions["onClose"];
    /** @internal */
    readonly actionHandlers: Map<string, CanvasAction["handler"]>;

    /** @internal */
    constructor(options: CanvasOptions) {
        const actionHandlers = new Map<string, CanvasAction["handler"]>();
        const wireActions: Omit<CanvasAction, "handler">[] | undefined = options.actions?.map(
            ({ handler, ...wire }) => {
                actionHandlers.set(wire.name, handler);
                return wire;
            }
        );

        this.declaration = {
            id: options.id,
            displayName: options.displayName,
            description: options.description,
            inputSchema: options.inputSchema,
            actions: wireActions,
        };
        this.open = options.open;
        this.onClose = options.onClose;
        this.actionHandlers = actionHandlers;
    }
}

/** Create a canvas declaration with bound in-process handlers.
 *
 * Node intentionally uses this per-canvas factory pattern (mirroring
 * `DefineTool`'s co-location ergonomics) where other SDKs (Rust, Python, Go,
 * .NET) expose a single `CanvasHandler` per session that switches on
 * `canvasId`. Both shapes target the same JSON-RPC wire protocol.
 */
export function createCanvas(options: CanvasOptions): Canvas {
    return new Canvas(options);
}

