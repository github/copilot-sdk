/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Copilot SDK - TypeScript/Node.js Client
 *
 * JSON-RPC based SDK for programmatic control of GitHub Copilot CLI
 */

export { CopilotClient } from "./client.js";
export { RuntimeConnection } from "./types.js";
export { BuiltInTools, ToolSet } from "./toolSet.js";
export { CopilotSession, type AssistantMessageEvent } from "./session.js";
export {
    Canvas,
    CanvasError,
    createCanvas,
    type CanvasAction,
    type CanvasDeclaration,
    type CanvasHostContext,
    type CanvasHostContextCapabilities,
    type CanvasJsonSchema,
    type CanvasOptions,
} from "./canvas.js";
export {
    defineTool,
    approveAll,
    convertMcpCallToolResult,
    createSessionFsAdapter,
    SYSTEM_MESSAGE_SECTIONS,
} from "./types.js";
// Re-export the generated session-event types (every *Event interface and
// its corresponding *Data payload type, plus supporting unions/aliases) so
// consumers can import them directly from "@github/copilot-sdk" instead of
// reaching into the package's internal dist layout. See issue #1156.
//
// Three names from this file are also explicitly exported elsewhere in this
// module — `SessionEvent` (re-exported below from `./types.js`),
// `PermissionRequest` (re-exported below from `./types.js`), and
// `AssistantMessageEvent` (re-exported above from `./session.js`). Per the
// ECMAScript module spec, the explicit named re-exports shadow the names
// arriving via `export type *`, so the hand-authored public API surface for
// those three identifiers is preserved unchanged.
export type * from "./generated/session-events.js";
export type {
    CommandContext,
    CommandDefinition,
    CommandHandler,
    CloudSessionOptions,
    CloudSessionRepository,
    AutoModeSwitchHandler,
    AutoModeSwitchRequest,
    AutoModeSwitchResponse,
    CopilotClientMode,
    CopilotClientOptions,
    StdioRuntimeConnection,
    TcpRuntimeConnection,
    UriRuntimeConnection,
    CustomAgentConfig,
    ElicitationFieldValue,
    ElicitationHandler,
    ElicitationParams,
    ElicitationContext,
    ElicitationResult,
    ElicitationSchema,
    ElicitationSchemaField,
    ExitPlanModeHandler,
    ExitPlanModeRequest,
    ExitPlanModeResult,
    ExtensionInfo,
    ForegroundSessionInfo,
    GetAuthStatusResponse,
    GetStatusResponse,
    InfiniteSessionConfig,
    LargeToolOutputConfig,
    MemoryConfiguration,
    UiInputOptions,
    MCPStdioServerConfig,
    MCPHTTPServerConfig,
    MCPServerConfig,
    DefaultAgentConfig,
    MessageOptions,
    ModelBilling,
    ModelBillingTokenPrices,
    ModelBillingTokenPricesLongContext,
    CapiSessionOptions,
    ModelCapabilities,
    ModelCapabilitiesOverride,
    ModelInfo,
    ModelPolicy,
    NamedProviderConfig,
    PermissionHandler,
    PermissionRequest,
    PermissionRequestResult,
    ProviderConfig,
    ProviderModelConfig,
    RemoteSessionMode,
    ResumeSessionConfig,
    SectionOverride,
    SectionOverrideAction,
    SectionTransformFn,
    SessionCapabilities,
    SessionConfig,
    SessionConfigBase,
    SessionEvent,
    SessionEventHandler,
    SessionEventPayload,
    SessionEventType,
    SessionLifecycleEvent,
    SessionLifecycleEventMetadata,
    SessionLifecycleEventType,
    SessionLifecycleHandler,
    SessionCreatedEvent,
    SessionDeletedEvent,
    SessionUpdatedEvent,
    SessionForegroundEvent,
    SessionBackgroundEvent,
    SessionContext,
    SessionListFilter,
    SessionMetadata,
    SessionUiApi,
    SessionFsConfig,
    SessionFsProvider,
    SessionFsFileInfo,
    SessionFsSqliteQueryResult,
    SessionFsSqliteQueryType,
    SessionFsSqliteProvider,
    SystemMessageAppendConfig,
    SystemMessageConfig,
    SystemMessageCustomizeConfig,
    SystemMessageReplaceConfig,
    SystemMessageSection,
    TelemetryConfig,
    TraceContext,
    TraceContextProvider,
    Tool,
    ToolHandler,
    ToolInvocation,
    ToolTelemetry,
    ToolResultObject,
    TypedSessionEventHandler,
    TypedSessionLifecycleHandler,
    ZodSchema,
} from "./types.js";
