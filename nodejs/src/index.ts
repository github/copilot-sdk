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
export { defineTool } from "./types.js";
export type {
    ConnectionState,
    CopilotClientOptions,
    CustomAgentConfig,
    MCPLocalServerConfig,
    MCPRemoteServerConfig,
    MCPServerConfig,
    MessageOptions,
    PermissionHandler,
    PermissionRequest,
    PermissionRequestResult,
    ResumeSessionConfig,
    SessionConfig,
    SessionEvent,
    SessionEventHandler,
    SessionMetadata,
    SystemMessageAppendConfig,
    SystemMessageConfig,
    SystemMessageReplaceConfig,
    Tool,
    ToolHandler,
    ToolInvocation,
    ToolResultObject,
    ZodSchema,
} from "./types.js";
