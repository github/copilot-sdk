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
import { randomUUID } from "node:crypto";
import { existsSync } from "node:fs";
import { createRequire } from "node:module";
import { Socket } from "node:net";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import {
    createMessageConnection,
    MessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { createServerRpc, registerClientSessionApiHandlers } from "./generated/rpc.js";
import { getSdkProtocolVersion } from "./sdkProtocolVersion.js";
import { CopilotSession, NO_RESULT_PERMISSION_V2_ERROR } from "./session.js";
import { getTraceContext } from "./telemetry.js";
import type {
    ConnectionState,
    CopilotClientOptions,
    ForegroundSessionInfo,
    GetAuthStatusResponse,
    GetStatusResponse,
    ModelInfo,
    ResumeSessionConfig,
    SectionTransformFn,
    SessionConfig,
    SessionContext,
    SessionEvent,
    SessionFsConfig,
    SessionLifecycleEvent,
    SessionLifecycleEventType,
    SessionLifecycleHandler,
    SessionListFilter,
    SessionMetadata,
    SystemMessageCustomizeConfig,
    TelemetryConfig,
    Tool,
    ToolCallRequestPayload,
    ToolCallResponsePayload,
    ToolResultObject,
    TraceContextProvider,
    TypedSessionLifecycleHandler,
} from "./types.js";

/**
 * Minimum protocol version this SDK can communicate with.
 * Servers reporting a version below this are rejected.
 */
const MIN_PROTOCOL_VERSION = 2;

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
 * Extract transform callbacks from a system message config and prepare the wire payload.
 * Function-valued actions are replaced with `{ action: "transform" }` for serialization,
 * and the original callbacks are returned in a separate map.
 */
function extractTransformCallbacks(systemMessage: SessionConfig["systemMessage"]): {
    wirePayload: SessionConfig["systemMessage"];
    transformCallbacks: Map<string, SectionTransformFn> | undefined;
} {
    if (!systemMessage || systemMessage.mode !== "customize" || !systemMessage.sections) {
        return { wirePayload: systemMessage, transformCallbacks: undefined };
    }

    const transformCallbacks = new Map<string, SectionTransformFn>();
    const wireSections: Record<string, { action: string; content?: string }> = {};

    for (const [sectionId, override] of Object.entries(systemMessage.sections)) {
        if (!override) continue;

        if (typeof override.action === "function") {
            transformCallbacks.set(sectionId, override.action);
            wireSections[sectionId] = { action: "transform" };
        } else {
            wireSections[sectionId] = { action: override.action, content: override.content };
        }
    }

    if (transformCallbacks.size === 0) {
        return { wirePayload: systemMessage, transformCallbacks: undefined };
    }

    const wirePayload: SystemMessageCustomizeConfig = {
        ...systemMessage,
        sections: wireSections as SystemMessageCustomizeConfig["sections"],
    };

    return { wirePayload, transformCallbacks };
}

function getNodeExecPath(): string {
    if (process.versions.bun) {
        return "node";
    }
    return process.execPath;
}

/**
 * Gets the path to the bundled CLI from the @github/copilot package.
 * Uses index.js directly rather than npm-loader.js (which spawns the native binary).
 *
 * In ESM, uses import.meta.resolve directly. In CJS (e.g., VS Code extensions
 * bundled with esbuild format:"cjs"), import.meta is empty so we fall back to
 * walking node_modules to find the package.
 */
function getBundledCliPath(): string {
    if (typeof import.meta.resolve === "function") {
        // ESM: resolve via import.meta.resolve
        const sdkUrl = import.meta.resolve("@github/copilot/sdk");
        const sdkPath = fileURLToPath(sdkUrl);
        // sdkPath is like .../node_modules/@github/copilot/sdk/index.js
        // Go up two levels to get the package root, then append index.js
        return join(dirname(dirname(sdkPath)), "index.js");
    }

    // CJS fallback: the @github/copilot package has ESM-only exports so
    // require.resolve cannot reach it. Walk the module search paths instead.
    const req = createRequire(__filename);
    const searchPaths = req.resolve.paths("@github/copilot") ?? [];
    for (const base of searchPaths) {
        const candidate = join(base, "@github", "copilot", "index.js");
        if (existsSync(candidate)) {
            return candidate;
        }
    }
    throw new Error(
        `Could not find @github/copilot package. Searched ${searchPaths.length} paths. ` +
            `Ensure it is installed, or pass cliPath/cliUrl to CopilotClient.`
    );
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
 * const session = await client.createSession({ onPermissionRequest: approveAll, model: "gpt-4" });
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
 * await session.disconnect();
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
    private stderrBuffer: string = ""; // Captures CLI stderr for error messages
    private options: Required<
        Omit<
            CopilotClientOptions,
            | "cliPath"
            | "cliUrl"
            | "githubToken"
            | "useLoggedInUser"
            | "onListModels"
            | "telemetry"
            | "onGetTraceContext"
            | "sessionFs"
        >
    > & {
        cliPath?: string;
        cliUrl?: string;
        githubToken?: string;
        useLoggedInUser?: boolean;
        telemetry?: TelemetryConfig;
    };
    private isExternalServer: boolean = false;
    private forceStopping: boolean = false;
    private onListModels?: () => Promise<ModelInfo[]> | ModelInfo[];
    private onGetTraceContext?: TraceContextProvider;
    private modelsCache: ModelInfo[] | null = null;
    private modelsCacheLock: Promise<void> = Promise.resolve();
    private sessionLifecycleHandlers: Set<SessionLifecycleHandler> = new Set();
    private typedLifecycleHandlers: Map<
        SessionLifecycleEventType,
        Set<(event: SessionLifecycleEvent) => void>
    > = new Map();
    private _rpc: ReturnType<typeof createServerRpc> | null = null;
    private processExitPromise: Promise<never> | null = null; // Rejects when CLI process exits
    private negotiatedProtocolVersion: number | null = null;
    /** Connection-level session filesystem config, set via constructor option. */
    private sessionFsConfig: SessionFsConfig | null = null;

    /**
     * Typed server-scoped RPC methods.
     * @throws Error if the client is not connected
     */
    get rpc(): ReturnType<typeof createServerRpc> {
        if (!this.connection) {
            throw new Error("Client is not connected. Call start() first.");
        }
        if (!this._rpc) {
            this._rpc = createServerRpc(this.connection);
        }
        return this._rpc;
    }

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

        if (options.isChildProcess && (options.cliUrl || options.useStdio === false)) {
            throw new Error(
                "isChildProcess must be used in conjunction with useStdio and not with cliUrl"
            );
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

        if (options.isChildProcess) {
            this.isExternalServer = true;
        }

        this.onListModels = options.onListModels;
        this.onGetTraceContext = options.onGetTraceContext;
        this.sessionFsConfig = options.sessionFs ?? null;

        const effectiveEnv = options.env ?? process.env;
        this.options = {
            cliPath: options.cliUrl
                ? undefined
                : options.cliPath || effectiveEnv.COPILOT_CLI_PATH || getBundledCliPath(),
            cliArgs: options.cliArgs ?? [],
            cwd: options.cwd ?? process.cwd(),
            port: options.port || 0,
            useStdio: options.cliUrl ? false : (options.useStdio ?? true), // Default to stdio unless cliUrl is provided
            isChildProcess: options.isChildProcess ?? false,
            cliUrl: options.cliUrl,
            logLevel: options.logLevel || "debug",
            autoStart: options.autoStart ?? true,
            autoRestart: false,

            env: effectiveEnv,
            githubToken: options.githubToken,
            // Default useLoggedInUser to false when githubToken is provided, otherwise true
            useLoggedInUser: options.useLoggedInUser ?? (options.githubToken ? false : true),
            telemetry: options.telemetry,
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

            // If a session filesystem provider was configured, register it
            if (this.sessionFsConfig) {
                await this.connection!.sendRequest("sessionFs.setProvider", {
                    initialCwd: this.sessionFsConfig.initialCwd,
                    sessionStatePath: this.sessionFsConfig.sessionStatePath,
                    conventions: this.sessionFsConfig.conventions,
                });
            }

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
     * 1. Closes all active sessions (releases in-memory resources)
     * 2. Closes the JSON-RPC connection
     * 3. Terminates the CLI server process (if spawned by this client)
     *
     * Note: session data on disk is preserved, so sessions can be resumed later.
     * To permanently remove session data before stopping, call
     * {@link deleteSession} for each session first.
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

        // Disconnect all active sessions with retry logic
        for (const session of this.sessions.values()) {
            const sessionId = session.sessionId;
            let lastError: Error | null = null;

            // Try up to 3 times with exponential backoff
            for (let attempt = 1; attempt <= 3; attempt++) {
                try {
                    await session.disconnect();
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
                        `Failed to disconnect session ${sessionId} after 3 attempts: ${lastError.message}`
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
            this._rpc = null;
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
        this.stderrBuffer = "";
        this.processExitPromise = null;

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
            this._rpc = null;
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
        this.stderrBuffer = "";
        this.processExitPromise = null;
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
     * const session = await client.createSession({ onPermissionRequest: approveAll });
     *
     * // Session with model and tools
     * const session = await client.createSession({
     *   onPermissionRequest: approveAll,
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
    async createSession(config: SessionConfig): Promise<CopilotSession> {
        if (!config?.onPermissionRequest) {
            throw new Error(
                "An onPermissionRequest handler is required when creating a session. For example, to allow all permissions, use { onPermissionRequest: approveAll }."
            );
        }

        if (!this.connection) {
            if (this.options.autoStart) {
                await this.start();
            } else {
                throw new Error("Client not connected. Call start() first.");
            }
        }

        const sessionId = config.sessionId ?? randomUUID();

        // Create and register the session before issuing the RPC so that
        // events emitted by the CLI (e.g. session.start) are not dropped.
        const session = new CopilotSession(
            sessionId,
            this.connection!,
            undefined,
            this.onGetTraceContext
        );
        session.registerTools(config.tools);
        session.registerCommands(config.commands);
        session.registerPermissionHandler(config.onPermissionRequest);
        if (config.onUserInputRequest) {
            session.registerUserInputHandler(config.onUserInputRequest);
        }
        if (config.onElicitationRequest) {
            session.registerElicitationHandler(config.onElicitationRequest);
        }
        if (config.hooks) {
            session.registerHooks(config.hooks);
        }

        // Extract transform callbacks from system message config before serialization.
        const { wirePayload: wireSystemMessage, transformCallbacks } = extractTransformCallbacks(
            config.systemMessage
        );
        if (transformCallbacks) {
            session.registerTransformCallbacks(transformCallbacks);
        }

        if (config.onEvent) {
            session.on(config.onEvent);
        }
        this.sessions.set(sessionId, session);
        if (this.sessionFsConfig) {
            if (config.createSessionFsHandler) {
                session.clientSessionApis.sessionFs = config.createSessionFsHandler(session);
            } else {
                throw new Error(
                    "createSessionFsHandler is required in session config when sessionFs is enabled in client options."
                );
            }
        }

        try {
            const response = await this.connection!.sendRequest("session.create", {
                ...(await getTraceContext(this.onGetTraceContext)),
                model: config.model,
                sessionId,
                clientName: config.clientName,
                reasoningEffort: config.reasoningEffort,
                tools: config.tools?.map((tool) => ({
                    name: tool.name,
                    description: tool.description,
                    parameters: toJsonSchema(tool.parameters),
                    overridesBuiltInTool: tool.overridesBuiltInTool,
                    skipPermission: tool.skipPermission,
                })),
                commands: config.commands?.map((cmd) => ({
                    name: cmd.name,
                    description: cmd.description,
                })),
                systemMessage: wireSystemMessage,
                availableTools: config.availableTools,
                excludedTools: config.excludedTools,
                provider: config.provider,
                requestPermission: true,
                requestUserInput: !!config.onUserInputRequest,
                requestElicitation: !!config.onElicitationRequest,
                hooks: !!(config.hooks && Object.values(config.hooks).some(Boolean)),
                workingDirectory: config.workingDirectory,
                streaming: config.streaming,
                mcpServers: config.mcpServers,
                envValueMode: "direct",
                customAgents: config.customAgents,
                agent: config.agent,
                configDir: config.configDir,
                skillDirectories: config.skillDirectories,
                disabledSkills: config.disabledSkills,
                infiniteSessions: config.infiniteSessions,
            });

            const { workspacePath, capabilities } = response as {
                sessionId: string;
                workspacePath?: string;
                capabilities?: { ui?: { elicitation?: boolean } };
            };
            session["_workspacePath"] = workspacePath;
            session.setCapabilities(capabilities);
        } catch (e) {
            this.sessions.delete(sessionId);
            throw e;
        }

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
     * const session = await client.resumeSession("session-123", { onPermissionRequest: approveAll });
     *
     * // Resume with new tools
     * const session = await client.resumeSession("session-123", {
     *   onPermissionRequest: approveAll,
     *   tools: [myNewTool]
     * });
     * ```
     */
    async resumeSession(sessionId: string, config: ResumeSessionConfig): Promise<CopilotSession> {
        if (!config?.onPermissionRequest) {
            throw new Error(
                "An onPermissionRequest handler is required when resuming a session. For example, to allow all permissions, use { onPermissionRequest: approveAll }."
            );
        }

        if (!this.connection) {
            if (this.options.autoStart) {
                await this.start();
            } else {
                throw new Error("Client not connected. Call start() first.");
            }
        }

        // Create and register the session before issuing the RPC so that
        // events emitted by the CLI (e.g. session.start) are not dropped.
        const session = new CopilotSession(
            sessionId,
            this.connection!,
            undefined,
            this.onGetTraceContext
        );
        session.registerTools(config.tools);
        session.registerCommands(config.commands);
        session.registerPermissionHandler(config.onPermissionRequest);
        if (config.onUserInputRequest) {
            session.registerUserInputHandler(config.onUserInputRequest);
        }
        if (config.onElicitationRequest) {
            session.registerElicitationHandler(config.onElicitationRequest);
        }
        if (config.hooks) {
            session.registerHooks(config.hooks);
        }

        // Extract transform callbacks from system message config before serialization.
        const { wirePayload: wireSystemMessage, transformCallbacks } = extractTransformCallbacks(
            config.systemMessage
        );
        if (transformCallbacks) {
            session.registerTransformCallbacks(transformCallbacks);
        }

        if (config.onEvent) {
            session.on(config.onEvent);
        }
        this.sessions.set(sessionId, session);
        if (this.sessionFsConfig) {
            if (config.createSessionFsHandler) {
                session.clientSessionApis.sessionFs = config.createSessionFsHandler(session);
            } else {
                throw new Error(
                    "createSessionFsHandler is required in session config when sessionFs is enabled in client options."
                );
            }
        }

        try {
            const response = await this.connection!.sendRequest("session.resume", {
                ...(await getTraceContext(this.onGetTraceContext)),
                sessionId,
                clientName: config.clientName,
                model: config.model,
                reasoningEffort: config.reasoningEffort,
                systemMessage: wireSystemMessage,
                availableTools: config.availableTools,
                excludedTools: config.excludedTools,
                tools: config.tools?.map((tool) => ({
                    name: tool.name,
                    description: tool.description,
                    parameters: toJsonSchema(tool.parameters),
                    overridesBuiltInTool: tool.overridesBuiltInTool,
                    skipPermission: tool.skipPermission,
                })),
                commands: config.commands?.map((cmd) => ({
                    name: cmd.name,
                    description: cmd.description,
                })),
                provider: config.provider,
                requestPermission: true,
                requestUserInput: !!config.onUserInputRequest,
                requestElicitation: !!config.onElicitationRequest,
                hooks: !!(config.hooks && Object.values(config.hooks).some(Boolean)),
                workingDirectory: config.workingDirectory,
                configDir: config.configDir,
                streaming: config.streaming,
                mcpServers: config.mcpServers,
                envValueMode: "direct",
                customAgents: config.customAgents,
                agent: config.agent,
                skillDirectories: config.skillDirectories,
                disabledSkills: config.disabledSkills,
                infiniteSessions: config.infiniteSessions,
                disableResume: config.disableResume,
            });

            const { workspacePath, capabilities } = response as {
                sessionId: string;
                workspacePath?: string;
                capabilities?: { ui?: { elicitation?: boolean } };
            };
            session["_workspacePath"] = workspacePath;
            session.setCapabilities(capabilities);
        } catch (e) {
            this.sessions.delete(sessionId);
            throw e;
        }

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
     *   const session = await client.createSession({ onPermissionRequest: approveAll });
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
     * If an `onListModels` handler was provided in the client options,
     * it is called instead of querying the CLI server.
     *
     * Results are cached after the first successful call to avoid rate limiting.
     * The cache is cleared when the client disconnects.
     *
     * @throws Error if not connected (when no custom handler is set)
     */
    async listModels(): Promise<ModelInfo[]> {
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

            let models: ModelInfo[];
            if (this.onListModels) {
                // Use custom handler instead of CLI RPC
                models = await this.onListModels();
            } else {
                if (!this.connection) {
                    throw new Error("Client not connected");
                }
                // Cache miss - fetch from backend while holding lock
                const result = await this.connection.sendRequest("models.list", {});
                const response = result as { models: ModelInfo[] };
                models = response.models;
            }

            // Update cache before releasing lock (copy to prevent external mutation)
            this.modelsCache = [...models];

            return [...models]; // Return a copy to prevent cache mutation
        } finally {
            resolveLock!();
        }
    }

    /**
     * Verify that the server's protocol version is within the supported range
     * and store the negotiated version.
     */
    private async verifyProtocolVersion(): Promise<void> {
        const maxVersion = getSdkProtocolVersion();

        // Race ping against process exit to detect early CLI failures
        let pingResult: Awaited<ReturnType<typeof this.ping>>;
        if (this.processExitPromise) {
            pingResult = await Promise.race([this.ping(), this.processExitPromise]);
        } else {
            pingResult = await this.ping();
        }

        const serverVersion = pingResult.protocolVersion;

        if (serverVersion === undefined) {
            throw new Error(
                `SDK protocol version mismatch: SDK supports versions ${MIN_PROTOCOL_VERSION}-${maxVersion}, but server does not report a protocol version. ` +
                    `Please update your server to ensure compatibility.`
            );
        }

        if (serverVersion < MIN_PROTOCOL_VERSION || serverVersion > maxVersion) {
            throw new Error(
                `SDK protocol version mismatch: SDK supports versions ${MIN_PROTOCOL_VERSION}-${maxVersion}, but server reports version ${serverVersion}. ` +
                    `Please update your SDK or server to ensure compatibility.`
            );
        }

        this.negotiatedProtocolVersion = serverVersion;
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
     *   const session = await client.resumeSession(lastId, { onPermissionRequest: approveAll });
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
     * Permanently deletes a session and all its data from disk, including
     * conversation history, planning state, and artifacts.
     *
     * Unlike {@link CopilotSession.disconnect}, which only releases in-memory
     * resources and preserves session data for later resumption, this method
     * is irreversible. The session cannot be resumed after deletion.
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
     * List all available sessions.
     *
     * @param filter - Optional filter to limit returned sessions by context fields
     *
     * @example
     * // List all sessions
     * const sessions = await client.listSessions();
     *
     * @example
     * // List sessions for a specific repository
     * const sessions = await client.listSessions({ repository: "owner/repo" });
     */
    async listSessions(filter?: SessionListFilter): Promise<SessionMetadata[]> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }

        const response = await this.connection.sendRequest("session.list", {
            filter,
        });
        const { sessions } = response as {
            sessions: Array<{
                sessionId: string;
                startTime: string;
                modifiedTime: string;
                summary?: string;
                isRemote: boolean;
                context?: SessionContext;
            }>;
        };

        return sessions.map(CopilotClient.toSessionMetadata);
    }

    /**
     * Gets metadata for a specific session by ID.
     *
     * This provides an efficient O(1) lookup of a single session's metadata
     * instead of listing all sessions. Returns undefined if the session is not found.
     *
     * @param sessionId - The ID of the session to look up
     * @returns A promise that resolves with the session metadata, or undefined if not found
     * @throws Error if the client is not connected
     *
     * @example
     * ```typescript
     * const metadata = await client.getSessionMetadata("session-123");
     * if (metadata) {
     *   console.log(`Session started at: ${metadata.startTime}`);
     * }
     * ```
     */
    async getSessionMetadata(sessionId: string): Promise<SessionMetadata | undefined> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }

        const response = await this.connection.sendRequest("session.getMetadata", { sessionId });
        const { session } = response as {
            session?: {
                sessionId: string;
                startTime: string;
                modifiedTime: string;
                summary?: string;
                isRemote: boolean;
                context?: SessionContext;
            };
        };

        if (!session) {
            return undefined;
        }

        return CopilotClient.toSessionMetadata(session);
    }

    private static toSessionMetadata(raw: {
        sessionId: string;
        startTime: string;
        modifiedTime: string;
        summary?: string;
        isRemote: boolean;
        context?: SessionContext;
    }): SessionMetadata {
        return {
            sessionId: raw.sessionId,
            startTime: new Date(raw.startTime),
            modifiedTime: new Date(raw.modifiedTime),
            summary: raw.summary,
            isRemote: raw.isRemote,
            context: raw.context,
        };
    }

    /**
     * Gets the foreground session ID in TUI+server mode.
     *
     * This returns the ID of the session currently displayed in the TUI.
     * Only available when connecting to a server running in TUI+server mode (--ui-server).
     *
     * @returns A promise that resolves with the foreground session ID, or undefined if none
     * @throws Error if the client is not connected
     *
     * @example
     * ```typescript
     * const sessionId = await client.getForegroundSessionId();
     * if (sessionId) {
     *   console.log(`TUI is displaying session: ${sessionId}`);
     * }
     * ```
     */
    async getForegroundSessionId(): Promise<string | undefined> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }

        const response = await this.connection.sendRequest("session.getForeground", {});
        return (response as ForegroundSessionInfo).sessionId;
    }

    /**
     * Sets the foreground session in TUI+server mode.
     *
     * This requests the TUI to switch to displaying the specified session.
     * Only available when connecting to a server running in TUI+server mode (--ui-server).
     *
     * @param sessionId - The ID of the session to display in the TUI
     * @returns A promise that resolves when the session is switched
     * @throws Error if the client is not connected or if the operation fails
     *
     * @example
     * ```typescript
     * // Switch the TUI to display a specific session
     * await client.setForegroundSessionId("session-123");
     * ```
     */
    async setForegroundSessionId(sessionId: string): Promise<void> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }

        const response = await this.connection.sendRequest("session.setForeground", { sessionId });
        const result = response as { success: boolean; error?: string };

        if (!result.success) {
            throw new Error(result.error || "Failed to set foreground session");
        }
    }

    /**
     * Subscribes to a specific session lifecycle event type.
     *
     * Lifecycle events are emitted when sessions are created, deleted, updated,
     * or change foreground/background state (in TUI+server mode).
     *
     * @param eventType - The specific event type to listen for
     * @param handler - A callback function that receives events of the specified type
     * @returns A function that, when called, unsubscribes the handler
     *
     * @example
     * ```typescript
     * // Listen for when a session becomes foreground in TUI
     * const unsubscribe = client.on("session.foreground", (event) => {
     *   console.log(`Session ${event.sessionId} is now displayed in TUI`);
     * });
     *
     * // Later, to stop receiving events:
     * unsubscribe();
     * ```
     */
    on<K extends SessionLifecycleEventType>(
        eventType: K,
        handler: TypedSessionLifecycleHandler<K>
    ): () => void;

    /**
     * Subscribes to all session lifecycle events.
     *
     * @param handler - A callback function that receives all lifecycle events
     * @returns A function that, when called, unsubscribes the handler
     *
     * @example
     * ```typescript
     * const unsubscribe = client.on((event) => {
     *   switch (event.type) {
     *     case "session.foreground":
     *       console.log(`Session ${event.sessionId} is now in foreground`);
     *       break;
     *     case "session.created":
     *       console.log(`New session created: ${event.sessionId}`);
     *       break;
     *   }
     * });
     *
     * // Later, to stop receiving events:
     * unsubscribe();
     * ```
     */
    on(handler: SessionLifecycleHandler): () => void;

    on<K extends SessionLifecycleEventType>(
        eventTypeOrHandler: K | SessionLifecycleHandler,
        handler?: TypedSessionLifecycleHandler<K>
    ): () => void {
        // Overload 1: on(eventType, handler) - typed event subscription
        if (typeof eventTypeOrHandler === "string" && handler) {
            const eventType = eventTypeOrHandler;
            if (!this.typedLifecycleHandlers.has(eventType)) {
                this.typedLifecycleHandlers.set(eventType, new Set());
            }
            const storedHandler = handler as (event: SessionLifecycleEvent) => void;
            this.typedLifecycleHandlers.get(eventType)!.add(storedHandler);
            return () => {
                const handlers = this.typedLifecycleHandlers.get(eventType);
                if (handlers) {
                    handlers.delete(storedHandler);
                }
            };
        }

        // Overload 2: on(handler) - wildcard subscription
        const wildcardHandler = eventTypeOrHandler as SessionLifecycleHandler;
        this.sessionLifecycleHandlers.add(wildcardHandler);
        return () => {
            this.sessionLifecycleHandlers.delete(wildcardHandler);
        };
    }

    /**
     * Start the CLI server process
     */
    private async startCLIServer(): Promise<void> {
        return new Promise((resolve, reject) => {
            // Clear stderr buffer for fresh capture
            this.stderrBuffer = "";

            const args = [
                ...this.options.cliArgs,
                "--headless",
                "--no-auto-update",
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

            if (!this.options.cliPath) {
                throw new Error(
                    "Path to Copilot CLI is required. Please provide it via the cliPath option, or use cliUrl to rely on a remote CLI."
                );
            }

            // Set OpenTelemetry environment variables if telemetry is configured
            if (this.options.telemetry) {
                const t = this.options.telemetry;
                envWithoutNodeDebug.COPILOT_OTEL_ENABLED = "true";
                if (t.otlpEndpoint !== undefined)
                    envWithoutNodeDebug.OTEL_EXPORTER_OTLP_ENDPOINT = t.otlpEndpoint;
                if (t.filePath !== undefined)
                    envWithoutNodeDebug.COPILOT_OTEL_FILE_EXPORTER_PATH = t.filePath;
                if (t.exporterType !== undefined)
                    envWithoutNodeDebug.COPILOT_OTEL_EXPORTER_TYPE = t.exporterType;
                if (t.sourceName !== undefined)
                    envWithoutNodeDebug.COPILOT_OTEL_SOURCE_NAME = t.sourceName;
                if (t.captureContent !== undefined)
                    envWithoutNodeDebug.OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT = String(
                        t.captureContent
                    );
            }

            // Verify CLI exists before attempting to spawn
            if (!existsSync(this.options.cliPath)) {
                throw new Error(
                    `Copilot CLI not found at ${this.options.cliPath}. Ensure @github/copilot is installed.`
                );
            }

            const stdioConfig: ["pipe", "pipe", "pipe"] | ["ignore", "pipe", "pipe"] = this.options
                .useStdio
                ? ["pipe", "pipe", "pipe"]
                : ["ignore", "pipe", "pipe"];

            // For .js files, spawn node explicitly; for executables, spawn directly
            const isJsFile = this.options.cliPath.endsWith(".js");
            if (isJsFile) {
                this.cliProcess = spawn(getNodeExecPath(), [this.options.cliPath, ...args], {
                    stdio: stdioConfig,
                    cwd: this.options.cwd,
                    env: envWithoutNodeDebug,
                    windowsHide: true,
                });
            } else {
                this.cliProcess = spawn(this.options.cliPath, args, {
                    stdio: stdioConfig,
                    cwd: this.options.cwd,
                    env: envWithoutNodeDebug,
                    windowsHide: true,
                });
            }

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
                // Capture stderr for error messages
                this.stderrBuffer += data.toString();
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
                    const stderrOutput = this.stderrBuffer.trim();
                    if (stderrOutput) {
                        reject(
                            new Error(
                                `Failed to start CLI server: ${error.message}\nstderr: ${stderrOutput}`
                            )
                        );
                    } else {
                        reject(new Error(`Failed to start CLI server: ${error.message}`));
                    }
                }
            });

            // Set up a promise that rejects when the process exits (used to race against RPC calls)
            this.processExitPromise = new Promise<never>((_, rejectProcessExit) => {
                this.cliProcess!.on("exit", (code) => {
                    // Give a small delay for stderr to be fully captured
                    setTimeout(() => {
                        const stderrOutput = this.stderrBuffer.trim();
                        if (stderrOutput) {
                            rejectProcessExit(
                                new Error(
                                    `CLI server exited with code ${code}\nstderr: ${stderrOutput}`
                                )
                            );
                        } else {
                            rejectProcessExit(
                                new Error(`CLI server exited unexpectedly with code ${code}`)
                            );
                        }
                    }, 50);
                });
            });
            // Prevent unhandled rejection when process exits normally (we only use this in Promise.race)
            this.processExitPromise.catch(() => {});

            this.cliProcess.on("exit", (code) => {
                if (!resolved) {
                    resolved = true;
                    const stderrOutput = this.stderrBuffer.trim();
                    if (stderrOutput) {
                        reject(
                            new Error(
                                `CLI server exited with code ${code}\nstderr: ${stderrOutput}`
                            )
                        );
                    } else {
                        reject(new Error(`CLI server exited with code ${code}`));
                    }
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
        if (this.options.isChildProcess) {
            return this.connectToParentProcessViaStdio();
        } else if (this.options.useStdio) {
            return this.connectToChildProcessViaStdio();
        } else {
            return this.connectViaTcp();
        }
    }

    /**
     * Connect to child via stdio pipes
     */
    private async connectToChildProcessViaStdio(): Promise<void> {
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
     * Connect to parent via stdio pipes
     */
    private async connectToParentProcessViaStdio(): Promise<void> {
        if (this.cliProcess) {
            throw new Error("CLI child process was unexpectedly started in parent process mode");
        }

        // Create JSON-RPC connection over stdin/stdout
        this.connection = createMessageConnection(
            new StreamMessageReader(process.stdin),
            new StreamMessageWriter(process.stdout)
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

        this.connection.onNotification("session.lifecycle", (notification: unknown) => {
            this.handleSessionLifecycleNotification(notification);
        });

        // Protocol v3 servers send tool calls and permission requests as broadcast events
        // (external_tool.requested / permission.requested) handled in CopilotSession._dispatchEvent.
        // Protocol v2 servers use the older tool.call / permission.request RPC model instead.
        // We always register v2 adapters because handlers are set up before version negotiation;
        // a v3 server will simply never send these requests.
        this.connection.onRequest(
            "tool.call",
            async (params: ToolCallRequestPayload): Promise<ToolCallResponsePayload> =>
                await this.handleToolCallRequestV2(params)
        );

        this.connection.onRequest(
            "permission.request",
            async (params: {
                sessionId: string;
                permissionRequest: unknown;
            }): Promise<{ result: unknown }> => await this.handlePermissionRequestV2(params)
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

        this.connection.onRequest(
            "systemMessage.transform",
            async (params: {
                sessionId: string;
                sections: Record<string, { content: string }>;
            }): Promise<{ sections: Record<string, { content: string }> }> =>
                await this.handleSystemMessageTransform(params)
        );

        // Register client session API handlers.
        const sessions = this.sessions;
        registerClientSessionApiHandlers(this.connection, (sessionId) => {
            const session = sessions.get(sessionId);
            if (!session) throw new Error(`No session found for sessionId: ${sessionId}`);
            return session.clientSessionApis;
        });

        this.connection.onClose(() => {
            this.state = "disconnected";
        });

        this.connection.onError((_error) => {
            this.state = "disconnected";
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

    private handleSessionLifecycleNotification(notification: unknown): void {
        if (
            typeof notification !== "object" ||
            !notification ||
            !("type" in notification) ||
            typeof (notification as { type?: unknown }).type !== "string" ||
            !("sessionId" in notification) ||
            typeof (notification as { sessionId?: unknown }).sessionId !== "string"
        ) {
            return;
        }

        const event = notification as SessionLifecycleEvent;

        // Dispatch to typed handlers for this specific event type
        const typedHandlers = this.typedLifecycleHandlers.get(event.type);
        if (typedHandlers) {
            for (const handler of typedHandlers) {
                try {
                    handler(event);
                } catch {
                    // Ignore handler errors
                }
            }
        }

        // Dispatch to wildcard handlers
        for (const handler of this.sessionLifecycleHandlers) {
            try {
                handler(event);
            } catch {
                // Ignore handler errors
            }
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

    private async handleSystemMessageTransform(params: {
        sessionId: string;
        sections: Record<string, { content: string }>;
    }): Promise<{ sections: Record<string, { content: string }> }> {
        if (
            !params ||
            typeof params.sessionId !== "string" ||
            !params.sections ||
            typeof params.sections !== "object"
        ) {
            throw new Error("Invalid systemMessage.transform payload");
        }

        const session = this.sessions.get(params.sessionId);
        if (!session) {
            throw new Error(`Session not found: ${params.sessionId}`);
        }

        return await session._handleSystemMessageTransform(params.sections);
    }

    // ========================================================================
    // Protocol v2 backward-compatibility adapters
    // ========================================================================

    /**
     * Handles a v2-style tool.call RPC request from the server.
     * Looks up the session and tool handler, executes it, and returns the result
     * in the v2 response format.
     */
    private async handleToolCallRequestV2(
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
            return {
                result: {
                    textResultForLlm: `Tool '${params.toolName}' is not supported by this client instance.`,
                    resultType: "failure",
                    error: `tool '${params.toolName}' not supported`,
                    toolTelemetry: {},
                },
            };
        }

        try {
            const traceparent = (params as { traceparent?: string }).traceparent;
            const tracestate = (params as { tracestate?: string }).tracestate;
            const invocation = {
                sessionId: params.sessionId,
                toolCallId: params.toolCallId,
                toolName: params.toolName,
                arguments: params.arguments,
                traceparent,
                tracestate,
            };
            const result = await handler(params.arguments, invocation);
            return { result: this.normalizeToolResultV2(result) };
        } catch (error) {
            const message = error instanceof Error ? error.message : String(error);
            return {
                result: {
                    textResultForLlm:
                        "Invoking this tool produced an error. Detailed information is not available.",
                    resultType: "failure",
                    error: message,
                    toolTelemetry: {},
                },
            };
        }
    }

    /**
     * Handles a v2-style permission.request RPC request from the server.
     */
    private async handlePermissionRequestV2(params: {
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
            const result = await session._handlePermissionRequestV2(params.permissionRequest);
            return { result };
        } catch (error) {
            if (error instanceof Error && error.message === NO_RESULT_PERMISSION_V2_ERROR) {
                throw error;
            }
            return {
                result: {
                    kind: "denied-no-approval-rule-and-could-not-request-from-user",
                },
            };
        }
    }

    private normalizeToolResultV2(result: unknown): ToolResultObject {
        if (result === undefined || result === null) {
            return {
                textResultForLlm: "Tool returned no result",
                resultType: "failure",
                error: "tool returned no result",
                toolTelemetry: {},
            };
        }

        if (this.isToolResultObject(result)) {
            return result;
        }

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
}
