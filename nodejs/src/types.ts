/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Type definitions for the Copilot SDK
 */

// Import and re-export generated session event types
import type { SessionFsProvider } from "./sessionFsProvider.js";
import type { SessionEvent as GeneratedSessionEvent } from "./generated/session-events.js";
import type { CopilotSession } from "./session.js";
export type SessionEvent = GeneratedSessionEvent;
export type { SessionFsProvider } from "./sessionFsProvider.js";
export { createSessionFsAdapter } from "./sessionFsProvider.js";
export type { SessionFsFileInfo } from "./sessionFsProvider.js";

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
    /** File path for JSON-lines trace output. Sets COPILOT_OTEL_FILE_EXPORTER_PATH. */
    filePath?: string;
    /** Exporter backend type: "otlp-http" or "file". Sets COPILOT_OTEL_EXPORTER_TYPE. */
    exporterType?: string;
    /** Instrumentation scope name. Sets COPILOT_OTEL_SOURCE_NAME. */
    sourceName?: string;
    /** Whether to capture message content (prompts, responses). Sets OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT. */
    captureContent?: boolean;
}

export interface CopilotClientOptions {
    /**
     * Path to the CLI executable or JavaScript entry point.
     * If not specified, uses the bundled CLI from the @github/copilot package.
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
     * When true, indicates the SDK is running as a child process of the Copilot CLI server, and should
     * use its own stdio for communicating with the existing parent process. Can only be used in combination
     * with useStdio: true.
     */
    isChildProcess?: boolean;

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
     * @deprecated This option has no effect and will be removed in a future release.
     */
    autoRestart?: boolean;

    /**
     * Environment variables to pass to the CLI process. If not set, inherits process.env.
     */
    env?: Record<string, string | undefined>;

    /**
     * GitHub token to use for authentication.
     * When provided, the token is passed to the CLI server via environment variable.
     * This takes priority over other authentication methods.
     */
    githubToken?: string;

    /**
     * Whether to use the logged-in user for authentication.
     * When true, the CLI server will attempt to use stored OAuth tokens or gh CLI auth.
     * When false, only explicit tokens (githubToken or environment variables) are used.
     * @default true (but defaults to false when githubToken is provided)
     */
    useLoggedInUser?: boolean;

    /**
     * Custom handler for listing available models.
     * When provided, client.listModels() calls this handler instead of
     * querying the CLI server. Useful in BYOK mode to return models
     * available from your custom provider.
     */
    onListModels?: () => Promise<ModelInfo[]> | ModelInfo[];

