/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Copilot Session - represents a single conversation session with the Copilot CLI.
 * @module session
 */

import type { MessageConnection } from "vscode-jsonrpc/node.js";
import { ConnectionError, ResponseError } from "vscode-jsonrpc/node.js";
import { createSessionRpc } from "./generated/rpc.js";
import { getTraceContext } from "./telemetry.js";
import type {
    CommandHandler,
    ElicitationHandler,
    ElicitationParams,
    ElicitationResult,
    ElicitationRequest,
    InputOptions,
    MessageOptions,
    PermissionHandler,
    PermissionRequest,
    PermissionRequestResult,
    ReasoningEffort,
    SectionTransformFn,
    SessionCapabilities,
    SessionEvent,
    SessionEventHandler,
    SessionEventPayload,
    SessionEventType,
    SessionHooks,
    SessionUiApi,
    Tool,
    ToolHandler,
    TraceContextProvider,
    TypedSessionEventHandler,
    UserInputHandler,
    UserInputRequest,
    UserInputResponse,
} from "./types.js";

export const NO_RESULT_PERMISSION_V2_ERROR =
    "Permission handlers cannot return 'no-result' when connected to a protocol v2 server.";

/** Assistant message event - the final response from the assistant. */
export type AssistantMessageEvent = Extract<SessionEvent, { type: "assistant.message" }>;

/**
 * Represents a single conversation session with the Copilot CLI.
 *
 * A session maintains conversation state, handles events, and manages tool execution.
 * Sessions are created via {@link CopilotClient.createSession} or resumed via
 * {@link CopilotClient.resumeSession}.
 *
 * @example
 * ```typescript
 * const session = await client.createSession({ model: "gpt-4" });
 *
 * // Subscribe to events
 * session.on((event) => {
 *   if (event.type === "assistant.message") {
 *     console.log(event.data.content);
 *   }
 * });
 *
 * // Send a message and wait for completion
 * await session.sendAndWait({ prompt: "Hello, world!" });
 *
 * // Clean up
 * await session.disconnect();
 * ```
 */
export class CopilotSession {
    private eventHandlers: Set<SessionEventHandler> = new Set();
    private typedEventHandlers: Map<SessionEventType, Set<(event: SessionEvent) => void>> =
        new Map();
    private toolHandlers: Map<string, ToolHandler> = new Map();
    private commandHandlers: Map<string, CommandHandler> = new Map();
    private permissionHandler?: PermissionHandler;
    private userInputHandler?: UserInputHandler;
    private elicitationHandler?: ElicitationHandler;
    private hooks?: SessionHooks;
    private transformCallbacks?: Map<string, SectionTransformFn>;
    private _rpc: ReturnType<typeof createSessionRpc> | null = null;
    private traceContextProvider?: TraceContextProvider;
    private _capabilities: SessionCapabilities = {};

    /**
     * Creates a new CopilotSession instance.
     *
     * @param sessionId - The unique identifier for this session
     * @param connection - The JSON-RPC message connection to the Copilot CLI
     * @param workspacePath - Path to the session workspace directory (when infinite sessions enabled)
     * @param traceContextProvider - Optional callback to get W3C Trace Context for outbound RPCs
     * @internal This constructor is internal. Use {@link CopilotClient.createSession} to create sessions.
     */
    constructor(
        public readonly sessionId: string,
        private connection: MessageConnection,
        private _workspacePath?: string,
        traceContextProvider?: TraceContextProvider
    ) {
        this.traceContextProvider = traceContextProvider;
    }

    /**
     * Typed session-scoped RPC methods.
     */
    get rpc(): ReturnType<typeof createSessionRpc> {
        if (!this._rpc) {
            this._rpc = createSessionRpc(this.connection, this.sessionId);
        }
        return this._rpc;
    }

