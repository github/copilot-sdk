/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Canvas V1.1 — extension-owned canvases declared via
 * `joinSession({ canvases: [createCanvas({...})] })`.
 *
 * The on-the-wire declaration shape mirrors the runtime's `CanvasDeclaration`
 * interface verbatim. The `createCanvas` helper packages the declaration with
 * in-process handler closures; the SDK serializes the declaration onto
 * `session.create` / `session.resume` and routes incoming
 * `canvas.action.invoke` dispatches by `(canvasId, actionName)` back to the
 * handlers.
 *
 * The wire RPC method is still `hostExtension.invoke` (runtime preserves the
 * legacy name); inside, `method === "canvas.action.invoke"` identifies canvas
 * dispatches. The runtime synthesizes an internal
 * `implementationId = "v1.1.<extensionId>/<canvasId>"`, but the SDK ignores
 * it and routes purely on `params.canvasId` + `params.actionName`.
 */

/**
 * A single agent-callable action contributed by a canvas. Names MUST NOT
 * start with `canvas.` — that prefix is reserved for the lifecycle verbs
 * `canvas.{open,focus,close,reload}`.
 */
export interface CanvasAgentActionDeclaration {
    /** Action identifier, unique within the canvas. */
    name: string;
    /** Description shown to the model when picking an action. */
    description: string;
    /** Optional JSON Schema for the action's `input` payload. */
    inputSchema?: Record<string, unknown>;
}

/**
 * A single toolbar button contributed by a canvas. The host canvas chrome
 * renders these and dispatches `actionName` with optional `input` when
 * clicked. `actionName` may be a reserved `canvas.*` verb (e.g.
 * `canvas.reload`) — the runtime routes those to the matching lifecycle
 * method.
 */
export interface CanvasToolbarItemDeclaration {
    /** Stable id used by the host to key the button. */
    id: string;
    /** User-visible label. */
    label: string;
    /** Optional icon identifier; semantics are host-defined. */
    icon?: string;
    /** Optional tooltip shown on hover. */
    tooltip?: string;
    /** The `agentActions[].name` (or reserved `canvas.*` verb) to dispatch. */
    actionName: string;
    /** Optional fixed input payload passed verbatim to the action handler. */
    input?: unknown;
}

/**
 * Declarative metadata for a single canvas, serialized over the wire on
 * `session.create` / `session.resume`. The declaring connection becomes the
 * live provider for dispatched canvas operations targeting this `id` for the
 * lifetime of the connection; re-declaring the same `id` on resume replaces
 * the prior declaration.
 */
export interface CanvasDeclaration {
    /** Canvas id, unique within the declaring connection. */
    id: string;
    /** Human-readable label shown in `discover_canvases` and host UI chrome. */
    displayName?: string;
    /** One-line description shown in `discover_canvases` for agent reasoning. */
    description?: string;
    /**
     * Optional JSON Schema for the `input` payload accepted by `canvas.open`.
     * The runtime validates incoming `open_canvas` calls against this;
     * handlers never see malformed input.
     */
    inputSchema?: Record<string, unknown>;
    /** Static toolbar items rendered as host chrome. */
    toolbar?: CanvasToolbarItemDeclaration[];
    /** Agent-invocable actions exposed via `invoke_canvas_action`. */
    agentActions?: CanvasAgentActionDeclaration[];
}

/**
 * Response returned from `onOpen`. The extension's URL is embedded by the
 * host in its webview surface when the host advertises the `canvas.webview`
 * capability.
 */
export interface CanvasOpenResponse {
    /** URL the host should embed. Optional for canvases with no visual surface. */
    url?: string;
    /**
     * Stable per-instance identifier the extension can correlate with its own
     * state. The host echoes this back on subsequent lifecycle calls.
     */
    instanceId?: string;
}

/** Context handed to a canvas's `onOpen` handler. */
export interface CanvasOpenContext {
    /** Session that requested the canvas. */
    sessionId: string;
    /** Canvas id (matches the declaring `CanvasDeclaration.id`). */
    canvasId: string;
    /** Validated `input` payload, shaped by `CanvasDeclaration.inputSchema`. */
    input: unknown;
    /** Toolbar items declared on the canvas, passed through for convenience. */
    toolbar?: CanvasToolbarItemDeclaration[];
}

/** Context handed to a canvas's `onAction` handler. */
export interface CanvasActionContext {
    /** Session that invoked the action. */
    sessionId: string;
    /** Canvas id targeted by the action. */
    canvasId: string;
    /** Instance id targeted by the action. */
    instanceId: string;
    /** Action name from `CanvasAgentActionDeclaration.name`. */
    actionName: string;
    /** Validated `input` payload, shaped by the action's `inputSchema`. */
    input: unknown;
}

/** Context handed to a canvas's lifecycle hooks (`onFocus`, `onClose`, `onReload`). */
export interface CanvasLifecycleContext {
    /** Session owning the canvas instance. */
    sessionId: string;
    /** Canvas id (matches the declaring `CanvasDeclaration.id`). */
    canvasId: string;
    /** Instance id this lifecycle event applies to. */
    instanceId: string;
}

/**
 * Structured error returned from canvas handlers. Serialized into the
 * `canvas.action.invoke` error envelope.
 *
 * Reserved codes:
 * - `canvas_action_no_handler` — action declared but no `onAction` provided
 * - `canvas_input_invalid` — input failed schema validation (runtime emits)
 */
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
 * {@link CanvasDeclaration} fields with the in-process handler closures
 * the SDK invokes on `canvas.action.invoke` dispatch.
 */
