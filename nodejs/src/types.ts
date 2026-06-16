/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Type definitions for the Copilot SDK
 */

// Import and re-export generated session event types
import type { Canvas } from "./canvas.js";
import type { SessionFsProvider } from "./sessionFsProvider.js";
import type {
    ReasoningSummary,
    SessionEvent as GeneratedSessionEvent,
} from "./generated/session-events.js";
import type { CopilotSession } from "./session.js";
import type {
    ModelBillingTokenPrices,
    OpenCanvasInstance,
    RemoteSessionMode,
} from "./generated/rpc.js";
import type { ToolSet } from "./toolSet.js";
export type { RemoteSessionMode } from "./generated/rpc.js";
export type {
    ModelBillingTokenPrices,
    ModelBillingTokenPricesLongContext,
} from "./generated/rpc.js";
export type SessionEvent = GeneratedSessionEvent;
export type { ReasoningSummary } from "./generated/session-events.js";
export type { SessionFsProvider } from "./sessionFsProvider.js";
export { createSessionFsAdapter } from "./sessionFsProvider.js";
export type { SessionFsFileInfo } from "./sessionFsProvider.js";
export type { SessionFsSqliteQueryResult } from "./sessionFsProvider.js";
export type { SessionFsSqliteQueryType } from "./sessionFsProvider.js";
export type { SessionFsSqliteProvider } from "./sessionFsProvider.js";

/**
 * Options for creating a CopilotClient
 */
/**
 * W3C Trace Context headers used for distributed trace propagation.
 */
export interface TraceContext {
    traceparent?: string;
    tracestate?: string;
}

/**
 * Callback that returns the current W3C Trace Context.
 * Wire this up to your OpenTelemetry (or other tracing) SDK to enable
 * distributed trace propagation between your app and the Copilot CLI.
 */
export type TraceContextProvider = () => TraceContext | Promise<TraceContext>;

/**
 * Configuration for OpenTelemetry instrumentation.
 *
 * When provided via {@link CopilotClientOptions.telemetry}, the SDK sets
 * the corresponding environment variables on the spawned CLI process so
 * that the CLI's built-in OTel exporter is configured automatically.
 */
export interface TelemetryConfig {
    /** OTLP HTTP endpoint URL for trace/metric export. Sets OTEL_EXPORTER_OTLP_ENDPOINT. */
    otlpEndpoint?: string;
    /** OTLP HTTP protocol for all signals. Sets OTEL_EXPORTER_OTLP_PROTOCOL. */
    otlpProtocol?: "http/json" | "http/protobuf";
    /** File path for JSON-lines trace output. Sets COPILOT_OTEL_FILE_EXPORTER_PATH. */
    filePath?: string;
    /** Exporter backend type: "otlp-http" or "file". Sets COPILOT_OTEL_EXPORTER_TYPE. */
    exporterType?: string;
    /** Instrumentation scope name. Sets COPILOT_OTEL_SOURCE_NAME. */
    sourceName?: string;
    /** Whether to capture message content (prompts, responses). Sets OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT. */
    captureContent?: boolean;
}

/**
 * Configures how a {@link CopilotClient} connects to the Copilot runtime.
 * Construct via the factory functions on {@link RuntimeConnection}.
 */
export type RuntimeConnection =
    | StdioRuntimeConnection
    | TcpRuntimeConnection
    | UriRuntimeConnection;

/**
 * Spawns a runtime child process and communicates over its stdin/stdout.
 * This is the default if no {@link CopilotClientOptions.connection} is set.
 */
export interface StdioRuntimeConnection {
    readonly kind: "stdio";
    /** Path to the runtime executable. When omitted, the bundled runtime is used. */
    readonly path?: string;
    /** Extra command-line arguments to pass to the runtime process. */
    readonly args?: readonly string[];
}

/**
 * Spawns a runtime child process that listens on a TCP socket and connects to it.
 */
export interface TcpRuntimeConnection {
    readonly kind: "tcp";
    /**
     * TCP port to listen on. `0` (the default) auto-allocates a free port.
     * If the chosen port is already in use, startup fails.
     */
    readonly port?: number;
    /**
     * Optional shared secret the SDK sends to the spawned runtime to authenticate
     * the TCP connection. When omitted, a UUID is generated automatically so the
     * loopback listener is safe by default.
     */
    readonly connectionToken?: string;
    /** Path to the runtime executable. When omitted, the bundled runtime is used. */
    readonly path?: string;
    /** Extra command-line arguments to pass to the runtime process. */
    readonly args?: readonly string[];
}

/**
 * Connects to an already-running runtime at the specified URL. The SDK does not
 * spawn a process in this mode.
 */
export interface UriRuntimeConnection {
    readonly kind: "uri";
    /**
     * URL of the runtime to connect to. Accepts `"port"`, `"host:port"`, or a
     * full URL (`"http://host:port"`).
     */
    readonly url: string;
    /** Optional shared secret to authenticate the connection. */
    readonly connectionToken?: string;
}

/** Factory functions for constructing {@link RuntimeConnection} instances. */
export const RuntimeConnection = {
    /**
     * Spawn a runtime child process and communicate over its stdin/stdout.
     * This is the default if no {@link CopilotClientOptions.connection} is set.
     */
    forStdio(opts: { path?: string; args?: readonly string[] } = {}): StdioRuntimeConnection {
        return { kind: "stdio", path: opts.path, args: opts.args };
    },
    /**
     * Spawn a runtime child process that listens on a TCP socket and connect to it.
     */
    forTcp(
        opts: {
            port?: number;
            connectionToken?: string;
            path?: string;
            args?: readonly string[];
        } = {}
    ): TcpRuntimeConnection {
        return {
            kind: "tcp",
            port: opts.port,
            connectionToken: opts.connectionToken,
            path: opts.path,
            args: opts.args,
        };
    },
    /**
     * Connect to an already-running runtime at the given URL. The SDK does not
     * spawn a process in this mode.
     */
    forUri(url: string, opts: { connectionToken?: string } = {}): UriRuntimeConnection {
        return { kind: "uri", url, connectionToken: opts.connectionToken };
    },
} as const;

/**
 * @internal Marker used by `joinSession()` to signal that the SDK is running
 * as a child process of the Copilot runtime and should use its own stdio to
 * talk back to the parent. Not part of the public API.
 */
export interface ParentProcessRuntimeConnection {
    readonly kind: "parent-process";
}

/** @internal */
export type InternalRuntimeConnection = RuntimeConnection | ParentProcessRuntimeConnection;

/**
 * Controls SDK defaults for ambient features.
 *
 * - `"copilot-cli"` (default): Defaults equivalent to Copilot CLI. Useful when
 *   building a coding agent that shares sessions with Copilot CLI. Do not use
 *   this mode for server-based multi-user applications — the default coding
 *   agent has tools and capabilities that operate across sessions and can
 *   access the host OS environment.
 * - `"empty"`: Disables optional features by default. The app must explicitly
 *   opt into anything it needs. Required for any scenario where CLI-like
 *   ambient behavior is unsafe (e.g. multi-user servers).
 */
export type CopilotClientMode = "empty" | "copilot-cli";

export interface CopilotClientOptions {
    /**
     * How to connect to the Copilot runtime. When omitted, defaults to
     * {@link RuntimeConnection.forStdio} with the bundled runtime.
     */
    connection?: RuntimeConnection;

    /**
     * Selects the SDK defaulting strategy. See {@link CopilotClientMode}.
     *
     * When set to `"empty"`, the SDK validates that the app has supplied the
     * required configuration ({@link CopilotClientOptions.baseDirectory} or
     * {@link CopilotClientOptions.sessionFs}, plus
     * {@link SessionConfigBase.availableTools} on each session) and translates
     * session creation requests into runtime options that flip tool filter
     * precedence to deny-wins so exclusions are expressible.
     *
     * @default "copilot-cli"
     */
    mode?: CopilotClientMode;

    /**
     * Working directory for the runtime process.
     * If not set, inherits the current process's working directory.
     */
    workingDirectory?: string;

    /**
     * Base directory for Copilot data (session state, config, etc.).
     * Sets the COPILOT_HOME environment variable on the spawned runtime.
     * When not set, the runtime defaults to ~/.copilot.
     * Ignored when connecting to an existing runtime via {@link RuntimeConnection.forUri}.
     */
    baseDirectory?: string;