    /**
     * Path to the session workspace directory when infinite sessions are enabled.
     * Contains checkpoints/, plan.md, and files/ subdirectories.
     * Undefined if infinite sessions are disabled.
     */
    get workspacePath(): string | undefined {
        return this._workspacePath;
    }

    /**
     * Host capabilities reported when the session was created or resumed.
     * Use this to check feature support before calling capability-gated APIs.
     */
    get capabilities(): SessionCapabilities {
        return this._capabilities;
    }

    /**
     * Interactive UI methods for showing dialogs to the user.
     * Only available when the CLI host supports elicitation
     * (`session.capabilities.ui?.elicitation === true`).
     *
     * @example
     * ```typescript
     * if (session.capabilities.ui?.elicitation) {
     *   const ok = await session.ui.confirm("Deploy to production?");
     * }
     * ```
     */
    get ui(): SessionUiApi {
        return {
            elicitation: (params: ElicitationParams) => this._elicitation(params),
            confirm: (message: string) => this._confirm(message),
            select: (message: string, options: string[]) => this._select(message, options),
            input: (message: string, options?: InputOptions) => this._input(message, options),
        };
    }

    /**
     * Sends a message to this session and waits for the response.
     *
     * The message is processed asynchronously. Subscribe to events via {@link on}
     * to receive streaming responses and other session events.
     *
     * @param options - The message options including the prompt and optional attachments
     * @returns A promise that resolves with the message ID of the response
     * @throws Error if the session has been disconnected or the connection fails
     *
     * @example
     * ```typescript
     * const messageId = await session.send({
     *   prompt: "Explain this code",
     *   attachments: [{ type: "file", path: "./src/index.ts" }]
     * });
     * ```
     */
    async send(options: MessageOptions): Promise<string> {
        const response = await this.connection.sendRequest("session.send", {
            ...(await getTraceContext(this.traceContextProvider)),
            sessionId: this.sessionId,
            prompt: options.prompt,
            attachments: options.attachments,
            mode: options.mode,
        });

        return (response as { messageId: string }).messageId;
    }

    /**
     * Sends a message to this session and waits until the session becomes idle.
     *
     * This is a convenience method that combines {@link send} with waiting for
     * the `session.idle` event. Use this when you want to block until the
     * assistant has finished processing the message.
     *
     * Events are still delivered to handlers registered via {@link on} while waiting.
     *
     * @param options - The message options including the prompt and optional attachments
     * @param timeout - Timeout in milliseconds (default: 60000). Controls how long to wait; does not abort in-flight agent work.
     * @returns A promise that resolves with the final assistant message when the session becomes idle,
     *          or undefined if no assistant message was received
     * @throws Error if the timeout is reached before the session becomes idle
     * @throws Error if the session has been disconnected or the connection fails
     *
     * @example
     * ```typescript
     * // Send and wait for completion with default 60s timeout
     * const response = await session.sendAndWait({ prompt: "What is 2+2?" });
     * console.log(response?.data.content); // "4"
     * ```
     */
    async sendAndWait(
        options: MessageOptions,
        timeout?: number
    ): Promise<AssistantMessageEvent | undefined> {
        const effectiveTimeout = timeout ?? 60_000;

        let resolveIdle: () => void;
        let rejectWithError: (error: Error) => void;
        const idlePromise = new Promise<void>((resolve, reject) => {
            resolveIdle = resolve;
            rejectWithError = reject;
        });

        let lastAssistantMessage: AssistantMessageEvent | undefined;

        // Register event handler BEFORE calling send to avoid race condition
        // where session.idle fires before we start listening
        const unsubscribe = this.on((event) => {
            if (event.type === "assistant.message") {
                lastAssistantMessage = event;
            } else if (event.type === "session.idle") {
                resolveIdle();
            } else if (event.type === "session.error") {
                const error = new Error(event.data.message);
                error.stack = event.data.stack;
                rejectWithError(error);
            }
        });

        let timeoutId: ReturnType<typeof setTimeout> | undefined;
        try {
            await this.send(options);

            const timeoutPromise = new Promise<never>((_, reject) => {
                timeoutId = setTimeout(
                    () =>
                        reject(
                            new Error(
                                `Timeout after ${effectiveTimeout}ms waiting for session.idle`
                            )
                        ),
                    effectiveTimeout
                );
            });
            await Promise.race([idlePromise, timeoutPromise]);

            return lastAssistantMessage;
        } finally {
            if (timeoutId !== undefined) {
                clearTimeout(timeoutId);
            }
            unsubscribe();
        }
    }

