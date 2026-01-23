/**
 * Type definitions for the Copilot SDK Playground
 *
 * These types mirror the SDK's event and session types for the frontend.
 */

/** Session information stored on the frontend */
export interface Session {
    id: string;
    model: string;
}

/** Provider configuration for custom API endpoints */
export interface ProviderConfig {
    type: "openai" | "azure" | "anthropic";
    baseUrl: string;
    apiKey?: string;
    bearerToken?: string;
    wireApi?: "completions" | "responses";
    azure?: {
        apiVersion?: string;
    };
}

/** Options for creating a new session */
export interface CreateSessionOptions {
    model: string;
    systemMessage?: {
        mode: "append" | "replace";
        content: string;
    };
    provider?: ProviderConfig;
}

/** A message in the conversation */
export interface Message {
    type: "user" | "assistant" | "tool" | "error" | "usage" | "compaction";
    content: string;
    // Tool-specific fields
    toolName?: string;
    toolCallId?: string;
    arguments?: unknown;
    completed?: boolean;
    success?: boolean;
    result?: unknown;
    error?: string;
    // Error-specific fields
    details?: string;
    // Usage-specific fields
    usage?: UsageData;
    // Compaction-specific fields
    compaction?: CompactionData;
}

/** Usage data from assistant.usage events */
export interface UsageData {
    model?: string;
    inputTokens?: number;
    outputTokens?: number;
    cacheReadTokens?: number;
    cacheWriteTokens?: number;
    cost?: number;
    duration?: number;
    // Context window info from session.usage_info
    tokenLimit?: number;
    currentTokens?: number;
    messagesLength?: number;
}

/** Compaction data from session.compaction_* events */
export interface CompactionData {
    status: "started" | "complete";
    success?: boolean;
    error?: string;
    preCompactionTokens?: number;
    postCompactionTokens?: number;
    preCompactionMessagesLength?: number;
    messagesRemoved?: number;
    tokensRemoved?: number;
    summaryContent?: string;
    compactionTokensUsed?: {
        input: number;
        output: number;
        cachedInput: number;
    };
}

/** A log entry for debugging */
export interface LogEntry {
    timestamp: string;
    level: "info" | "error" | "debug";
    message: string;
}

/** SDK event from the server */
export interface SDKEvent {
    type: string;
    data: Record<string, unknown>;
}

/** Server status response */
export interface StatusResponse {
    connected: boolean;
    sessions: Array<{ id: string; model: string }>;
}

/** Session creation response */
export interface CreateSessionResponse {
    sessionId: string;
}

/** Messages response */
export interface MessagesResponse {
    sessionId: string;
    messages: SDKEvent[];
}

/** Events response */
export interface EventsResponse {
    sessionId: string;
    events: SDKEvent[];
}
