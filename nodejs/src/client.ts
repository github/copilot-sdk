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
    ErrorCodes,
    MessageConnection,
    ResponseError,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import {
    createServerRpc,
    createInternalServerRpc,
    registerClientGlobalApiHandlers,
    registerClientSessionApiHandlers,
} from "./generated/rpc.js";
import type { OpenCanvasInstance, SessionUpdateOptionsParams } from "./generated/rpc.js";
import { getSdkProtocolVersion } from "./sdkProtocolVersion.js";
import { CopilotSession } from "./session.js";
import { createSessionFsAdapter, type SessionFsProvider } from "./sessionFsProvider.js";
import { createCopilotRequestAdapter } from "./copilotRequestHandler.js";
import type { CopilotRequestHandler } from "./copilotRequestHandler.js";
import { getTraceContext } from "./telemetry.js";
import { ToolSet } from "./toolSet.js";
import type {
    AutoModeSwitchRequest,
    AutoModeSwitchResponse,
    CopilotClientMode,
    CopilotClientOptions,
    CustomAgentConfig,
    ExitPlanModeRequest,
    ExitPlanModeResult,
    ForegroundSessionInfo,
    GetAuthStatusResponse,
    GetStatusResponse,
    InternalRuntimeConnection,
    LargeToolOutputConfig,
    MCPServerConfig,
    ModelInfo,
    ResumeSessionConfig,
    SectionTransformFn,
    SessionConfig,
    SessionConfigBase,
    SystemMessageConfig,
    SessionCapabilities,
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
    TraceContextProvider,
    TypedSessionLifecycleHandler,
} from "./types.js";
import { defaultJoinSessionPermissionHandler } from "./types.js";

/**
 * Minimum protocol version this SDK can communicate with.
 * Servers reporting a version below this are rejected.
 */
const MIN_PROTOCOL_VERSION = 3;
const RUNTIME_SHUTDOWN_TIMEOUT_MS = 10_000;

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

async function withTimeout<T>(promise: Promise<T>, timeoutMs: number, message: string): Promise<T> {
    let timeout: ReturnType<typeof setTimeout> | undefined;
    try {
        return await Promise.race([
            promise,
            new Promise<never>((_, reject) => {
                timeout = setTimeout(() => reject(new Error(message)), timeoutMs);
            }),
        ]);
    } finally {
        if (timeout !== undefined) {
            clearTimeout(timeout);
        }
    }
}