    /**
     * Subscribes to events from this session.
     *
     * Events include assistant messages, tool executions, errors, and session state changes.
     * Multiple handlers can be registered and will all receive events.
     *
     * @param eventType - The specific event type to listen for (e.g., "assistant.message", "session.idle")
     * @param handler - A callback function that receives events of the specified type
     * @returns A function that, when called, unsubscribes the handler
     *
     * @example
     * ```typescript
     * // Listen for a specific event type
     * const unsubscribe = session.on("assistant.message", (event) => {
     *   console.log("Assistant:", event.data.content);
     * });
     *
     * // Later, to stop receiving events:
     * unsubscribe();
     * ```
     */
    on<K extends SessionEventType>(eventType: K, handler: TypedSessionEventHandler<K>): () => void;

    /**
     * Subscribes to all events from this session.
     *
     * @param handler - A callback function that receives all session events
     * @returns A function that, when called, unsubscribes the handler
     *
     * @example
     * ```typescript
     * const unsubscribe = session.on((event) => {
     *   switch (event.type) {
     *     case "assistant.message":
     *       console.log("Assistant:", event.data.content);
     *       break;
     *     case "session.error":
     *       console.error("Error:", event.data.message);
     *       break;
     *   }
     * });
     *
     * // Later, to stop receiving events:
     * unsubscribe();
     * ```
     */
    on(handler: SessionEventHandler): () => void;

    on<K extends SessionEventType>(
        eventTypeOrHandler: K | SessionEventHandler,
        handler?: TypedSessionEventHandler<K>
    ): () => void {
        // Overload 1: on(eventType, handler) - typed event subscription
        if (typeof eventTypeOrHandler === "string" && handler) {
            const eventType = eventTypeOrHandler;
            if (!this.typedEventHandlers.has(eventType)) {
                this.typedEventHandlers.set(eventType, new Set());
            }
            // Cast is safe: handler receives the correctly typed event at dispatch time
            const storedHandler = handler as (event: SessionEvent) => void;
            this.typedEventHandlers.get(eventType)!.add(storedHandler);
            return () => {
                const handlers = this.typedEventHandlers.get(eventType);
                if (handlers) {
                    handlers.delete(storedHandler);
                }
            };
        }

        // Overload 2: on(handler) - wildcard subscription
        const wildcardHandler = eventTypeOrHandler as SessionEventHandler;
        this.eventHandlers.add(wildcardHandler);
        return () => {
            this.eventHandlers.delete(wildcardHandler);
        };
    }

    /**
     * Dispatches an event to all registered handlers.
     * Also handles broadcast request events internally (external tool calls, permissions).
     *
     * @param event - The session event to dispatch
     * @internal This method is for internal use by the SDK.
     */
    _dispatchEvent(event: SessionEvent): void {
        // Handle broadcast request events internally (fire-and-forget)
        this._handleBroadcastEvent(event);

        // Dispatch to typed handlers for this specific event type
        const typedHandlers = this.typedEventHandlers.get(event.type);
        if (typedHandlers) {
            for (const handler of typedHandlers) {
                try {
                    handler(event as SessionEventPayload<typeof event.type>);
                } catch (_error) {
                    // Handler error
                }
            }
        }

        // Dispatch to wildcard handlers
        for (const handler of this.eventHandlers) {
            try {
                handler(event);
            } catch (_error) {
                // Handler error
            }
        }
    }

