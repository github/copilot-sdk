/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * OpenTelemetry instrumentation for the Copilot SDK, implementing
 * the Semantic Conventions for Generative AI systems.
 *
 * @see https://opentelemetry.io/docs/specs/semconv/gen-ai/
 * @internal This module is NOT exported from the public API.
 */

import {
    type Span,
    type Context,
    type Tracer,
    type Meter,
    type Histogram,
    SpanKind,
    SpanStatusCode,
    trace,
    metrics,
    context,
} from "@opentelemetry/api";
import type {
    TelemetryConfig,
    ProviderConfig,
    SystemMessageConfig,
    Tool,
    SessionEvent,
} from "./types.js";

// Read version from package.json at module load time.
import { createRequire } from "node:module";
const _require = createRequire(import.meta.url);
const SDK_VERSION: string | undefined = (_require("../package.json") as { version?: string })
    .version;

// ============================================================================
// Error subclasses
// ============================================================================

class SessionDestroyedError extends Error {
    constructor(message: string) {
        super(message);
        this.name = "SessionDestroyedError";
    }
}

class SessionTurnError extends Error {
    constructor(message: string) {
        super(message);
        this.name = "SessionTurnError";
    }
}

// ============================================================================
// Constants (matching .NET OpenTelemetryConsts)
// ============================================================================

const DEFAULT_SOURCE_NAME = "github.copilot.sdk";
const DEFAULT_PROVIDER_NAME = "github";
const CAPTURE_MESSAGE_CONTENT_ENV_VAR = "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT";

const SECONDS_UNIT = "s";
const TOKENS_UNIT = "{token}";

// Attribute keys
const ATTR = {
    GEN_AI_OPERATION_NAME: "gen_ai.operation.name",
    GEN_AI_PROVIDER_NAME: "gen_ai.provider.name",
    GEN_AI_AGENT_ID: "gen_ai.agent.id",
    GEN_AI_AGENT_NAME: "gen_ai.agent.name",
    GEN_AI_AGENT_DESCRIPTION: "gen_ai.agent.description",
    GEN_AI_CONVERSATION_ID: "gen_ai.conversation.id",
    GEN_AI_REQUEST_MODEL: "gen_ai.request.model",
    GEN_AI_RESPONSE_MODEL: "gen_ai.response.model",
    GEN_AI_RESPONSE_ID: "gen_ai.response.id",
    GEN_AI_RESPONSE_FINISH_REASONS: "gen_ai.response.finish_reasons",
    GEN_AI_USAGE_INPUT_TOKENS: "gen_ai.usage.input_tokens",
    GEN_AI_USAGE_OUTPUT_TOKENS: "gen_ai.usage.output_tokens",
    GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS: "gen_ai.usage.cache_read.input_tokens",
    GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS: "gen_ai.usage.cache_creation.input_tokens",
    GEN_AI_TOKEN_TYPE: "gen_ai.token.type",
    GEN_AI_TOOL_NAME: "gen_ai.tool.name",
    GEN_AI_TOOL_CALL_ID: "gen_ai.tool.call.id",
    GEN_AI_TOOL_TYPE: "gen_ai.tool.type",
    GEN_AI_TOOL_DESCRIPTION: "gen_ai.tool.description",
    GEN_AI_TOOL_CALL_ARGUMENTS: "gen_ai.tool.call.arguments",
    GEN_AI_TOOL_CALL_RESULT: "gen_ai.tool.call.result",
    GEN_AI_TOOL_DEFINITIONS: "gen_ai.tool.definitions",
    GEN_AI_INPUT_MESSAGES: "gen_ai.input.messages",
    GEN_AI_OUTPUT_MESSAGES: "gen_ai.output.messages",
    GEN_AI_SYSTEM_INSTRUCTIONS: "gen_ai.system_instructions",
    SERVER_ADDRESS: "server.address",
    SERVER_PORT: "server.port",
    ERROR_TYPE: "error.type",

    // Vendor-prefixed span attributes for Copilot-specific data
    COPILOT_COST: "github.copilot.cost",
    COPILOT_SERVER_DURATION: "github.copilot.server_duration",
    COPILOT_INITIATOR: "github.copilot.initiator",
    COPILOT_AIU: "github.copilot.aiu",
    COPILOT_TURN_ID: "github.copilot.turn_id",
    COPILOT_INTERACTION_ID: "github.copilot.interaction_id",
} as const;

// Vendor-prefixed span event names and attribute keys
const COPILOT_EVENT = {
    SESSION_TRUNCATION: "github.copilot.session.truncation",
    SESSION_COMPACTION_START: "github.copilot.session.compaction_start",
    SESSION_COMPACTION_COMPLETE: "github.copilot.session.compaction_complete",
    SKILL_INVOKED: "github.copilot.skill.invoked",

    // Attribute keys for custom events
    MESSAGE: "github.copilot.message",
    TOKEN_LIMIT: "github.copilot.token_limit",
    PRE_TOKENS: "github.copilot.pre_tokens",
    POST_TOKENS: "github.copilot.post_tokens",
    PRE_MESSAGES: "github.copilot.pre_messages",
    POST_MESSAGES: "github.copilot.post_messages",
    TOKENS_REMOVED: "github.copilot.tokens_removed",
    MESSAGES_REMOVED: "github.copilot.messages_removed",
    PERFORMED_BY: "github.copilot.performed_by",
    SUCCESS: "github.copilot.success",
    SKILL_NAME: "github.copilot.skill.name",
    SKILL_PATH: "github.copilot.skill.path",
    SKILL_CONTENT: "github.copilot.skill.content",
    SKILL_PLUGIN_NAME: "github.copilot.skill.plugin_name",
    SKILL_PLUGIN_VERSION: "github.copilot.skill.plugin_version",
} as const;

// Operation names
const INVOKE_AGENT = "invoke_agent";
const CHAT = "chat";
const EXECUTE_TOOL = "execute_tool";

// Histogram metric names
const METRIC_OPERATION_DURATION = "gen_ai.client.operation.duration";
const METRIC_TOKEN_USAGE = "gen_ai.client.token.usage";
const METRIC_TIME_TO_FIRST_CHUNK = "gen_ai.client.operation.time_to_first_chunk";
const METRIC_TIME_PER_OUTPUT_CHUNK = "gen_ai.client.operation.time_per_output_chunk";

// Bucket boundaries matching .NET
const DURATION_BOUNDARIES = [
    0.01, 0.02, 0.04, 0.08, 0.16, 0.32, 0.64, 1.28, 2.56, 5.12, 10.24, 20.48, 40.96, 81.92,
];
const TOKEN_BOUNDARIES = [
    1, 4, 16, 64, 256, 1024, 4096, 16384, 65536, 262144, 1048576, 4194304, 16777216, 67108864,
];

// ============================================================================
// OTel message types for parts-based JSON
// ============================================================================

interface OtelPart {
    type: string;
    content?: string;
    id?: string;
    name?: string;
    arguments?: unknown;
    response?: unknown;
    mcpServerName?: string;
}

interface OtelMsg {
    role: string;
    parts: OtelPart[];
    finishReason?: string;
}

// ============================================================================
// Subagent state
// ============================================================================

interface SubagentState {
    invokeAgentSpan: Span;
    invokeAgentContext: Context;
    invokeAgentStartTime: number;
    chatSpan: Span | undefined;
    agentName: string | undefined;
    responseModel: string | undefined;
    responseId: string | undefined;
    inputTokens: number;
    outputTokens: number;
    cacheReadTokens: number;
    cacheCreationTokens: number;
    inputMessages: OtelMsg[];
    outputMessages: OtelMsg[];
}

