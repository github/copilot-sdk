/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * ACP (Agent Client Protocol) adapter implementation.
 * @module protocols/acp/acp-adapter
 */

import { spawn, type ChildProcess } from "node:child_process";
import type { ProtocolAdapter, ProtocolConnection } from "../protocol-adapter.js";
import type { CopilotClientOptions, SessionConfig } from "../../types.js";
import { AcpTransport } from "./acp-transport.js";
import {
    ACP_PROTOCOL_VERSION,
    UNSUPPORTED_ACP_METHODS,
    type AcpSessionUpdateParams,
    type AcpInitializeResult,
    type AcpSessionNewResult,
    type AcpSessionPromptResult,
    type AcpRequestPermissionParams,
    type AcpRequestPermissionResult,
    type AcpToolCallUpdateInner,
    type AcpToolCallUpdateUpdateInner,
} from "./acp-types.js";
import {
    stringToAcpContent,
    acpSessionUpdateToSessionEvent,
    acpToolCallToSessionEvent,
    copilotSessionConfigToAcpParams,
} from "./acp-mapper.js";

/**
 * ACP protocol connection implementation.
 * Translates Copilot SDK method calls to ACP protocol format.
 */
class AcpConnection implements ProtocolConnection {
    private transport: AcpTransport;
    private requestId = 0;
    private notificationHandlers: Map<string, Set<(params: unknown) => void>> = new Map();
    private requestHandlers: Map<string, (params: unknown) => Promise<unknown>> = new Map();
    private closeHandlers: Set<() => void> = new Set();
    private errorHandlers: Set<(error: Error) => void> = new Set();

    constructor(transport: AcpTransport) {
        this.transport = transport;

        // Set up transport notification handler for session/update
        this.transport.onNotification("session/update", (params) => {
            this.handleAcpUpdate(params as AcpSessionUpdateParams);
        });

        // Set up transport request handler for session/request_permission
        this.transport.onRequest("session/request_permission", async (id, params) => {
            await this.handleAcpPermissionRequest(id, params as AcpRequestPermissionParams);
        });

        this.transport.onClose(() => {
            for (const handler of this.closeHandlers) {
                try {
                    handler();
                } catch {
                    // Ignore handler errors
                }
            }
        });

        this.transport.onError((error) => {
            for (const handler of this.errorHandlers) {
                try {
                    handler(error);
                } catch {
                    // Ignore handler errors
                }
            }
        });
    }

    async sendRequest<T>(method: string, params?: unknown): Promise<T> {
        // Check for unsupported methods
        if (UNSUPPORTED_ACP_METHODS.includes(method as (typeof UNSUPPORTED_ACP_METHODS)[number])) {
            throw new Error(
                `Method '${method}' is not supported in ACP mode. ` +
                    `ACP protocol has limited feature support compared to the full Copilot protocol.`
            );
        }

        // Translate method and params
        const { acpMethod, acpParams } = this.translateRequest(method, params);

        // Handle no-op methods (like session.destroy which ACP doesn't support)
        if (acpMethod === "_noop") {
            return {} as T;
        }

        const id = ++this.requestId;
        const result = await this.transport.sendRequest<unknown>(id, acpMethod, acpParams);

        // Translate response if needed
        return this.translateResponse(method, result, params) as T;
    }

    sendNotification(method: string, params?: unknown): void {
        this.transport.sendNotification(method, params);
    }

    onNotification(method: string, handler: (params: unknown) => void): void {
        if (!this.notificationHandlers.has(method)) {
            this.notificationHandlers.set(method, new Set());
        }
        this.notificationHandlers.get(method)!.add(handler);
    }

    onRequest(method: string, handler: (params: unknown) => Promise<unknown>): void {
        // Store the handler for ACP requests (permission.request maps to session/request_permission)
        this.requestHandlers.set(method, handler);
    }

    onClose(handler: () => void): void {
        this.closeHandlers.add(handler);
    }

    onError(handler: (error: Error) => void): void {
        this.errorHandlers.add(handler);
    }

