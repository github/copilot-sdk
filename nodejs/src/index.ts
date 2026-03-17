/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Copilot SDK - TypeScript/Node.js Client
 *
 * JSON-RPC based SDK for programmatic control of GitHub Copilot CLI
 */

export { CopilotClient } from "./client.js";
export { CopilotSession, type AssistantMessageEvent } from "./session.js";
export { defineTool, approveAll, defineCommand } from "./types.js";
export type {
    Command,
    CommandHandler,
    CommandInvocation,
    ConnectionState,
    CopilotClientOptions,
    CustomAgentConfig,
    ForegroundSessionInfo,
    GetAuthStatusResponse,
    GetStatusResponse,
    HostCapabilities,
    InfiniteSessionConfig,
    MCPLocalServerConfig,
    MCPRemoteServerConfig,
    MCPServerConfig,
    MessageOptions,
    ModelBilling,
    ModelCapabilities,
    ModelInfo,
    ModelPolicy,
    PermissionHandler,
    PermissionRequest,
    PermissionRequestResult,
    ResumeSessionConfig,
    SessionConfig,
    SessionEvent,
    SessionEventHandler,
    SessionEventPayload,
    SessionEventType,
    SessionLifecycleEvent,
    SessionLifecycleEventType,
    SessionLifecycleHandler,
    SessionContext,
    SessionListFilter,
    SessionMetadata,
    SessionUI,
    SelectOption,
    SelectOptions,
    ConfirmOptions,
    InputOptions,
    SystemMessageAppendConfig,
    SystemMessageConfig,
    SystemMessageReplaceConfig,
    TelemetryConfig,
    TraceContext,
    TraceContextProvider,
    Tool,
    ToolHandler,
    ToolInvocation,
    ToolResultObject,
    TypedSessionEventHandler,
    TypedSessionLifecycleHandler,
    ZodSchema,
} from "./types.js";