    /**
     * Log level for the Copilot runtime. When omitted, the runtime uses its
     * own default (currently `"info"`).
     */
    logLevel?: "none" | "error" | "warning" | "info" | "debug" | "all";

    /**
     * Environment variables to pass to the runtime process. If not set, inherits process.env.
     */
    env?: Record<string, string | undefined>;

    /**
     * GitHub token to use for authentication.
     * When provided, the token is passed to the runtime via environment variable.
     * This takes priority over other authentication methods.
     */
    gitHubToken?: string;

    /**
     * Whether to use the logged-in user for authentication.
     * When true, the runtime will attempt to use stored OAuth tokens or gh CLI auth.
     * When false, only explicit tokens (gitHubToken or environment variables) are used.
     * @default true (but defaults to false when gitHubToken is provided)
     */
    useLoggedInUser?: boolean;

    /**
     * Custom handler for listing available models.
     * When provided, client.listModels() calls this handler instead of
     * querying the runtime. Useful in BYOK mode to return models
     * available from your custom provider.
     */
    onListModels?: () => Promise<ModelInfo[]> | ModelInfo[];

    /**
     * OpenTelemetry configuration for the runtime process.
     * When provided, the corresponding OTel environment variables are set
     * on the spawned runtime.
     */
    telemetry?: TelemetryConfig;

    /**
     * Advanced: callback that returns the current W3C Trace Context for distributed
     * trace propagation.  Most users do not need this — the {@link telemetry} config
     * alone is sufficient to collect traces from the CLI.
     *
     * This callback is only useful when your application creates its own
     * OpenTelemetry spans and you want them to appear in the **same** distributed
     * trace as the CLI's spans.  The SDK calls this before `session.create`,
     * `session.resume`, and `session.send` RPCs to inject `traceparent`/`tracestate`
     * into the request.
     *
     * @example
     * ```typescript
     * import { propagation, context } from "@opentelemetry/api";
     *
     * const client = new CopilotClient({
     *   onGetTraceContext: () => {
     *     const carrier: Record<string, string> = {};
     *     propagation.inject(context.active(), carrier);
     *     return carrier;
     *   },
     * });
     * ```
     */
    onGetTraceContext?: TraceContextProvider;

    /**
     * Custom session filesystem provider.
     * When provided, the client registers as the session filesystem provider
     * on connection, routing all session-scoped file I/O through these callbacks
     * instead of the server's default local filesystem storage.
     */
    sessionFs?: SessionFsConfig;

    /**
     * Server-wide idle timeout for sessions in seconds.
     * Sessions without activity for this duration are automatically cleaned up.
     * Set to 0 or omit to disable (sessions live indefinitely).
     * Ignored when connecting to an existing runtime via {@link RuntimeConnection.forUri}.
     * @default undefined (disabled)
     */
    sessionIdleTimeoutSeconds?: number;

    /**
     * Enable remote session support (Mission Control integration).
     * When true, sessions in a GitHub repository working directory are
     * accessible from GitHub web and mobile.
     * Ignored when connecting to an existing runtime via {@link RuntimeConnection.forUri}.
     * @default false
     */
    enableRemoteSessions?: boolean;

    /**
     * @internal Hook used by `joinSession()` to construct a client that talks
     * to its parent process over stdio. Not part of the public API.
     */
    _internalConnection?: InternalRuntimeConnection;
}

/**
 * Configuration for creating a session
 */
export type ToolResultType = "success" | "failure" | "rejected" | "denied" | "timeout";

export type ToolBinaryResult = {
    data: string;
    mimeType: string;
    type: "image" | "resource";
    description?: string;
};

export type ToolTelemetry = Record<string, Record<string, unknown> | undefined>;

export type ToolResultObject = {
    textResultForLlm: string;
    binaryResultsForLlm?: ToolBinaryResult[];
    resultType: ToolResultType;
    error?: string;
    sessionLog?: string;
    toolTelemetry?: ToolTelemetry;
};

export type ToolResult = string | ToolResultObject;

/**
 * GitHub repository metadata to associate with a cloud session.
 */
export interface CloudSessionRepository {
    owner: string;
    name: string;
    branch?: string;
}

/**
 * Options for creating a remote session in the cloud.
 */
export interface CloudSessionOptions {
    repository?: CloudSessionRepository;
}

// ============================================================================
// MCP CallToolResult support
// ============================================================================

/**
 * Content block types within an MCP CallToolResult.
 */
type McpCallToolResultTextContent = {
    type: "text";
    text: string;
};

type McpCallToolResultImageContent = {
    type: "image";
    data: string;
    mimeType: string;
};

type McpCallToolResultResourceContent = {
    type: "resource";
    resource: {
        uri: string;
        mimeType?: string;
        text?: string;
        blob?: string;
    };
};

type McpCallToolResultContent =
    | McpCallToolResultTextContent
    | McpCallToolResultImageContent
    | McpCallToolResultResourceContent;

/**
 * MCP-compatible CallToolResult type. Can be passed to
 * {@link convertMcpCallToolResult} to produce a {@link ToolResultObject}.
 */
type McpCallToolResult = {
    content: McpCallToolResultContent[];
    isError?: boolean;
};

/**
 * Converts an MCP CallToolResult into the SDK's ToolResultObject format.
 */
export function convertMcpCallToolResult(callResult: McpCallToolResult): ToolResultObject {
    const textParts: string[] = [];
    const binaryResults: ToolBinaryResult[] = [];

    for (const block of callResult.content) {
        switch (block.type) {
            case "text":
                // Guard against malformed input where text field is missing at runtime
                if (typeof block.text === "string") {
                    textParts.push(block.text);
                }
                break;
            case "image":
                if (
                    typeof block.data === "string" &&
                    block.data &&
                    typeof block.mimeType === "string"
                ) {
                    binaryResults.push({
                        data: block.data,
                        mimeType: block.mimeType,
                        type: "image",
                    });
                }
                break;
            case "resource": {
                // Use optional chaining: resource field may be absent in malformed input
                if (block.resource?.text) {
                    textParts.push(block.resource.text);
                }
                if (block.resource?.blob) {
                    const mimeType = block.resource.mimeType;
                    binaryResults.push({
                        data: block.resource.blob,
                        mimeType:
                            typeof mimeType === "string" && mimeType
                                ? mimeType
                                : "application/octet-stream",
                        type: "resource",
                        description: block.resource.uri,
                    });
                }
                break;
            }
        }
    }

    return {
        textResultForLlm: textParts.join("\n"),
        resultType: callResult.isError ? "failure" : "success",
        ...(binaryResults.length > 0 ? { binaryResultsForLlm: binaryResults } : {}),
    };
}

export interface ToolInvocation {
    sessionId: string;
    toolCallId: string;
    toolName: string;
    arguments: unknown;
    /** W3C Trace Context traceparent from the CLI's execute_tool span. */
    traceparent?: string;
    /** W3C Trace Context tracestate from the CLI's execute_tool span. */
    tracestate?: string;
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
 *
 * If `handler` is omitted, the SDK exposes the declaration but does not
 * automatically invoke the tool. Consumers can resolve tool calls by observing
 * external tool request events and calling the pending-tool RPC.
 */
export interface Tool<TArgs = unknown> {
    name: string;
    description?: string;
    parameters?: ZodSchema<TArgs> | Record<string, unknown>;
    handler?: ToolHandler<TArgs>;
    /**
     * When true, explicitly indicates this tool is intended to override a built-in tool
     * of the same name. If not set and the name clashes with a built-in tool, the runtime
     * will return an error.
     */
    overridesBuiltInTool?: boolean;
    /**
     * When true, the tool can execute without a permission prompt.
     */
    skipPermission?: boolean;
    /**
     * Controls whether the tool may be deferred (loaded lazily via tool search)
     * rather than always pre-loaded. When `"auto"`, the tool can be deferred and
     * surfaced through tool search. When `"never"`, the tool is always pre-loaded.
     * Optional; defaults to `"auto"`.
     */
    defer?: "auto" | "never";
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
        handler?: ToolHandler<T>;
        overridesBuiltInTool?: boolean;
        skipPermission?: boolean;
        defer?: "auto" | "never";
    }
): Tool<T> {
    return { name, ...config };
}

// ============================================================================
// Commands
// ============================================================================

/**
 * Context passed to a command handler when a command is executed.
 */