    dispose(): void {
        this.transport.dispose();
        this.notificationHandlers.clear();
        this.requestHandlers.clear();
        this.closeHandlers.clear();
        this.errorHandlers.clear();
    }

    listen(): void {
        this.transport.listen();
    }

    private translateRequest(
        method: string,
        params?: unknown
    ): { acpMethod: string; acpParams?: unknown } {
        switch (method) {
            case "ping":
                return {
                    acpMethod: "initialize",
                    acpParams: { protocolVersion: ACP_PROTOCOL_VERSION },
                };

            case "session.create": {
                const config = params as SessionConfig & { workingDirectory?: string };
                const acpParams = copilotSessionConfigToAcpParams(config);
                // ACP requires cwd and mcpServers (as array)
                return {
                    acpMethod: "session/new",
                    acpParams: {
                        cwd: acpParams.cwd || process.cwd(),
                        mcpServers: acpParams.mcpServers
                            ? Object.entries(acpParams.mcpServers).map(([name, config]) => ({
                                  name,
                                  ...config,
                              }))
                            : [],
                    },
                };
            }

            case "session.send": {
                const sendParams = params as { sessionId: string; prompt: string };
                return {
                    acpMethod: "session/prompt",
                    acpParams: {
                        sessionId: sendParams.sessionId,
                        prompt: stringToAcpContent(sendParams.prompt),
                    },
                };
            }

            case "session.destroy": {
                // ACP doesn't have session/end - just return success
                // The session will be cleaned up when the process exits
                return {
                    acpMethod: "_noop",
                    acpParams: params,
                };
            }

            case "session.abort": {
                return {
                    acpMethod: "session/abort",
                    acpParams: params,
                };
            }

            default:
                // Pass through unrecognized methods
                return { acpMethod: method, acpParams: params };
        }
    }

    private translateResponse(method: string, result: unknown, originalParams?: unknown): unknown {
        switch (method) {
            case "ping": {
                const acpResult = result as AcpInitializeResult;
                return {
                    message: "pong",
                    timestamp: Date.now(),
                    protocolVersion: acpResult.protocolVersion,
                };
            }

            case "session.create": {
                const acpResult = result as AcpSessionNewResult;
                return {
                    sessionId: acpResult.sessionId,
                };
            }

            case "session.send": {
                const acpResult = result as AcpSessionPromptResult;

                // Gemini returns stopReason in the response instead of sending
                // a separate end_turn notification. Emit session.idle event.
                if (acpResult.stopReason === "end_turn") {
                    // Dispatch session.idle event after a microtask to ensure
                    // it's processed after the send() promise resolves
                    queueMicrotask(() => {
                        const handlers = this.notificationHandlers.get("session.event");
                        if (handlers) {
                            const idleEvent = {
                                sessionId: (originalParams as { sessionId?: string })?.sessionId ?? "",
                                event: {
                                    id: `acp-idle-${Date.now()}`,
                                    timestamp: new Date().toISOString(),
                                    parentId: null,
                                    ephemeral: true,
                                    type: "session.idle",
                                    data: {},
                                },
                            };
                            for (const handler of handlers) {
                                try {
                                    handler(idleEvent);
                                } catch {
                                    // Ignore handler errors
                                }
                            }
                        }
                    });
                }

                return {
                    messageId: acpResult.messageId,
                };
            }

            default:
                return result;
        }
    }

    private handleAcpUpdate(updateParams: AcpSessionUpdateParams): void {
        const { update } = updateParams;

        // Check if this is a tool call update
        if (update.sessionUpdate === "tool_call" || update.sessionUpdate === "tool_call_update") {
            const toolEvent = acpToolCallToSessionEvent(
                update as AcpToolCallUpdateInner | AcpToolCallUpdateUpdateInner
            );
            if (toolEvent) {
                this.dispatchSessionEvent(updateParams.sessionId, toolEvent);
            }
            return;
        }

        // Handle message updates
        const sessionEvent = acpSessionUpdateToSessionEvent(updateParams);
        if (!sessionEvent) {
            return;
        }

        this.dispatchSessionEvent(updateParams.sessionId, sessionEvent);
    }