// ============================================================================
// CopilotTelemetry
// ============================================================================

/**
 * Provides OpenTelemetry instrumentation for the Copilot SDK.
 * @internal
 */
export class CopilotTelemetry {
    readonly tracer: Tracer;
    private readonly meter: Meter;

    readonly operationDurationHistogram: Histogram;
    readonly tokenUsageHistogram: Histogram;
    readonly timeToFirstChunkHistogram: Histogram;
    readonly timePerOutputChunkHistogram: Histogram;

    readonly enableSensitiveData: boolean;

    constructor(config: TelemetryConfig) {
        const sourceName = config.sourceName ?? DEFAULT_SOURCE_NAME;

        this.enableSensitiveData =
            config.enableSensitiveData ??
            process.env[CAPTURE_MESSAGE_CONTENT_ENV_VAR]?.toLowerCase() === "true";

        this.tracer = trace.getTracer(sourceName, SDK_VERSION);
        this.meter = metrics.getMeter(sourceName, SDK_VERSION);

        this.operationDurationHistogram = this.meter.createHistogram(METRIC_OPERATION_DURATION, {
            unit: SECONDS_UNIT,
            description: "GenAI operation duration.",
            advice: {
                explicitBucketBoundaries: DURATION_BOUNDARIES,
            },
        });

        this.tokenUsageHistogram = this.meter.createHistogram(METRIC_TOKEN_USAGE, {
            unit: TOKENS_UNIT,
            description: "Number of input and output tokens used.",
            advice: {
                explicitBucketBoundaries: TOKEN_BOUNDARIES,
            },
        });

        this.timeToFirstChunkHistogram = this.meter.createHistogram(METRIC_TIME_TO_FIRST_CHUNK, {
            unit: SECONDS_UNIT,
            description: "Time to receive the first chunk from a streaming response.",
            advice: {
                explicitBucketBoundaries: DURATION_BOUNDARIES,
            },
        });

        this.timePerOutputChunkHistogram = this.meter.createHistogram(
            METRIC_TIME_PER_OUTPUT_CHUNK,
            {
                unit: SECONDS_UNIT,
                description: "Time elapsed between streamed output chunks after the first chunk.",
                advice: {
                    explicitBucketBoundaries: DURATION_BOUNDARIES,
                },
            }
        );
    }

    // ========================================================================
    // Span creation
    // ========================================================================

    /** Starts an invoke_agent span for a session turn. */
    startInvokeAgentSpan(
        sessionId: string,
        model: string | undefined,
        providerName: string,
        serverAddress: string | undefined,
        serverPort: number | undefined,
        agentName?: string,
        agentDescription?: string,
        parentContext?: Context
    ): Span {
        const displayName = agentName ? `${INVOKE_AGENT} ${agentName}` : INVOKE_AGENT;

        const attributes: Record<string, string | number> = {
            [ATTR.GEN_AI_OPERATION_NAME]: INVOKE_AGENT,
            [ATTR.GEN_AI_PROVIDER_NAME]: providerName,
            [ATTR.GEN_AI_AGENT_ID]: sessionId,
            [ATTR.GEN_AI_CONVERSATION_ID]: sessionId,
        };

        if (model) {
            attributes[ATTR.GEN_AI_REQUEST_MODEL] = model;
        }
        if (agentName) {
            attributes[ATTR.GEN_AI_AGENT_NAME] = agentName;
        }
        if (agentDescription) {
            attributes[ATTR.GEN_AI_AGENT_DESCRIPTION] = agentDescription;
        }
        if (serverAddress) {
            attributes[ATTR.SERVER_ADDRESS] = serverAddress;
            if (serverPort != null) {
                attributes[ATTR.SERVER_PORT] = serverPort;
            }
        }

        const span = this.tracer.startSpan(
            displayName,
            { kind: SpanKind.CLIENT, attributes },
            parentContext
        );

        return span;
    }

    /** Starts a chat child span for an individual LLM turn. */
    startChatSpan(
        model: string | undefined,
        providerName: string,
        serverAddress: string | undefined,
        serverPort: number | undefined,
        parentContext: Context,
        conversationId?: string
    ): Span {
        const displayName = model ? `${CHAT} ${model}` : CHAT;

        const attributes: Record<string, string | number> = {
            [ATTR.GEN_AI_OPERATION_NAME]: CHAT,
            [ATTR.GEN_AI_PROVIDER_NAME]: providerName,
        };

        if (model) {
            attributes[ATTR.GEN_AI_REQUEST_MODEL] = model;
        }
        if (conversationId) {
            attributes[ATTR.GEN_AI_CONVERSATION_ID] = conversationId;
        }
        if (serverAddress) {
            attributes[ATTR.SERVER_ADDRESS] = serverAddress;
            if (serverPort != null) {
                attributes[ATTR.SERVER_PORT] = serverPort;
            }
        }

        return this.tracer.startSpan(
            displayName,
            { kind: SpanKind.CLIENT, attributes },
            parentContext
        );
    }

    /** Starts an execute_tool span for a tool call. */
    startExecuteToolSpan(
        toolName: string,
        toolCallId: string,
        description: string | undefined,
        args: unknown,
        parentContext?: Context
    ): Span {
        const displayName = `${EXECUTE_TOOL} ${toolName}`;

        const attributes: Record<string, string> = {
            [ATTR.GEN_AI_OPERATION_NAME]: EXECUTE_TOOL,
            [ATTR.GEN_AI_TOOL_NAME]: toolName,
            [ATTR.GEN_AI_TOOL_CALL_ID]: toolCallId,
            [ATTR.GEN_AI_TOOL_TYPE]: "function",
        };

        if (description) {
            attributes[ATTR.GEN_AI_TOOL_DESCRIPTION] = description;
        }

        if (this.enableSensitiveData && args != null) {
            attributes[ATTR.GEN_AI_TOOL_CALL_ARGUMENTS] = serializeValue(args);
        }

        const span = this.tracer.startSpan(
            displayName,
            { kind: SpanKind.INTERNAL, attributes },
            parentContext ?? context.active()
        );

        return span;
    }

    /** Sets the execute_tool result attribute (sensitive-gated). */
    setExecuteToolResult(span: Span, result: unknown): void {
        if (!this.enableSensitiveData || result == null) {
            return;
        }
        span.setAttribute(ATTR.GEN_AI_TOOL_CALL_RESULT, serializeValue(result));
    }

    // ========================================================================
    // Metric recording
    // ========================================================================

    /** Records token usage metrics at turn completion (so error.type can be included). */
    recordTokenUsageMetrics(
        inputTokens: number | undefined,
        outputTokens: number | undefined,
        requestModel: string | undefined,
        responseModel: string | undefined,
        providerName: string,
        serverAddress: string | undefined,
        serverPort: number | undefined,
        error: Error | undefined,
        operationName: string = INVOKE_AGENT
    ): void {
        const baseTags = createMetricTags(
            operationName,
            requestModel,
            responseModel,
            providerName,
            serverAddress,
            serverPort
        );
        if (error) {
            baseTags[ATTR.ERROR_TYPE] = error.name || error.constructor.name;
        }

        if (inputTokens != null) {
            this.tokenUsageHistogram.record(inputTokens, {
                ...baseTags,
                [ATTR.GEN_AI_TOKEN_TYPE]: "input",
            });
        }
        if (outputTokens != null) {
            this.tokenUsageHistogram.record(outputTokens, {
                ...baseTags,
                [ATTR.GEN_AI_TOKEN_TYPE]: "output",
            });
        }
    }

