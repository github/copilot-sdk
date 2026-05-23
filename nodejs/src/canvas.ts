/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Extension-owned canvases declared via
 * `joinSession({ canvases: [createCanvas({...})] })`.
 *
 * The runtime sends provider callbacks directly as `canvas.open`,
 * `canvas.focus`, `canvas.reload`, `canvas.close`, and
 * `canvas.action.invoke` JSON-RPC requests. The SDK routes those requests by
 * `canvasId` to the in-process handlers bound by `createCanvas`.
 */

/** JSON Schema object used for canvas inputs and canvas-scoped tools. */
export type CanvasJsonSchema = Record<string, unknown>;

/** Tool definition exposed to a canvas instance. */
export interface CanvasToolDefinition {
    name: string;
    description: string;
    title?: string;
    parameters?: CanvasJsonSchema;
    overridesBuiltInTool?: boolean;
    skipPermission?: boolean;
    defer?: "auto" | "never";
}

/**
 * A single agent-callable action contributed by a canvas. Names MUST NOT
 * start with `canvas.` - that prefix is reserved for lifecycle verbs.
 */
export interface CanvasAgentActionDeclaration {
    /** Action identifier, unique within the canvas. */
    name: string;
    /** Description shown to the model when picking an action. */
    description?: string;
    /** Optional JSON Schema for the action's `input` payload. */
    inputSchema?: CanvasJsonSchema;
}

/** A single toolbar button contributed by a canvas. */
export interface CanvasToolbarItemDeclaration {
    /** Stable id used by the host to key the button. */
    id: string;
    /** User-visible label. */
    label: string;
    /** The `agentActions[].name` to dispatch when clicked. */
    actionName: string;
    /** Optional fixed input payload passed verbatim to the action handler. */
    input?: unknown;
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
    agentActions?: CanvasAgentActionDeclaration[];
    /** Static toolbar items rendered as host chrome. */
    toolbar?: CanvasToolbarItemDeclaration[];
}

/** Response returned from `onOpen`. */
export interface CanvasOpenResponse {
    /** URL the host should render. Optional for native canvases. */
    url?: string;
    /** Provider-supplied title shown in host chrome. */
    title?: string;
    /** Provider-supplied status text shown in host chrome. */
    status?: string;
    /** Toolbar items for host-rendered chrome. */
    toolbar?: CanvasToolbarItemDeclaration[];
    /** Tools available to the canvas instance. */
    tools?: CanvasToolDefinition[];
}

/** Host capabilities passed to canvas callbacks. */
export interface CanvasHostContext {
    capabilities?: {
        canvases?: boolean;
    };
}

/** Context handed to a canvas's `onOpen` handler. */
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

/** Context handed to a canvas's `onAction` handler. */
export interface CanvasActionContext {
    /** Session that invoked the action. */
    sessionId: string;
    /** Extension id that owns the canvas. */
    extensionId: string;
    /** Canvas id targeted by the action. */
    canvasId: string;
    /** Instance id targeted by the action. */
    instanceId: string;
    /** Action name from `CanvasAgentActionDeclaration.name`. */
    actionName: string;
    /** Validated `input` payload, shaped by the action's `inputSchema`. */
    input: unknown;
    /** Host capabilities supplied by the runtime. */
    host?: CanvasHostContext;
}

/** Context handed to a canvas's lifecycle hooks (`onFocus`, `onClose`, `onReload`). */
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

/** Structured error returned from canvas handlers. */
export class CanvasError extends Error {
    constructor(
        public readonly code: string,
        message: string
    ) {
        super(message);
        this.name = "CanvasError";
    }

    /** Default error when an action is declared but no `onAction` is wired. */
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
    /** @see CanvasDeclaration.agentActions */
    agentActions?: CanvasAgentActionDeclaration[];
    /** @see CanvasDeclaration.toolbar */
    toolbar?: CanvasToolbarItemDeclaration[];

    /** Required. Open a new canvas instance. */
    onOpen: (ctx: CanvasOpenContext) => Promise<CanvasOpenResponse> | CanvasOpenResponse;

    /**
     * Optional. Handle a non-lifecycle action declared in `agentActions`.
     * If omitted, dispatched actions return `canvas_action_no_handler`.
     */
    onAction?: (ctx: CanvasActionContext) => Promise<unknown> | unknown;

    /** Optional. Canvas was brought to the foreground. */
    onFocus?: (ctx: CanvasLifecycleContext) => Promise<void> | void;

    /** Optional. Canvas was closed by the user or agent. */
    onClose?: (ctx: CanvasLifecycleContext) => Promise<void> | void;

    /** Optional. Host requested a reload. */
    onReload?: (ctx: CanvasLifecycleContext) => Promise<void> | void;
}

/** A registered canvas: declarative metadata + in-process handler closures. */
export class Canvas {
    readonly declaration: CanvasDeclaration;
    readonly onOpen: NonNullable<CanvasOptions["onOpen"]>;
    readonly onAction?: CanvasOptions["onAction"];
    readonly onFocus?: CanvasOptions["onFocus"];
    readonly onClose?: CanvasOptions["onClose"];
    readonly onReload?: CanvasOptions["onReload"];

    /** @internal */
    constructor(options: CanvasOptions) {
        this.declaration = {
            id: options.id,
            displayName: options.displayName,
            description: options.description,
            inputSchema: options.inputSchema,
            agentActions: options.agentActions,
            toolbar: options.toolbar,
        };
        this.onOpen = options.onOpen;
        this.onAction = options.onAction;
        this.onFocus = options.onFocus;
        this.onClose = options.onClose;
        this.onReload = options.onReload;
    }
}

/** Create a canvas declaration with bound in-process handlers. */
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
    actionName: "canvas.open" | "canvas.focus" | "canvas.close" | "canvas.reload" | string,
    params: CanvasActionInvokeParams | CanvasProviderRequestParams
): Promise<unknown> {
    switch (actionName) {
        case "canvas.open": {
            const result = await canvas.onOpen({
                sessionId: params.sessionId,
                extensionId: params.extensionId,
                canvasId: params.canvasId,
                instanceId: params.instanceId,
                input: params.input,
                host: params.host,
            });
            return result ?? {};
        }
        case "canvas.focus":
        case "canvas.close":
        case "canvas.reload": {
            const hook =
                actionName === "canvas.focus"
                    ? canvas.onFocus
                    : actionName === "canvas.close"
                      ? canvas.onClose
                      : canvas.onReload;
            if (!hook) return undefined;
            await hook({
                sessionId: params.sessionId,
                extensionId: params.extensionId,
                canvasId: params.canvasId,
                instanceId: params.instanceId,
                host: params.host,
            });
            return undefined;
        }
        default: {
            if (!canvas.onAction) {
                throw CanvasError.noHandler();
            }
            return canvas.onAction({
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
