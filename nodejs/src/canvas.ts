/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Extension-owned canvases declared via
 * `joinSession({ canvases: [createCanvas({...})] })`.
 *
 * The runtime sends provider callbacks directly as `canvas.open`,
 * `canvas.close`, and `canvas.action.invoke` JSON-RPC requests. The SDK
 * routes those requests by `canvasId` to the in-process handlers bound by
 * `createCanvas`. Re-opening with an existing `instanceId` is how the host
 * focuses an existing panel; reload is a renderer-only concern.
 *
 * **Experimental.** All exports in this module are part of an experimental
 * wire-protocol surface and may change or be removed in future SDK or CLI
 * releases.
 */

/**
 * JSON Schema object used for canvas inputs.
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
 */
export type CanvasJsonSchema = Record<string, unknown>;

/**
 * A single agent-callable action contributed by a canvas. The metadata
 * (`name`, `description`, `inputSchema`) is serialized over the wire on
 * `session.create` / `session.resume`; the `handler` closure is stripped
 * before the declaration is sent and dispatched in-process by the SDK.
 *
 * Names MUST NOT start with `canvas.` — that prefix is reserved for
 * lifecycle verbs.
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
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
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
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

/**
 * Response returned from `open`.
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
 */
export interface CanvasOpenResponse {
    /** URL the host should render. Optional for native canvases. */
    url?: string;
    /** Provider-supplied title shown in host chrome. */
    title?: string;
    /** Provider-supplied status text shown in host chrome. */
    status?: string;
}

/**
 * Host capability flags carried by {@link CanvasHostContext.capabilities}.
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
 */
export interface CanvasHostCapabilities {
    /** Whether the host supports canvas rendering. */
    canvases?: boolean;
}

/**
 * Host capabilities passed to canvas callbacks.
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
 */
export interface CanvasHostContext {
    capabilities?: CanvasHostCapabilities;
}

/**
 * Context handed to a canvas's `open` handler.
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
 */
export interface CanvasOpenContext {
    /** Session that requested the canvas. */
    sessionId: string;
    /** Extension id that owns the canvas. */
    extensionId: string;
    /** Canvas id (matches the declaring `CanvasDeclaration.id`). */
    canvasId: string;
    /** Stable instance id supplied by the runtime. */
    instanceId: string;
    /** Validated `input` payload, shaped by `CanvasDeclaration.inputSchema`. */
    input: unknown;
    /** Host capabilities supplied by the runtime. */
    host?: CanvasHostContext;
}

/**
 * Context handed to a canvas action handler.
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
 */
export interface CanvasActionContext {
    /** Session that invoked the action. */
    sessionId: string;
    /** Extension id that owns the canvas. */
    extensionId: string;
    /** Canvas id targeted by the action. */
    canvasId: string;
    /** Instance id targeted by the action. */
    instanceId: string;
    /** Action name from `CanvasAction.name`. */
    actionName: string;
    /** Validated `input` payload, shaped by the action's `inputSchema`. */
    input: unknown;
    /** Host capabilities supplied by the runtime. */
    host?: CanvasHostContext;
}

/**
 * Context handed to a canvas's `onClose` handler.
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
 */
export interface CanvasLifecycleContext {
    /** Session owning the canvas instance. */
    sessionId: string;
    /** Extension id that owns the canvas. */
    extensionId: string;
    /** Canvas id (matches the declaring `CanvasDeclaration.id`). */
    canvasId: string;
    /** Instance id this lifecycle event applies to. */
    instanceId: string;
    /** Host capabilities supplied by the runtime. */
    host?: CanvasHostContext;
}

/**
 * Structured error returned from canvas handlers.
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
 */
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
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
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
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
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
 *
 * @experimental Part of an experimental wire-protocol surface; may change or
 * be removed in future SDK or CLI releases.
 */
export function createCanvas(options: CanvasOptions): Canvas {
    return new Canvas(options);
}

/** @internal */
export interface CanvasProviderRequestParams {
    sessionId: string;
    extensionId: string;
    canvasId: string;
    instanceId: string;
    input?: unknown;
    host?: CanvasHostContext;
}

/** @internal */
export interface CanvasActionInvokeParams extends CanvasProviderRequestParams {
    actionName: string;
}

/**
 * Dispatch a direct `canvas.*` provider request to the matching {@link Canvas}
 * handler.
 *
 * @internal
 */
export async function dispatchCanvasProviderRequest(
    canvas: Canvas,
    actionName: "canvas.open" | "canvas.close" | string,
    params: CanvasActionInvokeParams | CanvasProviderRequestParams
): Promise<unknown> {
    switch (actionName) {
        case "canvas.open": {
            const result = await canvas.open({
                sessionId: params.sessionId,
                extensionId: params.extensionId,
                canvasId: params.canvasId,
                instanceId: params.instanceId,
                input: params.input,
                host: params.host,
            });
            return result ?? {};
        }
        case "canvas.close": {
            if (canvas.onClose) {
                await canvas.onClose({
                    sessionId: params.sessionId,
                    extensionId: params.extensionId,
                    canvasId: params.canvasId,
                    instanceId: params.instanceId,
                    host: params.host,
                });
            }
            return undefined;
        }
        default: {
            const perAction = canvas.actionHandlers.get(actionName);
            if (!perAction) {
                throw CanvasError.noHandler();
            }
            return perAction({
                sessionId: params.sessionId,
                extensionId: params.extensionId,
                canvasId: params.canvasId,
                instanceId: params.instanceId,
                actionName,
                input: params.input,
                host: params.host,
            });
        }
    }
}