    /** Records operation duration metric. */
    recordOperationDuration(
        durationSeconds: number,
        requestModel: string | undefined,
        responseModel: string | undefined,
        providerName: string,
        serverAddress: string | undefined,
        serverPort: number | undefined,
        error: Error | undefined,
        operationName: string = INVOKE_AGENT
    ): void {
        const tags = createMetricTags(
            operationName,
            requestModel,
            responseModel,
            providerName,
            serverAddress,
            serverPort
        );
        if (error) {
            tags[ATTR.ERROR_TYPE] = error.name || error.constructor.name;
        }
        this.operationDurationHistogram.record(durationSeconds, tags);
    }

    /** Records time-to-first-chunk metric. */
    recordTimeToFirstChunk(
        durationSeconds: number,
        requestModel: string | undefined,
        responseModel: string | undefined,
        providerName: string,
        serverAddress: string | undefined,
        serverPort: number | undefined
    ): void {
        this.timeToFirstChunkHistogram.record(
            durationSeconds,
            createMetricTags(
                CHAT,
                requestModel,
                responseModel,
                providerName,
                serverAddress,
                serverPort
            )
        );
    }

    /** Records time-per-output-chunk metric. */
    recordTimePerOutputChunk(
        durationSeconds: number,
        requestModel: string | undefined,
        responseModel: string | undefined,
        providerName: string,
        serverAddress: string | undefined,
        serverPort: number | undefined
    ): void {
        this.timePerOutputChunkHistogram.record(
            durationSeconds,
            createMetricTags(
                CHAT,
                requestModel,
                responseModel,
                providerName,
                serverAddress,
                serverPort
            )
        );
    }

    /** Records an error on a span. */
    static recordError(span: Span, error: Error): void {
        span.setAttribute(ATTR.ERROR_TYPE, error.name || error.constructor.name);
        span.setStatus({ code: SpanStatusCode.ERROR, message: error.message });
    }

    // ========================================================================
    // Utility
    // ========================================================================

    /** Normalizes a provider type string to its OTel semantic convention name. */
    static normalizeProviderName(providerType?: string): string {
        if (!providerType) {
            return DEFAULT_PROVIDER_NAME;
        }
        switch (providerType.trim().toLowerCase()) {
            case "anthropic":
                return "anthropic";
            case "azure":
                return "azure.ai.openai";
            case "openai":
                return "openai";
            default:
                return DEFAULT_PROVIDER_NAME;
        }
    }

    /** Parses a URL into host and port components. */
    static parseServerAddress(baseUrl?: string): {
        address: string | undefined;
        port: number | undefined;
    } {
        if (!baseUrl) {
            return { address: undefined, port: undefined };
        }
        try {
            const url = new URL(baseUrl);
            if (!url.hostname) {
                return { address: undefined, port: undefined };
            }
            let port: number | undefined;
            if (url.port) {
                port = parseInt(url.port, 10);
            } else if (url.protocol === "https:") {
                port = 443;
            } else if (url.protocol === "http:") {
                port = 80;
            }
            return { address: url.hostname, port };
        } catch {
            return { address: undefined, port: undefined };
        }
    }
}

// ============================================================================
// Helper functions
// ============================================================================

function createMetricTags(
    operationName: string,
    requestModel: string | undefined,
    responseModel: string | undefined,
    providerName: string,
    serverAddress: string | undefined,
    serverPort: number | undefined
): Record<string, string | number> {
    const tags: Record<string, string | number> = {
        [ATTR.GEN_AI_OPERATION_NAME]: operationName,
        [ATTR.GEN_AI_PROVIDER_NAME]: providerName,
    };

    if (requestModel) {
        tags[ATTR.GEN_AI_REQUEST_MODEL] = requestModel;
    }
    if (responseModel) {
        tags[ATTR.GEN_AI_RESPONSE_MODEL] = responseModel;
    }
    if (serverAddress) {
        tags[ATTR.SERVER_ADDRESS] = serverAddress;
        if (serverPort != null) {
            tags[ATTR.SERVER_PORT] = serverPort;
        }
    }

    return tags;
}

function serializeValue(value: unknown): string {
    if (typeof value === "string") return value;
    try {
        return JSON.stringify(value);
    } catch {
        return String(value);
    }
}

// ============================================================================
// Parts-based message JSON builder (matching .NET BuildMessagesJson)
// ============================================================================

function buildMessagesJson(
    messages: OtelMsg[] | undefined,
    finishReason?: string
): string | undefined {
    if (!messages || messages.length === 0) {
        return undefined;
    }

    const result: unknown[] = [];
    for (const message of messages) {
        if (message.parts.length === 0) {
            continue;
        }

        const parts: unknown[] = [];
        for (const part of message.parts) {
            const obj: Record<string, unknown> = { type: part.type };

            switch (part.type) {
                case "server_tool_call":
                    if (part.id != null) obj.id = part.id;
                    if (part.name != null) obj.name = part.name;
                    obj.server_tool_call = {
                        type: "mcp",
                        ...(part.mcpServerName != null && { server_name: part.mcpServerName }),
                        ...(part.arguments != null && { arguments: part.arguments }),
                    };
                    break;

                case "server_tool_call_response":
                    if (part.id != null) obj.id = part.id;
                    obj.server_tool_call_response = {
                        type: "mcp",
                        ...(part.mcpServerName != null && { server_name: part.mcpServerName }),
                        ...(part.response != null && { response: part.response }),
                    };
                    break;

                default:
                    if (part.content != null) obj.content = part.content;
                    if (part.id != null) obj.id = part.id;
                    if (part.name != null) obj.name = part.name;
                    if (part.arguments != null) obj.arguments = part.arguments;
                    if (part.response != null) obj.response = part.response;
                    break;
            }

            parts.push(obj);
        }

        const msg: Record<string, unknown> = { role: message.role, parts };
        const effectiveFinishReason = message.finishReason ?? finishReason;
        if (effectiveFinishReason) {
            msg.finish_reason = effectiveFinishReason;
        }
        result.push(msg);
    }

    return result.length > 0 ? JSON.stringify(result) : undefined;
}

function buildSystemInstructionsJson(
    systemMessage: SystemMessageConfig | undefined
): string | undefined {
    const content = systemMessage?.content;
    if (!content) {
        return undefined;
    }
    return JSON.stringify([{ type: "text", content }]);
}

function buildToolDefinitionsJson(tools: Tool[] | undefined): string | undefined {
    if (!tools || tools.length === 0) {
        return undefined;
    }

    const defs = tools.map((tool) => {
        const def: Record<string, unknown> = {
            type: "function",
            name: tool.name,
        };
        if (tool.description) {
            def.description = tool.description;
        }
        if (tool.parameters) {
            def.parameters =
                typeof tool.parameters === "object" &&
                "toJSONSchema" in tool.parameters &&
                typeof (tool.parameters as { toJSONSchema?: unknown }).toJSONSchema === "function"
                    ? (
                          tool.parameters as { toJSONSchema(): Record<string, unknown> }
                      ).toJSONSchema()
                    : tool.parameters;
        }
        return def;
    });

    return JSON.stringify(defs);
}

// ============================================================================
// Helper to extract parentToolCallId from events
// ============================================================================

function getParentToolCallId(event: SessionEvent): string | undefined {
    switch (event.type) {
        case "assistant.usage":
        case "assistant.message":
        case "assistant.message_delta":
        case "tool.execution_start":
        case "tool.execution_complete":
            return (event.data as { parentToolCallId?: string }).parentToolCallId ?? undefined;
        default:
            return undefined;
    }
}

// ============================================================================
// AgentTurnTracker
// ============================================================================

/**
 * Encapsulates per-turn telemetry state and logic for a Copilot session.
 * Implements a two-level span hierarchy: invoke_agent → chat, matching the
 * .NET AgentTurnTracker. Supports subagent spans and tool call parenting.
 * @internal
 */
