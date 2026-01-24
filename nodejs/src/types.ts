/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Type definitions for the Copilot SDK
 */

// Import and re-export generated session event types
import type { SessionEvent as GeneratedSessionEvent } from "./generated/session-events.js";
export type SessionEvent = GeneratedSessionEvent;

/**
 * Options for creating a CopilotClient
 */
export interface CopilotClientOptions {
    /**
     * Path to the Copilot CLI executable
     * @default "copilot" (searches PATH)
     */
    cliPath?: string;

    /**
     * Extra arguments to pass to the CLI executable (inserted before SDK-managed args)
     */
    cliArgs?: string[];

    /**
     * Working directory for the CLI process
     * If not set, inherits the current process's working directory
     */
    cwd?: string;

    /**
     * Port for the CLI server (TCP mode only)
     * @default 0 (random available port)
     */
    port?: number;

    /**
     * Use stdio transport instead of TCP
     * When true, communicates with CLI via stdin/stdout pipes
     * @default true
     */
    useStdio?: boolean;

    /**
     * URL of an existing Copilot CLI server to connect to over TCP
     * When provided, the client will not spawn a CLI process
     * Format: "host:port" or "http://host:port" or just "port" (defaults to localhost)
     * Examples: "localhost:8080", "http://127.0.0.1:9000", "8080"
     * Mutually exclusive with cliPath, useStdio
     */
    cliUrl?: string;

    /**
     * Log level for the CLI server
     */
    logLevel?: "none" | "error" | "warning" | "info" | "debug" | "all";

    /**
     * Auto-start the CLI server on first use
     * @default true
     */
    autoStart?: boolean;

    /**
     * Auto-restart the CLI server if it crashes
     * @default true
     */
    autoRestart?: boolean;

    /**
     * Environment variables to pass to the CLI process. If not set, inherits process.env.
     */
    env?: Record<string, string | undefined>;
}

/**
 * Configuration for creating a session
 */
export type ToolResultType = "success" | "failure" | "rejected" | "denied";

export type ToolBinaryResult = {
    data: string;
    mimeType: string;
    type: string;
    description?: string;
};

export type ToolResultObject = {
    textResultForLlm: string;
    binaryResultsForLlm?: ToolBinaryResult[];
    resultType: ToolResultType;
    error?: string;
    sessionLog?: string;
    toolTelemetry?: Record<string, unknown>;
};

export type ToolResult = string | ToolResultObject;

export interface ToolInvocation {
    sessionId: string;
    toolCallId: string;
    toolName: string;
    arguments: unknown;
}

export type ToolHandler<TArgs = unknown> = (
    args: TArgs,
    invocation: ToolInvocation
) => Promise<unknown> | unknown;

/**
 * Zod-like schema interface for type inference.
 * Any object with `toJSONSchema()` method is treated as a Zod schema.
 */
export interface ZodSchema<T = unknown> {
    _output: T;
    toJSONSchema(): Record<string, unknown>;
}

/**
 * Tool definition. Parameters can be either:
 * - A Zod schema (provides type inference for handler)
 * - A raw JSON schema object
 * - Omitted (no parameters)
 */
export interface Tool<TArgs = unknown> {
    name: string;
    description?: string;
    parameters?: ZodSchema<TArgs> | Record<string, unknown>;
    handler: ToolHandler<TArgs>;
}

/**
 * Helper to define a tool with Zod schema and get type inference for the handler.
 * Without this helper, TypeScript cannot infer handler argument types from Zod schemas.
 */
export function defineTool<T = unknown>(
    name: string,
    config: {
        description?: string;
        parameters?: ZodSchema<T> | Record<string, unknown>;
        handler: ToolHandler<T>;
    }
): Tool<T> {
    return { name, ...config };
}

export interface ToolCallRequestPayload {
    sessionId: string;
    toolCallId: string;
    toolName: string;
    arguments: unknown;
}

export interface ToolCallResponsePayload {
    result: ToolResult;
}

/**
 * Append mode: Use CLI foundation with optional appended content (default).
 */
export interface SystemMessageAppendConfig {
    mode?: "append";

    /**
     * Additional instructions appended after SDK-managed sections.
     */
    content?: string;
}

/**
 * Replace mode: Use caller-provided system message entirely.
 * Removes all SDK guardrails including security restrictions.
 */
export interface SystemMessageReplaceConfig {
    mode: "replace";

    /**
     * Complete system message content.
     * Replaces the entire SDK-managed system message.
     */
    content: string;
}

/**
 * System message configuration for session creation.
 * - Append mode (default): SDK foundation + optional custom content
 * - Replace mode: Full control, caller provides entire system message
 */