    /**
     * OpenTelemetry configuration for the CLI process.
     * When provided, the corresponding OTel environment variables are set
     * on the spawned CLI server.
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
}

/**
 * Configuration for creating a session
 */
export type ToolResultType = "success" | "failure" | "rejected" | "denied" | "timeout";

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
                    binaryResults.push({
                        data: block.resource.blob,
                        mimeType: block.resource.mimeType ?? "application/octet-stream",
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
 */
export interface Tool<TArgs = unknown> {
    name: string;
    description?: string;
    parameters?: ZodSchema<TArgs> | Record<string, unknown>;
    handler: ToolHandler<TArgs>;
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
        overridesBuiltInTool?: boolean;
        skipPermission?: boolean;
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
export interface InputOptions {
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
    input(message: string, options?: InputOptions): Promise<string | null>;
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
 * Known system prompt section identifiers for the "customize" mode.
 * Each section corresponds to a distinct part of the system prompt.
 */
export type SystemPromptSection =
    | "identity"
    | "tone"
    | "tool_efficiency"
    | "environment_context"
    | "code_change_rules"
    | "guidelines"
    | "safety"
    | "tool_instructions"
    | "custom_instructions"
    | "last_instructions";

/** Section metadata for documentation and tooling. */
export const SYSTEM_PROMPT_SECTIONS: Record<SystemPromptSection, { description: string }> = {
    identity: { description: "Agent identity preamble and mode statement" },
    tone: { description: "Response style, conciseness rules, output formatting preferences" },
    tool_efficiency: { description: "Tool usage patterns, parallel calling, batching guidelines" },
    environment_context: { description: "CWD, OS, git root, directory listing, available tools" },
    code_change_rules: { description: "Coding rules, linting/testing, ecosystem tools, style" },
    guidelines: { description: "Tips, behavioral best practices, behavioral guidelines" },
    safety: { description: "Environment limitations, prohibited actions, security policies" },
    tool_instructions: { description: "Per-tool usage instructions" },
    custom_instructions: { description: "Repository and organization custom instructions" },
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
 * Override operation for a single system prompt section.
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
    sections?: Partial<Record<SystemPromptSection, SectionOverride>>;

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
 * Permission request types from the server
 */
export interface PermissionRequest {
    kind: "shell" | "write" | "mcp" | "read" | "url" | "custom-tool" | "memory" | "hook";
    toolCallId?: string;
}

import type { PermissionDecisionRequest } from "./generated/rpc.js";

export type PermissionRequestResult = PermissionDecisionRequest["result"] | { kind: "no-result" };

export type PermissionHandler = (
    request: PermissionRequest,
    invocation: { sessionId: string }
) => Promise<PermissionRequestResult> | PermissionRequestResult;

export const approveAll: PermissionHandler = () => ({ kind: "approved" });

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

// ============================================================================
// Hook Types
// ============================================================================

/**
 * Base interface for all hook inputs
 */
export interface BaseHookInput {
    timestamp: number;
    cwd: string;
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
     * Called after a tool is executed
     */
    onPostToolUse?: PostToolUseHandler;

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
     * List of tools to include from this server. [] means none. "*" means all.
     */
    tools: string[];
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
 * Valid reasoning effort levels for models that support it.
 */
export type ReasoningEffort = "low" | "medium" | "high" | "xhigh";

export interface SessionConfig {
    /**
     * Optional custom session ID
     * If not provided, server will generate one
     */
    sessionId?: string;

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

    /** Per-property overrides for model capabilities, deep-merged over runtime defaults. */
    modelCapabilities?: ModelCapabilitiesOverride;

    /**
     * Override the default configuration directory location.
     * When specified, the session will use this directory for storing config and state.
     */
    configDir?: string;

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
     * Tools exposed to the CLI server
     */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    tools?: Tool<any>[];

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
    onPermissionRequest: PermissionHandler;

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
     * Hook handlers for intercepting session lifecycle events.
     * When provided, enables hooks callback allowing custom logic at various points.
     */
    hooks?: SessionHooks;

    /**
     * Working directory for the session.
     * Tool operations will be relative to this directory.
     */
    workingDirectory?: string;

    /*
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
    createSessionFsHandler?: (session: CopilotSession) => SessionFsProvider;
}

/**
 * Configuration for resuming a session
 */
export type ResumeSessionConfig = Pick<
    SessionConfig,
    | "clientName"
    | "model"
    | "tools"
    | "commands"
    | "systemMessage"
    | "availableTools"
    | "excludedTools"
    | "provider"
    | "modelCapabilities"
    | "streaming"
    | "includeSubAgentStreamingEvents"
    | "reasoningEffort"
    | "onPermissionRequest"
    | "onUserInputRequest"
    | "onElicitationRequest"
    | "hooks"
    | "workingDirectory"
    | "configDir"
    | "enableConfigDiscovery"
    | "mcpServers"
    | "customAgents"
    | "defaultAgent"
    | "agent"
    | "skillDirectories"
    | "disabledSkills"
    | "infiniteSessions"
    | "onEvent"
    | "createSessionFsHandler"
> & {
    /**
     * When true, skips emitting the session.resume event.
     * Useful for reconnecting to a session without triggering resume-related side effects.
     * @default false
     */
    disableResume?: boolean;
};

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
     * Custom HTTP headers to include in outbound model requests for this turn.
     */
    requestHeaders?: Record<string, string>;
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
 * Connection state
 */
export type ConnectionState = "disconnected" | "connecting" | "connected" | "error";

/**
 * Working directory context for a session
 */
export interface SessionContext {
    /** Working directory where the session was created */
    cwd: string;
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
}

/**
 * Filter options for listing sessions
 */
export interface SessionListFilter {
    /** Filter by exact cwd match */
    cwd?: string;
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
    /** Working directory context (cwd, git info) from session creation */
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
    /** Supported reasoning effort levels (only present if model supports reasoning effort) */
    supportedReasoningEfforts?: ReasoningEffort[];
    /** Default reasoning effort level (only present if model supports reasoning effort) */
    defaultReasoningEffort?: ReasoningEffort;
}

// ============================================================================
// Session Lifecycle Types (for TUI+server mode)
// ============================================================================

/**
 * Types of session lifecycle events
 */
export type SessionLifecycleEventType =
    | "session.created"
    | "session.deleted"
    | "session.updated"
    | "session.foreground"
    | "session.background";

/**
 * Session lifecycle event notification
 * Sent when sessions are created, deleted, updated, or change foreground/background state
 */
export interface SessionLifecycleEvent {
    /** Type of lifecycle event */
    type: SessionLifecycleEventType;
    /** ID of the session this event relates to */
    sessionId: string;
    /** Session metadata (not included for deleted sessions) */
    metadata?: {
        startTime: string;
        modifiedTime: string;
        summary?: string;
    };
}

/**
 * Handler for session lifecycle events
 */
export type SessionLifecycleHandler = (event: SessionLifecycleEvent) => void;

/**
 * Typed handler for specific session lifecycle event types
 */
export type TypedSessionLifecycleHandler<K extends SessionLifecycleEventType> = (
    event: SessionLifecycleEvent & { type: K }
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