export class AgentTurnTracker {
    private readonly _telemetry: CopilotTelemetry;
    private readonly _sessionId: string;

    // Context fields set once at construction
    private readonly _requestModel: string | undefined;
    private readonly _agentName: string | undefined;
    private readonly _agentDescription: string | undefined;
    private readonly _systemInstructionsJson: string | undefined;
    private readonly _toolDefinitionsJson: string | undefined;
    private readonly _isStreaming: boolean;
    readonly providerName: string;
    readonly serverAddress: string | undefined;
    readonly serverPort: number | undefined;

    // Per-invoke_agent mutable state
    private _agentSpan: Span | undefined;
    private _agentSpanContext: Context | undefined;
    private _agentStartTime: number | undefined;
    private _agentInputMessages: OtelMsg[] | undefined;
    private _agentOutputMessages: OtelMsg[] | undefined;
    private _activeSubagents: Map<string, SubagentState> | undefined;

    // Pending tool call parent contexts
    private _pendingToolParents: Map<string, Context> | undefined;

    // MCP server tool tracking: toolCallId → mcpServerName
    private _serverToolCallIds: Map<string, string> | undefined;

    // Agent-level accumulated usage across all chat turns
    private _agentResponseModel: string | undefined;
    private _agentResponseId: string | undefined;
    private _agentTotalInputTokens = 0;
    private _agentTotalOutputTokens = 0;
    private _agentTotalCacheReadTokens = 0;
    private _agentTotalCacheCreationTokens = 0;
    private _agentTotalCost = 0;
    private _agentTotalAiu = 0;

    // Per-chat-turn mutable state (reset on each assistant.turn_start)
    private _turnSpan: Span | undefined;
    private _turnStartTime: number | undefined;
    private _firstOutputChunkRecorded = false;
    private _lastOutputChunkTime = 0;
    private _responseModel: string | undefined;
    private _responseId: string | undefined;
    private _inputTokens = 0;
    private _outputTokens = 0;
    private _cacheReadTokens = 0;
    private _cacheCreationTokens = 0;
    private _inputMessages: OtelMsg[] | undefined;
    private _outputMessages: OtelMsg[] | undefined;

    // Copilot-specific per-turn attributes
    private _turnCost: number | undefined;
    private _turnServerDuration: number | undefined;
    private _turnInitiator: string | undefined;
    private _turnAiu: number | undefined;
    private _turnId: string | undefined;
    private _turnInteractionId: string | undefined;

    // Stashed user prompt for the first chat turn
    private _pendingUserPrompt: string | undefined;

    constructor(
        telemetry: CopilotTelemetry,
        sessionId: string,
        model: string | undefined,
        provider: ProviderConfig | undefined,
        systemMessage: SystemMessageConfig | undefined,
        tools: Tool[] | undefined,
        streaming: boolean | undefined,
        agentName?: string,
        agentDescription?: string
    ) {
        this._telemetry = telemetry;
        this._sessionId = sessionId;
        this._requestModel = model;
        this._agentName = agentName;
        this._agentDescription = agentDescription;
        this.providerName = CopilotTelemetry.normalizeProviderName(provider?.type);
        const parsed = CopilotTelemetry.parseServerAddress(provider?.baseUrl);
        this.serverAddress = parsed.address;
        this.serverPort = parsed.port;
        this._systemInstructionsJson = buildSystemInstructionsJson(systemMessage);
        this._toolDefinitionsJson = buildToolDefinitionsJson(tools);
        this._isStreaming = streaming ?? false;
    }

    /** The CopilotTelemetry instance (for client.ts tool instrumentation). */
    get telemetry(): CopilotTelemetry {
        return this._telemetry;
    }

    /** Returns the current invoke_agent span context (for parenting tool spans). */
    getSpanContext(): Context | undefined {
        return this._agentSpanContext;
    }

    /**
     * Gets the parent context for a tool call span. Consumes the stored context (one-time use).
     * For subagent tool calls this returns the subagent's invoke_agent context;
     * for main agent tool calls this returns the root invoke_agent context.
     */
    getToolCallParentContext(toolCallId: string): Context | undefined {
        if (this._pendingToolParents) {
            const ctx = this._pendingToolParents.get(toolCallId);
            if (ctx) {
                this._pendingToolParents.delete(toolCallId);
                return ctx;
            }
        }
        return this._agentSpanContext;
    }

    /**
     * Closes any active spans with an error status. Called when the session is
     * destroyed while a turn may still be in progress, ensuring spans are not orphaned.
     */
    completeOnDispose(): void {
        if (this._agentSpan) {
            const disposeError = new SessionDestroyedError(
                "Session destroyed while agent turn was in progress"
            );
            this._completeChatTurn(disposeError);
            this._completeAgentTurn(disposeError);
        }
    }

    /** Called at start of send(); starts the invoke_agent span if needed. */
    beginSend(prompt: string | undefined): void {
        if (!this._agentSpan) {
            this._agentSpan = this._telemetry.startInvokeAgentSpan(
                this._sessionId,
                this._requestModel,
                this.providerName,
                this.serverAddress,
                this.serverPort,
                this._agentName,
                this._agentDescription
            );
            this._agentSpanContext = trace.setSpan(context.active(), this._agentSpan);
            this._agentStartTime = performance.now();
            this._agentInputMessages = [];
        }

        // Agent-level input = what the caller sent (all user prompts).
        if (this._agentInputMessages && prompt) {
            this._agentInputMessages.push({
                role: "user",
                parts: [{ type: "text", content: prompt }],
            });
        }

        // Stash user prompt for the first chat turn's input messages.
        this._pendingUserPrompt = prompt;
    }