    /**
     * Handles broadcast request events by executing local handlers and responding via RPC.
     * Handlers are dispatched as fire-and-forget — rejections propagate as unhandled promise
     * rejections, consistent with standard EventEmitter / event handler semantics.
     * @internal
     */
    private _handleBroadcastEvent(event: SessionEvent): void {
        if (event.type === "external_tool.requested") {
            const { requestId, toolName } = event.data as {
                requestId: string;
                toolName: string;
                arguments: unknown;
                toolCallId: string;
                sessionId: string;
            };
            const args = (event.data as { arguments: unknown }).arguments;
            const toolCallId = (event.data as { toolCallId: string }).toolCallId;
            const traceparent = (event.data as { traceparent?: string }).traceparent;
            const tracestate = (event.data as { tracestate?: string }).tracestate;
            const handler = this.toolHandlers.get(toolName);
            if (handler) {
                void this._executeToolAndRespond(
                    requestId,
                    toolName,
                    toolCallId,
                    args,
                    handler,
                    traceparent,
                    tracestate
                );
            }
        } else if (event.type === "permission.requested") {
            const { requestId, permissionRequest } = event.data as {
                requestId: string;
                permissionRequest: PermissionRequest;
            };
            if (this.permissionHandler) {
                void this._executePermissionAndRespond(requestId, permissionRequest);
            }
        } else if (event.type === "command.execute") {
            const { requestId, commandName, command, args } = event.data as {
                requestId: string;
                command: string;
                commandName: string;
                args: string;
            };
            void this._executeCommandAndRespond(requestId, commandName, command, args);
        } else if ((event as { type: string }).type === "capabilities.changed") {
            const data = (event as { data: Partial<SessionCapabilities> }).data;
            this._capabilities = { ...this._capabilities, ...data };
        }
    }

    /**
     * Executes a tool handler and sends the result back via RPC.
     * @internal
     */
    private async _executeToolAndRespond(
        requestId: string,
        toolName: string,
        toolCallId: string,
        args: unknown,
        handler: ToolHandler,
        traceparent?: string,
        tracestate?: string
    ): Promise<void> {
        try {
            const rawResult = await handler(args, {
                sessionId: this.sessionId,
                toolCallId,
                toolName,
                arguments: args,
                traceparent,
                tracestate,
            });
            let result: string;
            if (rawResult == null) {
                result = "";
            } else if (typeof rawResult === "string") {
                result = rawResult;
            } else {
                result = JSON.stringify(rawResult);
            }
            await this.rpc.tools.handlePendingToolCall({ requestId, result });
        } catch (error) {
            const message = error instanceof Error ? error.message : String(error);
            try {
                await this.rpc.tools.handlePendingToolCall({ requestId, error: message });
            } catch (rpcError) {
                if (!(rpcError instanceof ConnectionError || rpcError instanceof ResponseError)) {
                    throw rpcError;
                }
                // Connection lost or RPC error — nothing we can do
            }
        }
    }

    /**
     * Executes a permission handler and sends the result back via RPC.
     * @internal
     */
    private async _executePermissionAndRespond(
        requestId: string,
        permissionRequest: PermissionRequest
    ): Promise<void> {
        try {
            const result = await this.permissionHandler!(permissionRequest, {
                sessionId: this.sessionId,
            });
            if (result.kind === "no-result") {
                return;
            }
            await this.rpc.permissions.handlePendingPermissionRequest({ requestId, result });
        } catch (_error) {
            try {
                await this.rpc.permissions.handlePendingPermissionRequest({
                    requestId,
                    result: {
                        kind: "denied-no-approval-rule-and-could-not-request-from-user",
                    },
                });
            } catch (rpcError) {
                if (!(rpcError instanceof ConnectionError || rpcError instanceof ResponseError)) {
                    throw rpcError;
                }
                // Connection lost or RPC error — nothing we can do
            }
        }
    }