export interface CommandContext {
    /** Session ID where the command was invoked */
    sessionId: string;
    /** The full command text (e.g. "/deploy production") */
    command: string;
    /** Command name without leading / */
    commandName: string;
    /** Raw argument string after the command name */
    args: string;
}

/**
 * Handler invoked when a registered command is executed by a user.
 */
export type CommandHandler = (context: CommandContext) => Promise<void> | void;

/**
 * Definition of a slash command registered with the session.
 * When the CLI is running with a TUI, registered commands appear as
 * `/commandName` for the user to invoke.
 */
export interface CommandDefinition {
    /** Command name (without leading /). */
    name: string;
    /** Human-readable description shown in command completion UI. */
    description?: string;
    /** Handler invoked when the command is executed. */
    handler: CommandHandler;
}

// ============================================================================
// UI Elicitation
// ============================================================================

/**
 * Capabilities reported by the CLI host for this session.
 */
export interface SessionCapabilities {
    ui?: {
        /** Whether the host supports interactive elicitation dialogs. */
        elicitation?: boolean;
        /**
         * Whether the runtime has accepted the session's MCP Apps (SEP-1865)
         * opt-in. `true` when the consumer set `enableMcpApps: true` on
         * create/resume **and** the runtime's `MCP_APPS` feature flag (or
         * `COPILOT_MCP_APPS=true` env override) is on. Otherwise absent or
         * `false`, indicating the runtime silently dropped the opt-in.
         *
         * @experimental This property is part of an experimental wire-protocol surface
         * (SEP-1865) and may change or be removed in a future release.
         */
        mcpApps?: boolean;
        /** Whether the host supports canvas rendering. */
        canvases?: boolean;
    };
}

/**
 * A single field in an elicitation schema — matches the MCP SDK's
 * `PrimitiveSchemaDefinition` union.
 */
export type ElicitationSchemaField =
    | {
          type: "string";
          title?: string;
          description?: string;
          enum: string[];
          enumNames?: string[];
          default?: string;
      }
    | {
          type: "string";
          title?: string;
          description?: string;
          oneOf: { const: string; title: string }[];
          default?: string;
      }
    | {
          type: "array";
          title?: string;
          description?: string;
          minItems?: number;
          maxItems?: number;
          items: { type: "string"; enum: string[] };
          default?: string[];
      }
    | {
          type: "array";
          title?: string;
          description?: string;
          minItems?: number;
          maxItems?: number;
          items: { anyOf: { const: string; title: string }[] };
          default?: string[];
      }
    | {
          type: "boolean";
          title?: string;
          description?: string;
          default?: boolean;
      }
    | {
          type: "string";
          title?: string;
          description?: string;
          minLength?: number;
          maxLength?: number;
          format?: "email" | "uri" | "date" | "date-time";
          default?: string;
      }
    | {
          type: "number" | "integer";
          title?: string;
          description?: string;
          minimum?: number;
          maximum?: number;
          default?: number;
      };

/**
 * Schema describing the form fields for an elicitation request.
 */
export interface ElicitationSchema {
    type: "object";
    properties: Record<string, ElicitationSchemaField>;
    required?: string[];
}

/**
 * Primitive field value in an elicitation result.
 * Matches MCP SDK's `ElicitResult.content` value type.
 */
export type ElicitationFieldValue = string | number | boolean | string[];

/**
 * Result returned from an elicitation request.
 */
export interface ElicitationResult {
    /** User action: "accept" (submitted), "decline" (rejected), or "cancel" (dismissed). */
    action: "accept" | "decline" | "cancel";
    /** Form values submitted by the user (present when action is "accept"). */
    content?: Record<string, ElicitationFieldValue>;
}

/**
 * Parameters for a raw elicitation request.
 */
export interface ElicitationParams {
    /** Message describing what information is needed from the user. */
    message: string;
    /** JSON Schema describing the form fields to present. */
    requestedSchema: ElicitationSchema;
}

/**
 * Context for an elicitation handler invocation, combining the request data
 * with session context. Mirrors the single-argument pattern of {@link CommandContext}.
 */
export interface ElicitationContext {
    /** Identifier of the session that triggered the elicitation request. */
    sessionId: string;
    /** Message describing what information is needed from the user. */
    message: string;
    /** JSON Schema describing the form fields to present. */
    requestedSchema?: ElicitationSchema;
    /** Elicitation mode: "form" for structured input, "url" for browser redirect. */
    mode?: "form" | "url";
    /** The source that initiated the request (e.g. MCP server name). */
    elicitationSource?: string;
    /** URL to open in the user's browser (url mode only). */
    url?: string;
}

/**
 * Handler invoked when the server dispatches an elicitation request to this client.
 * Return an {@link ElicitationResult} with the user's response.
 */
export type ElicitationHandler = (
    context: ElicitationContext
) => Promise<ElicitationResult> | ElicitationResult;

/**
 * Options for the `input()` convenience method.
 */
export interface UiInputOptions {
    /** Title label for the input field. */
    title?: string;
    /** Descriptive text shown below the field. */
    description?: string;
    /** Minimum character length. */
    minLength?: number;
    /** Maximum character length. */
    maxLength?: number;
    /** Semantic format hint. */
    format?: "email" | "uri" | "date" | "date-time";
    /** Default value pre-populated in the field. */
    default?: string;
}

/**
 * The `session.ui` API object providing interactive UI methods.
 * Only usable when the CLI host supports elicitation.
 */
export interface SessionUiApi {
    /**
     * Shows a generic elicitation dialog with a custom schema.
     * @throws Error if the host does not support elicitation.
     */
    elicitation(params: ElicitationParams): Promise<ElicitationResult>;

    /**
     * Shows a confirmation dialog and returns the user's boolean answer.
     * Returns `false` if the user declines or cancels.
     * @throws Error if the host does not support elicitation.
     */
    confirm(message: string): Promise<boolean>;

    /**
     * Shows a selection dialog with the given options.
     * Returns the selected value, or `null` if the user declines/cancels.
     * @throws Error if the host does not support elicitation.
     */
    select(message: string, options: string[]): Promise<string | null>;

    /**
     * Shows a text input dialog.
     * Returns the entered text, or `null` if the user declines/cancels.
     * @throws Error if the host does not support elicitation.
     */
    input(message: string, options?: UiInputOptions): Promise<string | null>;
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
 * Known system message section identifiers for the "customize" mode.
 * Each section corresponds to a distinct part of the system prompt.
 */
export type SystemMessageSection =
    | "identity"
    | "tone"
    | "tool_efficiency"
    | "environment_context"
    | "code_change_rules"
    | "guidelines"
    | "safety"
    | "tool_instructions"
    | "custom_instructions"
    | "runtime_instructions"
    | "last_instructions";

/** Section metadata for documentation and tooling. */
export const SYSTEM_MESSAGE_SECTIONS: Record<SystemMessageSection, { description: string }> = {
    identity: { description: "Agent identity preamble and mode statement" },
    tone: { description: "Response style, conciseness rules, output formatting preferences" },
    tool_efficiency: { description: "Tool usage patterns, parallel calling, batching guidelines" },
    environment_context: { description: "CWD, OS, git root, directory listing, available tools" },
    code_change_rules: { description: "Coding rules, linting/testing, ecosystem tools, style" },
    guidelines: { description: "Tips, behavioral best practices, behavioral guidelines" },
    safety: { description: "Environment limitations, prohibited actions, security policies" },
    tool_instructions: { description: "Per-tool usage instructions" },
    custom_instructions: { description: "Repository and organization custom instructions" },
    runtime_instructions: {
        description:
            "Runtime-provided context and instructions (e.g. system notifications, memories, workspace context, mode-specific instructions, content-exclusion policy)",
    },
    last_instructions: {
        description:
            "End-of-prompt instructions: parallel tool calling, persistence, task completion",
    },
};

/**
 * Transform callback for a single section: receives current content, returns new content.
 */
export type SectionTransformFn = (currentContent: string) => string | Promise<string>;

/**
 * Override action: a string literal for static overrides, or a callback for transforms.
 *
 * - `"replace"`: Replace section content entirely
 * - `"remove"`: Remove the section
 * - `"append"`: Append to existing section content
 * - `"prepend"`: Prepend to existing section content
 * - `function`: Transform callback — receives current section content, returns new content
 */
export type SectionOverrideAction =
    | "replace"
    | "remove"
    | "append"
    | "prepend"
    | SectionTransformFn;

/**
 * Override operation for a single system message section.
 */
export interface SectionOverride {
    /**
     * The operation to perform on this section.
     * Can be a string action or a transform callback function.
     */
    action: SectionOverrideAction;

