/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Copilot CLI SDK Client - Main entry point for the Copilot SDK.
 *
 * This module provides the {@link CopilotClient} class, which manages the connection
 * to the Copilot CLI server and provides session management capabilities.
 *
 * @module client
 */

import { spawn, type ChildProcess } from "node:child_process";
import { Socket } from "node:net";
import {
    createMessageConnection,
    MessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { getSdkProtocolVersion } from "./sdkProtocolVersion.js";
import { CopilotSession } from "./session.js";
import type {
    ConnectionState,
    CopilotClientOptions,
    GetAuthStatusResponse,
    GetStatusResponse,
    ModelInfo,
    ResumeSessionConfig,
    SessionConfig,
    SessionEvent,
    SessionMetadata,
    Tool,
    ToolCallRequestPayload,
    ToolCallResponsePayload,
    ToolHandler,
    ToolResult,
    ToolResultObject,
} from "./types.js";

/**
 * Check if value is a Zod schema (has toJSONSchema method)
 */
function isZodSchema(value: unknown): value is { toJSONSchema(): Record<string, unknown> } {
    return (
        value != null &&
        typeof value === "object" &&
        "toJSONSchema" in value &&
        typeof (value as { toJSONSchema: unknown }).toJSONSchema === "function"
    );
}

/**
 * Convert tool parameters to JSON schema format for sending to CLI
 */
function toJsonSchema(parameters: Tool["parameters"]): Record<string, unknown> | undefined {
    if (!parameters) return undefined;
    if (isZodSchema(parameters)) {
        return parameters.toJSONSchema();
    }
    return parameters;
}

/**
 * Main client for interacting with the Copilot CLI.
 *
 * The CopilotClient manages the connection to the Copilot CLI server and provides
 * methods to create and manage conversation sessions. It can either spawn a CLI
 * server process or connect to an existing server.
 *
 * @example
 * ```typescript
 * import { CopilotClient } from "@github/copilot-sdk";
 *
 * // Create a client with default options (spawns CLI server)
 * const client = new CopilotClient();
 *
 * // Or connect to an existing server
 * const client = new CopilotClient({ cliUrl: "localhost:3000" });
 *
 * // Create a session
 * const session = await client.createSession({ model: "gpt-4" });
 *
 * // Send messages and handle responses
 * session.on((event) => {
 *   if (event.type === "assistant.message") {
 *     console.log(event.data.content);
 *   }
 * });
 * await session.send({ prompt: "Hello!" });
 *
 * // Clean up
 * await session.destroy();
 * await client.stop();
 * ```
 */
export class CopilotClient {
    private cliProcess: ChildProcess | null = null;
    private connection: MessageConnection | null = null;
    private socket: Socket | null = null;
    private actualPort: number | null = null;
    private actualHost: string = "localhost";
    private state: ConnectionState = "disconnected";
    private sessions: Map<string, CopilotSession> = new Map();
    private options: Required<
        Omit<CopilotClientOptions, "cliUrl" | "githubToken" | "useLoggedInUser">
    > & {
        cliUrl?: string;
        githubToken?: string;
        useLoggedInUser?: boolean;
    };
    private isExternalServer: boolean = false;
    private forceStopping: boolean = false;
    private modelsCache: ModelInfo[] | null = null;
    private modelsCacheLock: Promise<void> = Promise.resolve();

    /**
     * Creates a new CopilotClient instance.
     *
     * @param options - Configuration options for the client
     * @throws Error if mutually exclusive options are provided (e.g., cliUrl with useStdio or cliPath)
     *
     * @example
     * ```typescript
     * // Default options - spawns CLI server using stdio
     * const client = new CopilotClient();
     *
     * // Connect to an existing server
     * const client = new CopilotClient({ cliUrl: "localhost:3000" });
     *
     * // Custom CLI path with specific log level
     * const client = new CopilotClient({
     *   cliPath: "/usr/local/bin/copilot",
     *   logLevel: "debug"
     * });
     * ```
     */
    constructor(options: CopilotClientOptions = {}) {
        // Validate mutually exclusive options
        if (options.cliUrl && (options.useStdio === true || options.cliPath)) {
            throw new Error("cliUrl is mutually exclusive with useStdio and cliPath");
        }

        // Validate auth options with external server
        if (options.cliUrl && (options.githubToken || options.useLoggedInUser !== undefined)) {
            throw new Error(
                "githubToken and useLoggedInUser cannot be used with cliUrl (external server manages its own auth)"
            );
        }

        // Parse cliUrl if provided
        if (options.cliUrl) {
            const { host, port } = this.parseCliUrl(options.cliUrl);
            this.actualHost = host;
            this.actualPort = port;
            this.isExternalServer = true;
        }

        this.options = {
            cliPath: options.cliPath || "copilot",
            cliArgs: options.cliArgs ?? [],
            cwd: options.cwd ?? process.cwd(),
            port: options.port || 0,
            useStdio: options.cliUrl ? false : (options.useStdio ?? true), // Default to stdio unless cliUrl is provided
            cliUrl: options.cliUrl,
            logLevel: options.logLevel || "debug",
            autoStart: options.autoStart ?? true,
            autoRestart: options.autoRestart ?? true,
            env: options.env ?? process.env,
            githubToken: options.githubToken,
            // Default useLoggedInUser to false when githubToken is provided, otherwise true
            useLoggedInUser: options.useLoggedInUser ?? (options.githubToken ? false : true),
        };
    }

    /**
     * Parse CLI URL into host and port
     * Supports formats: "host:port", "http://host:port", "https://host:port", or just "port"
     */
    private parseCliUrl(url: string): { host: string; port: number } {
        // Remove protocol if present
        let cleanUrl = url.replace(/^https?:\/\//, "");

        // Check if it's just a port number
        if (/^\d+$/.test(cleanUrl)) {
            return { host: "localhost", port: parseInt(cleanUrl, 10) };
        }

        // Parse host:port format
        const parts = cleanUrl.split(":");
        if (parts.length !== 2) {
            throw new Error(
                `Invalid cliUrl format: ${url}. Expected "host:port", "http://host:port", or "port"`
            );
        }

        const host = parts[0] || "localhost";
        const port = parseInt(parts[1], 10);

        if (isNaN(port) || port <= 0 || port > 65535) {
            throw new Error(`Invalid port in cliUrl: ${url}`);
        }

        return { host, port };
    }

    /**
     * Starts the CLI server and establishes a connection.
     *
     * If connecting to an external server (via cliUrl), only establishes the connection.
     * Otherwise, spawns the CLI server process and then connects.
     *
     * This method is called automatically when creating a session if `autoStart` is true (default).
     *
     * @returns A promise that resolves when the connection is established
     * @throws Error if the server fails to start or the connection fails
     *
     * @example
     * ```typescript
     * const client = new CopilotClient({ autoStart: false });
     * await client.start();
     * // Now ready to create sessions
     * ```
     */
    async start(): Promise<void> {
        if (this.state === "connected") {
            return;
        }

        this.state = "connecting";

        try {
            // Only start CLI server process if not connecting to external server
            if (!this.isExternalServer) {
                await this.startCLIServer();
            }

            // Connect to the server
            await this.connectToServer();

            // Verify protocol version compatibility
            await this.verifyProtocolVersion();

            this.state = "connected";
        } catch (error) {
            this.state = "error";
            throw error;
        }
    }

    /**
     * Stops the CLI server and closes all active sessions.
     *
     * This method performs graceful cleanup:
     * 1. Destroys all active sessions with retry logic
     * 2. Closes the JSON-RPC connection
     * 3. Terminates the CLI server process (if spawned by this client)
     *
     * @returns A promise that resolves with an array of errors encountered during cleanup.
     *          An empty array indicates all cleanup succeeded.
     *
     * @example
     * ```typescript
     * const errors = await client.stop();
     * if (errors.length > 0) {
     *   console.error("Cleanup errors:", errors);
     * }
     * ```
     */
    async stop(): Promise<Error[]> {
        const errors: Error[] = [];

        // Destroy all active sessions with retry logic
        for (const session of this.sessions.values()) {
            const sessionId = session.sessionId;
            let lastError: Error | null = null;

            // Try up to 3 times with exponential backoff
            for (let attempt = 1; attempt <= 3; attempt++) {
                try {
                    await session.destroy();
                    lastError = null;
                    break; // Success
                } catch (error) {
                    lastError = error instanceof Error ? error : new Error(String(error));

                    if (attempt < 3) {
                        // Exponential backoff: 100ms, 200ms
                        const delay = 100 * Math.pow(2, attempt - 1);
                        await new Promise((resolve) => setTimeout(resolve, delay));
                    }
                }
            }

            if (lastError) {
                errors.push(
                    new Error(
                        `Failed to destroy session ${sessionId} after 3 attempts: ${lastError.message}`
                    )
                );
            }
        }
        this.sessions.clear();

        // Close connection
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

        // Clear models cache
        this.modelsCache = null;

        if (this.socket) {
            try {
                this.socket.end();
            } catch (error) {
                errors.push(
                    new Error(
                        `Failed to close socket: ${error instanceof Error ? error.message : String(error)}`
                    )
                );
            }
            this.socket = null;
        }

        // Kill CLI process (only if we spawned it)
        if (this.cliProcess && !this.isExternalServer) {
            try {
                this.cliProcess.kill();
            } catch (error) {
                errors.push(
                    new Error(
                        `Failed to kill CLI process: ${error instanceof Error ? error.message : String(error)}`
                    )
                );
            }
            this.cliProcess = null;
        }

        this.state = "disconnected";
        this.actualPort = null;

        return errors;
    }

    /**
     * Forcefully stops the CLI server without graceful cleanup.
     *
     * Use this when {@link stop} fails or takes too long. This method:
     * - Clears all sessions immediately without destroying them
     * - Force closes the connection
     * - Sends SIGKILL to the CLI process (if spawned by this client)
     *
     * @returns A promise that resolves when the force stop is complete
     *
     * @example
     * ```typescript
     * // If normal stop hangs, force stop
     * const stopPromise = client.stop();
     * const timeout = new Promise((_, reject) =>
     *   setTimeout(() => reject(new Error("Timeout")), 5000)
     * );
     *
     * try {
     *   await Promise.race([stopPromise, timeout]);
     * } catch {
     *   await client.forceStop();
     * }
     * ```
     */
    async forceStop(): Promise<void> {
        this.forceStopping = true;

        // Clear sessions immediately without trying to destroy them
        this.sessions.clear();

        // Force close connection
        if (this.connection) {
            try {
                this.connection.dispose();
            } catch {
                // Ignore errors during force stop
            }
            this.connection = null;
        }

        // Clear models cache
        this.modelsCache = null;

        if (this.socket) {
            try {
                this.socket.destroy(); // destroy() is more forceful than end()
            } catch {
                // Ignore errors
            }
            this.socket = null;
        }

        // Force kill CLI process (only if we spawned it)
        if (this.cliProcess && !this.isExternalServer) {
            try {
                this.cliProcess.kill("SIGKILL");
            } catch {
                // Ignore errors
            }
            this.cliProcess = null;
        }

        this.state = "disconnected";
        this.actualPort = null;
    }

    /**
     * Creates a new conversation session with the Copilot CLI.
     *
     * Sessions maintain conversation state, handle events, and manage tool execution.
     * If the client is not connected and `autoStart` is enabled, this will automatically
     * start the connection.
     *
     * @param config - Optional configuration for the session
     * @returns A promise that resolves with the created session
     * @throws Error if the client is not connected and autoStart is disabled
     *
     * @example
     * ```typescript
     * // Basic session
     * const session = await client.createSession();
     *
     * // Session with model and tools
     * const session = await client.createSession({
     *   model: "gpt-4",
     *   tools: [{
     *     name: "get_weather",
     *     description: "Get weather for a location",
     *     parameters: { type: "object", properties: { location: { type: "string" } } },
     *     handler: async (args) => ({ temperature: 72 })
     *   }]
     * });
     * ```
     */
    async createSession(config: SessionConfig = {}): Promise<CopilotSession> {
        if (!this.connection) {
            if (this.options.autoStart) {
                await this.start();
            } else {
                throw new Error("Client not connected. Call start() first.");
            }
        }

        const response = await this.connection!.sendRequest("session.create", {
            model: config.model,
            sessionId: config.sessionId,
            reasoningEffort: config.reasoningEffort,
            tools: config.tools?.map((tool) => ({
                name: tool.name,
                description: tool.description,
                parameters: toJsonSchema(tool.parameters),
            })),
            systemMessage: config.systemMessage,
            availableTools: config.availableTools,
            excludedTools: config.excludedTools,
            provider: config.provider,
            requestPermission: !!config.onPermissionRequest,
            requestUserInput: !!config.onUserInputRequest,
            hooks: !!(config.hooks && Object.values(config.hooks).some(Boolean)),
            workingDirectory: config.workingDirectory,
            streaming: config.streaming,
            mcpServers: config.mcpServers,
            customAgents: config.customAgents,
            configDir: config.configDir,
            skillDirectories: config.skillDirectories,
            disabledSkills: config.disabledSkills,
            infiniteSessions: config.infiniteSessions,
        });

        const { sessionId, workspacePath } = response as {
            sessionId: string;
            workspacePath?: string;
        };
        const session = new CopilotSession(sessionId, this.connection!, workspacePath);
        session.registerTools(config.tools);
        if (config.onPermissionRequest) {
            session.registerPermissionHandler(config.onPermissionRequest);
        }
        if (config.onUserInputRequest) {
            session.registerUserInputHandler(config.onUserInputRequest);
        }
        if (config.hooks) {
            session.registerHooks(config.hooks);
        }
        this.sessions.set(sessionId, session);

        return session;
    }

    /**
     * Resumes an existing conversation session by its ID.
     *
     * This allows you to continue a previous conversation, maintaining all
     * conversation history. The session must have been previously created
     * and not deleted.
     *
     * @param sessionId - The ID of the session to resume
     * @param config - Optional configuration for the resumed session
     * @returns A promise that resolves with the resumed session
     * @throws Error if the session does not exist or the client is not connected
     *
     * @example
     * ```typescript
     * // Resume a previous session
     * const session = await client.resumeSession("session-123");
     *
     * // Resume with new tools
     * const session = await client.resumeSession("session-123", {
     *   tools: [myNewTool]
     * });
     * ```
     */
    async resumeSession(
        sessionId: string,
        config: ResumeSessionConfig = {}
    ): Promise<CopilotSession> {
        if (!this.connection) {
            if (this.options.autoStart) {
                await this.start();
            } else {
                throw new Error("Client not connected. Call start() first.");
            }
        }

        const response = await this.connection!.sendRequest("session.resume", {
            sessionId,
            reasoningEffort: config.reasoningEffort,
            tools: config.tools?.map((tool) => ({
                name: tool.name,
                description: tool.description,
                parameters: toJsonSchema(tool.parameters),
            })),
            provider: config.provider,
            requestPermission: !!config.onPermissionRequest,
            requestUserInput: !!config.onUserInputRequest,
            hooks: !!(config.hooks && Object.values(config.hooks).some(Boolean)),
            workingDirectory: config.workingDirectory,
            streaming: config.streaming,
            mcpServers: config.mcpServers,
            customAgents: config.customAgents,
            skillDirectories: config.skillDirectories,
            disabledSkills: config.disabledSkills,
            disableResume: config.disableResume,
        });

        const { sessionId: resumedSessionId, workspacePath } = response as {
            sessionId: string;
            workspacePath?: string;
        };
        const session = new CopilotSession(resumedSessionId, this.connection!, workspacePath);
        session.registerTools(config.tools);
        if (config.onPermissionRequest) {
            session.registerPermissionHandler(config.onPermissionRequest);
        }
        if (config.onUserInputRequest) {
            session.registerUserInputHandler(config.onUserInputRequest);
        }
        if (config.hooks) {
            session.registerHooks(config.hooks);
        }
        this.sessions.set(resumedSessionId, session);

        return session;
    }

    /**
     * Gets the current connection state of the client.
     *
     * @returns The current connection state: "disconnected", "connecting", "connected", or "error"
     *
     * @example
     * ```typescript
     * if (client.getState() === "connected") {
     *   const session = await client.createSession();
     * }
     * ```
     */
    getState(): ConnectionState {
        return this.state;
    }

    /**
     * Sends a ping request to the server to verify connectivity.
     *
     * @param message - Optional message to include in the ping
     * @returns A promise that resolves with the ping response containing the message and timestamp
     * @throws Error if the client is not connected
     *
     * @example
     * ```typescript
     * const response = await client.ping("health check");
     * console.log(`Server responded at ${new Date(response.timestamp)}`);
     * ```
     */
    async ping(
        message?: string
    ): Promise<{ message: string; timestamp: number; protocolVersion?: number }> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }

        const result = await this.connection.sendRequest("ping", { message });
        return result as {
            message: string;
            timestamp: number;
            protocolVersion?: number;
        };
    }

    /**
     * Get CLI status including version and protocol information
     */
    async getStatus(): Promise<GetStatusResponse> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }

        const result = await this.connection.sendRequest("status.get", {});
        return result as GetStatusResponse;
    }

    /**
     * Get current authentication status
     */
    async getAuthStatus(): Promise<GetAuthStatusResponse> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }

        const result = await this.connection.sendRequest("auth.getStatus", {});
        return result as GetAuthStatusResponse;
    }

    /**
     * List available models with their metadata.
     *
     * Results are cached after the first successful call to avoid rate limiting.
     * The cache is cleared when the client disconnects.
     *
     * @throws Error if not authenticated
     */
    async listModels(): Promise<ModelInfo[]> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }

        // Use promise-based locking to prevent race condition with concurrent calls
        await this.modelsCacheLock;

        let resolveLock: () => void;
        this.modelsCacheLock = new Promise((resolve) => {
            resolveLock = resolve;
        });

        try {
            // Check cache (already inside lock)
            if (this.modelsCache !== null) {
                return [...this.modelsCache]; // Return a copy to prevent cache mutation
            }

            // Cache miss - fetch from backend while holding lock
            const result = await this.connection.sendRequest("models.list", {});
            const response = result as { models: ModelInfo[] };
            const models = response.models;

            // Update cache before releasing lock
            this.modelsCache = models;

            return [...models]; // Return a copy to prevent cache mutation
        } finally {
            resolveLock!();
        }
    }

    /**
     * Verify that the server's protocol version matches the SDK's expected version
     */
    private async verifyProtocolVersion(): Promise<void> {
        const expectedVersion = getSdkProtocolVersion();
        const pingResult = await this.ping();
        const serverVersion = pingResult.protocolVersion;

        if (serverVersion === undefined) {
            throw new Error(
                `SDK protocol version mismatch: SDK expects version ${expectedVersion}, but server does not report a protocol version. ` +
                    `Please update your server to ensure compatibility.`
            );
        }

        if (serverVersion !== expectedVersion) {
            throw new Error(
                `SDK protocol version mismatch: SDK expects version ${expectedVersion}, but server reports version ${serverVersion}. ` +
                    `Please update your SDK or server to ensure compatibility.`
            );
        }
    }

    /**
     * Gets the ID of the most recently updated session.
     *
     * This is useful for resuming the last conversation when the session ID
     * was not stored.
     *
     * @returns A promise that resolves with the session ID, or undefined if no sessions exist
     * @throws Error if the client is not connected
     *
     * @example
     * ```typescript
     * const lastId = await client.getLastSessionId();
     * if (lastId) {
     *   const session = await client.resumeSession(lastId);
     * }
     * ```
     */
    async getLastSessionId(): Promise<string | undefined> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }

        const response = await this.connection.sendRequest("session.getLastId", {});
        return (response as { sessionId?: string }).sessionId;
    }

    /**
     * Deletes a session and its data from disk.
     *
     * This permanently removes the session and all its conversation history.
     * The session cannot be resumed after deletion.
     *
     * @param sessionId - The ID of the session to delete
     * @returns A promise that resolves when the session is deleted
     * @throws Error if the session does not exist or deletion fails
     *
     * @example
     * ```typescript
     * await client.deleteSession("session-123");
     * ```
     */
    async deleteSession(sessionId: string): Promise<void> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }

        const response = await this.connection.sendRequest("session.delete", {
            sessionId,
        });

        const { success, error } = response as { success: boolean; error?: string };
        if (!success) {
            throw new Error(`Failed to delete session ${sessionId}: ${error || "Unknown error"}`);
        }

        // Remove from local sessions map if present
        this.sessions.delete(sessionId);
    }

    /**
     * Lists all available sessions known to the server.
     *
     * Returns metadata about each session including ID, timestamps, and summary.
     *
     * @returns A promise that resolves with an array of session metadata
     * @throws Error if the client is not connected
     *
     * @example
     * ```typescript
     * const sessions = await client.listSessions();
     * for (const session of sessions) {
     *   console.log(`${session.sessionId}: ${session.summary}`);
     * }
     * ```
     */
    async listSessions(): Promise<SessionMetadata[]> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }

        const response = await this.connection.sendRequest("session.list", {});
        const { sessions } = response as {
            sessions: Array<{
                sessionId: string;
                startTime: string;
                modifiedTime: string;
                summary?: string;
                isRemote: boolean;
            }>;
        };

        return sessions.map((s) => ({
            sessionId: s.sessionId,
            startTime: new Date(s.startTime),
            modifiedTime: new Date(s.modifiedTime),
            summary: s.summary,
            isRemote: s.isRemote,
        }));
    }

    /**
     * Start the CLI server process
     */
    private async startCLIServer(): Promise<void> {
        return new Promise((resolve, reject) => {
            const args = [
                ...this.options.cliArgs,
                "--server",
                "--log-level",
                this.options.logLevel,
            ];

            // Choose transport mode
            if (this.options.useStdio) {
                args.push("--stdio");
            } else if (this.options.port > 0) {
                args.push("--port", this.options.port.toString());
            }

            // Add auth-related flags
            if (this.options.githubToken) {
                args.push("--auth-token-env", "COPILOT_SDK_AUTH_TOKEN");
            }
            if (!this.options.useLoggedInUser) {
                args.push("--no-auto-login");
            }

            // Suppress debug/trace output that might pollute stdout
            const envWithoutNodeDebug = { ...this.options.env };
            delete envWithoutNodeDebug.NODE_DEBUG;

            // Set auth token in environment if provided
            if (this.options.githubToken) {
                envWithoutNodeDebug.COPILOT_SDK_AUTH_TOKEN = this.options.githubToken;
            }

            // If cliPath is a .js file, spawn it with node
            // Note that we can't rely on the shebang as Windows doesn't support it
            const isJsFile = this.options.cliPath.endsWith(".js");
            const isAbsolutePath =
                this.options.cliPath.startsWith("/") || /^[a-zA-Z]:/.test(this.options.cliPath);

            let command: string;
            let spawnArgs: string[];

            if (isJsFile) {
                command = "node";
                spawnArgs = [this.options.cliPath, ...args];
            } else if (process.platform === "win32" && !isAbsolutePath) {
                // On Windows, spawn doesn't search PATHEXT, so use cmd /c to resolve the executable.
                command = "cmd";
                spawnArgs = ["/c", `${this.options.cliPath}`, ...args];
            } else {
                command = this.options.cliPath;
                spawnArgs = args;
            }

            this.cliProcess = spawn(command, spawnArgs, {
                stdio: this.options.useStdio
                    ? ["pipe", "pipe", "pipe"]
                    : ["ignore", "pipe", "pipe"],
                cwd: this.options.cwd,
                env: envWithoutNodeDebug,
            });

            let stdout = "";
            let resolved = false;

            // For stdio mode, we're ready immediately after spawn
            if (this.options.useStdio) {
                resolved = true;
                resolve();
            } else {
                // For TCP mode, wait for port announcement
                this.cliProcess.stdout?.on("data", (data: Buffer) => {
                    stdout += data.toString();
                    const match = stdout.match(/listening on port (\d+)/i);
                    if (match && !resolved) {
                        this.actualPort = parseInt(match[1], 10);
                        resolved = true;
                        resolve();
                    }
                });
            }

            this.cliProcess.stderr?.on("data", (data: Buffer) => {
                // Forward CLI stderr to parent's stderr so debug logs are visible
                const lines = data.toString().split("\n");
                for (const line of lines) {
                    if (line.trim()) {
                        process.stderr.write(`[CLI subprocess] ${line}\n`);
                    }
                }
            });

            this.cliProcess.on("error", (error) => {
                if (!resolved) {
                    resolved = true;
                    reject(new Error(`Failed to start CLI server: ${error.message}`));
                }
            });

            this.cliProcess.on("exit", (code) => {
                if (!resolved) {
                    resolved = true;
                    reject(new Error(`CLI server exited with code ${code}`));
                } else if (this.options.autoRestart && this.state === "connected") {
                    void this.reconnect();
                }
            });

            // Timeout after 10 seconds
            setTimeout(() => {
                if (!resolved) {
                    resolved = true;
                    reject(new Error("Timeout waiting for CLI server to start"));
                }
            }, 10000);
        });
    }

    /**
     * Connect to the CLI server (via socket or stdio)
     */
    private async connectToServer(): Promise<void> {
        if (this.options.useStdio) {
            return this.connectViaStdio();
        } else {
            return this.connectViaTcp();
        }
    }

    /**
     * Connect via stdio pipes
     */
    private async connectViaStdio(): Promise<void> {
        if (!this.cliProcess) {
            throw new Error("CLI process not started");
        }

        // Add error handler to stdin to prevent unhandled rejections during forceStop
        this.cliProcess.stdin?.on("error", (err) => {
            if (!this.forceStopping) {
                throw err;
            }
        });

        // Create JSON-RPC connection over stdin/stdout
        this.connection = createMessageConnection(
            new StreamMessageReader(this.cliProcess.stdout!),
            new StreamMessageWriter(this.cliProcess.stdin!)
        );

        this.attachConnectionHandlers();
        this.connection.listen();
    }

    /**
     * Connect to the CLI server via TCP socket
     */
    private async connectViaTcp(): Promise<void> {
        if (!this.actualPort) {
            throw new Error("Server port not available");
        }

        return new Promise((resolve, reject) => {
            this.socket = new Socket();

            this.socket.connect(this.actualPort!, this.actualHost, () => {
                // Create JSON-RPC connection
                this.connection = createMessageConnection(
                    new StreamMessageReader(this.socket!),
                    new StreamMessageWriter(this.socket!)
                );

                this.attachConnectionHandlers();
                this.connection.listen();
                resolve();
            });

            this.socket.on("error", (error) => {
                reject(new Error(`Failed to connect to CLI server: ${error.message}`));
            });
        });
    }

    private attachConnectionHandlers(): void {
        if (!this.connection) {
            return;
        }

        this.connection.onNotification("session.event", (notification: unknown) => {
            this.handleSessionEventNotification(notification);
        });

        this.connection.onRequest(
            "tool.call",
            async (params: ToolCallRequestPayload): Promise<ToolCallResponsePayload> =>
                await this.handleToolCallRequest(params)
        );

        this.connection.onRequest(
            "permission.request",
            async (params: {
                sessionId: string;
                permissionRequest: unknown;
            }): Promise<{ result: unknown }> => await this.handlePermissionRequest(params)
        );

        this.connection.onRequest(
            "userInput.request",
            async (params: {
                sessionId: string;
                question: string;
                choices?: string[];
                allowFreeform?: boolean;
            }): Promise<{ answer: string; wasFreeform: boolean }> =>
                await this.handleUserInputRequest(params)
        );

        this.connection.onRequest(
            "hooks.invoke",
            async (params: {
                sessionId: string;
                hookType: string;
                input: unknown;
            }): Promise<{ output?: unknown }> => await this.handleHooksInvoke(params)
        );

        this.connection.onClose(() => {
            if (this.state === "connected" && this.options.autoRestart) {
                void this.reconnect();
            }
        });

        this.connection.onError((_error) => {
            // Connection errors are handled via autoRestart if enabled
        });
    }

    private handleSessionEventNotification(notification: unknown): void {
        if (
            typeof notification !== "object" ||
            !notification ||
            !("sessionId" in notification) ||
            typeof (notification as { sessionId?: unknown }).sessionId !== "string" ||
            !("event" in notification)
        ) {
            return;
        }

        const session = this.sessions.get((notification as { sessionId: string }).sessionId);
        if (session) {
            session._dispatchEvent((notification as { event: SessionEvent }).event);
        }
    }

    private async handleToolCallRequest(
        params: ToolCallRequestPayload
    ): Promise<ToolCallResponsePayload> {
        if (
            !params ||
            typeof params.sessionId !== "string" ||
            typeof params.toolCallId !== "string" ||
            typeof params.toolName !== "string"
        ) {
            throw new Error("Invalid tool call payload");
        }

        const session = this.sessions.get(params.sessionId);
        if (!session) {
            throw new Error(`Unknown session ${params.sessionId}`);
        }

        const handler = session.getToolHandler(params.toolName);
        if (!handler) {
            return { result: this.buildUnsupportedToolResult(params.toolName) };
        }

        return await this.executeToolCall(handler, params);
    }

    private async executeToolCall(
        handler: ToolHandler,
        request: ToolCallRequestPayload
    ): Promise<ToolCallResponsePayload> {
        try {
            const invocation = {
                sessionId: request.sessionId,
                toolCallId: request.toolCallId,
                toolName: request.toolName,
                arguments: request.arguments,
            };
            const result = await handler(request.arguments, invocation);

            return { result: this.normalizeToolResult(result) };
        } catch (error) {
            const message = error instanceof Error ? error.message : String(error);
            return {
                result: {
                    // Don't expose detailed error information to the LLM for security reasons
                    textResultForLlm:
                        "Invoking this tool produced an error. Detailed information is not available.",
                    resultType: "failure",
                    error: message,
                    toolTelemetry: {},
                },
            };
        }
    }

    private async handlePermissionRequest(params: {
        sessionId: string;
        permissionRequest: unknown;
    }): Promise<{ result: unknown }> {
        if (!params || typeof params.sessionId !== "string" || !params.permissionRequest) {
            throw new Error("Invalid permission request payload");
        }

        const session = this.sessions.get(params.sessionId);
        if (!session) {
            throw new Error(`Session not found: ${params.sessionId}`);
        }

        try {
            const result = await session._handlePermissionRequest(params.permissionRequest);
            return { result };
        } catch (_error) {
            // If permission handler fails, deny the permission
            return {
                result: {
                    kind: "denied-no-approval-rule-and-could-not-request-from-user",
                },
            };
        }
    }

    private async handleUserInputRequest(params: {
        sessionId: string;
        question: string;
        choices?: string[];
        allowFreeform?: boolean;
    }): Promise<{ answer: string; wasFreeform: boolean }> {
        if (
            !params ||
            typeof params.sessionId !== "string" ||
            typeof params.question !== "string"
        ) {
            throw new Error("Invalid user input request payload");
        }

        const session = this.sessions.get(params.sessionId);
        if (!session) {
            throw new Error(`Session not found: ${params.sessionId}`);
        }

        const result = await session._handleUserInputRequest({
            question: params.question,
            choices: params.choices,
            allowFreeform: params.allowFreeform,
        });
        return result;
    }

    private async handleHooksInvoke(params: {
        sessionId: string;
        hookType: string;
        input: unknown;
    }): Promise<{ output?: unknown }> {
        if (
            !params ||
            typeof params.sessionId !== "string" ||
            typeof params.hookType !== "string"
        ) {
            throw new Error("Invalid hooks invoke payload");
        }

        const session = this.sessions.get(params.sessionId);
        if (!session) {
            throw new Error(`Session not found: ${params.sessionId}`);
        }

        const output = await session._handleHooksInvoke(params.hookType, params.input);
        return { output };
    }

    private normalizeToolResult(result: unknown): ToolResultObject {
        if (result === undefined || result === null) {
            return {
                textResultForLlm: "Tool returned no result",
                resultType: "failure",
                error: "tool returned no result",
                toolTelemetry: {},
            };
        }

        // ToolResultObject passes through directly (duck-type check)
        if (this.isToolResultObject(result)) {
            return result;
        }

        // Everything else gets wrapped as a successful ToolResultObject
        const textResult = typeof result === "string" ? result : JSON.stringify(result);
        return {
            textResultForLlm: textResult,
            resultType: "success",
            toolTelemetry: {},
        };
    }

    private isToolResultObject(value: unknown): value is ToolResultObject {
        return (
            typeof value === "object" &&
            value !== null &&
            "textResultForLlm" in value &&
            typeof (value as ToolResultObject).textResultForLlm === "string" &&
            "resultType" in value
        );
    }

    private buildUnsupportedToolResult(toolName: string): ToolResult {
        return {
            textResultForLlm: `Tool '${toolName}' is not supported by this client instance.`,
            resultType: "failure",
            error: `tool '${toolName}' not supported`,
            toolTelemetry: {},
        };
    }

    /**
     * Attempt to reconnect to the server
     */
    private async reconnect(): Promise<void> {
        this.state = "disconnected";
        try {
            await this.stop();
            await this.start();
        } catch (_error) {
            // Reconnection failed
        }
    }
}