async function waitForChildExit(child: ChildProcess, timeoutMs: number): Promise<boolean> {
    if (child.exitCode != null || child.signalCode != null) {
        return true;
    }

    return new Promise<boolean>((resolve) => {
        let timeout: ReturnType<typeof setTimeout>;
        let settled = false;
        const onExit = () => {
            if (settled) {
                return;
            }
            settled = true;
            clearTimeout(timeout);
            resolve(true);
        };
        timeout = setTimeout(() => {
            if (settled) {
                return;
            }
            settled = true;
            child.off("exit", onExit);
            resolve(false);
        }, timeoutMs);
        child.once("exit", onExit);
        if (child.exitCode != null || child.signalCode != null) {
            onExit();
        }
    });
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
 * Convert MCP server configs from public API format (workingDirectory) to
 * wire format (cwd) expected by the runtime.
 */
function toWireMcpServers(
    mcpServers: Record<string, MCPServerConfig> | undefined
): Record<string, unknown> | undefined {
    if (!mcpServers) return undefined;
    return Object.fromEntries(
        Object.entries(mcpServers).map(([name, server]) => {
            if ("workingDirectory" in server) {
                const { workingDirectory, ...rest } = server;
                return [name, { ...rest, cwd: workingDirectory }];
            }
            return [name, server];
        })
    );
}

/**
 * Convert custom agent configs, transforming nested mcpServers from
 * public API format (workingDirectory) to wire format (cwd).
 */
function toWireCustomAgents(agents: CustomAgentConfig[] | undefined): unknown[] | undefined {
    if (!agents) return undefined;
    return agents.map((agent) => {
        if (!agent.mcpServers) return agent;
        const { mcpServers, ...rest } = agent;
        return { ...rest, mcpServers: toWireMcpServers(mcpServers) };
    });
}

/**
 * Convert a {@link LargeToolOutputConfig} from the public API shape
 * (`outputDirectory`) to the wire shape (`outputDir`).
 */
function toWireLargeOutput(
    config: LargeToolOutputConfig | undefined
): Record<string, unknown> | undefined {
    if (!config) return undefined;
    const { outputDirectory, ...rest } = config;
    const wire: Record<string, unknown> = { ...rest };
    if (outputDirectory !== undefined) {
        wire.outputDir = outputDirectory;
    }
    return wire;
}

function toolFilterListToArray(value: string[] | ToolSet | undefined): string[] | undefined {
    if (value === undefined) {
        return undefined;
    }
    return value instanceof ToolSet ? value.toArray() : value;
}

/**
 * Catches misuse of `availableTools`/`excludedTools` at the SDK boundary so
 * users get an actionable error rather than a silently-empty filter.
 *
 * The runtime treats a bare `"*"` as a literal name match for a tool whose
 * name is the single character `*`, which the runtime's charset guard would
 * reject at registration — so the filter effectively matches nothing. We
 * surface that here as an error pointing the developer at the source-qualified
 * forms produced by {@link ToolSet}.
 */
function validateToolFilterList(field: string, list: string[] | undefined): void {
    if (!list) {
        return;
    }
    for (const entry of list) {
        if (entry === "*") {
            throw new Error(
                `Invalid ${field} entry '*': there is no bare wildcard. ` +
                    "Use one or more of `new ToolSet().addBuiltIn('*')`, `.addMcp('*')`, " +
                    "or `.addCustom('*')` to target a specific source."
            );
        }
    }
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
 * Computes the candidate platform-specific CLI package names for the current
 * platform/arch, mirroring @github/copilot's npm-loader. As of CLI 1.0.64-1 the
 * @github/copilot package is a thin loader and the actual CLI ships in a
 * platform package (e.g. @github/copilot-darwin-arm64). For Linux we try both
 * the glibc and musl variants since only the matching one is installed.
 */
function getCliPlatformPackageNames(): string[] {
    const arch = process.arch;
    const variants = process.platform === "linux" ? ["linux", "linuxmusl"] : [process.platform];
    return variants.map((variant) => `@github/copilot-${variant}-${arch}`);
}

/**
 * Gets the path to the bundled CLI from the platform-specific @github/copilot-*
 * package. Uses index.js directly rather than the native binary so the CLI runs
 * under the current Node.js runtime.
 *
 * In ESM, uses import.meta.resolve directly. In CJS (e.g., VS Code extensions
 * bundled with esbuild format:"cjs"), import.meta is empty so we fall back to
 * walking node_modules to find the package.
 */
function getBundledCliPath(): string {
    const packageNames = getCliPlatformPackageNames();

    if (typeof import.meta.resolve === "function") {
        // ESM: resolve via import.meta.resolve
        for (const packageName of packageNames) {
            try {
                const sdkUrl = import.meta.resolve(`${packageName}/sdk`);
                const sdkPath = fileURLToPath(sdkUrl);
                // sdkPath is like .../node_modules/@github/copilot-<platform>/sdk/index.js
                // Go up two levels to get the package root, then append index.js
                return join(dirname(dirname(sdkPath)), "index.js");
            } catch {
                // Try the next candidate platform package.
            }
        }
        throw new Error(
            `Could not resolve a @github/copilot platform package (tried ${packageNames.join(", ")}). ` +
                `Ensure @github/copilot is installed, or pass cliPath/cliUrl to CopilotClient.`
        );
    }

    // CJS fallback: the platform packages have ESM-only exports so
    // require.resolve cannot reach them. Walk the module search paths instead.
    const req = createRequire(__filename);
    const searchPaths = req.resolve.paths("@github/copilot") ?? [];
    for (const base of searchPaths) {
        for (const packageName of packageNames) {
            const candidate = join(base, ...packageName.split("/"), "index.js");
            if (existsSync(candidate)) {
                return candidate;
            }
        }
    }
    throw new Error(
        `Could not find a @github/copilot platform package (tried ${packageNames.join(", ")}). ` +
            `Searched ${searchPaths.length} paths. ` +
            `Ensure @github/copilot is installed, or pass cliPath/cliUrl to CopilotClient.`
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
 * const client = new CopilotClient({ connection: RuntimeConnection.forUri("localhost:3000") });
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
    private cliStartTimeout: ReturnType<typeof setTimeout> | null = null;
    private cliProcess: ChildProcess | null = null;
    private connection: MessageConnection | null = null;
    private socket: Socket | null = null;
    private runtimePort: number | null = null;
    private actualHost: string = "localhost";
    private state: "disconnected" | "connecting" | "connected" | "error" = "disconnected";
    private sessions: Map<string, CopilotSession> = new Map();
    private stderrBuffer: string = ""; // Captures CLI stderr for error messages
    /** Resolved connection mode chosen in the constructor. */
    private connectionConfig: InternalRuntimeConnection;
    /** Resolved path to the runtime executable (only used for child-process kinds). */
    private resolvedCliPath: string | undefined;
    /** Resolved environment passed to the spawned runtime. */
    private resolvedEnv: Record<string, string | undefined>;
    private options: {
        workingDirectory: string;
        logLevel?: string;
        gitHubToken?: string;
        useLoggedInUser: boolean;
        telemetry?: TelemetryConfig;
        baseDirectory?: string;
        sessionIdleTimeoutSeconds: number;
        enableRemoteSessions: boolean;
        mode: CopilotClientMode;
    };
    private isExternalServer: boolean = false;
    private forceStopping: boolean = false;
    /** Token sent in `connect`; auto-generated when the SDK spawns its own CLI in TCP mode. */
    private effectiveConnectionToken?: string;
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
    private _internalRpc: ReturnType<typeof createInternalServerRpc> | null = null;
    private processExitPromise: Promise<never> | null = null; // Rejects when CLI process exits
    private negotiatedProtocolVersion: number | null = null;
    /** Connection-level session filesystem config, set via constructor option. */
    private sessionFsConfig: SessionFsConfig | null = null;
    private requestHandler: CopilotRequestHandler | null = null;
    private llmInferenceHandlers: import("./generated/rpc.js").ClientGlobalApiHandlers = {};

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
     * Internal RPC surface (e.g. handshake helpers). Not part of the public API.
     * @internal
     */
    private get internalRpc(): ReturnType<typeof createInternalServerRpc> {
        if (!this.connection) {
            throw new Error("Client is not connected. Call start() first.");
        }
        if (!this._internalRpc) {
            this._internalRpc = createInternalServerRpc(this.connection);
        }
        return this._internalRpc;
    }

    private logDebugTiming(message: string, startMs: number): void {
        const level = this.options.logLevel?.toLowerCase();
        if (level === "debug" || level === "all") {
            process.stderr.write(`[copilot-sdk] ${message}. Elapsed=${Date.now() - startMs}ms\n`);
        }
    }

    /**
     * Creates a new CopilotClient instance.
     *
     * @param options - Configuration options for the client
     *
     * @example
     * ```typescript
     * // Default: spawns the bundled runtime over stdio
     * const client = new CopilotClient();
     *
     * // Connect to an existing runtime
     * const client = new CopilotClient({
     *   connection: RuntimeConnection.forUri("localhost:3000"),
     * });
     *
     * // Spawn the runtime over TCP on a chosen port
     * const client = new CopilotClient({
     *   connection: RuntimeConnection.forTcp({ port: 9001 }),
     * });
     *
     * // Use a custom runtime binary
     * const client = new CopilotClient({
     *   connection: RuntimeConnection.forStdio({ path: "/usr/local/bin/copilot" }),
     *   logLevel: "debug",
     * });
     * ```
     */
    constructor(options: CopilotClientOptions = {}) {
        // Resolve the connection mode. `_internalConnection` is set by
        // `joinSession()` to opt into the parent-process stdio path; consumers
        // should always go through the public `connection` field.
        const conn: InternalRuntimeConnection = options._internalConnection ??
            options.connection ?? { kind: "stdio" };

        if (
            conn.kind === "uri" &&
            (options.gitHubToken !== undefined || options.useLoggedInUser !== undefined)
        ) {
            throw new Error(
                "gitHubToken and useLoggedInUser cannot be used with RuntimeConnection.forUri (external server manages its own auth)"
            );
        }
        if (conn.kind === "tcp" && conn.connectionToken !== undefined) {
            if (typeof conn.connectionToken !== "string" || conn.connectionToken.length === 0) {
                throw new Error("connectionToken must be a non-empty string");
            }
        }

        this.connectionConfig = conn;

        if (options.sessionFs) {
            this.validateSessionFsConfig(options.sessionFs);
        }

        // Pre-parse the URI host/port and mark as external if applicable.
        if (conn.kind === "uri") {
            const { host, port } = this.parseCliUrl(conn.url);
            this.actualHost = host;
            this.runtimePort = port;
            this.isExternalServer = true;
        } else if (conn.kind === "parent-process") {
            this.isExternalServer = true;
        }

        // Effective TCP connection token: explicit, else auto-generated when we
        // spawn our own runtime over TCP, else undefined.
        if (conn.kind === "tcp") {
            this.effectiveConnectionToken = conn.connectionToken ?? randomUUID();
        } else if (conn.kind === "uri") {
            this.effectiveConnectionToken = conn.connectionToken;
        }

        this.onListModels = options.onListModels;
        this.onGetTraceContext = options.onGetTraceContext;
        this.sessionFsConfig = options.sessionFs ?? null;
        this.requestHandler = options.requestHandler ?? null;
        this.setupLlmInference();

        const effectiveEnv = options.env ?? process.env;
        this.resolvedEnv = effectiveEnv;
        this.resolvedCliPath =
            conn.kind === "stdio" || conn.kind === "tcp"
                ? (conn.path ?? effectiveEnv.COPILOT_CLI_PATH ?? getBundledCliPath())
                : undefined;

        // Collect extra CLI args from the connection variant (if any).
        const connArgs: readonly string[] =
            conn.kind === "stdio" || conn.kind === "tcp" ? (conn.args ?? []) : [];
        this.connectionExtraArgs = [...connArgs];

        this.options = {
            workingDirectory: options.workingDirectory ?? process.cwd(),
            logLevel: options.logLevel,
            gitHubToken: options.gitHubToken,
            // Default useLoggedInUser to false when gitHubToken is provided, otherwise true.
            useLoggedInUser: options.useLoggedInUser ?? (options.gitHubToken ? false : true),
            telemetry: options.telemetry,
            baseDirectory: options.baseDirectory,
            sessionIdleTimeoutSeconds: options.sessionIdleTimeoutSeconds ?? 0,
            enableRemoteSessions: options.enableRemoteSessions ?? false,
            mode: options.mode ?? "copilot-cli",
        };

        // Empty mode: validate at construction time that the app supplied a
        // per-session persistence location. The runtime is mode-agnostic, so
        // without this check it would silently fall back to ~/.copilot, which
        // defeats the point of empty mode for multi-tenant scenarios.
        if (this.options.mode === "empty") {
            const hasPersistence =
                this.options.baseDirectory !== undefined ||
                this.sessionFsConfig !== null ||
                // External runtimes manage their own persistence layer; the SDK
                // can't enforce it from here.
                conn.kind === "uri" ||
                conn.kind === "parent-process";
            if (!hasPersistence) {
                throw new Error(
                    "CopilotClient was created with mode: 'empty' but neither " +
                        "'baseDirectory' nor 'sessionFs' was set. Empty mode requires " +
                        "an explicit per-session persistence location; pick one."
                );
            }
        }
    }

    private connectionExtraArgs: string[] = [];

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

    private validateSessionFsConfig(config: SessionFsConfig): void {
        if (!config.initialCwd) {
            throw new Error("sessionFs.initialCwd is required");
        }

        if (!config.sessionStatePath) {
            throw new Error("sessionFs.sessionStatePath is required");
        }

        if (config.conventions !== "windows" && config.conventions !== "posix") {
            throw new Error("sessionFs.conventions must be either 'windows' or 'posix'");
        }
    }

    private setupSessionFs(
        session: CopilotSession,
        config: { createSessionFsProvider?: (session: CopilotSession) => SessionFsProvider }
    ): void {
        if (!this.sessionFsConfig) {
            return;
        }
        if (!config.createSessionFsProvider) {
            throw new Error(
                "createSessionFsProvider is required in session config when sessionFs is enabled in client options."
            );
        }
        const provider = config.createSessionFsProvider(session);
        if (this.sessionFsConfig.capabilities?.sqlite && !provider.sqlite) {
            throw new Error(
                "SessionFsConfig declares capabilities.sqlite but the provider does not implement sqlite."
            );
        }
        session.clientSessionApis.sessionFs = createSessionFsAdapter(provider);
    }

    private setupLlmInference(): void {
        if (!this.requestHandler) {
            return;
        }
        this.llmInferenceHandlers = {
            llmInference: createCopilotRequestAdapter(this.requestHandler, () => {
                if (!this.connection) {
                    return undefined;
                }
                this._rpc ??= createServerRpc(this.connection);
                return this._rpc;
            }),
        };
    }

    /**
     * Starts the CLI server and establishes a connection.
     *
     * If connecting to an external server (via cliUrl), only establishes the connection.
     * Otherwise, spawns the CLI server process and then connects.
     *
     * This method is called automatically the first time you create or resume a session.
     *
     * @returns A promise that resolves when the connection is established
     * @throws Error if the server fails to start or the connection fails
     *
     * @example
     * ```typescript
     * const client = new CopilotClient();
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
                    capabilities: this.sessionFsConfig.capabilities,
                });
            }

            // If a request handler was configured, register it. The runtime
            // will then route outbound model HTTP requests through the
            // registered handler for the duration of each session.
            if (this.requestHandler) {
                await this.connection!.sendRequest("llmInference.setProvider", {});
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
     * 2. Requests runtime shutdown for SDK-owned CLI processes
     * 3. Closes the JSON-RPC connection
     * 4. Terminates the CLI server process (if spawned by this client)
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

        // Ask SDK-owned runtimes to flush and clean up before we tear down
        // their transport/process. External runtimes may be shared, so only
        // close our connection to them.
        if (this.connection && this.cliProcess && !this.isExternalServer) {
            const runtimeShutdownStart = Date.now();
            const shutdownPromise = this.rpc.runtime.shutdown();
            void shutdownPromise.catch(() => undefined);
            try {
                await withTimeout(
                    shutdownPromise,
                    RUNTIME_SHUTDOWN_TIMEOUT_MS,
                    `runtime.shutdown timed out after ${RUNTIME_SHUTDOWN_TIMEOUT_MS}ms`
                );
                this.logDebugTiming(
                    "CopilotClient.stop runtime shutdown complete",
                    runtimeShutdownStart
                );
            } catch (error) {
                this.logDebugTiming(
                    "CopilotClient.stop runtime shutdown failed",
                    runtimeShutdownStart
                );
                errors.push(
                    new Error(
                        `Failed to gracefully shut down runtime: ${error instanceof Error ? error.message : String(error)}`
                    )
                );
            }
        }

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
            this._internalRpc = null;
        }

        // Clear models cache
        this.modelsCache = null;

        // Close the TCP socket and wait for the close to complete before returning.
        if (this.socket) {
            const socket = this.socket;
            this.socket = null;
            try {
                if (!socket.destroyed) {
                    await new Promise<void>((resolve) => {
                        socket.once("close", () => resolve());
                        socket.end();
                    });
                }
            } catch (error) {
                errors.push(
                    new Error(
                        `Failed to close socket: ${error instanceof Error ? error.message : String(error)}`
                    )
                );
            }
        }

        // The runtime completes all cleanup before responding to
        // runtime.shutdown and then leaves termination to us; it deliberately
        // keeps its JSON-RPC server alive to send the response and never
        // self-exits. Waiting a grace window for a self-exit that will never
        // come just wastes time, so terminate the child immediately and only
        // wait to reap it.
        if (this.cliProcess && !this.isExternalServer) {
            const child = this.cliProcess;
            this.cliProcess = null;
            try {
                if (child.exitCode == null && child.signalCode == null) {
                    child.kill();
                    if (!(await waitForChildExit(child, RUNTIME_SHUTDOWN_TIMEOUT_MS))) {
                        errors.push(
                            new Error(
                                `Timed out waiting for CLI process to exit after kill: ${RUNTIME_SHUTDOWN_TIMEOUT_MS}ms`
                            )
                        );
                    }
                }
            } catch (error) {
                errors.push(
                    new Error(
                        `Failed to kill CLI process: ${error instanceof Error ? error.message : String(error)}`
                    )
                );
            }
        }
        if (this.cliStartTimeout) {
            clearTimeout(this.cliStartTimeout);
            this.cliStartTimeout = null;
        }

        this.state = "disconnected";
        this.runtimePort = null;
        this.stderrBuffer = "";
        this.processExitPromise = null;

        return errors;
    }

    /**
     * Alias for {@link stop} that lets `CopilotClient` participate in `await using`
     * blocks for automatic cleanup.
     *
     * @example
     * ```typescript
     * await using client = new CopilotClient();
     * const session = await client.createSession({ onPermissionRequest: approveAll });
     * await session.sendAndWait("Hello");
     * // client.stop() is called automatically when the block exits.
     * ```
     */
    async [Symbol.asyncDispose](): Promise<void> {
        await this.stop();
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
            this._internalRpc = null;
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

        if (this.cliStartTimeout) {
            clearTimeout(this.cliStartTimeout);
            this.cliStartTimeout = null;
        }

        this.state = "disconnected";
        this.runtimePort = null;
        this.stderrBuffer = "";
        this.processExitPromise = null;
    }

    /**
     * Creates a new conversation session with the Copilot CLI.
     *
     * Sessions maintain conversation state, handle events, and manage tool execution.
     * If the client is not connected, this method automatically starts the connection.
     *
     * @param config - Optional configuration for the session
     * @returns A promise that resolves with the created session
     * @throws Error if the client fails to start
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
    /**
     * Normalizes session-level tool filter options. Converts {@link ToolSet}
     * instances to plain string arrays, rejects misuse (bare `"*"`) and the
     * missing-availableTools case in `mode = "empty"`.
     *
     * The SDK always sends `toolFilterPrecedence: "excluded"` so callers can
     * compose include + exclude lists naturally (e.g. "everything matching X
     * except Y") regardless of mode. Allowlist-precedence is intentionally not
     * exposed — it's available on the runtime side as a CLI-only concession to
     * legacy behavior, but SDK consumers always get the composable semantics.
     *
     * @internal
     */
    private resolveToolFilterOptions(config: {
        availableTools?: string[] | ToolSet;
        excludedTools?: string[] | ToolSet;
    }): {
        availableTools: string[] | undefined;
        excludedTools: string[] | undefined;
        toolFilterPrecedence: "excluded";
    } {
        const availableTools = toolFilterListToArray(config.availableTools);
        const excludedTools = toolFilterListToArray(config.excludedTools);
        validateToolFilterList("availableTools", availableTools);
        validateToolFilterList("excludedTools", excludedTools);

        if (this.options.mode === "empty") {
            if (availableTools === undefined) {
                throw new Error(
                    "CopilotClient is in mode: 'empty' but the session config did not " +
                        "specify 'availableTools'. Empty mode requires every session to " +
                        "explicitly opt into the tools it wants — e.g. " +
                        "`new ToolSet().addBuiltIn(BuiltInTools.Isolated)`."
                );
            }
        }

        return { availableTools, excludedTools, toolFilterPrecedence: "excluded" };
    }

    /** Mode-specific defaults spread under the caller's config (app values win). */
    private configDefaultsForMode(): Partial<SessionConfigBase> {
        if (this.options.mode === "empty") {
            return {
                enableSessionTelemetry: false,
                mcpOAuthTokenStorage: "in-memory",
                skipEmbeddingRetrieval: true,
                embeddingCacheStorage: "in-memory",
                enableOnDemandInstructionDiscovery: false,
                enableFileHooks: false,
                enableHostGitOperations: false,
                enableSessionStore: false,
                enableSkills: false,
                memory: { enabled: false },
            };
        }
        return {};
    }

    /**
     * Returns the systemMessage config to use, adjusted for the current mode.
     * In empty mode we ensure the environment_context section is removed
     * unless the app has already taken control of it. `append` (and
     * unspecified) mode is promoted to `customize` so we can also strip
     * environment_context; the caller's `content` is preserved verbatim
     * because the runtime appends it as additional instructions in both
     * customize and append modes.
     */
    private getSystemMessageConfigForMode(
        supplied: SystemMessageConfig | undefined
    ): SystemMessageConfig | undefined {
        if (this.options.mode !== "empty") return supplied;
        if (!supplied) {
            return {
                mode: "customize",
                sections: { environment_context: { action: "remove" } },
            };
        }
        switch (supplied.mode) {
            case "replace":
                return supplied;
            case "customize":
                if (supplied.sections?.environment_context) return supplied;
                return {
                    ...supplied,
                    sections: {
                        ...supplied.sections,
                        environment_context: { action: "remove" },
                    },
                };
            case "append":
            case undefined:
                // Promote to customize so we can also strip environment_context.
                // The runtime appends `content` to additional instructions in
                // both customize and append modes, so the caller's text is
                // preserved verbatim.
                return {
                    mode: "customize",
                    content: supplied.content,
                    sections: { environment_context: { action: "remove" } },
                };
        }
    }

    /**
     * Mode-specific options applied via session.options.update after create/resume.
     *
     * In empty mode, defaults the four overridable feature flags to safe values
     * (caller values from `config` win). `installedPlugins=[]` is unconditional
     * in empty mode — apps that need custom plugins should switch modes.
     */
    private async updateSessionOptionsForMode(
        session: CopilotSession,
        config: SessionConfigBase
    ): Promise<void> {
        const patch: SessionUpdateOptionsParams = {};
        if (this.options.mode === "empty") {
            patch.skipCustomInstructions = config.skipCustomInstructions ?? true;
            patch.customAgentsLocalOnly = config.customAgentsLocalOnly ?? true;
            patch.coauthorEnabled = config.coauthorEnabled ?? false;
            patch.manageScheduleEnabled = config.manageScheduleEnabled ?? false;
            patch.installedPlugins = [];
        } else {
            if (config.skipCustomInstructions !== undefined)
                patch.skipCustomInstructions = config.skipCustomInstructions;
            if (config.customAgentsLocalOnly !== undefined)
                patch.customAgentsLocalOnly = config.customAgentsLocalOnly;
            if (config.coauthorEnabled !== undefined)
                patch.coauthorEnabled = config.coauthorEnabled;
            if (config.manageScheduleEnabled !== undefined)
                patch.manageScheduleEnabled = config.manageScheduleEnabled;
        }
        if (Object.keys(patch).length === 0) {
            return;
        }
        try {
            await session.rpc.options.update(patch);
        } catch (e) {
            // The runtime session exists but the post-create options
            // patch failed — best-effort disconnect so we don't leak
            // it (in empty mode it would otherwise keep running with
            // permissive defaults).
            try {
                await session.disconnect();
            } catch {
                // Swallow: original error is the one the caller needs.
            }
            throw e;
        }
    }

    async createSession(config: SessionConfig): Promise<CopilotSession> {
        if (!this.connection) {
            await this.start();
        }

        config = { ...this.configDefaultsForMode(), ...config };
        config.systemMessage = this.getSystemMessageConfigForMode(config.systemMessage);

        // For cloud sessions, let the CLI/server assign the session id and
        // register the session lazily once the response arrives. For non-cloud
        // sessions we generate the id client-side (when the caller didn't
        // supply one) so the session can be registered BEFORE the RPC — the
        // CLI may issue session-scoped requests (e.g. `sessionFs.writeFile`
        // for workspace metadata) during `session.create` processing, before
        // it has sent the response.
        const callerSessionId = config.sessionId;
        const useServerGeneratedId = config.cloud != null && callerSessionId == null;
        const localSessionId = useServerGeneratedId ? undefined : (callerSessionId ?? randomUUID());

        // Extract transform callbacks from system message config before serialization.
        const { wirePayload: wireSystemMessage, transformCallbacks } = extractTransformCallbacks(
            config.systemMessage
        );

        // Creates the session object, wires up handlers, and registers it in
        // the sessions map.
        const initializeSession = (sessionId: string): CopilotSession => {
            const s = new CopilotSession(
                sessionId,
                this.connection!,
                undefined,
                this.onGetTraceContext
            );
            s.registerTools(config.tools);
            s.registerCanvases(config.canvases);
            s.registerCommands(config.commands);
            s.registerPermissionHandler(config.onPermissionRequest);
            if (config.onUserInputRequest) {
                s.registerUserInputHandler(config.onUserInputRequest);
            }
            if (config.onElicitationRequest) {
                s.registerElicitationHandler(config.onElicitationRequest);
            }
            if (config.onExitPlanModeRequest) {
                s.registerExitPlanModeHandler(config.onExitPlanModeRequest);
            }
            if (config.onAutoModeSwitchRequest) {
                s.registerAutoModeSwitchHandler(config.onAutoModeSwitchRequest);
            }
            if (config.hooks) {
                s.registerHooks(config.hooks);
            }
            if (transformCallbacks) {
                s.registerTransformCallbacks(transformCallbacks);
            }
            if (config.onEvent) {
                s.on(config.onEvent);
            }
            this.sessions.set(sessionId, s);
            this.setupSessionFs(s, config);
            return s;
        };

        let session: CopilotSession | undefined;
        let registeredId: string | undefined;

        // Pre-register non-cloud sessions BEFORE issuing the RPC so any
        // session-scoped requests the CLI emits during `session.create`
        // processing (e.g. sessionFs.writeFile for workspace metadata) can be
        // routed to the correct handlers.
        if (localSessionId !== undefined) {
            session = initializeSession(localSessionId);
            registeredId = localSessionId;
        }

        const toolFilterOptions = this.resolveToolFilterOptions(config);

        try {
            const response = await this.connection!.sendRequest("session.create", {
                ...(await getTraceContext(this.onGetTraceContext)),
                model: config.model,
                sessionId: localSessionId,
                clientName: config.clientName,
                reasoningEffort: config.reasoningEffort,
                reasoningSummary: config.reasoningSummary,
                contextTier: config.contextTier,
                tools: config.tools?.map((tool) => ({
                    name: tool.name,
                    description: tool.description,
                    parameters: toJsonSchema(tool.parameters),
                    overridesBuiltInTool: tool.overridesBuiltInTool,
                    skipPermission: tool.skipPermission,
                    defer: tool.defer,
                })),
                canvases: config.canvases?.map((canvas) => canvas.declaration),
                requestCanvasRenderer: config.requestCanvasRenderer,
                requestExtensions: config.requestExtensions,
                extensionSdkPath: config.extensionSdkPath,
                extensionInfo: config.extensionInfo,
                commands: config.commands?.map((cmd) => ({
                    name: cmd.name,
                    description: cmd.description,
                })),
                systemMessage: wireSystemMessage,
                availableTools: toolFilterOptions.availableTools,
                excludedTools: toolFilterOptions.excludedTools,
                toolFilterPrecedence: toolFilterOptions.toolFilterPrecedence,
                provider: config.provider,
                providers: config.providers,
                models: config.models,
                enableSessionTelemetry: config.enableSessionTelemetry,
                modelCapabilities: config.modelCapabilities,
                largeOutput: toWireLargeOutput(config.largeOutput),
                requestPermission: !!config.onPermissionRequest,
                requestUserInput: !!config.onUserInputRequest,
                requestElicitation: !!config.onElicitationRequest,
                ...(config.enableMcpApps ? { requestMcpApps: true } : {}),
                requestExitPlanMode: !!config.onExitPlanModeRequest,
                requestAutoModeSwitch: !!config.onAutoModeSwitchRequest,
                hooks: !!(config.hooks && Object.values(config.hooks).some(Boolean)),
                workingDirectory: config.workingDirectory,
                streaming: config.streaming,
                includeSubAgentStreamingEvents: config.includeSubAgentStreamingEvents ?? true,
                mcpServers: toWireMcpServers(config.mcpServers),
                mcpOAuthTokenStorage: config.mcpOAuthTokenStorage,
                envValueMode: "direct",
                customAgents: toWireCustomAgents(config.customAgents),
                defaultAgent: config.defaultAgent,
                agent: config.agent,
                configDir: config.configDirectory,
                enableConfigDiscovery: config.enableConfigDiscovery,
                skipEmbeddingRetrieval: config.skipEmbeddingRetrieval,
                embeddingCacheStorage: config.embeddingCacheStorage,
                organizationCustomInstructions: config.organizationCustomInstructions,
                enableOnDemandInstructionDiscovery: config.enableOnDemandInstructionDiscovery,
                enableFileHooks: config.enableFileHooks,
                enableHostGitOperations: config.enableHostGitOperations,
                enableSessionStore: config.enableSessionStore,
                enableSkills: config.enableSkills,
                skillDirectories: config.skillDirectories,
                pluginDirectories: config.pluginDirectories,
                instructionDirectories: config.instructionDirectories,
                disabledSkills: config.disabledSkills,
                infiniteSessions: config.infiniteSessions,
                memory: config.memory,
                gitHubToken: config.gitHubToken,
                remoteSession: config.remoteSession,
                cloud: config.cloud,
            });

            const {
                sessionId: returnedSessionId,
                workspacePath,
                capabilities,
            } = response as {
                sessionId: string;
                workspacePath?: string;
                capabilities?: SessionCapabilities;
            };
            if (!returnedSessionId) {
                throw new Error("session.create response did not include a sessionId");
            }
            if (localSessionId !== undefined && localSessionId !== returnedSessionId) {
                throw new Error(
                    `session.create returned sessionId ${returnedSessionId} but the caller requested ${localSessionId}`
                );
            }
            if (session === undefined) {
                // Cloud / server-assigned path: register the session now that
                // the CLI has told us which id it chose.
                session = initializeSession(returnedSessionId);
                registeredId = returnedSessionId;
            }
            session["_workspacePath"] = workspacePath;
            session.setCapabilities(capabilities);

            await this.updateSessionOptionsForMode(session, config);
        } catch (e) {
            if (registeredId !== undefined) {
                this.sessions.delete(registeredId);
            }
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
        if (!this.connection) {
            await this.start();
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
        session.registerCanvases(config.canvases);
        session.registerCommands(config.commands);
        session.registerPermissionHandler(config.onPermissionRequest);
        if (config.onUserInputRequest) {
            session.registerUserInputHandler(config.onUserInputRequest);
        }
        if (config.onElicitationRequest) {
            session.registerElicitationHandler(config.onElicitationRequest);
        }
        if (config.onExitPlanModeRequest) {
            session.registerExitPlanModeHandler(config.onExitPlanModeRequest);
        }
        if (config.onAutoModeSwitchRequest) {
            session.registerAutoModeSwitchHandler(config.onAutoModeSwitchRequest);
        }
        if (config.hooks) {
            session.registerHooks(config.hooks);
        }

        config = { ...this.configDefaultsForMode(), ...config };
        config.systemMessage = this.getSystemMessageConfigForMode(config.systemMessage);

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
        this.setupSessionFs(session, config);

        const toolFilterOptions = this.resolveToolFilterOptions(config);

        try {
            const response = await this.connection!.sendRequest("session.resume", {
                ...(await getTraceContext(this.onGetTraceContext)),
                sessionId,
                clientName: config.clientName,
                model: config.model,
                reasoningEffort: config.reasoningEffort,
                reasoningSummary: config.reasoningSummary,
                contextTier: config.contextTier,
                systemMessage: wireSystemMessage,
                availableTools: toolFilterOptions.availableTools,
                excludedTools: toolFilterOptions.excludedTools,
                toolFilterPrecedence: toolFilterOptions.toolFilterPrecedence,
                enableSessionTelemetry: config.enableSessionTelemetry,
                tools: config.tools?.map((tool) => ({
                    name: tool.name,
                    description: tool.description,
                    parameters: toJsonSchema(tool.parameters),
                    overridesBuiltInTool: tool.overridesBuiltInTool,
                    skipPermission: tool.skipPermission,
                    defer: tool.defer,
                })),
                canvases: config.canvases?.map((canvas) => canvas.declaration),
                requestCanvasRenderer: config.requestCanvasRenderer,
                requestExtensions: config.requestExtensions,
                extensionSdkPath: config.extensionSdkPath,
                extensionInfo: config.extensionInfo,
                commands: config.commands?.map((cmd) => ({
                    name: cmd.name,
                    description: cmd.description,
                })),
                provider: config.provider,
                providers: config.providers,
                models: config.models,
                modelCapabilities: config.modelCapabilities,
                largeOutput: toWireLargeOutput(config.largeOutput),
                requestPermission:
                    config.onPermissionRequest !== defaultJoinSessionPermissionHandler,
                requestUserInput: !!config.onUserInputRequest,
                requestElicitation: !!config.onElicitationRequest,
                ...(config.enableMcpApps ? { requestMcpApps: true } : {}),
                requestExitPlanMode: !!config.onExitPlanModeRequest,
                requestAutoModeSwitch: !!config.onAutoModeSwitchRequest,
                hooks: !!(config.hooks && Object.values(config.hooks).some(Boolean)),
                workingDirectory: config.workingDirectory,
                configDir: config.configDirectory,
                enableConfigDiscovery: config.enableConfigDiscovery,
                skipEmbeddingRetrieval: config.skipEmbeddingRetrieval,
                embeddingCacheStorage: config.embeddingCacheStorage,
                organizationCustomInstructions: config.organizationCustomInstructions,
                enableOnDemandInstructionDiscovery: config.enableOnDemandInstructionDiscovery,
                enableFileHooks: config.enableFileHooks,
                enableHostGitOperations: config.enableHostGitOperations,
                enableSessionStore: config.enableSessionStore,
                enableSkills: config.enableSkills,
                streaming: config.streaming,
                includeSubAgentStreamingEvents: config.includeSubAgentStreamingEvents ?? true,
                mcpServers: toWireMcpServers(config.mcpServers),
                mcpOAuthTokenStorage: config.mcpOAuthTokenStorage,
                envValueMode: "direct",
                customAgents: toWireCustomAgents(config.customAgents),
                defaultAgent: config.defaultAgent,
                agent: config.agent,
                skillDirectories: config.skillDirectories,
                pluginDirectories: config.pluginDirectories,
                instructionDirectories: config.instructionDirectories,
                disabledSkills: config.disabledSkills,
                infiniteSessions: config.infiniteSessions,
                memory: config.memory,
                disableResume: config.suppressResumeEvent,
                continuePendingWork: config.continuePendingWork,
                gitHubToken: config.gitHubToken,
                remoteSession: config.remoteSession,
                openCanvases: config.openCanvases,
            });

            const { workspacePath, capabilities, openCanvases } = response as {
                sessionId: string;
                workspacePath?: string;
                capabilities?: SessionCapabilities;
                openCanvases?: OpenCanvasInstance[];
            };
            session["_workspacePath"] = workspacePath;
            session.setCapabilities(capabilities);
            session.setOpenCanvases(openCanvases ?? []);

            await this.updateSessionOptionsForMode(session, config);
        } catch (e) {
            this.sessions.delete(sessionId);
            throw e;
        }

        return session;
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
    ): Promise<{ message: string; timestamp: string; protocolVersion?: number }> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }

        const result = await this.connection.sendRequest("ping", { message });
        return result as {
            message: string;
            timestamp: string;
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

                // Normalize model capabilities — some models (e.g. embedding models)
                // may omit 'supports' or 'limits' in their capabilities.
                for (const model of models) {
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    const m = model as any;
                    if (!m.capabilities) {
                        m.capabilities = {
                            supports: {},
                            limits: { max_context_window_tokens: 0 },
                        };
                    } else {
                        if (!m.capabilities.supports) m.capabilities.supports = {};
                        if (!m.capabilities.limits) {
                            m.capabilities.limits = { max_context_window_tokens: 0 };
                        } else if (m.capabilities.limits.max_context_window_tokens === undefined) {
                            m.capabilities.limits.max_context_window_tokens = 0;
                        }
                    }
                }
            }

            // Update cache before releasing lock (copy to prevent external mutation)
            this.modelsCache = [...models];

            return [...models]; // Return a copy to prevent cache mutation
        } finally {
            resolveLock!();
        }
    }

    /**
     * Send the `connect` handshake (carrying the optional token) and verify the
     * server's protocol version. Falls back to `ping` against legacy servers
     * that don't implement `connect`.
     */
    private async verifyProtocolVersion(): Promise<void> {
        if (!this.connection) {
            throw new Error("Client not connected");
        }
        const maxVersion = getSdkProtocolVersion();
        const raceAgainstExit = <T>(p: Promise<T>): Promise<T> =>
            this.processExitPromise ? Promise.race([p, this.processExitPromise]) : p;

        let serverVersion: number | undefined;
        try {
            const result = await raceAgainstExit(
                this.internalRpc.connect({ token: this.effectiveConnectionToken })
            );
            serverVersion = result.protocolVersion;
        } catch (err) {
            if (
                err instanceof ResponseError &&
                (err.code === ErrorCodes.MethodNotFound ||
                    err.message === "Unhandled method connect")
            ) {
                // Legacy server without `connect`; fall back to `ping`. A token, if any,
                // is silently dropped — the legacy server can't enforce one.
                serverVersion = (await raceAgainstExit(this.ping())).protocolVersion;
            } else {
                throw err;
            }
        }

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

        // Transform filter to wire format (workingDirectory → cwd)
        let wireFilter: Record<string, unknown> | undefined;
        if (filter) {
            const { workingDirectory, ...rest } = filter;
            wireFilter = { ...rest, cwd: workingDirectory };
        }

        const response = await this.connection.sendRequest("session.list", {
            filter: wireFilter,
        });
        const { sessions } = response as {
            sessions: Array<{
                sessionId: string;
                startTime: string;
                modifiedTime: string;
                summary?: string;
                isRemote: boolean;
                context?: { cwd: string; gitRoot?: string; repository?: string; branch?: string };
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
                context?: { cwd: string; gitRoot?: string; repository?: string; branch?: string };
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
        context?: { cwd: string; gitRoot?: string; repository?: string; branch?: string };
    }): SessionMetadata {
        const { context } = raw;
        return {
            sessionId: raw.sessionId,
            startTime: new Date(raw.startTime),
            modifiedTime: new Date(raw.modifiedTime),
            summary: raw.summary,
            isRemote: raw.isRemote,
            context: context
                ? {
                      workingDirectory: context.cwd,
                      gitRoot: context.gitRoot,
                      repository: context.repository,
                      branch: context.branch,
                  }
                : undefined,
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
     * const unsubscribe = client.onLifecycle("session.foreground", (event) => {
     *   console.log(`Session ${event.sessionId} is now displayed in TUI`);
     * });
     *
     * // Later, to stop receiving events:
     * unsubscribe();
     * ```
     */
    onLifecycle<K extends SessionLifecycleEventType>(
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
     * const unsubscribe = client.onLifecycle((event) => {
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
    onLifecycle(handler: SessionLifecycleHandler): () => void;

    onLifecycle<K extends SessionLifecycleEventType>(
        eventTypeOrHandler: K | SessionLifecycleHandler,
        handler?: TypedSessionLifecycleHandler<K>
    ): () => void {
        // Overload 1: onLifecycle(eventType, handler) - typed event subscription
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

        // Overload 2: onLifecycle(handler) - wildcard subscription
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

            const args = [...this.connectionExtraArgs, "--headless", "--no-auto-update"];

            if (this.options.logLevel) {
                args.push("--log-level", this.options.logLevel);
            }

            // Choose transport mode based on the resolved connection config.
            if (this.connectionConfig.kind === "stdio") {
                args.push("--stdio");
            } else if (this.connectionConfig.kind === "tcp") {
                const requestedPort = this.connectionConfig.port ?? 0;
                if (requestedPort > 0) {
                    args.push("--port", requestedPort.toString());
                }
            }

            // Add auth-related flags
            if (this.options.gitHubToken) {
                args.push("--auth-token-env", "COPILOT_SDK_AUTH_TOKEN");
            }
            if (!this.options.useLoggedInUser) {
                args.push("--no-auto-login");
            }

            if (
                this.options.sessionIdleTimeoutSeconds !== undefined &&
                this.options.sessionIdleTimeoutSeconds > 0
            ) {
                args.push(
                    "--session-idle-timeout",
                    this.options.sessionIdleTimeoutSeconds.toString()
                );
            }

            if (this.options.enableRemoteSessions) {
                args.push("--remote");
            }

            // Suppress debug/trace output that might pollute stdout
            const envWithoutNodeDebug = { ...this.resolvedEnv };
            delete envWithoutNodeDebug.NODE_DEBUG;

            // Set auth token in environment if provided
            if (this.options.gitHubToken) {
                envWithoutNodeDebug.COPILOT_SDK_AUTH_TOKEN = this.options.gitHubToken;
            }

            if (this.effectiveConnectionToken) {
                envWithoutNodeDebug.COPILOT_CONNECTION_TOKEN = this.effectiveConnectionToken;
            }

            if (this.options.baseDirectory) {
                envWithoutNodeDebug.COPILOT_HOME = this.options.baseDirectory;
            }

            // In empty mode, disable the system keychain. Keytar reads from a
            // process-wide store that's shared across sessions, which is unsafe
            // for multi-tenant hosts. The runtime falls back to file-based
            // credential storage scoped to COPILOT_HOME.
            if (this.options.mode === "empty") {
                envWithoutNodeDebug.COPILOT_DISABLE_KEYTAR = "1";
            }

            if (!this.resolvedCliPath) {
                throw new Error(
                    "Path to Copilot CLI is required. Please supply it via " +
                        "`RuntimeConnection.forStdio({ path })` or " +
                        "`RuntimeConnection.forTcp({ path })`, set the COPILOT_CLI_PATH " +
                        "environment variable, or use `RuntimeConnection.forUri(...)` to " +
                        "connect to an already-running runtime."
                );
            }

            // Set OpenTelemetry environment variables if telemetry is configured
            if (this.options.telemetry) {
                const t = this.options.telemetry;
                envWithoutNodeDebug.COPILOT_OTEL_ENABLED = "true";
                if (t.otlpEndpoint !== undefined)
                    envWithoutNodeDebug.OTEL_EXPORTER_OTLP_ENDPOINT = t.otlpEndpoint;
                if (t.otlpProtocol !== undefined)
                    envWithoutNodeDebug.OTEL_EXPORTER_OTLP_PROTOCOL = t.otlpProtocol;
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
            if (!existsSync(this.resolvedCliPath)) {
                throw new Error(
                    `Copilot CLI not found at ${this.resolvedCliPath}. Ensure @github/copilot is installed.`
                );
            }

            const stdioConfig: ["pipe", "pipe", "pipe"] | ["ignore", "pipe", "pipe"] =
                this.connectionConfig.kind === "stdio"
                    ? ["pipe", "pipe", "pipe"]
                    : ["ignore", "pipe", "pipe"];

            // For .js files, spawn node explicitly; for executables, spawn directly
            const isJsFile = this.resolvedCliPath.endsWith(".js");
            if (isJsFile) {
                this.cliProcess = spawn(getNodeExecPath(), [this.resolvedCliPath, ...args], {
                    stdio: stdioConfig,
                    cwd: this.options.workingDirectory,
                    env: envWithoutNodeDebug,
                    windowsHide: true,
                });
            } else {
                this.cliProcess = spawn(this.resolvedCliPath, args, {
                    stdio: stdioConfig,
                    cwd: this.options.workingDirectory,
                    env: envWithoutNodeDebug,
                    windowsHide: true,
                });
            }

            let stdout = "";
            let resolved = false;

            // For stdio mode, we're ready immediately after spawn
            if (this.connectionConfig.kind === "stdio") {
                resolved = true;
                resolve();
            } else {
                // For TCP mode, wait for port announcement
                this.cliProcess.stdout?.on("data", (data: Buffer) => {
                    stdout += data.toString();
                    const match = stdout.match(/listening on port (\d+)/i);
                    if (match && !resolved) {
                        this.runtimePort = parseInt(match[1], 10);
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

            // Timeout after 30 seconds (Windows CI runners can be slow to spawn processes)
            this.cliStartTimeout = setTimeout(() => {
                if (!resolved) {
                    resolved = true;
                    reject(new Error("Timeout waiting for CLI server to start"));
                }
            }, 30000);
        });
    }

    /**
     * Connect to the CLI server (via socket or stdio)
     */
    private async connectToServer(): Promise<void> {
        switch (this.connectionConfig.kind) {
            case "parent-process":
                return this.connectToParentProcessViaStdio();
            case "stdio":
                return this.connectToChildProcessViaStdio();
            case "tcp":
            case "uri":
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
        if (!this.runtimePort) {
            throw new Error("Server port not available");
        }

        return new Promise((resolve, reject) => {
            this.socket = new Socket();

            const connectionTimeout = setTimeout(() => {
                this.socket?.destroy();
                reject(new Error("Timeout connecting to CLI server"));
            }, 10000);

            this.socket.connect(this.runtimePort!, this.actualHost, () => {
                clearTimeout(connectionTimeout);
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
                clearTimeout(connectionTimeout);
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
            "exitPlanMode.request",
            async (
                params: ExitPlanModeRequest & { sessionId: string }
            ): Promise<ExitPlanModeResult> => await this.handleExitPlanModeRequest(params)
        );

        this.connection.onRequest(
            "autoModeSwitch.request",
            async (
                params: AutoModeSwitchRequest & { sessionId: string }
            ): Promise<{ response: AutoModeSwitchResponse }> =>
                await this.handleAutoModeSwitchRequest(params)
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

        // Register client *global* API handlers (e.g. LLM inference) on the
        // same connection. These methods carry no implicit sessionId dispatch
        // — the runtime calls into a single handler for the whole connection.
        registerClientGlobalApiHandlers(this.connection, this.llmInferenceHandlers);

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

        const raw = notification as {
            type: SessionLifecycleEventType;
            sessionId: string;
            metadata?: { startTime?: string; modifiedTime?: string; summary?: string };
        };

        let metadata: SessionLifecycleEvent["metadata"];
        if (raw.metadata && raw.metadata.startTime && raw.metadata.modifiedTime) {
            metadata = {
                startTime: new Date(raw.metadata.startTime),
                modifiedTime: new Date(raw.metadata.modifiedTime),
                summary: raw.metadata.summary,
            };
        }

        const event = {
            type: raw.type,
            sessionId: raw.sessionId,
            metadata,
        } as SessionLifecycleEvent;

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

    private async handleExitPlanModeRequest(
        params: ExitPlanModeRequest & { sessionId: string }
    ): Promise<ExitPlanModeResult> {
        if (
            !params ||
            typeof params.sessionId !== "string" ||
            typeof params.summary !== "string" ||
            !Array.isArray(params.actions) ||
            typeof params.recommendedAction !== "string"
        ) {
            throw new Error("Invalid exit plan mode request payload");
        }

        const session = this.sessions.get(params.sessionId);
        if (!session) {
            throw new Error(`Session not found: ${params.sessionId}`);
        }

        return await session._handleExitPlanModeRequest({
            summary: params.summary,
            planContent: params.planContent,
            actions: params.actions,
            recommendedAction: params.recommendedAction,
        });
    }

    private async handleAutoModeSwitchRequest(
        params: AutoModeSwitchRequest & { sessionId: string }
    ): Promise<{ response: AutoModeSwitchResponse }> {
        if (!params || typeof params.sessionId !== "string") {
            throw new Error("Invalid auto mode switch request payload");
        }

        const session = this.sessions.get(params.sessionId);
        if (!session) {
            throw new Error(`Session not found: ${params.sessionId}`);
        }

        const response = await session._handleAutoModeSwitchRequest({
            errorCode: params.errorCode,
            retryAfterSeconds: params.retryAfterSeconds,
        });
        return { response };
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
}