    /**
     * Executes a command handler and sends the result back via RPC.
     * @internal
     */
    private async _executeCommandAndRespond(
        requestId: string,
        commandName: string,
        command: string,
        args: string
    ): Promise<void> {
        const handler = this.commandHandlers.get(commandName);
        if (!handler) {
            try {
                await this.rpc.commands.handlePendingCommand({
                    requestId,
                    error: `Unknown command: ${commandName}`,
                });
            } catch (rpcError) {
                if (!(rpcError instanceof ConnectionError || rpcError instanceof ResponseError)) {
                    throw rpcError;
                }
            }
            return;
        }

        try {
            await handler({ sessionId: this.sessionId, command, commandName, args });
            await this.rpc.commands.handlePendingCommand({ requestId });
        } catch (error) {
            const message = error instanceof Error ? error.message : String(error);
            try {
                await this.rpc.commands.handlePendingCommand({ requestId, error: message });
            } catch (rpcError) {
                if (!(rpcError instanceof ConnectionError || rpcError instanceof ResponseError)) {
                    throw rpcError;
                }
            }
        }
    }

    /**
     * Registers custom tool handlers for this session.
     *
     * Tools allow the assistant to execute custom functions. When the assistant
     * invokes a tool, the corresponding handler is called with the tool arguments.
     *
     * @param tools - An array of tool definitions with their handlers, or undefined to clear all tools
     * @internal This method is typically called internally when creating a session with tools.
     */
    registerTools(tools?: Tool[]): void {
        this.toolHandlers.clear();
        if (!tools) {
            return;
        }

        for (const tool of tools) {
            this.toolHandlers.set(tool.name, tool.handler);
        }
    }

    /**
     * Retrieves a registered tool handler by name.
     *
     * @param name - The name of the tool to retrieve
     * @returns The tool handler if found, or undefined
     * @internal This method is for internal use by the SDK.
     */
    getToolHandler(name: string): ToolHandler | undefined {
        return this.toolHandlers.get(name);
    }

    /**
     * Registers command handlers for this session.
     *
     * @param commands - An array of command definitions with handlers, or undefined to clear
     * @internal This method is typically called internally when creating/resuming a session.
     */
    registerCommands(commands?: { name: string; handler: CommandHandler }[]): void {
        this.commandHandlers.clear();
        if (!commands) {
            return;
        }
        for (const cmd of commands) {
            this.commandHandlers.set(cmd.name, cmd.handler);
        }
    }

    /**
     * Registers the elicitation handler for this session.
     *
     * @param handler - The handler to invoke when the server dispatches an elicitation request
     * @internal This method is typically called internally when creating/resuming a session.
     */
    registerElicitationHandler(handler?: ElicitationHandler): void {
        this.elicitationHandler = handler;
    }

    /**
     * Handles an elicitation.request RPC callback from the server.
     * @internal
     */
    async _handleElicitationRequest(
        request: ElicitationRequest,
        sessionId: string
    ): Promise<ElicitationResult> {
        if (!this.elicitationHandler) {
            throw new Error("Elicitation requested but no handler registered");
        }
        return await this.elicitationHandler(request, { sessionId });
    }

    /**
     * Sets the host capabilities for this session.
     *
     * @param capabilities - The capabilities object from the create/resume response
     * @internal This method is typically called internally when creating/resuming a session.
     */
    setCapabilities(capabilities?: SessionCapabilities): void {
        this._capabilities = capabilities ?? {};
    }

    private assertElicitation(): void {
        if (!this._capabilities.ui?.elicitation) {
            throw new Error(
                "Elicitation is not supported by the host. " +
                    "Check session.capabilities.ui?.elicitation before calling UI methods."
            );
        }
    }