    private dispatchSessionEvent(sessionId: string, event: unknown): void {
        const handlers = this.notificationHandlers.get("session.event");
        if (handlers) {
            const notification = {
                sessionId,
                event,
            };

            for (const handler of handlers) {
                try {
                    handler(notification);
                } catch {
                    // Ignore handler errors
                }
            }
        }
    }

    private async handleAcpPermissionRequest(
        id: string | number,
        params: AcpRequestPermissionParams
    ): Promise<void> {
        const handler = this.requestHandlers.get("permission.request");

        if (!handler) {
            // No handler registered, return cancelled
            const result: AcpRequestPermissionResult = { outcome: "cancelled" };
            this.transport.sendResponse(id, result);
            return;
        }

        try {
            // Translate ACP permission request to Copilot format
            const copilotParams = {
                sessionId: params.sessionId,
                permissionRequest: {
                    toolCallId: params.toolCall.toolCallId,
                    title: params.toolCall.title,
                    kind: params.toolCall.kind,
                    rawInput: params.toolCall.rawInput,
                    options: params.options,
                },
            };

            const response = (await handler(copilotParams)) as { result?: { optionId?: string } };

            // Translate response back to ACP format
            const result: AcpRequestPermissionResult = {
                outcome: "selected",
                optionId: response.result?.optionId,
            };
            this.transport.sendResponse(id, result);
        } catch {
            // Handler rejected or threw - return cancelled
            const result: AcpRequestPermissionResult = { outcome: "cancelled" };
            this.transport.sendResponse(id, result);
        }
    }
}

/**
 * ACP protocol adapter implementation.
 * Manages CLI process lifecycle and provides a ProtocolConnection for ACP communication.
 */
export class AcpProtocolAdapter implements ProtocolAdapter {
    private cliProcess: ChildProcess | null = null;
    private connection: AcpConnection | null = null;
    private options: CopilotClientOptions;
    private forceStopping = false;

    // Store bound handlers for cleanup
    private stderrHandler: ((data: Buffer) => void) | null = null;
    private stdinErrorHandler: ((err: Error) => void) | null = null;

    constructor(options: CopilotClientOptions) {
        this.options = options;
    }

    async start(): Promise<void> {
        await this.startCliProcess();
        this.createConnection();
    }

    async stop(): Promise<Error[]> {
        const errors: Error[] = [];

        if (this.connection) {
            try {
                this.connection.dispose();
            } catch (error) {
                errors.push(
                    new Error(
                        `Failed to dispose connection: ${error instanceof Error ? error.message : String(error)}`
                    )
                );
            }
            this.connection = null;
        }

        if (this.cliProcess) {
            const childProcess = this.cliProcess;
            this.cliProcess = null;

            // Remove event listeners to allow process to exit
            if (this.stderrHandler && childProcess.stderr) {
                childProcess.stderr.removeListener("data", this.stderrHandler);
                this.stderrHandler = null;
            }
            if (this.stdinErrorHandler && childProcess.stdin) {
                childProcess.stdin.removeListener("error", this.stdinErrorHandler);
                this.stdinErrorHandler = null;
            }

            // Remove all listeners from process and streams
            childProcess.removeAllListeners();
            childProcess.stdout?.removeAllListeners();
            childProcess.stderr?.removeAllListeners();
            childProcess.stdin?.removeAllListeners();

            try {
                // Kill the process first
                childProcess.kill();

                // Destroy all streams to properly close them
                childProcess.stdin?.destroy();
                childProcess.stdout?.destroy();
                childProcess.stderr?.destroy();
            } catch {
                // Process may already be dead
            }
        }

        return errors;
    }