    /**
     * Content for the override. Optional for all actions.
     * - For replace, omitting content replaces with an empty string.
     * - For append/prepend, content is added before/after the existing section.
     * - Ignored for the remove action.
     */
    content?: string;
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
 * Customize mode: Override individual sections of the system prompt.
 * Keeps the SDK-managed prompt structure while allowing targeted modifications.
 */
export interface SystemMessageCustomizeConfig {
    mode: "customize";

    /**
     * Override specific sections of the system prompt by section ID.
     * Unknown section IDs gracefully fall back: content-bearing overrides are appended
     * to additional instructions, and "remove" on unknown sections is a silent no-op.
     */
    sections?: Partial<Record<SystemMessageSection, SectionOverride>>;

    /**
     * Additional content appended after all sections.
     * Equivalent to append mode's content field — provided for convenience.
     */
    content?: string;
}

/**
 * System message configuration for session creation.
 * - Append mode (default): SDK foundation + optional custom content
 * - Replace mode: Full control, caller provides entire system message
 * - Customize mode: Section-level overrides with graceful fallback
 */
export type SystemMessageConfig =
    | SystemMessageAppendConfig
    | SystemMessageReplaceConfig
    | SystemMessageCustomizeConfig;

/**
 * Permission request types from the server. This is the generated
 * discriminated union from the runtime schema — switch on `kind` to
 * access the variant-specific fields (e.g. shell `commands`, write
 * `fileName`/`diff`, mcp `toolName`/`args`).
 */
export type { PermissionRequest } from "./generated/session-events.js";
import type { PermissionRequest } from "./generated/session-events.js";

import type { PermissionDecisionRequest } from "./generated/rpc.js";

/**
 * Permission decision result returned from a {@link PermissionHandler}.
 * The discriminated `kind` field selects the decision. Variant-specific
 * fields (e.g. `feedback` on `{ kind: "reject" }`) come from the generated
 * `PermissionDecisionRequest["result"]` union.
 */
export type PermissionRequestResult = PermissionDecisionRequest["result"] | { kind: "no-result" };

export type PermissionHandler = (
    request: PermissionRequest,
    invocation: { sessionId: string }
) => Promise<PermissionRequestResult> | PermissionRequestResult;

export const approveAll: PermissionHandler = () => ({ kind: "approve-once" });

export const defaultJoinSessionPermissionHandler: PermissionHandler =
    (): PermissionRequestResult => ({
        kind: "no-result",
    });

// ============================================================================
// User Input Request Types
// ============================================================================

/**
 * Request for user input from the agent (enables ask_user tool)
 */
export interface UserInputRequest {
    /**
     * The question to ask the user
     */
    question: string;

    /**
     * Optional choices for multiple choice questions
     */
    choices?: string[];

    /**
     * Whether to allow freeform text input in addition to choices
     * @default true
     */
    allowFreeform?: boolean;
}

/**
 * Response to a user input request
 */
export interface UserInputResponse {
    /**
     * The user's answer
     */
    answer: string;

    /**
     * Whether the answer was freeform (not from choices)
     */
    wasFreeform: boolean;
}

/**
 * Handler for user input requests from the agent
 */
export type UserInputHandler = (
    request: UserInputRequest,
    invocation: { sessionId: string }
) => Promise<UserInputResponse> | UserInputResponse;

/**
 * Request to exit plan mode and continue with a selected action.
 */
export interface ExitPlanModeRequest {
    /** Summary of the plan or proposed next step. */
    summary: string;
    /** Full plan content, when available. */
    planContent?: string;
    /** Available actions the user can select. */
    actions: string[];
    /** The action recommended by the runtime. */
    recommendedAction: string;
}

/**
 * Response to an exit-plan-mode request.
 */
export interface ExitPlanModeResult {
    /** Whether the user approved exiting plan mode. */
    approved: boolean;
    /** Selected action, if the user chose one. */
    selectedAction?: string;
    /** Optional feedback provided by the user. */
    feedback?: string;
}

/**
 * Handler for exit-plan-mode requests from the agent.
 */
export type ExitPlanModeHandler = (
    request: ExitPlanModeRequest,
    invocation: { sessionId: string }
) => Promise<ExitPlanModeResult> | ExitPlanModeResult;

/**
 * Request to switch to auto mode after an eligible rate limit.
 */
export interface AutoModeSwitchRequest {
    /** The rate-limit error code that triggered the request. */
    errorCode?: string;
    /** Seconds until the rate limit resets, when known. */
    retryAfterSeconds?: number;
}

/**
 * Response to an auto-mode-switch request.
 */
export type AutoModeSwitchResponse = "yes" | "yes_always" | "no";

/**
 * Handler for auto-mode-switch requests from the agent.
 */
export type AutoModeSwitchHandler = (
    request: AutoModeSwitchRequest,
    invocation: { sessionId: string }
) => Promise<AutoModeSwitchResponse> | AutoModeSwitchResponse;

// ============================================================================
// Hook Types
// ============================================================================

/**
 * Base interface for all hook inputs
 */
export interface BaseHookInput {
    /** The runtime session ID of the session that triggered the hook.
     * For sub-agent hooks this differs from `invocation.sessionId`. */
    sessionId: string;
    /** Time at which the hook event was emitted by the runtime. */
    timestamp: Date;
    workingDirectory: string;
}

/**
 * Input for pre-tool-use hook
 */
export interface PreToolUseHookInput extends BaseHookInput {
    toolName: string;
    toolArgs: unknown;
}

/**
 * Output for pre-tool-use hook
 */
export interface PreToolUseHookOutput {
    permissionDecision?: "allow" | "deny" | "ask";
    permissionDecisionReason?: string;
    modifiedArgs?: unknown;
    additionalContext?: string;
    suppressOutput?: boolean;
}

/**
 * Handler for pre-tool-use hook
 */
export type PreToolUseHandler = (
    input: PreToolUseHookInput,
    invocation: { sessionId: string }
) => Promise<PreToolUseHookOutput | void> | PreToolUseHookOutput | void;

/**
 * Input for pre-MCP-tool-call hook
 */
export interface PreMcpToolCallHookInput extends BaseHookInput {
    toolCallId?: string;
    serverName: string;
    toolName: string;
    arguments: unknown;
    _meta?: Record<string, unknown>;
}

/**
 * Output for pre-MCP-tool-call hook
 */
export interface PreMcpToolCallHookOutput {
    /**
     * Hook-controlled metadata to use for the outgoing MCP request.
     * - undefined/absent: preserve the current request `_meta`
     * - object: use this object as request `_meta`
     * - null: omit `_meta`
     */
    metaToUse?: Record<string, unknown> | null;
}

/**
 * Handler for pre-MCP-tool-call hook
 */
export type PreMcpToolCallHandler = (
    input: PreMcpToolCallHookInput,
    invocation: { sessionId: string }
) => Promise<PreMcpToolCallHookOutput | void> | PreMcpToolCallHookOutput | void;

/**
 * Input for post-tool-use hook
 */
export interface PostToolUseHookInput extends BaseHookInput {
    toolName: string;
    toolArgs: unknown;
    toolResult: ToolResultObject;
}

/**
 * Output for post-tool-use hook
 */
export interface PostToolUseHookOutput {
    modifiedResult?: ToolResultObject;
    additionalContext?: string;
    suppressOutput?: boolean;
}

/**
 * Handler for post-tool-use hook
 */
export type PostToolUseHandler = (
    input: PostToolUseHookInput,
    invocation: { sessionId: string }
) => Promise<PostToolUseHookOutput | void> | PostToolUseHookOutput | void;

/**
 * Input for post-tool-use-failure hook.
 *
 * Dispatched after a tool execution whose `resultType` is `"failure"`.
 * The input differs from {@link PostToolUseHookInput}: the host CLI does not
 * forward the full `ToolResultObject` to failure hooks — only `error`, the
 * stringified failure message extracted from the tool's result, is provided.
 */
export interface PostToolUseFailureHookInput extends BaseHookInput {
    toolName: string;
    toolArgs: unknown;
    /**
     * Failure message from the tool's result (the `error` field of the
     * underlying `ToolResultObject`, falling back to its text/log fields).
     */
    error: string;
}

/**
 * Output for post-tool-use-failure hook.
 *
 * Only `additionalContext` is consumed by the host CLI — it is appended as
 * hidden guidance to the model alongside the failed tool result. Other fields
 * such as `modifiedResult` or `suppressOutput` are not honored for failure
 * hooks (see {@link PostToolUseHookOutput} for the success-only hook).
 */
export interface PostToolUseFailureHookOutput {
    additionalContext?: string;
}

/**
 * Handler for post-tool-use-failure hook.
 *
 * Fires after a tool execution whose result was `"failure"`. `onPostToolUse`
 * only fires for successful results, so register this handler to observe or
 * react to failed tool outcomes.
 *
 * Note: `"rejected"`, `"denied"`, and `"timeout"` results do not currently
 * trigger this hook either — only `"failure"` does.
 */
export type PostToolUseFailureHandler = (
    input: PostToolUseFailureHookInput,
    invocation: { sessionId: string }
) => Promise<PostToolUseFailureHookOutput | void> | PostToolUseFailureHookOutput | void;

/**
 * Input for user-prompt-submitted hook
 */
export interface UserPromptSubmittedHookInput extends BaseHookInput {
    prompt: string;
}

/**
 * Output for user-prompt-submitted hook
 */
export interface UserPromptSubmittedHookOutput {
    modifiedPrompt?: string;
    additionalContext?: string;
    suppressOutput?: boolean;
}

/**
 * Handler for user-prompt-submitted hook
 */
export type UserPromptSubmittedHandler = (
    input: UserPromptSubmittedHookInput,
    invocation: { sessionId: string }
) => Promise<UserPromptSubmittedHookOutput | void> | UserPromptSubmittedHookOutput | void;

/**
 * Input for session-start hook
 */
export interface SessionStartHookInput extends BaseHookInput {
    source: "startup" | "resume" | "new";
    initialPrompt?: string;
}

/**
 * Output for session-start hook
 */
export interface SessionStartHookOutput {
    additionalContext?: string;
    modifiedConfig?: Record<string, unknown>;
}

/**
 * Handler for session-start hook
 */
export type SessionStartHandler = (
    input: SessionStartHookInput,
    invocation: { sessionId: string }
) => Promise<SessionStartHookOutput | void> | SessionStartHookOutput | void;

/**
 * Input for session-end hook
 */
export interface SessionEndHookInput extends BaseHookInput {
    reason: "complete" | "error" | "abort" | "timeout" | "user_exit";
    finalMessage?: string;
    error?: string;
}

/**
 * Output for session-end hook
 */
export interface SessionEndHookOutput {
    suppressOutput?: boolean;
    cleanupActions?: string[];
    sessionSummary?: string;
}

/**
 * Handler for session-end hook
 */
export type SessionEndHandler = (
    input: SessionEndHookInput,
    invocation: { sessionId: string }
) => Promise<SessionEndHookOutput | void> | SessionEndHookOutput | void;

/**
 * Input for error-occurred hook
 */
export interface ErrorOccurredHookInput extends BaseHookInput {
    error: string;
    errorContext: "model_call" | "tool_execution" | "system" | "user_input";
    recoverable: boolean;
}

/**
 * Output for error-occurred hook
 */
export interface ErrorOccurredHookOutput {
    suppressOutput?: boolean;
    errorHandling?: "retry" | "skip" | "abort";
    retryCount?: number;
    userNotification?: string;
}

/**
 * Handler for error-occurred hook
 */
export type ErrorOccurredHandler = (
    input: ErrorOccurredHookInput,
    invocation: { sessionId: string }
) => Promise<ErrorOccurredHookOutput | void> | ErrorOccurredHookOutput | void;

/**
 * Configuration for session hooks
 */
export interface SessionHooks {
    /**
     * Called before a tool is executed
     */
    onPreToolUse?: PreToolUseHandler;