    private async _elicitation(params: ElicitationParams): Promise<ElicitationResult> {
        this.assertElicitation();
        return this.rpc.ui.elicitation({
            message: params.message,
            requestedSchema: params.requestedSchema,
        });
    }

    private async _confirm(message: string): Promise<boolean> {
        this.assertElicitation();
        const result = await this.rpc.ui.elicitation({
            message,
            requestedSchema: {
                type: "object",
                properties: {
                    confirmed: { type: "boolean", default: true },
                },
                required: ["confirmed"],
            },
        });
        return result.action === "accept" && (result.content?.confirmed as boolean) === true;
    }

    private async _select(message: string, options: string[]): Promise<string | null> {
        this.assertElicitation();
        const result = await this.rpc.ui.elicitation({
            message,
            requestedSchema: {
                type: "object",
                properties: {
                    selection: { type: "string", enum: options },
                },
                required: ["selection"],
            },
        });
        if (result.action === "accept" && result.content?.selection != null) {
            return result.content.selection as string;
        }
        return null;
    }

    private async _input(message: string, options?: InputOptions): Promise<string | null> {
        this.assertElicitation();
        const field: Record<string, unknown> = { type: "string" as const };
        if (options?.title) field.title = options.title;
        if (options?.description) field.description = options.description;
        if (options?.minLength != null) field.minLength = options.minLength;
        if (options?.maxLength != null) field.maxLength = options.maxLength;
        if (options?.format) field.format = options.format;
        if (options?.default != null) field.default = options.default;

        const result = await this.rpc.ui.elicitation({
            message,
            requestedSchema: {
                type: "object",
                properties: {
                    value: field as ElicitationParams["requestedSchema"]["properties"][string],
                },
                required: ["value"],
            },
        });
        if (result.action === "accept" && result.content?.value != null) {
            return result.content.value as string;
        }
        return null;
    }

    /**
     * Registers a handler for permission requests.
     *
     * When the assistant needs permission to perform certain actions (e.g., file operations),
     * this handler is called to approve or deny the request.
     *
     * @param handler - The permission handler function, or undefined to remove the handler
     * @internal This method is typically called internally when creating a session.
     */
    registerPermissionHandler(handler?: PermissionHandler): void {
        this.permissionHandler = handler;
    }

    /**
     * Registers a user input handler for ask_user requests.
     *
     * When the agent needs input from the user (via ask_user tool),
     * this handler is called to provide the response.
     *
     * @param handler - The user input handler function, or undefined to remove the handler
     * @internal This method is typically called internally when creating a session.
     */
    registerUserInputHandler(handler?: UserInputHandler): void {
        this.userInputHandler = handler;
    }

    /**
     * Registers hook handlers for session lifecycle events.
     *
     * Hooks allow custom logic to be executed at various points during
     * the session lifecycle (before/after tool use, session start/end, etc.).
     *
     * @param hooks - The hook handlers object, or undefined to remove all hooks
     * @internal This method is typically called internally when creating a session.
     */
    registerHooks(hooks?: SessionHooks): void {
        this.hooks = hooks;
    }

    /**
     * Registers transform callbacks for system message sections.
     *
     * @param callbacks - Map of section ID to transform callback, or undefined to clear
     * @internal This method is typically called internally when creating a session.
     */
    registerTransformCallbacks(callbacks?: Map<string, SectionTransformFn>): void {
        this.transformCallbacks = callbacks;
    }

    /**
     * Handles a systemMessage.transform request from the runtime.
     * Dispatches each section to its registered transform callback.
     *
     * @param sections - Map of section IDs to their current rendered content
     * @returns A promise that resolves with the transformed sections
     * @internal This method is for internal use by the SDK.
     */
    async _handleSystemMessageTransform(
        sections: Record<string, { content: string }>
    ): Promise<{ sections: Record<string, { content: string }> }> {
        const result: Record<string, { content: string }> = {};

        for (const [sectionId, { content }] of Object.entries(sections)) {
            const callback = this.transformCallbacks?.get(sectionId);
            if (callback) {
                try {
                    const transformed = await callback(content);
                    result[sectionId] = { content: transformed };
                } catch (_error) {
                    // Callback failed — return original content
                    result[sectionId] = { content };
                }
            } else {
                // No callback for this section — pass through unchanged
                result[sectionId] = { content };
            }
        }

        return { sections: result };
    }