export type SystemMessageConfig = SystemMessageAppendConfig | SystemMessageReplaceConfig;

/**
 * Permission request types from the server
 */
export interface PermissionRequest {
    kind: "shell" | "write" | "mcp" | "read" | "url";
    toolCallId?: string;
    [key: string]: unknown;
}

export interface PermissionRequestResult {
    kind:
        | "approved"
        | "denied-by-rules"
        | "denied-no-approval-rule-and-could-not-request-from-user"
        | "denied-interactively-by-user";
    rules?: unknown[];
}

export type PermissionHandler = (
    request: PermissionRequest,
    invocation: { sessionId: string }
) => Promise<PermissionRequestResult> | PermissionRequestResult;

// ============================================================================
// MCP Server Configuration Types
// ============================================================================

/**
 * Base interface for MCP server configuration.
 */
interface MCPServerConfigBase {
    /**
     * List of tools to include from this server. [] means none. "*" means all.
     */
    tools: string[];
    /**
     * Indicates "remote" or "local" server type.
     * If not specified, defaults to "local".
     */
    type?: string;
    /**
     * Optional timeout in milliseconds for tool calls to this server.
     */
    timeout?: number;
}

/**
 * Configuration for a local/stdio MCP server.
 */
export interface MCPLocalServerConfig extends MCPServerConfigBase {
    type?: "local" | "stdio";
    command: string;
    args: string[];
    /**
     * Environment variables to pass to the server.
     */
    env?: Record<string, string>;
    cwd?: string;
}

/**
 * Configuration for a remote MCP server (HTTP or SSE).
 */
export interface MCPRemoteServerConfig extends MCPServerConfigBase {
    type: "http" | "sse";
    /**
     * URL of the remote server.
     */
    url: string;
    /**
     * Optional HTTP headers to include in requests.
     */
    headers?: Record<string, string>;
}

/**
 * Union type for MCP server configurations.
 */
export type MCPServerConfig = MCPLocalServerConfig | MCPRemoteServerConfig;

// ============================================================================
// Custom Agent Configuration Types
// ============================================================================

/**
 * Configuration for a custom agent.
 */
export interface CustomAgentConfig {
    /**
     * Unique name of the custom agent.
     */
    name: string;
    /**
     * Display name for UI purposes.
     */
    displayName?: string;
    /**
     * Description of what the agent does.
     */
    description?: string;
    /**
     * List of tool names the agent can use.
     * Use null or undefined for all tools.
     */
    tools?: string[] | null;
    /**
     * The prompt content for the agent.
     */
    prompt: string;
    /**
     * MCP servers specific to this agent.
     */
    mcpServers?: Record<string, MCPServerConfig>;
    /**
     * Whether the agent should be available for model inference.
     * @default true
     */
    infer?: boolean;
}

/**
 * Configuration for infinite sessions with automatic context compaction and workspace persistence.
 * When enabled, sessions automatically manage context window limits through background compaction
 * and persist state to a workspace directory.
 */
export interface InfiniteSessionConfig {
    /**
     * Whether infinite sessions are enabled.
     * @default true
     */
    enabled?: boolean;

    /**
     * Context utilization threshold (0.0-1.0) at which background compaction starts.
     * Compaction runs asynchronously, allowing the session to continue processing.
     * @default 0.80
     */
    backgroundCompactionThreshold?: number;

    /**
     * Context utilization threshold (0.0-1.0) at which the session blocks until compaction completes.
     * This prevents context overflow when compaction hasn't finished in time.
     * @default 0.95
     */
    bufferExhaustionThreshold?: number;
}

export interface SessionConfig {
    /**
     * Optional custom session ID
     * If not provided, server will generate one
     */
    sessionId?: string;

    /**
     * Model to use for this session
     */
    model?: string;

    /**
     * Override the default configuration directory location.
     * When specified, the session will use this directory for storing config and state.
     */
    configDir?: string;

    /**
     * Tools exposed to the CLI server
     */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    tools?: Tool<any>[];

    /**
     * System message configuration
     * Controls how the system prompt is constructed
     */
    systemMessage?: SystemMessageConfig;

    /**
     * List of tool names to allow. When specified, only these tools will be available.
     * Takes precedence over excludedTools.
     */
    availableTools?: string[];

    /**
     * List of tool names to disable. All other tools remain available.
     * Ignored if availableTools is specified.
     */
    excludedTools?: string[];

    /**
     * Custom provider configuration (BYOK - Bring Your Own Key).
     * When specified, uses the provided API endpoint instead of the Copilot API.
     */
    provider?: ProviderConfig;