    /**
     * Called before an MCP tool is called
     */
    onPreMcpToolCall?: PreMcpToolCallHandler;

    /**
     * Called after a tool is executed with a successful result.
     *
     * For failed tool executions, register {@link onPostToolUseFailure} instead;
     * this handler does not fire for non-success results.
     */
    onPostToolUse?: PostToolUseHandler;

    /**
     * Called after a tool execution whose result was `"failure"`.
     *
     * Register this handler alongside {@link onPostToolUse} to observe failed
     * tool calls — `onPostToolUse` only fires for successful results, so
     * without this hook failed tool calls are invisible to extensions.
     */
    onPostToolUseFailure?: PostToolUseFailureHandler;

    /**
     * Called when the user submits a prompt
     */
    onUserPromptSubmitted?: UserPromptSubmittedHandler;

    /**
     * Called when a session starts
     */
    onSessionStart?: SessionStartHandler;

    /**
     * Called when a session ends
     */
    onSessionEnd?: SessionEndHandler;

    /**
     * Called when an error occurs
     */
    onErrorOccurred?: ErrorOccurredHandler;
}

// ============================================================================
// MCP Server Configuration Types
// ============================================================================

/**
 * Base interface for MCP server configuration.
 */
interface MCPServerConfigBase {
    /**
     * List of tools to include from this server.
     * `undefined` (the default) or `["*"]` means include all tools.
     * `[]` means include none.
     */
    tools?: string[];
    /**
     * Indicates the server type: "stdio" for local/subprocess servers, "http"/"sse" for remote servers.
     * If not specified, defaults to "stdio".
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
export interface MCPStdioServerConfig extends MCPServerConfigBase {
    type?: "local" | "stdio";
    command: string;
    args?: string[];
    /**
     * Environment variables to pass to the server.
     */
    env?: Record<string, string>;
    /**
     * Working directory for the server process.
     */
    workingDirectory?: string;
}

/**
 * Configuration for a remote MCP server (HTTP or SSE).
 */
export interface MCPHTTPServerConfig extends MCPServerConfigBase {
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
export type MCPServerConfig = MCPStdioServerConfig | MCPHTTPServerConfig;

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
    /**
     * List of skill names to preload into this agent's context.
     * When set, the full content of each listed skill is eagerly injected into
     * the agent's context at startup. Skills are resolved by name from the
     * session's configured skill directories (`skillDirectories`).
     * When omitted, no skills are injected (opt-in model).
     */
    skills?: string[];
    /**
     * Model identifier for this agent (e.g. "claude-haiku-4.5").
     * When set, the runtime will attempt to use this model for the agent,
     * falling back to the parent session model if unavailable.
     */
    model?: string;
}

/**
 * Configuration for the default agent (the built-in agent that handles
 * turns when no custom agent is selected).
 * Use this to control tool visibility for the default agent independently of custom sub-agents.
 */
export interface DefaultAgentConfig {
    /**
     * List of tool names to exclude from the default agent.
     * These tools remain available to custom sub-agents that reference them in their `tools` array.
     * Use this to register tools that should only be accessed via delegation to sub-agents,
     * keeping the default agent's context clean.
     */
    excludedTools?: string[];
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

/**
 * Configuration for the memory feature, which lets the agent persist and recall
 * information across turns.
 */
export interface MemoryConfiguration {
    /**
     * Whether the memory feature is enabled for this session.
     */
    enabled: boolean;
}

/**
 * Configuration for handling large tool outputs.
 *
 * When a tool produces output exceeding the configured size, the output is
 * written to a temp file and a reference is returned to the model instead of
 * the full payload.
 */
export interface LargeToolOutputConfig {
    /**
     * Whether large output handling is enabled.
     * @default true
     */
    enabled?: boolean;

    /**
     * Maximum size in bytes before output is written to a temp file.
     * @default 51200
     */
    maxSizeBytes?: number;

    /**
     * Directory to write temp files to. Defaults to the OS temp directory.
     */
    outputDirectory?: string;
}

/**
 * Valid reasoning effort levels for models that support it.
 */
export type ReasoningEffort = "low" | "medium" | "high" | "xhigh";

/**
 * Context window tier for the session. "long_context" pins the session to the
 * long-context tier when the selected model supports it.
 */
export type ContextTier = "default" | "long_context";

/**
 * Stable extension identity for session participants that provide canvases.
 */
export interface ExtensionInfo {
    /** Extension namespace/source, e.g. "github-app". */
    source: string;
    /** Stable provider name within the source namespace. */
    name: string;
}

/**
 * Shared configuration fields used by both {@link SessionConfig} (for
 * creating a new session) and {@link ResumeSessionConfig} (for resuming
 * an existing one).
 */
export interface SessionConfigBase {
    /**
     * Client name to identify the application using the SDK.
     * Included in the User-Agent header for API requests.
     */
    clientName?: string;