    /**
     * Handles a permission request in the v2 protocol format (synchronous RPC).
     * Used as a back-compat adapter when connected to a v2 server.
     *
     * @param request - The permission request data from the CLI
     * @returns A promise that resolves with the permission decision
     * @internal This method is for internal use by the SDK.
     */
    async _handlePermissionRequestV2(request: unknown): Promise<PermissionRequestResult> {
        if (!this.permissionHandler) {
            return { kind: "denied-no-approval-rule-and-could-not-request-from-user" };
        }

        try {
            const result = await this.permissionHandler(request as PermissionRequest, {
                sessionId: this.sessionId,
            });
            if (result.kind === "no-result") {
                throw new Error(NO_RESULT_PERMISSION_V2_ERROR);
            }
            return result;
        } catch (error) {
            if (error instanceof Error && error.message === NO_RESULT_PERMISSION_V2_ERROR) {
                throw error;
            }
            return { kind: "denied-no-approval-rule-and-could-not-request-from-user" };
        }
    }

    /**
     * Handles a user input request from the Copilot CLI.
     *
     * @param request - The user input request data from the CLI
     * @returns A promise that resolves with the user's response
     * @internal This method is for internal use by the SDK.
     */
    async _handleUserInputRequest(request: unknown): Promise<UserInputResponse> {
        if (!this.userInputHandler) {
            // No handler registered, throw error
            throw new Error("User input requested but no handler registered");
        }

        try {
            const result = await this.userInputHandler(request as UserInputRequest, {
                sessionId: this.sessionId,
            });
            return result;
        } catch (error) {
            // Handler failed, rethrow
            throw error;
        }
    }

    /**
     * Handles a hooks invocation from the Copilot CLI.
     *
     * @param hookType - The type of hook being invoked
     * @param input - The input data for the hook
     * @returns A promise that resolves with the hook output, or undefined
     * @internal This method is for internal use by the SDK.
     */
    async _handleHooksInvoke(hookType: string, input: unknown): Promise<unknown> {
        if (!this.hooks) {
            return undefined;
        }

        // Type-safe handler lookup with explicit casting
        type GenericHandler = (
            input: unknown,
            invocation: { sessionId: string }
        ) => Promise<unknown> | unknown;

        const handlerMap: Record<string, GenericHandler | undefined> = {
            preToolUse: this.hooks.onPreToolUse as GenericHandler | undefined,
            postToolUse: this.hooks.onPostToolUse as GenericHandler | undefined,
            userPromptSubmitted: this.hooks.onUserPromptSubmitted as GenericHandler | undefined,
            sessionStart: this.hooks.onSessionStart as GenericHandler | undefined,
            sessionEnd: this.hooks.onSessionEnd as GenericHandler | undefined,
            errorOccurred: this.hooks.onErrorOccurred as GenericHandler | undefined,
        };

        const handler = handlerMap[hookType];
        if (!handler) {
            return undefined;
        }

        try {
            const result = await handler(input, { sessionId: this.sessionId });
            return result;
        } catch (_error) {
            // Hook failed, return undefined
            return undefined;
        }
    }