    /** Called from _dispatchEvent; handles telemetry enrichment and turn completion. */
    processEvent(event: SessionEvent): void {
        // Route subagent events by parentToolCallId.
        const parentToolCallId = getParentToolCallId(event);
        if (parentToolCallId) {
            const subagent = this._activeSubagents?.get(parentToolCallId);
            if (subagent) {
                this._processSubagentEvent(subagent, event);
            }
            return;
        }

        // Handle subagent lifecycle events.
        switch (event.type) {
            case "subagent.started":
                this._beginSubagent(event as Extract<SessionEvent, { type: "subagent.started" }>);
                return;
            case "subagent.completed": {
                const data = (event as Extract<SessionEvent, { type: "subagent.completed" }>).data;
                this._completeSubagent(data.toolCallId, undefined);
                return;
            }
            case "subagent.failed": {
                const data = (event as Extract<SessionEvent, { type: "subagent.failed" }>).data;
                const subagentError = new SessionTurnError(
                    `Subagent '${data.agentName}' failed: ${data.error}`
                );
                this._completeSubagent(data.toolCallId, subagentError);
                return;
            }
        }

        // Record chunk timing for main agent events during a turn.
        this._recordOutputChunkMetric();

        // Per-turn event processing (writes to the chat child span).
        if (this._turnSpan) {
            switch (event.type) {
                case "assistant.message": {
                    const data = (event as Extract<SessionEvent, { type: "assistant.message" }>)
                        .data;
                    const parts: OtelPart[] = [];
                    if (data.reasoningText) {
                        parts.push({ type: "reasoning", content: data.reasoningText });
                    }
                    if (data.content) {
                        parts.push({ type: "text", content: data.content });
                    }
                    if (parts.length > 0) {
                        this._outputMessages?.push({ role: "assistant", parts });
                    }
                    break;
                }

                case "assistant.usage": {
                    const data = (event as Extract<SessionEvent, { type: "assistant.usage" }>).data;
                    this._responseModel = data.model;
                    if (data.apiCallId) {
                        this._responseId = data.apiCallId;
                    } else if (data.providerCallId) {
                        this._responseId = data.providerCallId;
                    }
                    if (data.inputTokens != null) {
                        this._inputTokens += data.inputTokens;
                    }
                    if (data.outputTokens != null) {
                        this._outputTokens += data.outputTokens;
                    }
                    if (data.cacheReadTokens != null) {
                        this._cacheReadTokens += data.cacheReadTokens;
                    }
                    if (data.cacheWriteTokens != null) {
                        this._cacheCreationTokens += data.cacheWriteTokens;
                    }

                    // Copilot-specific vendor attributes
                    if (data.cost != null) {
                        this._turnCost = (this._turnCost ?? 0) + data.cost;
                    }
                    if (data.duration != null) {
                        this._turnServerDuration = (this._turnServerDuration ?? 0) + data.duration;
                    }
                    if (data.initiator) {
                        this._turnInitiator = data.initiator;
                    }
                    if (data.copilotUsage) {
                        this._turnAiu = (this._turnAiu ?? 0) + data.copilotUsage.totalNanoAiu;
                    }
                    break;
                }

                case "session.model_change": {
                    const data = (event as Extract<SessionEvent, { type: "session.model_change" }>)
                        .data;
                    this._responseModel = data.newModel;
                    break;
                }

                case "tool.execution_start": {
                    const data = (event as Extract<SessionEvent, { type: "tool.execution_start" }>)
                        .data;
                    const isServerTool = !!data.mcpServerName;
                    if (isServerTool && data.toolCallId) {
                        this._serverToolCallIds ??= new Map();
                        this._serverToolCallIds.set(data.toolCallId, data.mcpServerName!);
                    }

                    this._outputMessages?.push({
                        role: "assistant",
                        parts: [
                            {
                                type: isServerTool ? "server_tool_call" : "tool_call",
                                id: data.toolCallId,
                                name: data.toolName,
                                arguments: data.arguments,
                                mcpServerName: data.mcpServerName,
                            },
                        ],
                    });

                    // Store parent context for tool call span parenting.
                    if (this._agentSpan && data.toolCallId) {
                        this._pendingToolParents ??= new Map();
                        this._pendingToolParents.set(data.toolCallId, this._agentSpanContext!);
                    }
                    break;
                }

                case "tool.execution_complete": {
                    const data = (
                        event as Extract<SessionEvent, { type: "tool.execution_complete" }>
                    ).data;
                    const resultContent = data.result?.content ?? data.error?.message;
                    let serverName: string | undefined;
                    const isServerTool = this._serverToolCallIds?.has(data.toolCallId) ?? false;
                    if (isServerTool) {
                        serverName = this._serverToolCallIds!.get(data.toolCallId);
                        this._serverToolCallIds!.delete(data.toolCallId);
                    }

                    this._inputMessages?.push({
                        role: "tool",
                        parts: [
                            {
                                type: isServerTool
                                    ? "server_tool_call_response"
                                    : "tool_call_response",
                                id: data.toolCallId,
                                response: resultContent,
                                mcpServerName: serverName,
                            },
                        ],
                    });
                    break;
                }
            }
        }

        // Copilot-specific lifecycle events emitted as span events on the
        // current activity (chat turn if active, otherwise invoke_agent).
        {
            const target = this._turnSpan ?? this._agentSpan;
            if (target) {
                switch (event.type) {
                    case "session.truncation": {
                        const data = (
                            event as Extract<SessionEvent, { type: "session.truncation" }>
                        ).data;
                        const truncAttrs: Record<string, string | number> = {};
                        if (data.tokenLimit != 0) {
                            truncAttrs[COPILOT_EVENT.TOKEN_LIMIT] = data.tokenLimit;
                        }
                        if (data.preTruncationTokensInMessages != 0) {
                            truncAttrs[COPILOT_EVENT.PRE_TOKENS] =
                                data.preTruncationTokensInMessages;
                        }
                        if (data.postTruncationTokensInMessages != 0) {
                            truncAttrs[COPILOT_EVENT.POST_TOKENS] =
                                data.postTruncationTokensInMessages;
                        }
                        if (data.preTruncationMessagesLength != 0) {
                            truncAttrs[COPILOT_EVENT.PRE_MESSAGES] =
                                data.preTruncationMessagesLength;
                        }
                        if (data.postTruncationMessagesLength != 0) {
                            truncAttrs[COPILOT_EVENT.POST_MESSAGES] =
                                data.postTruncationMessagesLength;
                        }
                        if (data.tokensRemovedDuringTruncation != 0) {
                            truncAttrs[COPILOT_EVENT.TOKENS_REMOVED] =
                                data.tokensRemovedDuringTruncation;
                        }
                        if (data.messagesRemovedDuringTruncation != 0) {
                            truncAttrs[COPILOT_EVENT.MESSAGES_REMOVED] =
                                data.messagesRemovedDuringTruncation;
                        }
                        if (data.performedBy != null) {
                            truncAttrs[COPILOT_EVENT.PERFORMED_BY] = data.performedBy;
                        }
                        target.addEvent(COPILOT_EVENT.SESSION_TRUNCATION, truncAttrs);
                        break;
                    }

                    case "session.compaction_start": {
                        target.addEvent(COPILOT_EVENT.SESSION_COMPACTION_START);
                        break;
                    }

                    case "session.compaction_complete": {
                        const data = (
                            event as Extract<SessionEvent, { type: "session.compaction_complete" }>
                        ).data;
                        const attrs: Record<string, string | number | boolean> = {
                            [COPILOT_EVENT.SUCCESS]: data.success,
                        };
                        if (this._telemetry.enableSensitiveData && data.error != null) {
                            attrs[COPILOT_EVENT.MESSAGE] = data.error;
                        }
                        if (data.preCompactionTokens != null) {
                            attrs[COPILOT_EVENT.PRE_TOKENS] = data.preCompactionTokens;
                        }
                        if (data.postCompactionTokens != null) {
                            attrs[COPILOT_EVENT.POST_TOKENS] = data.postCompactionTokens;
                        }
                        if (data.tokensRemoved != null) {
                            attrs[COPILOT_EVENT.TOKENS_REMOVED] = data.tokensRemoved;
                        }
                        if (data.messagesRemoved != null) {
                            attrs[COPILOT_EVENT.MESSAGES_REMOVED] = data.messagesRemoved;
                        }
                        target.addEvent(COPILOT_EVENT.SESSION_COMPACTION_COMPLETE, attrs);
                        break;
                    }

                    case "skill.invoked": {
                        const data = (event as Extract<SessionEvent, { type: "skill.invoked" }>)
                            .data;
                        const attrs: Record<string, string> = {
                            [COPILOT_EVENT.SKILL_NAME]: data.name,
                            [COPILOT_EVENT.SKILL_PATH]: data.path,
                        };
                        if (data.pluginName != null) {
                            attrs[COPILOT_EVENT.SKILL_PLUGIN_NAME] = data.pluginName;
                        }
                        if (data.pluginVersion != null) {
                            attrs[COPILOT_EVENT.SKILL_PLUGIN_VERSION] = data.pluginVersion;
                        }
                        if (this._telemetry.enableSensitiveData) {
                            attrs[COPILOT_EVENT.SKILL_CONTENT] = data.content;
                        }
                        target.addEvent(COPILOT_EVENT.SKILL_INVOKED, attrs);
                        break;
                    }
                }
            }
        }

        // Lifecycle events that start/end turns or the overall agent operation.
        switch (event.type) {
            case "assistant.turn_start": {
                this._beginChatTurn();
                const data = (event as Extract<SessionEvent, { type: "assistant.turn_start" }>)
                    .data;
                this._turnId = data.turnId;
                this._turnInteractionId = data.interactionId;
                break;
            }

            case "assistant.turn_end":
                this._completeChatTurn(undefined);
                break;

            case "session.idle":
                this._completeChatTurn(undefined);
                this._completeAgentTurn(undefined);
                break;

            case "session.error": {
                const data = (event as Extract<SessionEvent, { type: "session.error" }>).data;
                const ex = new SessionTurnError(
                    `Session error: ${data.message ?? "unknown error"}`
                );
                this._completeChatTurn(ex);
                this._completeAgentTurn(ex);
                break;
            }
        }
    }