    /**
     * Model to use for this session
     */
    model?: string;

    /**
     * Reasoning effort level for models that support it.
     * Only valid for models where capabilities.supports.reasoningEffort is true.
     * Use client.listModels() to check supported values for each model.
     */
    reasoningEffort?: ReasoningEffort;

    /**
     * Reasoning summary mode for models that support configurable reasoning summaries.
     * Use "none" to suppress summary output regardless of whether reasoning is enabled.
     */
    reasoningSummary?: ReasoningSummary;

    /**
     * Context window tier for models that support it. Use "long_context" to pin
     * the session to the long-context tier; omit or use "default" otherwise.
     */
    contextTier?: ContextTier;

    /** Per-property overrides for model capabilities, deep-merged over runtime defaults. */
    modelCapabilities?: ModelCapabilitiesOverride;

    /**
     * Configuration for handling large tool outputs. When a tool produces
     * output exceeding the configured size, the output is written to a temp
     * file and a reference is returned to the model instead of the full
     * payload.
     */
    largeOutput?: LargeToolOutputConfig;

    /**
     * Override the default configuration directory location.
     * When specified, the session will use this directory for storing config and state.
     */
    configDirectory?: string;

    /**
     * When true, automatically discovers MCP server configurations (e.g. `.mcp.json`,
     * `.vscode/mcp.json`) and skill directories from the working directory and merges
     * them with any explicitly provided `mcpServers` and `skillDirectories`, with
     * explicit values taking precedence on name collision.
     *
     * Note: custom instruction files (`.github/copilot-instructions.md`, `AGENTS.md`, etc.)
     * are always loaded from the working directory regardless of this setting.
     *
     * @default false
     */
    enableConfigDiscovery?: boolean;

    /**
     * Tools exposed to the CLI server. Tools without a handler are declaration-only
     * and must be resolved by the consumer via pending external tool request RPCs.
     */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    tools?: Tool<any>[];

    /**
     * Canvases contributed by this session participant. The declaring
     * connection becomes the live provider for `canvas.open|focus|close|reload`
     * and `canvas.action.invoke` dispatches targeting each canvas's `id` for
     * the lifetime of the connection. Re-declaring the same id on resume
     * replaces the prior declaration.
     */
    canvases?: Canvas[];

    /**
     * Renderer-side opt-in: when true, the runtime surfaces canvas agent tools
     * (`list_canvas_capabilities`, `open_canvas`, `invoke_canvas_action`) to
     * the model for this connection. Default off so SDK callers that cannot
     * display canvases stay clean.
     */
    requestCanvasRenderer?: boolean;

    /**
     * Extension surface opt-in: when true, the runtime wires extension
     * management tools and per-extension tool dispatch onto the session for
     * this connection. Default off so callers that do not expose extensions
     * stay clean.
     */
    requestExtensions?: boolean;

    /**
     * Optional override path to a `copilot-sdk/` folder to inject into
     * extension subprocesses for this session in place of the bundled SDK.
     * When unset or invalid (missing folder or missing `index.js` /
     * `extension.js`), the runtime falls back to the bundled SDK without
     * throwing. Takes precedence over any server-level default.
     *
     * Only honored on session create and resume — extensions joining via
     * `joinSession` cannot override the SDK path, because the extension
     * subprocess has already been forked by the host with the SDK the host
     * chose. `JoinSessionConfig` omits this field for that reason.
     */
    extensionSdkPath?: string;

    /**
     * Stable extension identity for canvas providers on this connection. When
     * set, the runtime uses `${source}:${name}` as the agent-facing extension
     * id instead of a reconnect-specific connection id.
     */
    extensionInfo?: ExtensionInfo;

    /**
     * Slash commands registered for this session.
     * When the CLI has a TUI, each command appears as `/name` for the user to invoke.
     * The handler is called when the user executes the command.
     */
    commands?: CommandDefinition[];

    /**
     * System message configuration
     * Controls how the system prompt is constructed
     */
    systemMessage?: SystemMessageConfig;

    /**
     * List of tool names to allow. When specified, only these tools will be available.
     *
     * Supports source-qualified filter patterns (`builtin:*`, `builtin:<name>`,
     * `mcp:*`, `mcp:<name>`, `custom:*`, `custom:<name>`) as well as the bare
     * name form (exact match across any source). Build this list with
     * {@link ToolSet} for type safety and readable intent.
     *
     * Composes with {@link excludedTools}: a tool is enabled when it matches
     * `availableTools` (or `availableTools` is unset) AND it does not match
     * `excludedTools`. This lets you express "everything matching X except Y".
     */
    availableTools?: string[] | ToolSet;

    /**
     * List of tool names to disable. Supports the same pattern syntax as
     * {@link availableTools}.
     *
     * Always takes precedence over {@link availableTools}: a tool listed here
     * is disabled even if it also matches `availableTools`.
     */
    excludedTools?: string[] | ToolSet;

    /**
     * Custom provider configuration (BYOK - Bring Your Own Key).
     * When specified, uses the provided API endpoint instead of the Copilot API.
     */
    provider?: ProviderConfig;

    /**
     * Enables or disables internal session telemetry for this session.
     * When `false`, disables session telemetry. When omitted (the default) or `true`,
     * telemetry is enabled for GitHub-authenticated sessions.
     * When a custom {@link provider} (BYOK) is configured, session telemetry is always
     * disabled regardless of this setting.
     * This is independent of the OpenTelemetry configuration in {@link CopilotClientOptions.telemetry}.
     */
    enableSessionTelemetry?: boolean;

    /**
     * When true, the runtime skips loading custom-instruction sources
     * (e.g. `.github/copilot-instructions.md`, `AGENTS.md`, `CLAUDE.md`).
     *
     * Defaults to `false` (custom instructions are loaded). Under
     * {@link CopilotClientOptions.mode} = `"empty"`, defaults to `true`; apps
     * can pass `false` here to opt back in.
     */
    skipCustomInstructions?: boolean;

    /**
     * When true, custom agents default to local-only execution and are not
     * dispatched to remote workers.
     *
     * Defaults to `false`. Under {@link CopilotClientOptions.mode} = `"empty"`,
     * defaults to `true`; apps can pass `false` here to opt back in.
     */
    customAgentsLocalOnly?: boolean;

    /**
     * When true, the runtime instructs the agent to include a `Co-authored-by`
     * trailer in commit messages it composes.
     *
     * Defaults to `true`. Under {@link CopilotClientOptions.mode} = `"empty"`,
     * defaults to `false`; apps can pass `true` here to opt back in.
     */
    coauthorEnabled?: boolean;

    /**
     * When true, the `manage_schedule` tool is exposed to the agent.
     *
     * Defaults to whatever the runtime exposes (typically gated to staff
     * users). Under {@link CopilotClientOptions.mode} = `"empty"`, defaults to
     * `false`; apps can pass `true` here to opt back in.
     */
    manageScheduleEnabled?: boolean;

    /**
     * Optional handler for permission requests from the server.
     * When omitted, permission requests are surfaced as events and left pending for
     * the consumer to resolve via the pending permission RPC.
     */
    onPermissionRequest?: PermissionHandler;

    /**
     * Handler for user input requests from the agent.
     * When provided, enables the ask_user tool allowing the agent to ask questions.
     */
    onUserInputRequest?: UserInputHandler;

    /**
     * Handler for elicitation requests from the agent.
     * When provided, the server calls back to this client for form-based UI dialogs.
     * Also enables the `elicitation` capability on the session.
     */
    onElicitationRequest?: ElicitationHandler;

    /**
     * Enable MCP Apps (SEP-1865) UI passthrough on this session.
     *
     * When `true` **and** the runtime has MCP Apps enabled (via the
     * `MCP_APPS` feature flag or `COPILOT_MCP_APPS=true` environment
     * override), the runtime adds the `mcp-apps` capability to the session,
     * which causes it to advertise the `extensions.io.modelcontextprotocol/ui`
     * extension to MCP servers (so they expose `_meta.ui.resourceUri` on
     * tools) and to expose the `session.rpc.mcp.apps.{listTools,callTool,
     * readResource,setHostContext,getHostContext,diagnose}` JSON-RPC methods.
     *
     * If the runtime gate is off, the opt-in is silently dropped server-side
     * (the runtime logs a warning); the session is created normally but the
     * MCP Apps surface is unavailable. Inspect the runtime's
     * `capabilities.ui.mcpApps` on the create/resume response to detect this.
     *
     * SDK consumers MUST set this to `true` only when they have an iframe
     * renderer that can display `ui://` MCP App bundles. Setting it without a
     * renderer will cause MCP servers to register UI-enabled tool variants
     * the consumer cannot display.
     *
     * @experimental This option is part of an experimental wire-protocol surface
     * (SEP-1865) and may change or be removed in a future release.
     *
     * @default false
     */
    enableMcpApps?: boolean;