export interface CanvasOptions {
    /** @see CanvasDeclaration.id */
    id: string;
    /** @see CanvasDeclaration.displayName */
    displayName?: string;
    /** @see CanvasDeclaration.description */
    description?: string;
    /** @see CanvasDeclaration.inputSchema */
    inputSchema?: Record<string, unknown>;
    /** @see CanvasDeclaration.agentActions */
    agentActions?: CanvasAgentActionDeclaration[];
    /** @see CanvasDeclaration.toolbar */
    toolbar?: CanvasToolbarItemDeclaration[];

    /**
     * Required. Open a new canvas instance. Return its URL (if any) and an
     * extension-owned instance id (if any).
     */
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

    /** Optional. Host requested a reload (e.g. user hit refresh). */
    onReload?: (ctx: CanvasLifecycleContext) => Promise<void> | void;
}

/**
 * A registered canvas: declarative metadata + in-process handler closures.
 *
 * Construct via {@link createCanvas}. The {@link declaration} is serialized
 * onto the wire (handlers are dropped — they're not transferable); the
 * handlers are retained in the SDK's per-session registry and invoked by
 * `canvas.action.invoke` dispatch keyed by `(canvasId, actionName)`.
 */
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
            toolbar: options.toolbar,
            agentActions: options.agentActions,
        };
        this.onOpen = options.onOpen;
        this.onAction = options.onAction;
        this.onFocus = options.onFocus;
        this.onClose = options.onClose;
        this.onReload = options.onReload;
    }
}

/**
 * Create a canvas declaration with bound in-process handlers. Pass the result
 * to `joinSession({ canvases: [...] })` (or the client `createSession` /
 * `resumeSession` `canvases` field). The SDK serializes
 * {@link Canvas.declaration} onto `session.create` / `session.resume` and
 * routes incoming `canvas.action.invoke` dispatches back to the handlers.
 *
 * @example
 * ```typescript
 * import { joinSession, createCanvas } from "@github/copilot-sdk/extension";
 *
 * const counter = createCanvas({
 *   id: "counter",
 *   displayName: "Counter",
 *   description: "A trivial counter canvas",
 *   agentActions: [{ name: "increment", description: "Add one" }],
 *   onOpen: async (ctx) => ({ url: `http://localhost:3000/${ctx.canvasId}` }),
 *   onAction: async (ctx) => {
 *     if (ctx.actionName === "increment") return { value: 1 };
 *   },
 * });
 *
 * await joinSession({ canvases: [counter] });
 * ```
 */
export function createCanvas(options: CanvasOptions): Canvas {
    return new Canvas(options);
}

// ---------------------------------------------------------------------------
// Internal dispatch helpers (consumed by client.ts / session.ts).
// ---------------------------------------------------------------------------

/**
 * Inner envelope of a `hostExtension.invoke` request when the dispatched
 * method is `canvas.action.invoke`. Field names mirror the runtime contract.
 *
 * @internal
 */
export interface CanvasActionInvokeParams {
    canvasId: string;
    instanceId?: string;
    actionName: string;
    input?: unknown;
    toolbar?: CanvasToolbarItemDeclaration[];
}

/**
 * Reserved lifecycle action names. Any other `actionName` routes to
 * {@link Canvas.onAction}.
 *
 * @internal
 */
export const RESERVED_CANVAS_ACTIONS = {
    open: "canvas.open",
    focus: "canvas.focus",
    close: "canvas.close",
    reload: "canvas.reload",
} as const;

/**
 * Dispatch a `canvas.action.invoke` payload to the matching {@link Canvas}'s
 * handler. Returns the value the handler produced (for `onOpen`/`onAction`)
 * or `undefined` (for lifecycle hooks). Throws {@link CanvasError} when the
 * canvas declares no handler for the action.
 *
 * @internal
 */
export async function dispatchCanvasAction(
    canvas: Canvas,
    sessionId: string,
    params: CanvasActionInvokeParams
): Promise<unknown> {
    switch (params.actionName) {
        case RESERVED_CANVAS_ACTIONS.open: {
            const result = await canvas.onOpen({
                sessionId,
                canvasId: params.canvasId,
                input: params.input,
                toolbar: params.toolbar,
            });
            return result ?? {};
        }
        case RESERVED_CANVAS_ACTIONS.focus:
        case RESERVED_CANVAS_ACTIONS.close:
        case RESERVED_CANVAS_ACTIONS.reload: {
            const hook =
                params.actionName === RESERVED_CANVAS_ACTIONS.focus
                    ? canvas.onFocus
                    : params.actionName === RESERVED_CANVAS_ACTIONS.close
                      ? canvas.onClose
                      : canvas.onReload;
            if (!hook) return undefined;
            const ctx: CanvasLifecycleContext = {
                sessionId,
                canvasId: params.canvasId,
                instanceId: params.instanceId ?? "",
            };
            await hook(ctx);
            return undefined;
        }
        default: {
            if (!canvas.onAction) {
                throw CanvasError.noHandler();
            }
            return canvas.onAction({
                sessionId,
                canvasId: params.canvasId,
                instanceId: params.instanceId ?? "",
                actionName: params.actionName,
                input: params.input,
            });
        }
    }
}