    /** Called from send() error path; completes turn with error. */
    completeTurnWithError(error: Error): void {
        this._completeChatTurn(error);
        this._completeAgentTurn(error);
    }

    // ========================================================================
    // Chat turn lifecycle
    // ========================================================================

    /** Starts a new chat child span for an LLM turn. */
    private _beginChatTurn(): void {
        // If there's already an active turn, complete it first.
        this._completeChatTurn(undefined);

        this._responseModel = undefined;
        this._responseId = undefined;
        this._inputTokens = 0;
        this._outputTokens = 0;
        this._cacheReadTokens = 0;
        this._cacheCreationTokens = 0;
        this._firstOutputChunkRecorded = false;
        this._lastOutputChunkTime = 0;
        this._inputMessages = [];
        this._outputMessages = [];
        this._turnCost = undefined;
        this._turnServerDuration = undefined;
        this._turnInitiator = undefined;
        this._turnAiu = undefined;
        this._turnId = undefined;
        this._turnInteractionId = undefined;

        // Add stashed user prompt as input message for the first turn.
        if (this._pendingUserPrompt) {
            this._inputMessages.push({
                role: "user",
                parts: [{ type: "text", content: this._pendingUserPrompt }],
            });
            this._pendingUserPrompt = undefined;
        }

        const parentContext = this._agentSpanContext ?? context.active();
        this._turnSpan = this._telemetry.startChatSpan(
            this._requestModel,
            this.providerName,
            this.serverAddress,
            this.serverPort,
            parentContext,
            this._sessionId
        );
        this._turnStartTime = performance.now();
    }

    /** Completes the current chat child span with per-turn attributes and metrics. */
    private _completeChatTurn(error: Error | undefined): void {
        const span = this._turnSpan;
        if (!span) {
            return;
        }

        const startTime = this._turnStartTime;
        const inputMessages = this._inputMessages;
        const outputMessages = this._outputMessages;
        const responseModel = this._responseModel;
        const responseId = this._responseId;
        const inputTokens = this._inputTokens;
        const outputTokens = this._outputTokens;
        const cacheReadTokens = this._cacheReadTokens;
        const cacheCreationTokens = this._cacheCreationTokens;
        const turnCost = this._turnCost;
        const turnServerDuration = this._turnServerDuration;
        const turnInitiator = this._turnInitiator;
        const turnAiu = this._turnAiu;
        const turnId = this._turnId;
        const turnInteractionId = this._turnInteractionId;

        // Reset per-turn state
        this._turnSpan = undefined;
        this._turnStartTime = undefined;
        this._inputMessages = undefined;
        this._outputMessages = undefined;
        this._responseModel = undefined;
        this._responseId = undefined;
        this._inputTokens = 0;
        this._outputTokens = 0;
        this._cacheReadTokens = 0;
        this._cacheCreationTokens = 0;
        this._turnCost = undefined;
        this._turnServerDuration = undefined;
        this._turnInitiator = undefined;
        this._turnAiu = undefined;
        this._turnId = undefined;
        this._turnInteractionId = undefined;

        if (error) {
            CopilotTelemetry.recordError(span, error);
        }

        const finishReason = error ? "error" : "stop";
        span.setAttribute(ATTR.GEN_AI_RESPONSE_FINISH_REASONS, [finishReason]);

        // Agent-level output = only the final turn's output.
        if (outputMessages && outputMessages.length > 0) {
            this._agentOutputMessages = outputMessages.map((m) => ({
                ...m,
                finishReason,
            }));
        }

        // Accumulate agent-level usage across turns.
        if (responseModel) this._agentResponseModel = responseModel;
        if (responseId) this._agentResponseId = responseId;
        this._agentTotalInputTokens += inputTokens;
        this._agentTotalOutputTokens += outputTokens;
        this._agentTotalCacheReadTokens += cacheReadTokens;
        this._agentTotalCacheCreationTokens += cacheCreationTokens;
        if (turnCost != null) this._agentTotalCost += turnCost;
        if (turnAiu != null) this._agentTotalAiu += turnAiu;

        // Set usage-related span attributes for this LLM turn
        if (responseModel) {
            span.setAttribute(ATTR.GEN_AI_RESPONSE_MODEL, responseModel);
        }
        if (responseId) {
            span.setAttribute(ATTR.GEN_AI_RESPONSE_ID, responseId);
        }
        if (inputTokens > 0) {
            span.setAttribute(ATTR.GEN_AI_USAGE_INPUT_TOKENS, inputTokens);
        }
        if (outputTokens > 0) {
            span.setAttribute(ATTR.GEN_AI_USAGE_OUTPUT_TOKENS, outputTokens);
        }
        if (cacheReadTokens > 0) {
            span.setAttribute(ATTR.GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS, cacheReadTokens);
        }
        if (cacheCreationTokens > 0) {
            span.setAttribute(ATTR.GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS, cacheCreationTokens);
        }

        // Copilot vendor-prefixed attributes on chat spans
        if (turnCost != null) {
            span.setAttribute(ATTR.COPILOT_COST, turnCost);
        }
        if (turnServerDuration != null) {
            span.setAttribute(ATTR.COPILOT_SERVER_DURATION, turnServerDuration);
        }
        if (turnInitiator != null) {
            span.setAttribute(ATTR.COPILOT_INITIATOR, turnInitiator);
        }
        if (turnAiu != null) {
            span.setAttribute(ATTR.COPILOT_AIU, turnAiu);
        }
        if (turnId != null) {
            span.setAttribute(ATTR.COPILOT_TURN_ID, turnId);
        }
        if (turnInteractionId != null) {
            span.setAttribute(ATTR.COPILOT_INTERACTION_ID, turnInteractionId);
        }

        // Set input/output message content as span attributes (sensitive)
        if (this._telemetry.enableSensitiveData) {
            const inputMessagesJson = buildMessagesJson(inputMessages);
            if (inputMessagesJson) {
                span.setAttribute(ATTR.GEN_AI_INPUT_MESSAGES, inputMessagesJson);
            }
            const outputMessagesJson = buildMessagesJson(outputMessages, finishReason);
            if (outputMessagesJson) {
                span.setAttribute(ATTR.GEN_AI_OUTPUT_MESSAGES, outputMessagesJson);
            }
        }

        // Token usage metrics (per-turn)
        this._telemetry.recordTokenUsageMetrics(
            inputTokens > 0 ? inputTokens : undefined,
            outputTokens > 0 ? outputTokens : undefined,
            this._requestModel,
            responseModel,
            this.providerName,
            this.serverAddress,
            this.serverPort,
            error,
            CHAT
        );

        // Per-turn operation duration
        if (startTime != null) {
            const durationSeconds = (performance.now() - startTime) / 1000;
            this._telemetry.recordOperationDuration(
                durationSeconds,
                this._requestModel,
                responseModel,
                this.providerName,
                this.serverAddress,
                this.serverPort,
                error,
                CHAT
            );
        }

        this._firstOutputChunkRecorded = false;
        this._lastOutputChunkTime = 0;
        span.end();
    }

