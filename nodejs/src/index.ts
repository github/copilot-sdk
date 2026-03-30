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
export { defineTool, approveAll, SYSTEM_PROMPT_SECTIONS } from "./types.js";
export type {
    CommandContext,
    CommandDefinition,
    CommandHandler,
    ConnectionState,
    CopilotClientOptions,
    CustomAgentConfig,
    ElicitationFieldValue,
    ElicitationHandler,
    ElicitationParams,
    ElicitationRequest,
    ElicitationResult,
    ElicitationSchema,
    ElicitationSchemaField,
    ForegroundSessionInfo,
    GetAuthStatusResponse,
    GetStatusResponse,
    InfiniteSessionConfig,
    InputOptions,
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
    SectionOverride,
    SectionOverrideAction,
    SectionTransformFn,
    SessionCapabilities,
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
    SessionUiApi,
    SystemMessageAppendConfig,
    SystemMessageConfig,
    SystemMessageCustomizeConfig,
    SystemMessageReplaceConfig,
    SystemPromptSection,
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