    /**
     * Handler for permission requests from the server.
     * When provided, the server will call this handler to request permission for operations.
     */
    onPermissionRequest?: PermissionHandler;
    /*
     * Enable streaming of assistant message and reasoning chunks.
     * When true, ephemeral assistant.message_delta and assistant.reasoning_delta
     * events are sent as the response is generated. Clients should accumulate
     * deltaContent values to build the full response.
     * @default false
     */
    streaming?: boolean;

    /**
     * MCP server configurations for the session.
     * Keys are server names, values are server configurations.
     */
    mcpServers?: Record<string, MCPServerConfig>;

    /**
     * Custom agent configurations for the session.
     */
    customAgents?: CustomAgentConfig[];

    /**
     * Directories to load skills from.
     */
    skillDirectories?: string[];

    /**
     * List of skill names to disable.
     */
    disabledSkills?: string[];

    /**
     * Infinite session configuration for persistent workspaces and automatic compaction.
     * When enabled (default), sessions automatically manage context limits and persist state.
     * Set to `{ enabled: false }` to disable.
     */
    infiniteSessions?: InfiniteSessionConfig;
}

/**
 * Configuration for resuming a session
 */
export type ResumeSessionConfig = Pick<
    SessionConfig,
    | "tools"
    | "provider"
    | "streaming"
    | "onPermissionRequest"
    | "mcpServers"
    | "customAgents"
    | "skillDirectories"
    | "disabledSkills"
>;

/**
 * Configuration for a custom API provider.
 */
export interface ProviderConfig {
    /**
     * Provider type. Defaults to "openai" for generic OpenAI-compatible APIs.
     */
    type?: "openai" | "azure" | "anthropic";

    /**
     * API format (openai/azure only). Defaults to "completions".
     */
    wireApi?: "completions" | "responses";

    /**
     * API endpoint URL
     */
    baseUrl: string;

    /**
     * API key. Optional for local providers like Ollama.
     */
    apiKey?: string;

    /**
     * Bearer token for authentication. Sets the Authorization header directly.
     * Use this for services requiring bearer token auth instead of API key.
     * Takes precedence over apiKey when both are set.
     */
    bearerToken?: string;

    /**
     * Azure-specific options
     */
    azure?: {
        /**
         * API version. Defaults to "2024-10-21".
         */
        apiVersion?: string;
    };
}

/**
 * Options for sending a message to a session
 */
export interface MessageOptions {
    /**
     * The prompt/message to send
     */
    prompt: string;

    /**
     * File or directory attachments
     */
    attachments?: Array<{
        type: "file" | "directory";
        path: string;
        displayName?: string;
    }>;

    /**
     * Message delivery mode
     * - "enqueue": Add to queue (default)
     * - "immediate": Send immediately
     */
    mode?: "enqueue" | "immediate";
}

/**
 * Event handler callback type
 */
export type SessionEventHandler = (event: SessionEvent) => void;

/**
 * Connection state
 */
export type ConnectionState = "disconnected" | "connecting" | "connected" | "error";

/**
 * Metadata about a session
 */
export interface SessionMetadata {
    sessionId: string;
    startTime: Date;
    modifiedTime: Date;
    summary?: string;
    isRemote: boolean;
}

/**
 * Response from status.get
 */
export interface GetStatusResponse {
    /** Package version (e.g., "1.0.0") */
    version: string;
    /** Protocol version for SDK compatibility */
    protocolVersion: number;
}

/**
 * Response from auth.getStatus
 */
export interface GetAuthStatusResponse {
    /** Whether the user is authenticated */
    isAuthenticated: boolean;
    /** Authentication type */
    authType?: "user" | "env" | "gh-cli" | "hmac" | "api-key" | "token";
    /** GitHub host URL */
    host?: string;
    /** User login name */
    login?: string;
    /** Human-readable status message */
    statusMessage?: string;
}

/**
 * Model capabilities and limits
 */
export interface ModelCapabilities {
    supports: {
        vision: boolean;
    };
    limits: {
        max_prompt_tokens?: number;
        max_context_window_tokens: number;
        vision?: {
            supported_media_types: string[];
            max_prompt_images: number;
            max_prompt_image_size: number;
        };
    };
}

/**
 * Model policy state
 */
export interface ModelPolicy {
    state: "enabled" | "disabled" | "unconfigured";
    terms: string;
}

/**
 * Model billing information
 */
export interface ModelBilling {
    multiplier: number;
}

/**
 * Information about an available model
 */
export interface ModelInfo {
    /** Model identifier (e.g., "claude-sonnet-4.5") */
    id: string;
    /** Display name */
    name: string;
    /** Model capabilities and limits */
    capabilities: ModelCapabilities;
    /** Policy state */
    policy?: ModelPolicy;
    /** Billing information */
    billing?: ModelBilling;
}