    async forceStop(): Promise<void> {
        this.forceStopping = true;

        if (this.connection) {
            try {
                this.connection.dispose();
            } catch {
                // Ignore errors during force stop
            }
            this.connection = null;
        }

        if (this.cliProcess) {
            // Remove event listeners
            if (this.stderrHandler && this.cliProcess.stderr) {
                this.cliProcess.stderr.removeListener("data", this.stderrHandler);
                this.stderrHandler = null;
            }
            if (this.stdinErrorHandler && this.cliProcess.stdin) {
                this.cliProcess.stdin.removeListener("error", this.stdinErrorHandler);
                this.stdinErrorHandler = null;
            }
            this.cliProcess.removeAllListeners();

            try {
                this.cliProcess.kill("SIGKILL");
            } catch {
                // Ignore errors
            }
            this.cliProcess = null;
        }
    }

    getConnection(): ProtocolConnection {
        if (!this.connection) {
            throw new Error("Connection not established. Call start() first.");
        }
        return this.connection;
    }

    async verifyProtocolVersion(): Promise<void> {
        const result = (await this.getConnection().sendRequest("ping", {})) as {
            protocolVersion?: number;
        };

        if (result.protocolVersion !== ACP_PROTOCOL_VERSION) {
            throw new Error(
                `ACP protocol version mismatch: SDK expects version ${ACP_PROTOCOL_VERSION}, ` +
                    `but server reports version ${result.protocolVersion}.`
            );
        }
    }

    private startCliProcess(): Promise<void> {
        return new Promise((resolve, reject) => {
            // For ACP mode, we only use cliArgs - don't add --headless, --stdio, --log-level
            const args = [...(this.options.cliArgs ?? [])];

            // Suppress debug output
            const env = { ...(this.options.env ?? process.env) };
            delete env.NODE_DEBUG;

            // Handle different CLI path formats
            const cliPath = this.options.cliPath ?? "gemini";
            const isJsFile = cliPath.endsWith(".js");
            const isAbsolutePath = cliPath.startsWith("/") || /^[a-zA-Z]:/.test(cliPath);

            let command: string;
            let spawnArgs: string[];

            if (isJsFile) {
                command = "node";
                spawnArgs = [cliPath, ...args];
            } else if (process.platform === "win32" && !isAbsolutePath) {
                command = "cmd";
                spawnArgs = ["/c", cliPath, ...args];
            } else {
                command = cliPath;
                spawnArgs = args;
            }

            this.cliProcess = spawn(command, spawnArgs, {
                stdio: ["pipe", "pipe", "pipe"],
                cwd: this.options.cwd ?? process.cwd(),
                env,
            });

            let resolved = false;

            // For ACP mode with stdio, we're ready immediately after spawn
            resolved = true;
            resolve();

            // Store handler for cleanup
            this.stderrHandler = (data: Buffer) => {
                // Forward CLI stderr to parent's stderr
                const lines = data.toString().split("\n");
                for (const line of lines) {
                    if (line.trim()) {
                        process.stderr.write(`[ACP CLI] ${line}\n`);
                    }
                }
            };
            this.cliProcess.stderr?.on("data", this.stderrHandler);

            this.cliProcess.on("error", (error) => {
                if (!resolved) {
                    resolved = true;
                    reject(new Error(`Failed to start ACP CLI: ${error.message}`));
                }
            });

            this.cliProcess.on("exit", (code) => {
                if (!resolved) {
                    resolved = true;
                    reject(new Error(`ACP CLI exited with code ${code}`));
                }
            });

            // Handle stdin errors during force stop
            this.stdinErrorHandler = (err: Error) => {
                if (!this.forceStopping) {
                    throw err;
                }
            };
            this.cliProcess.stdin?.on("error", this.stdinErrorHandler);

            // Timeout after 10 seconds
            setTimeout(() => {
                if (!resolved) {
                    resolved = true;
                    reject(new Error("Timeout waiting for ACP CLI to start"));
                }
            }, 10000);
        });
    }

    private createConnection(): void {
        if (!this.cliProcess || !this.cliProcess.stdout || !this.cliProcess.stdin) {
            throw new Error("CLI process not started");
        }

        const transport = new AcpTransport(this.cliProcess.stdout, this.cliProcess.stdin);
        this.connection = new AcpConnection(transport);
    }
}