    /**
     * Handler for exit-plan-mode requests from the agent.
     * When provided, enables `exitPlanMode.request` callbacks.
     */
    onExitPlanModeRequest?: ExitPlanModeHandler;

    /**
     * Handler for auto-mode-switch requests from the agent.
     * When provided, enables `autoModeSwitch.request` callbacks.
     */
    onAutoModeSwitchRequest?: AutoModeSwitchHandler;

    /**
     * Hook handlers for intercepting session lifecycle events.
     * When provided, enables hooks callback allowing custom logic at various points.
     */
    hooks?: SessionHooks;

    /**
     * Working directory for the session.
     * Tool operations will be relative to this directory.
     */
    workingDirectory?: string;

    /**
     * Enable streaming of assistant message and reasoning chunks.
     * When true, ephemeral assistant.message_delta and assistant.reasoning_delta
     * events are sent as the response is generated. Clients should accumulate
     * deltaContent values to build the full response.
     * @default false
     */
    streaming?: boolean;

    /**
     * Include sub-agent streaming events in the event stream. When true, streaming
     * delta events from sub-agents (e.g., `assistant.message_delta`,
     * `assistant.reasoning_delta`, `assistant.streaming_delta` with `agentId` set)
     * are forwarded to this connection. When false, only non-streaming sub-agent
     * events and `subagent.*` lifecycle events are forwarded; streaming deltas from
     * sub-agents are suppressed.
     * @default true
     */
    includeSubAgentStreamingEvents?: boolean;

    /**
     * Controls how MCP OAuth tokens are stored for this session.
     * - `"persistent"` — tokens are stored in the OS keychain (shared across sessions)
     * - `"in-memory"` — tokens are stored in memory and discarded when the session ends
     *
     * @default "in-memory"
     */
    mcpOAuthTokenStorage?: "persistent" | "in-memory";

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
     * Configuration for the default agent (the built-in agent that handles
     * turns when no custom agent is selected).
     * Use `excludedTools` to hide specific tools from the default agent while keeping
     * them available to custom sub-agents.
     */
    defaultAgent?: DefaultAgentConfig;

    /**
     * Name of the custom agent to activate when the session starts.
     * Must match the `name` of one of the agents in `customAgents`.
     * Equivalent to calling `session.rpc.agent.select({ name })` after creation.
     */
    agent?: string;

    /**
     * Directories to load skills from.
     */
    skillDirectories?: string[];

    /**
     * Local filesystem paths to Open Plugins-format directories
     * (https://open-plugins.com/) to load for this session.
     *
     * Relative paths resolve against `workingDirectory` (or the runtime cwd if
     * unset); absolute paths are recommended. Invalid entries are logged and
     * skipped.
     *
     * Treated as an explicit opt-in: plugin agents and rules load even when
     * {@link SessionConfigBase.enableConfigDiscovery} is false. Loaded assets
     * slot between project (cwd) sources and personal/home sources in the
     * session-wide precedence order.
     */
    pluginDirectories?: string[];

    /**
     * Additional directories to search for custom instruction files.
     */
    instructionDirectories?: string[];

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

    /**
     * Memory configuration for the session. When omitted, the runtime default applies.
     */
    memory?: MemoryConfiguration;

    /**
     * GitHub token for per-session authentication.
     * When provided, the runtime resolves this token into a full GitHub identity
     * (login, Copilot plan, endpoints) and stores it on the session. This enables
     * multitenancy — different sessions can have different GitHub identities.
     *
     * This is independent of the client-level `gitHubToken` in {@link CopilotClientOptions},
     * which authenticates the CLI process itself. The session-level token determines
     * the identity used for content exclusion, model routing, and quota checks.
     */
    gitHubToken?: string;

    /**
     * When true, skips embedding-based retrieval for this session.
     * Use in multitenant deployments to prevent cross-session information leakage
     * through the shared embedding cache.
     */
    skipEmbeddingRetrieval?: boolean;

    /**
     * Controls how the embedding cache is stored for this session.
     * - `"persistent"`: Embeddings are cached on disk and shared across sessions/restarts.
     * - `"in-memory"`: Embeddings are cached in memory only and discarded when the session ends.
     */
    embeddingCacheStorage?: "persistent" | "in-memory";

    /**
     * Organization-level custom instructions to include in the system prompt.
     * Allows hosts to inject organization-specific guidance without relying on
     * filesystem-based instruction discovery.
     */
    organizationCustomInstructions?: string;

    /**
     * When true, enables on-demand discovery of instruction files (AGENTS.md,
     * .github/copilot-instructions.md, etc.) after successful file views.
     */
    enableOnDemandInstructionDiscovery?: boolean;

    /**
     * When true, enables loading of file-based hooks from `.github/hooks/`.
     * This is separate from the `hooks` callback parameter which gates SDK
     * hook event registration.
     */
    enableFileHooks?: boolean;

    /**
     * When true, enables git operations on the host filesystem (branch detection,
     * file status, commit history). When false, no git context is surfaced in
     * the system prompt.
     */
    enableHostGitOperations?: boolean;

    /**
     * When true, enables the cross-session store for search and retrieval
     * across sessions. When false, session content is not written to or
     * read from the shared session store.
     */
    enableSessionStore?: boolean;

    /**
     * When true, enables skill loading (including builtin skills and discovered
     * skill directories). When false, no skills are loaded regardless of
     * `skillDirectories` or `enableConfigDiscovery` settings.
     */
    enableSkills?: boolean;

    /**
     * Per-session remote behavior control:
     * - `"off"` — local only, no remote export (default)
     * - `"export"` — export session events to GitHub without enabling remote steering
     * - `"on"` — export to GitHub AND enable remote steering
     */
    remoteSession?: RemoteSessionMode;

    /**
     * Optional event handler that is registered on the session before the
     * session.create RPC is issued. This guarantees that early events emitted
     * by the CLI during session creation (e.g. session.start) are delivered to
     * the handler.
     *
     * Equivalent to calling `session.on(handler)` immediately after creation,
     * but executes earlier in the lifecycle so no events are missed.
     */
    onEvent?: SessionEventHandler;

    /**
     * Supplies a handler for session filesystem operations. This takes effect
     * only if {@link CopilotClientOptions.sessionFs} is configured.
     */
    createSessionFsProvider?: (session: CopilotSession) => SessionFsProvider;
}

/**
 * Configuration for creating a new session via {@link CopilotClient.createSession}.
 */
export interface SessionConfig extends SessionConfigBase {
    /**
     * Optional custom session ID. If not provided, the server generates one.
     */
    sessionId?: string;

    /**
     * Creates a remote session in the cloud instead of a local session.
     * The optional repository is associated with the cloud session.
     */
    cloud?: CloudSessionOptions;
}

/**
 * Configuration for resuming an existing session via
 * {@link CopilotClient.resumeSession}.
 */
export interface ResumeSessionConfig extends SessionConfigBase {
    /**
     * When true, skips emitting the session.resume event.
     * Useful for reconnecting to a session without triggering resume-related side effects.
     * @default false
     */
    suppressResumeEvent?: boolean;
    /**
     * When true, the runtime continues any tool calls or permission prompts that were
     * still pending when the session was last suspended. When false (the default), the
     * runtime treats pending work as interrupted on resume.
     *
     * For permission requests, the runtime re-emits `permission.requested` so the
     * registered `onPermissionRequest` handler can re-prompt; for external tool calls,
     * the consumer is expected to supply the result via the corresponding low-level
     * RPC method.
     * @default false
     */
    continuePendingWork?: boolean;
    /**
     * Snapshot of canvases that were already open when the session was suspended.
     * When provided on resume, the runtime can rehydrate canvas state so consumers
     * do not need to re-open canvases that were active before the previous shutdown.
     */
    openCanvases?: OpenCanvasInstance[];
}

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