    /**
     * Retrieves all events and messages from this session's history.
     *
     * This returns the complete conversation history including user messages,
     * assistant responses, tool executions, and other session events.
     *
     * @returns A promise that resolves with an array of all session events
     * @throws Error if the session has been disconnected or the connection fails
     *
     * @example
     * ```typescript
     * const events = await session.getMessages();
     * for (const event of events) {
     *   if (event.type === "assistant.message") {
     *     console.log("Assistant:", event.data.content);
     *   }
     * }
     * ```
     */
    async getMessages(): Promise<SessionEvent[]> {
        const response = await this.connection.sendRequest("session.getMessages", {
            sessionId: this.sessionId,
        });

        return (response as { events: SessionEvent[] }).events;
    }

    /**
     * Disconnects this session and releases all in-memory resources (event handlers,
     * tool handlers, permission handlers).
     *
     * Session state on disk (conversation history, planning state, artifacts) is
     * preserved, so the conversation can be resumed later by calling
     * {@link CopilotClient.resumeSession} with the session ID. To permanently
     * remove all session data including files on disk, use
     * {@link CopilotClient.deleteSession} instead.
     *
     * After calling this method, the session object can no longer be used.
     *
     * @returns A promise that resolves when the session is disconnected
     * @throws Error if the connection fails
     *
     * @example
     * ```typescript
     * // Clean up when done — session can still be resumed later
     * await session.disconnect();
     * ```
     */
    async disconnect(): Promise<void> {
        await this.connection.sendRequest("session.destroy", {
            sessionId: this.sessionId,
        });
        this.eventHandlers.clear();
        this.typedEventHandlers.clear();
        this.toolHandlers.clear();
        this.permissionHandler = undefined;
    }

    /**
     * @deprecated Use {@link disconnect} instead. This method will be removed in a future release.
     *
     * Disconnects this session and releases all in-memory resources.
     * Session data on disk is preserved for later resumption.
     *
     * @returns A promise that resolves when the session is disconnected
     * @throws Error if the connection fails
     */
    async destroy(): Promise<void> {
        return this.disconnect();
    }

    /** Enables `await using session = ...` syntax for automatic cleanup. */
    async [Symbol.asyncDispose](): Promise<void> {
        return this.disconnect();
    }

    /**
     * Aborts the currently processing message in this session.
     *
     * Use this to cancel a long-running request. The session remains valid
     * and can continue to be used for new messages.
     *
     * @returns A promise that resolves when the abort request is acknowledged
     * @throws Error if the session has been disconnected or the connection fails
     *
     * @example
     * ```typescript
     * // Start a long-running request
     * const messagePromise = session.send({ prompt: "Write a very long story..." });
     *
     * // Abort after 5 seconds
     * setTimeout(async () => {
     *   await session.abort();
     * }, 5000);
     * ```
     */
    async abort(): Promise<void> {
        await this.connection.sendRequest("session.abort", {
            sessionId: this.sessionId,
        });
    }

    /**
     * Change the model for this session.
     * The new model takes effect for the next message. Conversation history is preserved.
     *
     * @param model - Model ID to switch to
     * @param options - Optional settings for the new model
     *
     * @example
     * ```typescript
     * await session.setModel("gpt-4.1");
     * await session.setModel("claude-sonnet-4.6", { reasoningEffort: "high" });
     * ```
     */
    async setModel(model: string, options?: { reasoningEffort?: ReasoningEffort }): Promise<void> {
        await this.rpc.model.switchTo({ modelId: model, ...options });
    }

    /**
     * Log a message to the session timeline.
     * The message appears in the session event stream and is visible to SDK consumers
     * and (for non-ephemeral messages) persisted to the session event log on disk.
     *
     * @param message - Human-readable message text
     * @param options - Optional log level and ephemeral flag
     *
     * @example
     * ```typescript
     * await session.log("Processing started");
     * await session.log("Disk usage high", { level: "warning" });
     * await session.log("Connection failed", { level: "error" });
     * await session.log("Debug info", { ephemeral: true });
     * ```
     */
    async log(
        message: string,
        options?: { level?: "info" | "warning" | "error"; ephemeral?: boolean }
    ): Promise<void> {
        await this.rpc.log({ message, ...options });
    }
}