    // ========================================================================
    // Agent turn lifecycle
    // ========================================================================

    /** Completes the invoke_agent span and records overall operation duration. */
    private _completeAgentTurn(error: Error | undefined): void {
        const span = this._agentSpan;
        if (!span) {
            return;
        }

        const startTime = this._agentStartTime;
        const agentInputMessages = this._agentInputMessages;
        const agentOutputMessages = this._agentOutputMessages;

        // Reset agent state
        this._agentSpan = undefined;
        this._agentSpanContext = undefined;
        this._agentStartTime = undefined;
        this._pendingUserPrompt = undefined;
        this._agentInputMessages = undefined;
        this._agentOutputMessages = undefined;

        // Complete any remaining subagents before closing the parent.
        if (this._activeSubagents && this._activeSubagents.size > 0) {
            for (const key of [...this._activeSubagents.keys()]) {
                this._completeSubagent(key, error);
            }
        }
        this._activeSubagents = undefined;
        this._pendingToolParents = undefined;
        this._serverToolCallIds = undefined;

        if (error) {
            CopilotTelemetry.recordError(span, error);
        }

        const finishReason = error ? "error" : "stop";
        span.setAttribute(ATTR.GEN_AI_RESPONSE_FINISH_REASONS, [finishReason]);

        // Set accumulated usage across all chat turns on the invoke_agent span.
        const agentResponseModel = this._agentResponseModel;
        const agentResponseId = this._agentResponseId;
        const agentTotalInputTokens = this._agentTotalInputTokens;
        const agentTotalOutputTokens = this._agentTotalOutputTokens;
        const agentTotalCacheReadTokens = this._agentTotalCacheReadTokens;
        const agentTotalCacheCreationTokens = this._agentTotalCacheCreationTokens;
        const agentTotalCost = this._agentTotalCost;
        const agentTotalAiu = this._agentTotalAiu;

        this._agentResponseModel = undefined;
        this._agentResponseId = undefined;
        this._agentTotalInputTokens = 0;
        this._agentTotalOutputTokens = 0;
        this._agentTotalCacheReadTokens = 0;
        this._agentTotalCacheCreationTokens = 0;
        this._agentTotalCost = 0;
        this._agentTotalAiu = 0;

        if (agentResponseModel) {
            span.setAttribute(ATTR.GEN_AI_RESPONSE_MODEL, agentResponseModel);
        }
        if (agentResponseId) {
            span.setAttribute(ATTR.GEN_AI_RESPONSE_ID, agentResponseId);
        }
        if (agentTotalInputTokens > 0) {
            span.setAttribute(ATTR.GEN_AI_USAGE_INPUT_TOKENS, agentTotalInputTokens);
        }
        if (agentTotalOutputTokens > 0) {
            span.setAttribute(ATTR.GEN_AI_USAGE_OUTPUT_TOKENS, agentTotalOutputTokens);
        }
        if (agentTotalCacheReadTokens > 0) {
            span.setAttribute(ATTR.GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS, agentTotalCacheReadTokens);
        }
        if (agentTotalCacheCreationTokens > 0) {
            span.setAttribute(
                ATTR.GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS,
                agentTotalCacheCreationTokens
            );
        }

        // Copilot vendor-prefixed attributes on invoke_agent span
        if (agentTotalCost > 0) {
            span.setAttribute(ATTR.COPILOT_COST, agentTotalCost);
        }
        if (agentTotalAiu > 0) {
            span.setAttribute(ATTR.COPILOT_AIU, agentTotalAiu);
        }

        // Agent-level input = caller's message; output = agent's final response.
        if (this._telemetry.enableSensitiveData) {
            const inputJson = buildMessagesJson(agentInputMessages);
            if (inputJson) {
                span.setAttribute(ATTR.GEN_AI_INPUT_MESSAGES, inputJson);
            }
            const outputJson = buildMessagesJson(agentOutputMessages);
            if (outputJson) {
                span.setAttribute(ATTR.GEN_AI_OUTPUT_MESSAGES, outputJson);
            }
            if (this._systemInstructionsJson) {
                span.setAttribute(ATTR.GEN_AI_SYSTEM_INSTRUCTIONS, this._systemInstructionsJson);
            }
        }
        if (this._toolDefinitionsJson) {
            span.setAttribute(ATTR.GEN_AI_TOOL_DEFINITIONS, this._toolDefinitionsJson);
        }

        if (startTime != null) {
            const durationSeconds = (performance.now() - startTime) / 1000;
            this._telemetry.recordOperationDuration(
                durationSeconds,
                this._requestModel,
                agentResponseModel,
                this.providerName,
                this.serverAddress,
                this.serverPort,
                error,
                INVOKE_AGENT
            );
        }

        span.end();
    }

    // ========================================================================
    // Streaming chunk timing
    // ========================================================================

    /** Records streaming chunk timing metrics. Uses the chat turn stopwatch. */
    private _recordOutputChunkMetric(): void {
        if (!this._isStreaming || this._turnStartTime == null) {
            return;
        }

        const now = performance.now();
        const elapsed = now - this._turnStartTime;

        if (!this._firstOutputChunkRecorded) {
            this._firstOutputChunkRecorded = true;
            this._lastOutputChunkTime = now;
            this._telemetry.recordTimeToFirstChunk(
                elapsed / 1000,
                this._requestModel,
                undefined, // response model not yet known during streaming
                this.providerName,
                this.serverAddress,
                this.serverPort
            );
            return;
        }

        const deltaSeconds = (now - this._lastOutputChunkTime) / 1000;
        this._lastOutputChunkTime = now;
        this._telemetry.recordTimePerOutputChunk(
            deltaSeconds,
            this._requestModel,
            undefined, // response model not yet known during streaming
            this.providerName,
            this.serverAddress,
            this.serverPort
        );
    }

    // ========================================================================
    // Subagent support
    // ========================================================================

    /** Creates nested invoke_agent + chat spans for a subagent. */
    private _beginSubagent(event: Extract<SessionEvent, { type: "subagent.started" }>): void {
        const data = event.data;
        if (!data) return;

        const parentContext = this._agentSpanContext ?? context.active();
        const invokeSpan = this._telemetry.startInvokeAgentSpan(
            this._sessionId,
            this._requestModel,
            this.providerName,
            this.serverAddress,
            this.serverPort,
            data.agentName,
            data.agentDescription,
            parentContext
        );

        const invokeCtx = trace.setSpan(context.active(), invokeSpan);
        const chatSpan = this._telemetry.startChatSpan(
            this._requestModel,
            this.providerName,
            this.serverAddress,
            this.serverPort,
            invokeCtx,
            this._sessionId
        );

        const state: SubagentState = {
            invokeAgentSpan: invokeSpan,
            invokeAgentContext: invokeCtx,
            invokeAgentStartTime: performance.now(),
            chatSpan,
            agentName: data.agentName,
            responseModel: undefined,
            responseId: undefined,
            inputTokens: 0,
            outputTokens: 0,
            cacheReadTokens: 0,
            cacheCreationTokens: 0,
            inputMessages: [],
            outputMessages: [],
        };

        this._activeSubagents ??= new Map();
        this._activeSubagents.set(data.toolCallId, state);
    }