    /**
     * Custom HTTP headers to include in outbound provider requests.
     */
    headers?: Record<string, string>;

    /**
     * Well-known model name used by the runtime to look up agent configuration
     * (tools, prompts, reasoning behavior) and default token limits. Also used
     * as the wire model when {@link wireModel} is not set.
     * Falls back to {@link SessionConfig.model}.
     */
    modelId?: string;

    /**
     * Model name sent to the provider API for inference. Use this when the
     * provider's model name (e.g. an Azure deployment name or a custom
     * fine-tune name) differs from {@link modelId}.
     * Falls back to {@link modelId}, then {@link SessionConfig.model}.
     */
    wireModel?: string;

    /**
     * Overrides the resolved model's default max prompt tokens. The runtime
     * triggers conversation compaction before sending a request when the
     * prompt (system message, history, tool definitions, user message) would
     * exceed this limit.
     */
    maxPromptTokens?: number;

    /**
     * Overrides the resolved model's default max output tokens. When hit, the
     * model stops generating and returns a truncated response.
     */
    maxOutputTokens?: number;
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
     * File, directory, selection, or blob attachments
     */
    attachments?: Array<
        | {
              type: "file";
              path: string;
              displayName?: string;
          }
        | {
              type: "directory";
              path: string;
              displayName?: string;
          }
        | {
              type: "selection";
              filePath: string;
              displayName: string;
              selection?: {
                  start: { line: number; character: number };
                  end: { line: number; character: number };
              };
              text?: string;
          }
        | {
              type: "blob";
              data: string;
              mimeType: string;
              displayName?: string;
          }
    >;

    /**
     * Message delivery mode
     * - "enqueue": Add to queue (default)
     * - "immediate": Send immediately
     */
    mode?: "enqueue" | "immediate";

    /**
     * The UI mode the agent was in when this message was sent (for example "plan" or "autopilot").
     * Defaults to the session's current mode when unset.
     */
    agentMode?: "interactive" | "plan" | "autopilot" | "shell";

    /**
     * Custom HTTP headers to include in outbound model requests for this turn.
     */
    requestHeaders?: Record<string, string>;

    /**
     * If provided, this is shown in the timeline instead of `prompt`.
     */
    displayPrompt?: string;
}

/**
 * All possible event type strings from SessionEvent
 */
export type SessionEventType = SessionEvent["type"];

/**
 * Extract the specific event payload for a given event type
 */
export type SessionEventPayload<T extends SessionEventType> = Extract<SessionEvent, { type: T }>;

/**
 * Event handler for a specific event type
 */
export type TypedSessionEventHandler<T extends SessionEventType> = (
    event: SessionEventPayload<T>
) => void;

/**
 * Event handler callback type (for all events)
 */
export type SessionEventHandler = (event: SessionEvent) => void;

/**
 * Working directory context for a session
 */
export interface SessionContext {
    /** Working directory where the session was created */
    workingDirectory: string;
    /** Git repository root (if in a git repo) */
    gitRoot?: string;
    /** GitHub repository in "owner/repo" format */
    repository?: string;
    /** Current git branch */
    branch?: string;
}

/**
 * Configuration for a custom session filesystem provider.
 */
export interface SessionFsConfig {
    /**
     * Initial working directory for sessions (user's project directory).
     */
    initialCwd: string;

    /**
     * Path within each session's SessionFs where the runtime stores
     * session-scoped files (events, workspace, checkpoints, etc.).
     */
    sessionStatePath: string;

    /**
     * Path conventions used by this filesystem provider.
     */
    conventions: "windows" | "posix";

    /**
     * Optional capabilities declared by this provider.
     * The runtime uses these to determine which features are available.
     */
    capabilities?: {
        /**
         * Whether this provider supports SQLite query/exists operations.
         * When false or omitted, the runtime will not offer SQL tools or
         * todo tracking for sessions using this provider.
         * @default false
         */
        sqlite?: boolean;
    };
}

/**
 * Filter options for listing sessions
 */
export interface SessionListFilter {
    /** Filter by exact working directory match */
    workingDirectory?: string;
    /** Filter by git root */
    gitRoot?: string;
    /** Filter by repository (owner/repo format) */
    repository?: string;
    /** Filter by branch */
    branch?: string;
}

/**
 * Metadata about a session
 */
export interface SessionMetadata {
    sessionId: string;
    startTime: Date;
    modifiedTime: Date;
    summary?: string;
    isRemote: boolean;
    /** Working directory context (working directory, git info) from session creation */
    context?: SessionContext;
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
        /** Whether this model supports reasoning effort configuration */
        reasoningEffort: boolean;
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

/** Recursively makes all properties optional, preserving arrays as-is. */
type DeepPartial<T> = T extends readonly (infer U)[]
    ? DeepPartial<U>[]
    : T extends object
      ? { [K in keyof T]?: DeepPartial<T[K]> }
      : T;

/** Deep-partial override for model capabilities — every property at any depth is optional. */
export type ModelCapabilitiesOverride = DeepPartial<ModelCapabilities>;

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
    /** Billing cost multiplier relative to the base rate */
    multiplier?: number;
    /** Token-level pricing information for this model */
    tokenPrices?: ModelBillingTokenPrices;
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
    /** Supported reasoning effort levels (only present if model supports reasoning effort) */
    supportedReasoningEfforts?: ReasoningEffort[];
    /** Default reasoning effort level (only present if model supports reasoning effort) */
    defaultReasoningEffort?: ReasoningEffort;
}

// ============================================================================
// Session Lifecycle Types (for TUI+server mode)
// ============================================================================

/**
 * Types of session lifecycle events.
 */
export type SessionLifecycleEventType =
    | "session.created"
    | "session.deleted"
    | "session.updated"
    | "session.foreground"
    | "session.background";

/**
 * Metadata payload for session lifecycle events. Not present on
 * `session.deleted` events.
 */
export interface SessionLifecycleEventMetadata {
    /** Time the session was created. */
    startTime: Date;
    /** Time the session was last modified. */
    modifiedTime: Date;
    /** Human-readable summary of the session, if available. */
    summary?: string;
}

/** Base shape shared by every lifecycle event variant. */
interface SessionLifecycleEventBase {
    /** ID of the session this event relates to. */
    sessionId: string;
    /** Session metadata (not included for `session.deleted`). */
    metadata?: SessionLifecycleEventMetadata;
}

/** Emitted when a new session is created. */
export interface SessionCreatedEvent extends SessionLifecycleEventBase {
    type: "session.created";
    metadata: SessionLifecycleEventMetadata;
}

/** Emitted when a session is deleted. The metadata field is omitted. */
export interface SessionDeletedEvent extends SessionLifecycleEventBase {
    type: "session.deleted";
    metadata?: undefined;
}

/** Emitted when a session's metadata is updated. */
export interface SessionUpdatedEvent extends SessionLifecycleEventBase {
    type: "session.updated";
    metadata: SessionLifecycleEventMetadata;
}

/** Emitted when a session is brought to the foreground (TUI+server mode). */
export interface SessionForegroundEvent extends SessionLifecycleEventBase {
    type: "session.foreground";
    metadata: SessionLifecycleEventMetadata;
}

/** Emitted when a session is moved to the background (TUI+server mode). */
export interface SessionBackgroundEvent extends SessionLifecycleEventBase {
    type: "session.background";
    metadata: SessionLifecycleEventMetadata;
}

/**
 * Discriminated union of all session lifecycle events emitted in TUI+server mode.
 * Switch on `type` to access the variant-specific metadata.
 */
export type SessionLifecycleEvent =
    | SessionCreatedEvent
    | SessionDeletedEvent
    | SessionUpdatedEvent
    | SessionForegroundEvent
    | SessionBackgroundEvent;

/**
 * Handler for session lifecycle events.
 */
export type SessionLifecycleHandler = (event: SessionLifecycleEvent) => void;

/**
 * Typed handler for specific session lifecycle event types.
 */
export type TypedSessionLifecycleHandler<K extends SessionLifecycleEventType> = (
    event: Extract<SessionLifecycleEvent, { type: K }>
) => void;

/**
 * Information about the foreground session in TUI+server mode
 */
export interface ForegroundSessionInfo {
    /** ID of the foreground session, or undefined if none */
    sessionId?: string;
    /** Workspace path of the foreground session */
    workspacePath?: string;
}
