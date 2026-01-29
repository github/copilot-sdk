/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Copilot Session - represents a single conversation session with the Copilot CLI.
 * @module session
 */

import type { MessageConnection } from "vscode-jsonrpc/node";
import type {
    MessageOptions,
    PermissionHandler,
    PermissionRequest,
    PermissionRequestResult,
    SessionEvent,
    SessionEventHandler,
    Tool,
    ToolHandler,
} from "./types.js";

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
 * await session.destroy();
 * ```
 */
export class CopilotSession {
    private eventHandlers: Set<SessionEventHandler> = new Set();
    private toolHandlers: Map<string, ToolHandler> = new Map();
    private toolRequiresApproval: Map<string, boolean> = new Map();
    private permissionHandler?: PermissionHandler;

    /**
     * Creates a new CopilotSession instance.
     *
     * @param sessionId - The unique identifier for this session
     * @param connection - The JSON-RPC message connection to the Copilot CLI
     * @param workspacePath - Path to the session workspace directory (when infinite sessions enabled)
     * @internal This constructor is internal. Use {@link CopilotClient.createSession} to create sessions.
     */
    constructor(
        public readonly sessionId: string,
        private connection: MessageConnection,
        private readonly _workspacePath?: string
    ) {}

    /**
     * Path to the session workspace directory when infinite sessions are enabled.
     * Contains checkpoints/, plan.md, and files/ subdirectories.
     * Undefined if infinite sessions are disabled.
     */
    get workspacePath(): string | undefined {
        return this._workspacePath;
    }

    /**
     * Sends a message to this session and waits for the response.
     *
     * The message is processed asynchronously. Subscribe to events via {@link on}
     * to receive streaming responses and other session events.
     *
     * @param options - The message options including the prompt and optional attachments
     * @returns A promise that resolves with the message ID of the response
     * @throws Error if the session has been destroyed or the connection fails
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
     * @throws Error if the session has been destroyed or the connection fails
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

        try {
            await this.send(options);

            const timeoutPromise = new Promise<never>((_, reject) => {
                setTimeout(
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
            unsubscribe();
        }
    }

    /**
     * Subscribes to events from this session.
     *
     * Events include assistant messages, tool executions, errors, and session state changes.
     * Multiple handlers can be registered and will all receive events.
     *
     * @param handler - A callback function that receives session events
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
    on(handler: SessionEventHandler): () => void {
        this.eventHandlers.add(handler);
        return () => {
            this.eventHandlers.delete(handler);
        };
    }

    /**
     * Dispatches an event to all registered handlers.
     *
     * @param event - The session event to dispatch
     * @internal This method is for internal use by the SDK.
     */
    _dispatchEvent(event: SessionEvent): void {
        for (const handler of this.eventHandlers) {
            try {
                handler(event);
            } catch (_error) {
                // Handler error
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
        this.toolRequiresApproval.clear();
        if (!tools) {
            return;
        }

        for (const tool of tools) {
            this.toolHandlers.set(tool.name, tool.handler);
            this.toolRequiresApproval.set(tool.name, tool.requiresApproval ?? false);
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
     * Checks if a tool requires approval before execution.
     *
     * @param name - The name of the tool to check
     * @returns True if the tool requires approval, false otherwise
     * @internal This method is for internal use by the SDK.
     */
    toolRequiresApprovalCheck(name: string): boolean {
        return this.toolRequiresApproval.get(name) ?? false;
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
     * Handles a permission request from the Copilot CLI.
     *
     * @param request - The permission request data from the CLI
     * @returns A promise that resolves with the permission decision
     * @internal This method is for internal use by the SDK.
     */
    async _handlePermissionRequest(request: unknown): Promise<PermissionRequestResult> {
        if (!this.permissionHandler) {
            // No handler registered, deny permission
            return { kind: "denied-no-approval-rule-and-could-not-request-from-user" };
        }

        try {
            const result = await this.permissionHandler(request as PermissionRequest, {
                sessionId: this.sessionId,
            });
            return result;
        } catch (_error) {
            // Handler failed, deny permission
            return { kind: "denied-no-approval-rule-and-could-not-request-from-user" };
        }
    }

    /**
     * Retrieves all events and messages from this session's history.
     *
     * This returns the complete conversation history including user messages,
     * assistant responses, tool executions, and other session events.
     *
     * @returns A promise that resolves with an array of all session events
     * @throws Error if the session has been destroyed or the connection fails
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
     * Destroys this session and releases all associated resources.
     *
     * After calling this method, the session can no longer be used. All event
     * handlers and tool handlers are cleared. To continue the conversation,
     * use {@link CopilotClient.resumeSession} with the session ID.
     *
     * @returns A promise that resolves when the session is destroyed
     * @throws Error if the connection fails
     *
     * @example
     * ```typescript
     * // Clean up when done
     * await session.destroy();
     * ```
     */
    async destroy(): Promise<void> {
        await this.connection.sendRequest("session.destroy", {
            sessionId: this.sessionId,
        });
        this.eventHandlers.clear();
        this.toolHandlers.clear();
        this.permissionHandler = undefined;
    }

    /**
     * Aborts the currently processing message in this session.
     *
     * Use this to cancel a long-running request. The session remains valid
     * and can continue to be used for new messages.
     *
     * @returns A promise that resolves when the abort request is acknowledged
     * @throws Error if the session has been destroyed or the connection fails
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
}