    /** Routes an event to its owning subagent's spans. */
    private _processSubagentEvent(subagent: SubagentState, event: SessionEvent): void {
        switch (event.type) {
            case "assistant.usage": {
                const data = (event as Extract<SessionEvent, { type: "assistant.usage" }>).data;
                if (data.model) {
                    subagent.responseModel = data.model;
                    subagent.chatSpan?.setAttribute(ATTR.GEN_AI_RESPONSE_MODEL, data.model);
                }

                if (data.apiCallId) {
                    subagent.responseId = data.apiCallId;
                } else if (data.providerCallId) {
                    subagent.responseId = data.providerCallId;
                }
                if (data.inputTokens != null) subagent.inputTokens += data.inputTokens;
                if (data.outputTokens != null) subagent.outputTokens += data.outputTokens;
                if (data.cacheReadTokens != null) subagent.cacheReadTokens += data.cacheReadTokens;
                if (data.cacheWriteTokens != null)
                    subagent.cacheCreationTokens += data.cacheWriteTokens;
                break;
            }

            case "assistant.message": {
                const data = (event as Extract<SessionEvent, { type: "assistant.message" }>).data;
                const parts: OtelPart[] = [];
                if (data.reasoningText) {
                    parts.push({ type: "reasoning", content: data.reasoningText });
                }
                if (data.content) {
                    parts.push({ type: "text", content: data.content });
                }
                if (parts.length > 0) {
                    subagent.outputMessages.push({ role: "assistant", parts });
                }
                break;
            }

            case "tool.execution_start": {
                const data = (event as Extract<SessionEvent, { type: "tool.execution_start" }>)
                    .data;
                const isServerTool = !!data.mcpServerName;
                if (isServerTool && data.toolCallId) {
                    this._serverToolCallIds ??= new Map();
                    this._serverToolCallIds.set(data.toolCallId, data.mcpServerName!);
                }

                subagent.outputMessages.push({
                    role: "assistant",
                    parts: [
                        {
                            type: isServerTool ? "server_tool_call" : "tool_call",
                            id: data.toolCallId,
                            name: data.toolName,
                            arguments: data.arguments,
                            mcpServerName: data.mcpServerName,
                        },
                    ],
                });

                // For subagent tool calls, parent is the subagent's invoke_agent.
                if (data.toolCallId) {
                    this._pendingToolParents ??= new Map();
                    this._pendingToolParents.set(data.toolCallId, subagent.invokeAgentContext);
                }
                break;
            }

            case "tool.execution_complete": {
                const data = (event as Extract<SessionEvent, { type: "tool.execution_complete" }>)
                    .data;
                const resultContent = data.result?.content ?? data.error?.message;
                let serverName: string | undefined;
                const isServerTool = this._serverToolCallIds?.has(data.toolCallId) ?? false;
                if (isServerTool) {
                    serverName = this._serverToolCallIds!.get(data.toolCallId);
                    this._serverToolCallIds!.delete(data.toolCallId);
                }

                subagent.inputMessages.push({
                    role: "tool",
                    parts: [
                        {
                            type: isServerTool ? "server_tool_call_response" : "tool_call_response",
                            id: data.toolCallId,
                            response: resultContent,
                            mcpServerName: serverName,
                        },
                    ],
                });
                break;
            }
        }
    }

    /** Completes a subagent's chat and invoke_agent spans. */
    private _completeSubagent(toolCallId: string, error: Error | undefined): void {
        if (!this._activeSubagents) return;
        const subagent = this._activeSubagents.get(toolCallId);
        if (!subagent) return;
        this._activeSubagents.delete(toolCallId);

        const finishReason = error ? "error" : "stop";

        // -- Complete the chat child span --
        const chatSpan = subagent.chatSpan;
        if (chatSpan) {
            if (error) CopilotTelemetry.recordError(chatSpan, error);
            chatSpan.setAttribute(ATTR.GEN_AI_RESPONSE_FINISH_REASONS, [finishReason]);

            if (subagent.responseModel) {
                chatSpan.setAttribute(ATTR.GEN_AI_RESPONSE_MODEL, subagent.responseModel);
            }
            if (subagent.responseId) {
                chatSpan.setAttribute(ATTR.GEN_AI_RESPONSE_ID, subagent.responseId);
            }
            if (subagent.inputTokens > 0) {
                chatSpan.setAttribute(ATTR.GEN_AI_USAGE_INPUT_TOKENS, subagent.inputTokens);
            }
            if (subagent.outputTokens > 0) {
                chatSpan.setAttribute(ATTR.GEN_AI_USAGE_OUTPUT_TOKENS, subagent.outputTokens);
            }
            if (subagent.cacheReadTokens > 0) {
                chatSpan.setAttribute(
                    ATTR.GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS,
                    subagent.cacheReadTokens
                );
            }
            if (subagent.cacheCreationTokens > 0) {
                chatSpan.setAttribute(
                    ATTR.GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS,
                    subagent.cacheCreationTokens
                );
            }

            if (this._telemetry.enableSensitiveData) {
                if (subagent.inputMessages.length > 0) {
                    const inputJson = buildMessagesJson(subagent.inputMessages);
                    if (inputJson) {
                        chatSpan.setAttribute(ATTR.GEN_AI_INPUT_MESSAGES, inputJson);
                    }
                }
                if (subagent.outputMessages.length > 0) {
                    const outputJson = buildMessagesJson(subagent.outputMessages, finishReason);
                    if (outputJson) {
                        chatSpan.setAttribute(ATTR.GEN_AI_OUTPUT_MESSAGES, outputJson);
                    }
                }
            }

            this._telemetry.recordTokenUsageMetrics(
                subagent.inputTokens > 0 ? subagent.inputTokens : undefined,
                subagent.outputTokens > 0 ? subagent.outputTokens : undefined,
                subagent.responseModel ?? this._requestModel,
                subagent.responseModel,
                this.providerName,
                this.serverAddress,
                this.serverPort,
                error,
                CHAT
            );

            chatSpan.end();
        }

        // -- Complete the invoke_agent span --
        const invokeSpan = subagent.invokeAgentSpan;
        if (error) CopilotTelemetry.recordError(invokeSpan, error);
        invokeSpan.setAttribute(ATTR.GEN_AI_RESPONSE_FINISH_REASONS, [finishReason]);

        if (subagent.responseModel) {
            invokeSpan.setAttribute(ATTR.GEN_AI_RESPONSE_MODEL, subagent.responseModel);
        }
        if (subagent.responseId) {
            invokeSpan.setAttribute(ATTR.GEN_AI_RESPONSE_ID, subagent.responseId);
        }
        if (subagent.inputTokens > 0) {
            invokeSpan.setAttribute(ATTR.GEN_AI_USAGE_INPUT_TOKENS, subagent.inputTokens);
        }
        if (subagent.outputTokens > 0) {
            invokeSpan.setAttribute(ATTR.GEN_AI_USAGE_OUTPUT_TOKENS, subagent.outputTokens);
        }
        if (subagent.cacheReadTokens > 0) {
            invokeSpan.setAttribute(
                ATTR.GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS,
                subagent.cacheReadTokens
            );
        }
        if (subagent.cacheCreationTokens > 0) {
            invokeSpan.setAttribute(
                ATTR.GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS,
                subagent.cacheCreationTokens
            );
        }

        if (this._telemetry.enableSensitiveData && subagent.outputMessages.length > 0) {
            const outputJson = buildMessagesJson(
                subagent.outputMessages.map((m) => ({ ...m, finishReason }))
            );
            if (outputJson) {
                invokeSpan.setAttribute(ATTR.GEN_AI_OUTPUT_MESSAGES, outputJson);
            }
        }

        const durationSeconds = (performance.now() - subagent.invokeAgentStartTime) / 1000;
        this._telemetry.recordOperationDuration(
            durationSeconds,
            subagent.responseModel ?? this._requestModel,
            subagent.responseModel,
            this.providerName,
            this.serverAddress,
            this.serverPort,
            error,
            INVOKE_AGENT
        );

        invokeSpan.end();
    }
}
