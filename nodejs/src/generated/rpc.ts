/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: api.schema.json
 */

import type { MessageConnection } from "vscode-jsonrpc/node.js";

/**
 * Authentication type
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AuthInfoType".
 */
export type AuthInfoType = "hmac" | "env" | "user" | "gh-cli" | "api-key" | "token" | "copilot-api-token";
/**
 * Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandKind".
 */
export type SlashCommandKind = "builtin" | "skill" | "client";
/**
 * Optional completion hint for the input (e.g. 'directory' for filesystem path completion)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandInputCompletion".
 */
export type SlashCommandInputCompletion = "directory";
/**
 * Result of the queued command execution
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuedCommandResult".
 */
export type QueuedCommandResult = QueuedCommandHandled | QueuedCommandNotHandled;
/**
 * Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredMcpServerType".
 */
export type DiscoveredMcpServerType = "stdio" | "http" | "sse" | "memory";
/**
 * Configuration source
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredMcpServerSource".
 */
export type DiscoveredMcpServerSource = "user" | "workspace" | "plugin" | "builtin";
/**
 * Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionSource".
 */
export type ExtensionSource = "project" | "user";
/**
 * Current status: running, disabled, failed, or starting
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionStatus".
 */
export type ExtensionStatus = "running" | "disabled" | "failed" | "starting";
/**
 * Tool call result (string or expanded result object)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolResult".
 */
export type ExternalToolResult = string | ExternalToolTextResultForLlm;
/**
 * A content block within a tool result, which may be text, terminal output, image, audio, or a resource
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContent".
 */
export type ExternalToolTextResultForLlmContent =
  | ExternalToolTextResultForLlmContentText
  | ExternalToolTextResultForLlmContentTerminal
  | ExternalToolTextResultForLlmContentImage
  | ExternalToolTextResultForLlmContentAudio
  | ExternalToolTextResultForLlmContentResourceLink
  | ExternalToolTextResultForLlmContentResource;
/**
 * Theme variant this icon is intended for
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentResourceLinkIconTheme".
 */
export type ExternalToolTextResultForLlmContentResourceLinkIconTheme = "light" | "dark";
/**
 * The embedded resource contents, either text or base64-encoded binary
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentResourceDetails".
 */
export type ExternalToolTextResultForLlmContentResourceDetails =
  | EmbeddedTextResourceContents
  | EmbeddedBlobResourceContents;
/**
 * Content filtering mode to apply to all tools, or a map of tool name to content filtering mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FilterMapping".
 */
export type FilterMapping =
  | {
      [k: string]: FilterMappingValue;
    }
  | FilterMappingString;
/**
 * Allowed values for the `FilterMappingValue` enumeration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FilterMappingValue".
 */
export type FilterMappingValue = "none" | "markdown" | "hidden_characters";
/**
 * Allowed values for the `FilterMappingString` enumeration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FilterMappingString".
 */
export type FilterMappingString = "none" | "markdown" | "hidden_characters";
/**
 * Category of instruction source — used for merge logic
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionsSourcesType".
 */
export type InstructionsSourcesType = "home" | "repo" | "model" | "vscode" | "nested-agents" | "child-instructions";
/**
 * Where this source lives — used for UI grouping
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionsSourcesLocation".
 */
export type InstructionsSourcesLocation = "user" | "repository" | "working-directory";
/**
 * Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLogLevel".
 */
export type SessionLogLevel = "info" | "warning" | "error";
/**
 * MCP server configuration (local/stdio or remote/http)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfig".
 */
export type McpServerConfig = McpServerConfigLocal | McpServerConfigHttp;
/**
 * Local transport type. Defaults to "local".
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigLocalType".
 */
export type McpServerConfigLocalType = "local" | "stdio";
/**
 * Remote transport type. Defaults to "http" when omitted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigHttpType".
 */
export type McpServerConfigHttpType = "http" | "sse";
/**
 * OAuth grant type to use when authenticating to the remote MCP server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigHttpOauthGrantType".
 */
export type McpServerConfigHttpOauthGrantType = "authorization_code" | "client_credentials";
/**
 * Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerStatus".
 */
export type McpServerStatus = "connected" | "failed" | "needs-auth" | "pending" | "disabled" | "not_configured";
/**
 * Configuration source: user, workspace, plugin, or builtin
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerSource".
 */
export type McpServerSource = "user" | "workspace" | "plugin" | "builtin";
/**
 * Model capability category for grouping in the model picker
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelPickerCategory".
 */
export type ModelPickerCategory = "lightweight" | "versatile" | "powerful";
/**
 * Relative cost tier for token-based billing users
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelPickerPriceCategory".
 */
export type ModelPickerPriceCategory = "low" | "medium" | "high" | "very_high";
/**
 * Reasoning summary mode to request for supported model clients
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ReasoningSummary".
 */
export type ReasoningSummary = "none" | "concise" | "detailed";
/**
 * The agent mode. Valid values: "interactive", "plan", "autopilot".
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionMode".
 */
export type SessionMode = "interactive" | "plan" | "autopilot";
/**
 * Decision to apply to a pending permission request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecision".
 */
export type PermissionDecision =
  | PermissionDecisionApproveOnce
  | PermissionDecisionApproveForSession
  | PermissionDecisionApproveForLocation
  | PermissionDecisionApprovePermanently
  | PermissionDecisionReject
  | PermissionDecisionUserNotAvailable;
/**
 * The approval to add as a session-scoped rule
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApproval".
 */
export type PermissionDecisionApproveForSessionApproval =
  | PermissionDecisionApproveForSessionApprovalCommands
  | PermissionDecisionApproveForSessionApprovalRead
  | PermissionDecisionApproveForSessionApprovalWrite
  | PermissionDecisionApproveForSessionApprovalMcp
  | PermissionDecisionApproveForSessionApprovalMcpSampling
  | PermissionDecisionApproveForSessionApprovalMemory
  | PermissionDecisionApproveForSessionApprovalCustomTool
  | PermissionDecisionApproveForSessionApprovalExtensionManagement
  | PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess;
/**
 * The approval to persist for this location
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApproval".
 */
export type PermissionDecisionApproveForLocationApproval =
  | PermissionDecisionApproveForLocationApprovalCommands
  | PermissionDecisionApproveForLocationApprovalRead
  | PermissionDecisionApproveForLocationApprovalWrite
  | PermissionDecisionApproveForLocationApprovalMcp
  | PermissionDecisionApproveForLocationApprovalMcpSampling
  | PermissionDecisionApproveForLocationApprovalMemory
  | PermissionDecisionApproveForLocationApprovalCustomTool
  | PermissionDecisionApproveForLocationApprovalExtensionManagement
  | PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess;
/**
 * Per-session remote mode. "off" disables remote, "export" exports session events to GitHub without enabling remote steering, "on" enables both export and remote steering.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteSessionMode".
 */
export type RemoteSessionMode = "off" | "export" | "on";
/**
 * Error classification
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsErrorCode".
 */
export type SessionFsErrorCode = "ENOENT" | "UNKNOWN";
/**
 * Entry type
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirWithTypesEntryType".
 */
export type SessionFsReaddirWithTypesEntryType = "file" | "directory";
/**
 * Path conventions used by this filesystem
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSetProviderConventions".
 */
export type SessionFsSetProviderConventions = "windows" | "posix";
/**
 * How to execute the query: 'exec' for DDL/multi-statement (no results), 'query' for SELECT (returns rows), 'run' for INSERT/UPDATE/DELETE (returns rowsAffected)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteQueryType".
 */
export type SessionFsSqliteQueryType = "exec" | "query" | "run";
/**
 * Signal to send (default: SIGTERM)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellKillSignal".
 */
export type ShellKillSignal = "SIGTERM" | "SIGKILL" | "SIGINT";
/**
 * Optional target session mode
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandAgentPromptMode".
 */
export type SlashCommandAgentPromptMode = "interactive" | "plan" | "autopilot";
/**
 * Result of invoking the slash command (text output, prompt to send to the agent, or completion).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandInvocationResult".
 */
export type SlashCommandInvocationResult =
  | SlashCommandTextResult
  | SlashCommandAgentPromptResult
  | SlashCommandCompletedResult;
/**
 * Current lifecycle status of the task
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskAgentInfoStatus".
 */
export type TaskAgentInfoStatus = "running" | "idle" | "completed" | "failed" | "cancelled";
/**
 * How the agent is currently being managed by the runtime
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskAgentInfoExecutionMode".
 */
export type TaskAgentInfoExecutionMode = "sync" | "background";
/**
 * Schema for the `TaskInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskInfo".
 */
export type TaskInfo = TaskAgentInfo | TaskShellInfo;
/**
 * Current lifecycle status of the task
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskShellInfoStatus".
 */
export type TaskShellInfoStatus = "running" | "idle" | "completed" | "failed" | "cancelled";
/**
 * Whether the shell runs inside a managed PTY session or as an independent background process
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskShellInfoAttachmentMode".
 */
export type TaskShellInfoAttachmentMode = "attached" | "detached";
/**
 * Whether the shell command is currently sync-waited or background-managed
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskShellInfoExecutionMode".
 */
export type TaskShellInfoExecutionMode = "sync" | "background";
/**
 * Schema for the `UIElicitationFieldValue` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationFieldValue".
 */
export type UIElicitationFieldValue = string | number | boolean | string[];
/**
 * Definition for a single elicitation form field.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaProperty".
 */
export type UIElicitationSchemaProperty =
  | UIElicitationStringEnumField
  | UIElicitationStringOneOfField
  | UIElicitationArrayEnumField
  | UIElicitationArrayAnyOfField
  | UIElicitationSchemaPropertyBoolean
  | UIElicitationSchemaPropertyString
  | UIElicitationSchemaPropertyNumber;
/**
 * Optional format hint that constrains the accepted input.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaPropertyStringFormat".
 */
export type UIElicitationSchemaPropertyStringFormat = "email" | "uri" | "date" | "date-time";
/**
 * Numeric type accepted by the field.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaPropertyNumberType".
 */
export type UIElicitationSchemaPropertyNumberType = "number" | "integer";
/**
 * The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationResponseAction".
 */
export type UIElicitationResponseAction = "accept" | "decline" | "cancel";

export interface AccountGetQuotaRequest {
  /**
   * GitHub token for per-user quota lookup. When provided, resolves this token to determine the user's quota instead of using the global auth.
   */
  gitHubToken?: string;
}
/**
 * Quota usage snapshots for the resolved user, keyed by quota type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AccountGetQuotaResult".
 */
export interface AccountGetQuotaResult {
  /**
   * Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)
   */
  quotaSnapshots: {
    [k: string]: AccountQuotaSnapshot;
  };
}
/**
 * Schema for the `AccountQuotaSnapshot` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AccountQuotaSnapshot".
 */
export interface AccountQuotaSnapshot {
  /**
   * Whether the user has an unlimited usage entitlement
   */
  isUnlimitedEntitlement: boolean;
  /**
   * Number of requests included in the entitlement
   */
  entitlementRequests: number;
  /**
   * Number of requests used so far this period
   */
  usedRequests: number;
  /**
   * Whether usage is still permitted after quota exhaustion
   */
  usageAllowedWithExhaustedQuota: boolean;
  /**
   * Percentage of entitlement remaining
   */
  remainingPercentage: number;
  /**
   * Number of overage requests made this period
   */
  overage: number;
  /**
   * Whether overage is allowed when quota is exhausted
   */
  overageAllowedWithExhaustedQuota: boolean;
  /**
   * Date when the quota resets (ISO 8601 string)
   */
  resetDate?: string;
}
/**
 * The currently selected custom agent, or null when using the default agent.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentGetCurrentResult".
 */
/** @experimental */
export interface AgentGetCurrentResult {
  /**
   * Currently selected custom agent, or null if using the default agent
   */
  agent?: AgentInfo | null;
}
/**
 * Schema for the `AgentInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentInfo".
 */
export interface AgentInfo {
  /**
   * Unique identifier of the custom agent
   */
  name: string;
  /**
   * Human-readable display name
   */
  displayName: string;
  /**
   * Description of the agent's purpose
   */
  description: string;
  /**
   * Absolute local file path of the agent definition. Only set for file-based agents loaded from disk; remote agents do not have a path.
   */
  path?: string;
}
/**
 * Custom agents available to the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentList".
 */
/** @experimental */
export interface AgentList {
  /**
   * Available custom agents
   */
  agents: AgentInfo[];
}
/**
 * Custom agents available to the session after reloading definitions from disk.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentReloadResult".
 */
/** @experimental */
export interface AgentReloadResult {
  /**
   * Reloaded custom agents
   */
  agents: AgentInfo[];
}
/**
 * Name of the custom agent to select for subsequent turns.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentSelectRequest".
 */
/** @experimental */
export interface AgentSelectRequest {
  /**
   * Name of the custom agent to select
   */
  name: string;
}
/**
 * The newly selected custom agent.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentSelectResult".
 */
/** @experimental */
export interface AgentSelectResult {
  agent: AgentInfo;
}
/**
 * Slash commands available in the session, after applying any include/exclude filters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandList".
 */
export interface CommandList {
  /**
   * Commands available in this session
   */
  commands: SlashCommandInfo[];
}
/**
 * Schema for the `SlashCommandInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandInfo".
 */
export interface SlashCommandInfo {
  /**
   * Canonical command name without a leading slash
   */
  name: string;
  /**
   * Canonical aliases without leading slashes
   */
  aliases?: string[];
  /**
   * Human-readable command description
   */
  description: string;
  kind: SlashCommandKind;
  input?: SlashCommandInput;
  /**
   * Whether the command may run while an agent turn is active
   */
  allowDuringAgentExecution: boolean;
  /**
   * Whether the command is experimental
   */
  experimental?: boolean;
}
/**
 * Optional unstructured input hint
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandInput".
 */
export interface SlashCommandInput {
  /**
   * Hint to display when command input has not been provided
   */
  hint: string;
  /**
   * When true, the command requires non-empty input; clients should render the input hint as required
   */
  required?: boolean;
  completion?: SlashCommandInputCompletion;
  /**
   * When true, clients should pass the full text after the command name as a single argument rather than splitting on whitespace
   */
  preserveMultilineInput?: boolean;
}
/**
 * Pending command request ID and an optional error if the client handler failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsHandlePendingCommandRequest".
 */
export interface CommandsHandlePendingCommandRequest {
  /**
   * Request ID from the command invocation event
   */
  requestId: string;
  /**
   * Error message if the command handler failed
   */
  error?: string;
}
/**
 * Indicates whether the pending client-handled command was completed successfully.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsHandlePendingCommandResult".
 */
export interface CommandsHandlePendingCommandResult {
  /**
   * Whether the command was handled successfully
   */
  success: boolean;
}
/**
 * Slash command name and optional raw input string to invoke.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsInvokeRequest".
 */
export interface CommandsInvokeRequest {
  /**
   * Command name. Leading slashes are stripped and the name is matched case-insensitively.
   */
  name: string;
  /**
   * Raw input after the command name
   */
  input?: string;
}
/**
 * Optional filters controlling which command sources to include in the listing.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsListRequest".
 */
export interface CommandsListRequest {
  /**
   * Include runtime built-in commands
   */
  includeBuiltins?: boolean;
  /**
   * Include enabled user-invocable skills and commands
   */
  includeSkills?: boolean;
  /**
   * Include commands registered by protocol clients, including SDK clients and extensions
   */
  includeClientCommands?: boolean;
}
/**
 * Queued command request ID and the result indicating whether the client handled it.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsRespondToQueuedCommandRequest".
 */
export interface CommandsRespondToQueuedCommandRequest {
  /**
   * Request ID from the queued command event
   */
  requestId: string;
  result: QueuedCommandResult;
}
/**
 * Schema for the `QueuedCommandHandled` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuedCommandHandled".
 */
export interface QueuedCommandHandled {
  /**
   * The command was handled
   */
  handled: true;
  /**
   * If true, stop processing remaining queued items
   */
  stopProcessingQueue?: boolean;
}
/**
 * Schema for the `QueuedCommandNotHandled` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuedCommandNotHandled".
 */
export interface QueuedCommandNotHandled {
  /**
   * The command was not handled
   */
  handled: false;
}
/**
 * Indicates whether the queued-command response was accepted by the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsRespondToQueuedCommandResult".
 */
export interface CommandsRespondToQueuedCommandResult {
  /**
   * Whether the response was accepted (false if the requestId was not found or already resolved)
   */
  success: boolean;
}
/**
 * Optional connection token presented by the SDK client during the handshake.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectRequest".
 */
/** @internal */
export interface ConnectRequest {
  /**
   * Connection token; required when the server was started with COPILOT_CONNECTION_TOKEN
   */
  token?: string;
}
/**
 * Handshake result reporting the server's protocol version and package version on success.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectResult".
 */
/** @internal */
export interface ConnectResult {
  /**
   * Always true on success
   */
  ok: true;
  /**
   * Server protocol version number
   */
  protocolVersion: number;
  /**
   * Server package version
   */
  version: string;
}
/**
 * The currently selected model for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CurrentModel".
 */
export interface CurrentModel {
  /**
   * Currently active model identifier
   */
  modelId?: string;
}
/**
 * Schema for the `DiscoveredMcpServer` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredMcpServer".
 */
export interface DiscoveredMcpServer {
  /**
   * Server name (config key)
   */
  name: string;
  type?: DiscoveredMcpServerType;
  source: DiscoveredMcpServerSource;
  /**
   * Whether the server is enabled (not in the disabled list)
   */
  enabled: boolean;
}
/**
 * Schema for the `EmbeddedBlobResourceContents` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EmbeddedBlobResourceContents".
 */
export interface EmbeddedBlobResourceContents {
  /**
   * URI identifying the resource
   */
  uri: string;
  /**
   * MIME type of the blob content
   */
  mimeType?: string;
  /**
   * Base64-encoded binary content of the resource
   */
  blob: string;
}
/**
 * Schema for the `EmbeddedTextResourceContents` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EmbeddedTextResourceContents".
 */
export interface EmbeddedTextResourceContents {
  /**
   * URI identifying the resource
   */
  uri: string;
  /**
   * MIME type of the text content
   */
  mimeType?: string;
  /**
   * Text content of the resource
   */
  text: string;
}
/**
 * Schema for the `Extension` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "Extension".
 */
export interface Extension {
  /**
   * Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper')
   */
  id: string;
  /**
   * Extension name (directory name)
   */
  name: string;
  source: ExtensionSource;
  status: ExtensionStatus;
  /**
   * Process ID if the extension is running
   */
  pid?: number;
}
/**
 * Extensions discovered for the session, with their current status.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionList".
 */
/** @experimental */
export interface ExtensionList {
  /**
   * Discovered extensions and their current status
   */
  extensions: Extension[];
}
/**
 * Source-qualified extension identifier to disable for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionsDisableRequest".
 */
/** @experimental */
export interface ExtensionsDisableRequest {
  /**
   * Source-qualified extension ID to disable
   */
  id: string;
}
/**
 * Source-qualified extension identifier to enable for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionsEnableRequest".
 */
/** @experimental */
export interface ExtensionsEnableRequest {
  /**
   * Source-qualified extension ID to enable
   */
  id: string;
}
/**
 * Expanded external tool result payload
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlm".
 */
export interface ExternalToolTextResultForLlm {
  /**
   * Text result returned to the model
   */
  textResultForLlm: string;
  /**
   * Execution outcome classification. Optional for back-compat; normalized to 'success' (or 'failure' when error is present) when missing or unrecognized.
   */
  resultType?: string;
  /**
   * Optional error message for failed executions
   */
  error?: string;
  /**
   * Detailed log content for timeline display
   */
  sessionLog?: string;
  /**
   * Optional tool-specific telemetry
   */
  toolTelemetry?: {
    [k: string]: unknown;
  };
  /**
   * Structured content blocks from the tool
   */
  contents?: ExternalToolTextResultForLlmContent[];
  [k: string]: unknown;
}
/**
 * Plain text content block
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentText".
 */
export interface ExternalToolTextResultForLlmContentText {
  /**
   * Content block type discriminator
   */
  type: "text";
  /**
   * The text content
   */
  text: string;
}
/**
 * Terminal/shell output content block with optional exit code and working directory
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentTerminal".
 */
export interface ExternalToolTextResultForLlmContentTerminal {
  /**
   * Content block type discriminator
   */
  type: "terminal";
  /**
   * Terminal/shell output text
   */
  text: string;
  /**
   * Process exit code, if the command has completed
   */
  exitCode?: number;
  /**
   * Working directory where the command was executed
   */
  cwd?: string;
}
/**
 * Image content block with base64-encoded data
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentImage".
 */
export interface ExternalToolTextResultForLlmContentImage {
  /**
   * Content block type discriminator
   */
  type: "image";
  /**
   * Base64-encoded image data
   */
  data: string;
  /**
   * MIME type of the image (e.g., image/png, image/jpeg)
   */
  mimeType: string;
}
/**
 * Audio content block with base64-encoded data
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentAudio".
 */
export interface ExternalToolTextResultForLlmContentAudio {
  /**
   * Content block type discriminator
   */
  type: "audio";
  /**
   * Base64-encoded audio data
   */
  data: string;
  /**
   * MIME type of the audio (e.g., audio/wav, audio/mpeg)
   */
  mimeType: string;
}
/**
 * Resource link content block referencing an external resource
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentResourceLink".
 */
export interface ExternalToolTextResultForLlmContentResourceLink {
  /**
   * Icons associated with this resource
   */
  icons?: ExternalToolTextResultForLlmContentResourceLinkIcon[];
  /**
   * Resource name identifier
   */
  name: string;
  /**
   * Human-readable display title for the resource
   */
  title?: string;
  /**
   * URI identifying the resource
   */
  uri: string;
  /**
   * Human-readable description of the resource
   */
  description?: string;
  /**
   * MIME type of the resource content
   */
  mimeType?: string;
  /**
   * Size of the resource in bytes
   */
  size?: number;
  /**
   * Content block type discriminator
   */
  type: "resource_link";
}
/**
 * Icon image for a resource
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentResourceLinkIcon".
 */
export interface ExternalToolTextResultForLlmContentResourceLinkIcon {
  /**
   * URL or path to the icon image
   */
  src: string;
  /**
   * MIME type of the icon image
   */
  mimeType?: string;
  /**
   * Available icon sizes (e.g., ['16x16', '32x32'])
   */
  sizes?: string[];
  theme?: ExternalToolTextResultForLlmContentResourceLinkIconTheme;
}
/**
 * Embedded resource content block with inline text or binary data
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentResource".
 */
export interface ExternalToolTextResultForLlmContentResource {
  /**
   * Content block type discriminator
   */
  type: "resource";
  resource: ExternalToolTextResultForLlmContentResourceDetails;
}
/**
 * Optional user prompt to combine with the fleet orchestration instructions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FleetStartRequest".
 */
/** @experimental */
export interface FleetStartRequest {
  /**
   * Optional user prompt to combine with fleet instructions
   */
  prompt?: string;
}
/**
 * Indicates whether fleet mode was successfully activated.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FleetStartResult".
 */
/** @experimental */
export interface FleetStartResult {
  /**
   * Whether fleet mode was successfully activated
   */
  started: boolean;
}
/**
 * Pending external tool call request ID, with the tool result or an error describing why it failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HandlePendingToolCallRequest".
 */
export interface HandlePendingToolCallRequest {
  /**
   * Request ID of the pending tool call
   */
  requestId: string;
  result?: ExternalToolResult;
  /**
   * Error message if the tool call failed
   */
  error?: string;
}
/**
 * Indicates whether the external tool call result was handled successfully.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HandlePendingToolCallResult".
 */
export interface HandlePendingToolCallResult {
  /**
   * Whether the tool call result was handled successfully
   */
  success: boolean;
}
/**
 * Post-compaction context window usage breakdown
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryCompactContextWindow".
 */
export interface HistoryCompactContextWindow {
  /**
   * Maximum token count for the model's context window
   */
  tokenLimit: number;
  /**
   * Current total tokens in the context window (system + conversation + tool definitions)
   */
  currentTokens: number;
  /**
   * Current number of messages in the conversation
   */
  messagesLength: number;
  /**
   * Token count from system message(s)
   */
  systemTokens?: number;
  /**
   * Token count from non-system messages (user, assistant, tool)
   */
  conversationTokens?: number;
  /**
   * Token count from tool definitions
   */
  toolDefinitionsTokens?: number;
}
/**
 * Compaction outcome with the number of tokens and messages removed and the resulting context window breakdown.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryCompactResult".
 */
/** @experimental */
export interface HistoryCompactResult {
  /**
   * Whether compaction completed successfully
   */
  success: boolean;
  /**
   * Number of tokens freed by compaction
   */
  tokensRemoved: number;
  /**
   * Number of messages removed during compaction
   */
  messagesRemoved: number;
  contextWindow?: HistoryCompactContextWindow;
}
/**
 * Identifier of the event to truncate to; this event and all later events are removed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryTruncateRequest".
 */
/** @experimental */
export interface HistoryTruncateRequest {
  /**
   * Event ID to truncate to. This event and all events after it are removed from the session.
   */
  eventId: string;
}
/**
 * Number of events that were removed by the truncation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryTruncateResult".
 */
/** @experimental */
export interface HistoryTruncateResult {
  /**
   * Number of events that were removed
   */
  eventsRemoved: number;
}
/**
 * Instruction sources loaded for the session, in merge order.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionsGetSourcesResult".
 */
export interface InstructionsGetSourcesResult {
  /**
   * Instruction sources for the session
   */
  sources: InstructionsSources[];
}
/**
 * Schema for the `InstructionsSources` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionsSources".
 */
export interface InstructionsSources {
  /**
   * Unique identifier for this source (used for toggling)
   */
  id: string;
  /**
   * Human-readable label
   */
  label: string;
  /**
   * File path relative to repo or absolute for home
   */
  sourcePath: string;
  /**
   * Raw content of the instruction file
   */
  content: string;
  type: InstructionsSourcesType;
  location: InstructionsSourcesLocation;
  /**
   * Glob pattern from frontmatter — when set, this instruction applies only to matching files
   */
  applyTo?: string;
  /**
   * Short description (body after frontmatter) for use in instruction tables
   */
  description?: string;
}
/**
 * Message text, optional severity level, persistence flag, and optional follow-up URL.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LogRequest".
 */
export interface LogRequest {
  /**
   * Human-readable message
   */
  message: string;
  level?: SessionLogLevel;
  /**
   * When true, the message is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Optional URL the user can open in their browser for more details
   */
  url?: string;
}
/**
 * Identifier of the session event that was emitted for the log message.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LogResult".
 */
export interface LogResult {
  /**
   * The unique identifier of the emitted session event
   */
  eventId: string;
}
/**
 * MCP server name and configuration to add to user configuration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigAddRequest".
 */
export interface McpConfigAddRequest {
  /**
   * Unique name for the MCP server
   */
  name: string;
  config: McpServerConfig;
}
/**
 * Local MCP server configuration launched as a child process.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigLocal".
 */
export interface McpServerConfigLocal {
  /**
   * Tools to include. Defaults to all tools if not specified.
   */
  tools?: string[];
  type?: McpServerConfigLocalType;
  /**
   * Whether this server is a built-in fallback used when the user has not configured their own server.
   */
  isDefaultServer?: boolean;
  filterMapping?: FilterMapping;
  /**
   * Timeout in milliseconds for tool calls to this server.
   */
  timeout?: number;
  /**
   * Executable command used to start the local MCP server process.
   */
  command: string;
  /**
   * Command-line arguments passed to the local MCP server process.
   */
  args: string[];
  /**
   * Working directory for the local MCP server process.
   */
  cwd?: string;
  /**
   * Environment variables to pass to the local MCP server process.
   */
  env?: {
    [k: string]: string;
  };
}
/**
 * Remote MCP server configuration accessed over HTTP or SSE.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigHttp".
 */
export interface McpServerConfigHttp {
  /**
   * Tools to include. Defaults to all tools if not specified.
   */
  tools?: string[];
  type?: McpServerConfigHttpType;
  /**
   * Whether this server is a built-in fallback used when the user has not configured their own server.
   */
  isDefaultServer?: boolean;
  filterMapping?: FilterMapping;
  /**
   * Timeout in milliseconds for tool calls to this server.
   */
  timeout?: number;
  /**
   * URL of the remote MCP server endpoint.
   */
  url: string;
  /**
   * HTTP headers to include in requests to the remote MCP server.
   */
  headers?: {
    [k: string]: string;
  };
  /**
   * OAuth client ID for a pre-registered remote MCP OAuth client.
   */
  oauthClientId?: string;
  /**
   * Whether the configured OAuth client is public and does not require a client secret.
   */
  oauthPublicClient?: boolean;
  oauthGrantType?: McpServerConfigHttpOauthGrantType;
}
/**
 * MCP server names to disable for new sessions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigDisableRequest".
 */
export interface McpConfigDisableRequest {
  /**
   * Names of MCP servers to disable. Each server is added to the persisted disabled list so new sessions skip it. Already-disabled names are ignored. Active sessions keep their current connections until they end.
   */
  names: string[];
}
/**
 * MCP server names to enable for new sessions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigEnableRequest".
 */
export interface McpConfigEnableRequest {
  /**
   * Names of MCP servers to enable. Each server is removed from the persisted disabled list so new sessions spawn it. Unknown or already-enabled names are ignored.
   */
  names: string[];
}
/**
 * User-configured MCP servers, keyed by server name.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigList".
 */
export interface McpConfigList {
  /**
   * All MCP servers from user config, keyed by name
   */
  servers: {
    [k: string]: McpServerConfig;
  };
}
/**
 * MCP server name to remove from user configuration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigRemoveRequest".
 */
export interface McpConfigRemoveRequest {
  /**
   * Name of the MCP server to remove
   */
  name: string;
}
/**
 * MCP server name and replacement configuration to write to user configuration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigUpdateRequest".
 */
export interface McpConfigUpdateRequest {
  /**
   * Name of the MCP server to update
   */
  name: string;
  config: McpServerConfig;
}
/**
 * Name of the MCP server to disable for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpDisableRequest".
 */
/** @experimental */
export interface McpDisableRequest {
  /**
   * Name of the MCP server to disable
   */
  serverName: string;
}
/**
 * Optional working directory used as context for MCP server discovery.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpDiscoverRequest".
 */
export interface McpDiscoverRequest {
  /**
   * Working directory used as context for discovery (e.g., plugin resolution)
   */
  workingDirectory?: string;
}
/**
 * MCP servers discovered from user, workspace, plugin, and built-in sources.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpDiscoverResult".
 */
export interface McpDiscoverResult {
  /**
   * MCP servers discovered from all sources
   */
  servers: DiscoveredMcpServer[];
}
/**
 * Name of the MCP server to enable for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpEnableRequest".
 */
/** @experimental */
export interface McpEnableRequest {
  /**
   * Name of the MCP server to enable
   */
  serverName: string;
}
/**
 * Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, and the callback success-page copy.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthLoginRequest".
 */
/** @experimental */
export interface McpOauthLoginRequest {
  /**
   * Name of the remote MCP server to authenticate
   */
  serverName: string;
  /**
   * When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck.
   */
  forceReauth?: boolean;
  /**
   * Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees.
   */
  clientName?: string;
  /**
   * Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return.
   */
  callbackSuccessMessage?: string;
}
/**
 * OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthLoginResult".
 */
/** @experimental */
export interface McpOauthLoginResult {
  /**
   * URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. When present, the runtime starts the callback listener before returning and continues the flow in the background; completion is signaled via session.mcp_server_status_changed.
   */
  authorizationUrl?: string;
}
/**
 * Schema for the `McpServer` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServer".
 */
export interface McpServer {
  /**
   * Server name (config key)
   */
  name: string;
  status: McpServerStatus;
  source?: McpServerSource;
  /**
   * Error message if the server failed to connect
   */
  error?: string;
}
/**
 * MCP servers configured for the session, with their connection status.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerList".
 */
/** @experimental */
export interface McpServerList {
  /**
   * Configured MCP servers
   */
  servers: McpServer[];
}
/**
 * Schema for the `Model` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "Model".
 */
export interface Model {
  /**
   * Model identifier (e.g., "claude-sonnet-4.5")
   */
  id: string;
  /**
   * Display name
   */
  name: string;
  capabilities: ModelCapabilities;
  policy?: ModelPolicy;
  billing?: ModelBilling;
  /**
   * Supported reasoning effort levels (only present if model supports reasoning effort)
   */
  supportedReasoningEfforts?: string[];
  /**
   * Default reasoning effort level (only present if model supports reasoning effort)
   */
  defaultReasoningEffort?: string;
  modelPickerCategory?: ModelPickerCategory;
  modelPickerPriceCategory?: ModelPickerPriceCategory;
}
/**
 * Model capabilities and limits
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilities".
 */
export interface ModelCapabilities {
  supports?: ModelCapabilitiesSupports;
  limits?: ModelCapabilitiesLimits;
}
/**
 * Feature flags indicating what the model supports
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesSupports".
 */
export interface ModelCapabilitiesSupports {
  /**
   * Whether this model supports vision/image input
   */
  vision?: boolean;
  /**
   * Whether this model supports reasoning effort configuration
   */
  reasoningEffort?: boolean;
}
/**
 * Token limits for prompts, outputs, and context window
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesLimits".
 */
export interface ModelCapabilitiesLimits {
  /**
   * Maximum number of prompt/input tokens
   */
  max_prompt_tokens?: number;
  /**
   * Maximum number of output/completion tokens
   */
  max_output_tokens?: number;
  /**
   * Maximum total context window size in tokens
   */
  max_context_window_tokens?: number;
  vision?: ModelCapabilitiesLimitsVision;
}
/**
 * Vision-specific limits
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesLimitsVision".
 */
export interface ModelCapabilitiesLimitsVision {
  /**
   * MIME types the model accepts
   */
  supported_media_types: string[];
  /**
   * Maximum number of images per prompt
   */
  max_prompt_images: number;
  /**
   * Maximum image size in bytes
   */
  max_prompt_image_size: number;
}
/**
 * Policy state (if applicable)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelPolicy".
 */
export interface ModelPolicy {
  /**
   * Current policy state for this model
   */
  state: string;
  /**
   * Usage terms or conditions for this model
   */
  terms?: string;
}
/**
 * Billing information
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelBilling".
 */
export interface ModelBilling {
  /**
   * Billing cost multiplier relative to the base rate
   */
  multiplier?: number;
  tokenPrices?: ModelBillingTokenPrices;
}
/**
 * Token-level pricing information for this model
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelBillingTokenPrices".
 */
export interface ModelBillingTokenPrices {
  /**
   * Price per billing batch of input tokens in nano-AIUs (1 nano-AIU = 0.000000001 AIU, 1 AIU = $0.01 USD)
   */
  inputPrice?: number;
  /**
   * Price per billing batch of output tokens in nano-AIUs (1 nano-AIU = 0.000000001 AIU, 1 AIU = $0.01 USD)
   */
  outputPrice?: number;
  /**
   * Price per billing batch of cached tokens in nano-AIUs (1 nano-AIU = 0.000000001 AIU, 1 AIU = $0.01 USD)
   */
  cachePrice?: number;
  /**
   * Number of tokens per standard billing batch
   */
  batchSize?: number;
}
/**
 * Override individual model capabilities resolved by the runtime
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesOverride".
 */
export interface ModelCapabilitiesOverride {
  supports?: ModelCapabilitiesOverrideSupports;
  limits?: ModelCapabilitiesOverrideLimits;
}
/**
 * Feature flags indicating what the model supports
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesOverrideSupports".
 */
export interface ModelCapabilitiesOverrideSupports {
  /**
   * Whether this model supports vision/image input
   */
  vision?: boolean;
  /**
   * Whether this model supports reasoning effort configuration
   */
  reasoningEffort?: boolean;
}
/**
 * Token limits for prompts, outputs, and context window
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesOverrideLimits".
 */
export interface ModelCapabilitiesOverrideLimits {
  /**
   * Maximum number of prompt/input tokens
   */
  max_prompt_tokens?: number;
  /**
   * Maximum number of output/completion tokens
   */
  max_output_tokens?: number;
  /**
   * Maximum total context window size in tokens
   */
  max_context_window_tokens?: number;
  vision?: ModelCapabilitiesOverrideLimitsVision;
}
/**
 * Vision-specific limits
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesOverrideLimitsVision".
 */
export interface ModelCapabilitiesOverrideLimitsVision {
  /**
   * MIME types the model accepts
   */
  supported_media_types?: string[];
  /**
   * Maximum number of images per prompt
   */
  max_prompt_images?: number;
  /**
   * Maximum image size in bytes
   */
  max_prompt_image_size?: number;
}
/**
 * List of Copilot models available to the resolved user, including capabilities and billing metadata.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelList".
 */
export interface ModelList {
  /**
   * List of available models with full metadata
   */
  models: Model[];
}

export interface ModelsListRequest {
  /**
   * GitHub token for per-user model listing. When provided, resolves this token to determine the user's Copilot plan and available models instead of using the global auth.
   */
  gitHubToken?: string;
}
/**
 * Target model identifier and optional reasoning effort, summary, and capability overrides.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelSwitchToRequest".
 */
export interface ModelSwitchToRequest {
  /**
   * Model identifier to switch to
   */
  modelId: string;
  /**
   * Reasoning effort level to use for the model. "none" disables reasoning.
   */
  reasoningEffort?: string;
  reasoningSummary?: ReasoningSummary;
  modelCapabilities?: ModelCapabilitiesOverride;
}
/**
 * The model identifier active on the session after the switch.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelSwitchToResult".
 */
export interface ModelSwitchToResult {
  /**
   * Currently active model identifier after the switch
   */
  modelId?: string;
}
/**
 * Agent interaction mode to apply to the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModeSetRequest".
 */
export interface ModeSetRequest {
  mode: SessionMode;
}
/**
 * The session's friendly name, or null when not yet set.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "NameGetResult".
 */
export interface NameGetResult {
  /**
   * The session name (user-set or auto-generated), or null if not yet set
   */
  name: string | null;
}
/**
 * New friendly name to apply to the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "NameSetRequest".
 */
export interface NameSetRequest {
  /**
   * New session name (1–100 characters, trimmed of leading/trailing whitespace)
   */
  name: string;
}
/**
 * Schema for the `PermissionDecisionApproveOnce` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveOnce".
 */
export interface PermissionDecisionApproveOnce {
  /**
   * The permission request was approved for this one instance
   */
  kind: "approve-once";
}
/**
 * Schema for the `PermissionDecisionApproveForSession` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSession".
 */
export interface PermissionDecisionApproveForSession {
  /**
   * Approved and remembered for the rest of the session
   */
  kind: "approve-for-session";
  approval?: PermissionDecisionApproveForSessionApproval;
  /**
   * The URL domain to approve for this session
   */
  domain?: string;
}
/**
 * Schema for the `PermissionDecisionApproveForSessionApprovalCommands` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalCommands".
 */
export interface PermissionDecisionApproveForSessionApprovalCommands {
  /**
   * Approval scoped to specific command identifiers.
   */
  kind: "commands";
  /**
   * Command identifiers covered by this approval.
   */
  commandIdentifiers: string[];
}
/**
 * Schema for the `PermissionDecisionApproveForSessionApprovalRead` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalRead".
 */
export interface PermissionDecisionApproveForSessionApprovalRead {
  /**
   * Approval covering read-only filesystem operations.
   */
  kind: "read";
}
/**
 * Schema for the `PermissionDecisionApproveForSessionApprovalWrite` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalWrite".
 */
export interface PermissionDecisionApproveForSessionApprovalWrite {
  /**
   * Approval covering filesystem write operations.
   */
  kind: "write";
}
/**
 * Schema for the `PermissionDecisionApproveForSessionApprovalMcp` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalMcp".
 */
export interface PermissionDecisionApproveForSessionApprovalMcp {
  /**
   * Approval covering an MCP tool.
   */
  kind: "mcp";
  /**
   * MCP server name.
   */
  serverName: string;
  /**
   * MCP tool name, or null to cover every tool on the server.
   */
  toolName: string | null;
}
/**
 * Schema for the `PermissionDecisionApproveForSessionApprovalMcpSampling` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalMcpSampling".
 */
export interface PermissionDecisionApproveForSessionApprovalMcpSampling {
  /**
   * Approval covering MCP sampling requests for a server.
   */
  kind: "mcp-sampling";
  /**
   * MCP server name.
   */
  serverName: string;
}
/**
 * Schema for the `PermissionDecisionApproveForSessionApprovalMemory` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalMemory".
 */
export interface PermissionDecisionApproveForSessionApprovalMemory {
  /**
   * Approval covering writes to long-term memory.
   */
  kind: "memory";
}
/**
 * Schema for the `PermissionDecisionApproveForSessionApprovalCustomTool` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalCustomTool".
 */
export interface PermissionDecisionApproveForSessionApprovalCustomTool {
  /**
   * Approval covering a custom tool.
   */
  kind: "custom-tool";
  /**
   * Custom tool name.
   */
  toolName: string;
}
/**
 * Schema for the `PermissionDecisionApproveForSessionApprovalExtensionManagement` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalExtensionManagement".
 */
export interface PermissionDecisionApproveForSessionApprovalExtensionManagement {
  /**
   * Approval covering extension lifecycle operations such as enable, disable, or reload.
   */
  kind: "extension-management";
  /**
   * Optional operation identifier; when omitted, the approval covers all extension management operations.
   */
  operation?: string;
}
/**
 * Schema for the `PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess".
 */
export interface PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess {
  /**
   * Approval covering an extension's request to access a permission-gated capability.
   */
  kind: "extension-permission-access";
  /**
   * Extension name.
   */
  extensionName: string;
}
/**
 * Schema for the `PermissionDecisionApproveForLocation` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocation".
 */
export interface PermissionDecisionApproveForLocation {
  /**
   * Approved and persisted for this project location
   */
  kind: "approve-for-location";
  approval: PermissionDecisionApproveForLocationApproval;
  /**
   * The location key (git root or cwd) to persist the approval to
   */
  locationKey: string;
}
/**
 * Schema for the `PermissionDecisionApproveForLocationApprovalCommands` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalCommands".
 */
export interface PermissionDecisionApproveForLocationApprovalCommands {
  /**
   * Approval scoped to specific command identifiers.
   */
  kind: "commands";
  /**
   * Command identifiers covered by this approval.
   */
  commandIdentifiers: string[];
}
/**
 * Schema for the `PermissionDecisionApproveForLocationApprovalRead` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalRead".
 */
export interface PermissionDecisionApproveForLocationApprovalRead {
  /**
   * Approval covering read-only filesystem operations.
   */
  kind: "read";
}
/**
 * Schema for the `PermissionDecisionApproveForLocationApprovalWrite` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalWrite".
 */
export interface PermissionDecisionApproveForLocationApprovalWrite {
  /**
   * Approval covering filesystem write operations.
   */
  kind: "write";
}
/**
 * Schema for the `PermissionDecisionApproveForLocationApprovalMcp` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalMcp".
 */
export interface PermissionDecisionApproveForLocationApprovalMcp {
  /**
   * Approval covering an MCP tool.
   */
  kind: "mcp";
  /**
   * MCP server name.
   */
  serverName: string;
  /**
   * MCP tool name, or null to cover every tool on the server.
   */
  toolName: string | null;
}
/**
 * Schema for the `PermissionDecisionApproveForLocationApprovalMcpSampling` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalMcpSampling".
 */
export interface PermissionDecisionApproveForLocationApprovalMcpSampling {
  /**
   * Approval covering MCP sampling requests for a server.
   */
  kind: "mcp-sampling";
  /**
   * MCP server name.
   */
  serverName: string;
}
/**
 * Schema for the `PermissionDecisionApproveForLocationApprovalMemory` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalMemory".
 */
export interface PermissionDecisionApproveForLocationApprovalMemory {
  /**
   * Approval covering writes to long-term memory.
   */
  kind: "memory";
}
/**
 * Schema for the `PermissionDecisionApproveForLocationApprovalCustomTool` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalCustomTool".
 */
export interface PermissionDecisionApproveForLocationApprovalCustomTool {
  /**
   * Approval covering a custom tool.
   */
  kind: "custom-tool";
  /**
   * Custom tool name.
   */
  toolName: string;
}
/**
 * Schema for the `PermissionDecisionApproveForLocationApprovalExtensionManagement` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalExtensionManagement".
 */
export interface PermissionDecisionApproveForLocationApprovalExtensionManagement {
  /**
   * Approval covering extension lifecycle operations such as enable, disable, or reload.
   */
  kind: "extension-management";
  /**
   * Optional operation identifier; when omitted, the approval covers all extension management operations.
   */
  operation?: string;
}
/**
 * Schema for the `PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess".
 */
export interface PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess {
  /**
   * Approval covering an extension's request to access a permission-gated capability.
   */
  kind: "extension-permission-access";
  /**
   * Extension name.
   */
  extensionName: string;
}
/**
 * Schema for the `PermissionDecisionApprovePermanently` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApprovePermanently".
 */
export interface PermissionDecisionApprovePermanently {
  /**
   * Approved and persisted across sessions
   */
  kind: "approve-permanently";
  /**
   * The URL domain to approve permanently
   */
  domain: string;
}
/**
 * Schema for the `PermissionDecisionReject` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionReject".
 */
export interface PermissionDecisionReject {
  /**
   * Denied by the user during an interactive prompt
   */
  kind: "reject";
  /**
   * Optional feedback from the user explaining the denial
   */
  feedback?: string;
}
/**
 * Schema for the `PermissionDecisionUserNotAvailable` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionUserNotAvailable".
 */
export interface PermissionDecisionUserNotAvailable {
  /**
   * Denied because user confirmation was unavailable
   */
  kind: "user-not-available";
}
/**
 * Pending permission request ID and the decision to apply (approve/reject and scope).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionRequest".
 */
export interface PermissionDecisionRequest {
  /**
   * Request ID of the pending permission request
   */
  requestId: string;
  result: PermissionDecision;
}
/**
 * Indicates whether the permission decision was applied; false when the request was already resolved.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionRequestResult".
 */
export interface PermissionRequestResult {
  /**
   * Whether the permission request was handled successfully
   */
  success: boolean;
}
/**
 * No parameters; clears all session-scoped tool permission approvals.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsResetSessionApprovalsRequest".
 */
export interface PermissionsResetSessionApprovalsRequest {}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsResetSessionApprovalsResult".
 */
export interface PermissionsResetSessionApprovalsResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Whether to auto-approve all tool permission requests for the rest of the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetApproveAllRequest".
 */
export interface PermissionsSetApproveAllRequest {
  /**
   * Whether to auto-approve all tool permission requests
   */
  enabled: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetApproveAllResult".
 */
export interface PermissionsSetApproveAllResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Optional message to echo back to the caller.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PingRequest".
 */
export interface PingRequest {
  /**
   * Optional message to echo back
   */
  message?: string;
}
/**
 * Server liveness response, including the echoed message, current timestamp, and protocol version.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PingResult".
 */
export interface PingResult {
  /**
   * Echoed message (or default greeting)
   */
  message: string;
  /**
   * Server timestamp in milliseconds
   */
  timestamp: number;
  /**
   * Server protocol version number
   */
  protocolVersion: number;
}
/**
 * Existence, contents, and resolved path of the session plan file.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PlanReadResult".
 */
export interface PlanReadResult {
  /**
   * Whether the plan file exists in the workspace
   */
  exists: boolean;
  /**
   * The content of the plan file, or null if it does not exist
   */
  content: string | null;
  /**
   * Absolute file path of the plan file, or null if workspace is not enabled
   */
  path: string | null;
}
/**
 * Replacement contents to write to the session plan file.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PlanUpdateRequest".
 */
export interface PlanUpdateRequest {
  /**
   * The new content for the plan file
   */
  content: string;
}
/**
 * Schema for the `Plugin` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "Plugin".
 */
export interface Plugin {
  /**
   * Plugin name
   */
  name: string;
  /**
   * Marketplace the plugin came from
   */
  marketplace: string;
  /**
   * Installed version
   */
  version?: string;
  /**
   * Whether the plugin is currently enabled
   */
  enabled: boolean;
}
/**
 * Plugins installed for the session, with their enabled state and version metadata.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginList".
 */
/** @experimental */
export interface PluginList {
  /**
   * Installed plugins
   */
  plugins: Plugin[];
}
/**
 * Optional remote session mode ("off", "export", or "on"); defaults to enabling both export and remote steering.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteEnableRequest".
 */
/** @experimental */
export interface RemoteEnableRequest {
  mode?: RemoteSessionMode;
}
/**
 * GitHub URL for the session and a flag indicating whether remote steering is enabled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteEnableResult".
 */
/** @experimental */
export interface RemoteEnableResult {
  /**
   * GitHub frontend URL for this session
   */
  url?: string;
  /**
   * Whether remote steering is enabled
   */
  remoteSteerable: boolean;
}
/**
 * Schema for the `ServerSkill` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ServerSkill".
 */
export interface ServerSkill {
  /**
   * Unique identifier for the skill
   */
  name: string;
  /**
   * Description of what the skill does
   */
  description: string;
  /**
   * Source location type (e.g., project, personal-copilot, plugin, builtin)
   */
  source: string;
  /**
   * Whether the skill can be invoked by the user as a slash command
   */
  userInvocable: boolean;
  /**
   * Whether the skill is currently enabled (based on global config)
   */
  enabled: boolean;
  /**
   * Absolute path to the skill file
   */
  path?: string;
  /**
   * The project path this skill belongs to (only for project/inherited skills)
   */
  projectPath?: string;
}
/**
 * Skills discovered across global and project sources.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ServerSkillList".
 */
export interface ServerSkillList {
  /**
   * All discovered skills across all sources
   */
  skills: ServerSkill[];
}
/**
 * Authentication status and account metadata for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionAuthStatus".
 */
export interface SessionAuthStatus {
  /**
   * Whether the session has resolved authentication
   */
  isAuthenticated: boolean;
  authType?: AuthInfoType;
  /**
   * Authentication host URL
   */
  host?: string;
  /**
   * Authenticated login/username, if available
   */
  login?: string;
  /**
   * Human-readable authentication status description
   */
  statusMessage?: string;
  /**
   * Copilot plan tier (e.g., individual_pro, business)
   */
  copilotPlan?: string;
}
/**
 * File path, content to append, and optional mode for the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsAppendFileRequest".
 */
export interface SessionFsAppendFileRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
  /**
   * Content to append
   */
  content: string;
  /**
   * Optional POSIX-style mode for newly created files
   */
  mode?: number;
}
/**
 * Describes a filesystem error.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsError".
 */
export interface SessionFsError {
  code: SessionFsErrorCode;
  /**
   * Free-form detail about the error, for logging/diagnostics
   */
  message?: string;
}
/**
 * Path to test for existence in the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsExistsRequest".
 */
export interface SessionFsExistsRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}
/**
 * Indicates whether the requested path exists in the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsExistsResult".
 */
export interface SessionFsExistsResult {
  /**
   * Whether the path exists
   */
  exists: boolean;
}
/**
 * Directory path to create in the client-provided session filesystem, with options for recursive creation and POSIX mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsMkdirRequest".
 */
export interface SessionFsMkdirRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
  /**
   * Create parent directories as needed
   */
  recursive?: boolean;
  /**
   * Optional POSIX-style mode for newly created directories
   */
  mode?: number;
}
/**
 * Directory path whose entries should be listed from the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirRequest".
 */
export interface SessionFsReaddirRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}
/**
 * Names of entries in the requested directory, or a filesystem error if the read failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirResult".
 */
export interface SessionFsReaddirResult {
  /**
   * Entry names in the directory
   */
  entries: string[];
  error?: SessionFsError;
}
/**
 * Schema for the `SessionFsReaddirWithTypesEntry` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirWithTypesEntry".
 */
export interface SessionFsReaddirWithTypesEntry {
  /**
   * Entry name
   */
  name: string;
  type: SessionFsReaddirWithTypesEntryType;
}
/**
 * Directory path whose entries (with type information) should be listed from the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirWithTypesRequest".
 */
export interface SessionFsReaddirWithTypesRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}
/**
 * Entries in the requested directory paired with file/directory type information, or a filesystem error if the read failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirWithTypesResult".
 */
export interface SessionFsReaddirWithTypesResult {
  /**
   * Directory entries with type information
   */
  entries: SessionFsReaddirWithTypesEntry[];
  error?: SessionFsError;
}
/**
 * Path of the file to read from the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReadFileRequest".
 */
export interface SessionFsReadFileRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}
/**
 * File content as a UTF-8 string, or a filesystem error if the read failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReadFileResult".
 */
export interface SessionFsReadFileResult {
  /**
   * File content as UTF-8 string
   */
  content: string;
  error?: SessionFsError;
}
/**
 * Source and destination paths for renaming or moving an entry in the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsRenameRequest".
 */
export interface SessionFsRenameRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Source path using SessionFs conventions
   */
  src: string;
  /**
   * Destination path using SessionFs conventions
   */
  dest: string;
}
/**
 * Path to remove from the client-provided session filesystem, with options for recursive removal and force.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsRmRequest".
 */
export interface SessionFsRmRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
  /**
   * Remove directories and their contents recursively
   */
  recursive?: boolean;
  /**
   * Ignore errors if the path does not exist
   */
  force?: boolean;
}
/**
 * Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSetProviderRequest".
 */
export interface SessionFsSetProviderRequest {
  /**
   * Initial working directory for sessions
   */
  initialCwd: string;
  /**
   * Path within each session's SessionFs where the runtime stores files for that session
   */
  sessionStatePath: string;
  conventions: SessionFsSetProviderConventions;
}
/**
 * Indicates whether the calling client was registered as the session filesystem provider.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSetProviderResult".
 */
export interface SessionFsSetProviderResult {
  /**
   * Whether the provider was set successfully
   */
  success: boolean;
}
/**
 * Indicates whether the per-session SQLite database already exists.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteExistsResult".
 */
export interface SessionFsSqliteExistsResult {
  /**
   * Whether the session database already exists
   */
  exists: boolean;
}
/**
 * SQL query, query type, and optional bind parameters for executing a SQLite query against the per-session database.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteQueryRequest".
 */
export interface SessionFsSqliteQueryRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * SQL query to execute
   */
  query: string;
  queryType: SessionFsSqliteQueryType;
  /**
   * Optional named bind parameters
   */
  params?: {
    [k: string]: string | number | null;
  };
}
/**
 * Query results including rows, columns, and rows affected, or a filesystem error if execution failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteQueryResult".
 */
export interface SessionFsSqliteQueryResult {
  /**
   * For SELECT: array of row objects. For others: empty array.
   */
  rows: {
    [k: string]: unknown;
  }[];
  /**
   * Column names from the result set
   */
  columns: string[];
  /**
   * Number of rows affected (for INSERT/UPDATE/DELETE)
   */
  rowsAffected: number;
  /**
   * Last inserted row ID (for INSERT)
   */
  lastInsertRowid?: number;
  error?: SessionFsError;
}
/**
 * Path whose metadata should be returned from the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsStatRequest".
 */
export interface SessionFsStatRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}
/**
 * Filesystem metadata for the requested path, or a filesystem error if the stat failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsStatResult".
 */
export interface SessionFsStatResult {
  /**
   * Whether the path is a file
   */
  isFile: boolean;
  /**
   * Whether the path is a directory
   */
  isDirectory: boolean;
  /**
   * File size in bytes
   */
  size: number;
  /**
   * ISO 8601 timestamp of last modification
   */
  mtime: string;
  /**
   * ISO 8601 timestamp of creation
   */
  birthtime: string;
  error?: SessionFsError;
}
/**
 * File path, content to write, and optional mode for the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsWriteFileRequest".
 */
export interface SessionFsWriteFileRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
  /**
   * Content to write
   */
  content: string;
  /**
   * Optional POSIX-style mode for newly created files
   */
  mode?: number;
}
/**
 * Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsForkRequest".
 */
/** @experimental */
export interface SessionsForkRequest {
  /**
   * Source session ID to fork from
   */
  sessionId: string;
  /**
   * Optional event ID boundary. When provided, the fork includes only events before this ID (exclusive). When omitted, all events are included.
   */
  toEventId?: string;
  /**
   * Optional friendly name to assign to the forked session.
   */
  name?: string;
}
/**
 * Identifier and optional friendly name assigned to the newly forked session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsForkResult".
 */
/** @experimental */
export interface SessionsForkResult {
  /**
   * The new forked session's ID
   */
  sessionId: string;
  /**
   * Friendly name assigned to the forked session, if any.
   */
  name?: string;
}
/**
 * Shell command to run, with optional working directory and timeout in milliseconds.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellExecRequest".
 */
export interface ShellExecRequest {
  /**
   * Shell command to execute
   */
  command: string;
  /**
   * Working directory (defaults to session working directory)
   */
  cwd?: string;
  /**
   * Timeout in milliseconds (default: 30000)
   */
  timeout?: number;
}
/**
 * Identifier of the spawned process, used to correlate streamed output and exit notifications.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellExecResult".
 */
export interface ShellExecResult {
  /**
   * Unique identifier for tracking streamed output
   */
  processId: string;
}
/**
 * Identifier of a process previously returned by "shell.exec" and the signal to send.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellKillRequest".
 */
export interface ShellKillRequest {
  /**
   * Process identifier returned by shell.exec
   */
  processId: string;
  signal?: ShellKillSignal;
}
/**
 * Indicates whether the signal was delivered; false if the process was unknown or already exited.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellKillResult".
 */
export interface ShellKillResult {
  /**
   * Whether the signal was sent successfully
   */
  killed: boolean;
}
/**
 * Schema for the `Skill` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "Skill".
 */
export interface Skill {
  /**
   * Unique identifier for the skill
   */
  name: string;
  /**
   * Description of what the skill does
   */
  description: string;
  /**
   * Source location type (e.g., project, personal, plugin)
   */
  source: string;
  /**
   * Whether the skill can be invoked by the user as a slash command
   */
  userInvocable: boolean;
  /**
   * Whether the skill is currently enabled
   */
  enabled: boolean;
  /**
   * Absolute path to the skill file
   */
  path?: string;
}
/**
 * Skills available to the session, with their enabled state.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillList".
 */
/** @experimental */
export interface SkillList {
  /**
   * Available skills
   */
  skills: Skill[];
}
/**
 * Skill names to mark as disabled in global configuration, replacing any previous list.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsConfigSetDisabledSkillsRequest".
 */
export interface SkillsConfigSetDisabledSkillsRequest {
  /**
   * List of skill names to disable
   */
  disabledSkills: string[];
}
/**
 * Name of the skill to disable for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsDisableRequest".
 */
/** @experimental */
export interface SkillsDisableRequest {
  /**
   * Name of the skill to disable
   */
  name: string;
}
/**
 * Optional project paths and additional skill directories to include in discovery.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsDiscoverRequest".
 */
export interface SkillsDiscoverRequest {
  /**
   * Optional list of project directory paths to scan for project-scoped skills
   */
  projectPaths?: string[];
  /**
   * Optional list of additional skill directory paths to include
   */
  skillDirectories?: string[];
}
/**
 * Name of the skill to enable for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsEnableRequest".
 */
/** @experimental */
export interface SkillsEnableRequest {
  /**
   * Name of the skill to enable
   */
  name: string;
}
/**
 * Diagnostics from reloading skill definitions, with warnings and errors as separate lists.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsLoadDiagnostics".
 */
/** @experimental */
export interface SkillsLoadDiagnostics {
  /**
   * Warnings emitted while loading skills (e.g. skills that loaded but had issues)
   */
  warnings: string[];
  /**
   * Errors emitted while loading skills (e.g. skills that failed to load entirely)
   */
  errors: string[];
}
/**
 * Schema for the `SlashCommandAgentPromptResult` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandAgentPromptResult".
 */
export interface SlashCommandAgentPromptResult {
  /**
   * Agent prompt result discriminator
   */
  kind: "agent-prompt";
  /**
   * Prompt to submit to the agent
   */
  prompt: string;
  /**
   * Prompt text to display to the user
   */
  displayPrompt: string;
  mode?: SlashCommandAgentPromptMode;
  /**
   * True when the invocation mutated user runtime settings; consumers caching settings should refresh
   */
  runtimeSettingsChanged?: boolean;
}
/**
 * Schema for the `SlashCommandCompletedResult` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandCompletedResult".
 */
export interface SlashCommandCompletedResult {
  /**
   * Completed result discriminator
   */
  kind: "completed";
  /**
   * Optional user-facing message describing the completed command
   */
  message?: string;
  /**
   * True when the invocation mutated user runtime settings; consumers caching settings should refresh
   */
  runtimeSettingsChanged?: boolean;
}
/**
 * Schema for the `SlashCommandTextResult` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandTextResult".
 */
export interface SlashCommandTextResult {
  /**
   * Text result discriminator
   */
  kind: "text";
  /**
   * Text output for the client to render
   */
  text: string;
  /**
   * Whether text contains Markdown
   */
  markdown?: boolean;
  /**
   * Whether ANSI sequences should be preserved
   */
  preserveAnsi?: boolean;
  /**
   * True when the invocation mutated user runtime settings; consumers caching settings should refresh
   */
  runtimeSettingsChanged?: boolean;
}
/**
 * Schema for the `TaskAgentInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskAgentInfo".
 */
export interface TaskAgentInfo {
  /**
   * Task kind
   */
  type: "agent";
  /**
   * Unique task identifier
   */
  id: string;
  /**
   * Tool call ID associated with this agent task
   */
  toolCallId: string;
  /**
   * Short description of the task
   */
  description: string;
  status: TaskAgentInfoStatus;
  /**
   * ISO 8601 timestamp when the task was started
   */
  startedAt: string;
  /**
   * ISO 8601 timestamp when the task finished
   */
  completedAt?: string;
  /**
   * Accumulated active execution time in milliseconds
   */
  activeTimeMs?: number;
  /**
   * ISO 8601 timestamp when the current active period began
   */
  activeStartedAt?: string;
  /**
   * Error message when the task failed
   */
  error?: string;
  /**
   * Type of agent running this task
   */
  agentType: string;
  /**
   * Prompt passed to the agent
   */
  prompt: string;
  /**
   * Result text from the task when available
   */
  result?: string;
  /**
   * Model used for the task when specified
   */
  model?: string;
  executionMode?: TaskAgentInfoExecutionMode;
  /**
   * Whether the task is currently in the original sync wait and can be moved to background mode. False once it is already backgrounded, idle, finished, or no longer has a promotable sync waiter.
   */
  canPromoteToBackground?: boolean;
  /**
   * Most recent response text from the agent
   */
  latestResponse?: string;
  /**
   * ISO 8601 timestamp when the agent entered idle state
   */
  idleSince?: string;
}
/**
 * Schema for the `TaskShellInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskShellInfo".
 */
export interface TaskShellInfo {
  /**
   * Task kind
   */
  type: "shell";
  /**
   * Unique task identifier
   */
  id: string;
  /**
   * Short description of the task
   */
  description: string;
  status: TaskShellInfoStatus;
  /**
   * ISO 8601 timestamp when the task was started
   */
  startedAt: string;
  /**
   * ISO 8601 timestamp when the task finished
   */
  completedAt?: string;
  /**
   * Command being executed
   */
  command: string;
  attachmentMode: TaskShellInfoAttachmentMode;
  executionMode?: TaskShellInfoExecutionMode;
  /**
   * Whether this shell task can be promoted to background mode
   */
  canPromoteToBackground?: boolean;
  /**
   * Path to the detached shell log, when available
   */
  logPath?: string;
  /**
   * Process ID when available
   */
  pid?: number;
}
/**
 * Background tasks currently tracked by the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskList".
 */
/** @experimental */
export interface TaskList {
  /**
   * Currently tracked tasks
   */
  tasks: TaskInfo[];
}
/**
 * Identifier of the background task to cancel.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksCancelRequest".
 */
/** @experimental */
export interface TasksCancelRequest {
  /**
   * Task identifier
   */
  id: string;
}
/**
 * Indicates whether the background task was successfully cancelled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksCancelResult".
 */
/** @experimental */
export interface TasksCancelResult {
  /**
   * Whether the task was successfully cancelled
   */
  cancelled: boolean;
}
/**
 * Identifier of the task to promote to background mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksPromoteToBackgroundRequest".
 */
/** @experimental */
export interface TasksPromoteToBackgroundRequest {
  /**
   * Task identifier
   */
  id: string;
}
/**
 * Indicates whether the task was successfully promoted to background mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksPromoteToBackgroundResult".
 */
/** @experimental */
export interface TasksPromoteToBackgroundResult {
  /**
   * Whether the task was successfully promoted to background mode
   */
  promoted: boolean;
}
/**
 * Identifier of the completed or cancelled task to remove from tracking.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksRemoveRequest".
 */
/** @experimental */
export interface TasksRemoveRequest {
  /**
   * Task identifier
   */
  id: string;
}
/**
 * Indicates whether the task was removed. False when the task does not exist or is still running/idle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksRemoveResult".
 */
/** @experimental */
export interface TasksRemoveResult {
  /**
   * Whether the task was removed. Returns false if the task does not exist or is still running/idle (cancel it first).
   */
  removed: boolean;
}
/**
 * Identifier of the target agent task, message content, and optional sender agent ID.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksSendMessageRequest".
 */
/** @experimental */
export interface TasksSendMessageRequest {
  /**
   * Agent task identifier
   */
  id: string;
  /**
   * Message content to send to the agent
   */
  message: string;
  /**
   * Agent ID of the sender, if sent on behalf of another agent
   */
  fromAgentId?: string;
}
/**
 * Indicates whether the message was delivered, with an error message when delivery failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksSendMessageResult".
 */
/** @experimental */
export interface TasksSendMessageResult {
  /**
   * Whether the message was successfully delivered or steered
   */
  sent: boolean;
  /**
   * Error message if delivery failed
   */
  error?: string;
}
/**
 * Agent type, prompt, name, and optional description and model override for the new task.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksStartAgentRequest".
 */
/** @experimental */
export interface TasksStartAgentRequest {
  /**
   * Type of agent to start (e.g., 'explore', 'task', 'general-purpose')
   */
  agentType: string;
  /**
   * Task prompt for the agent
   */
  prompt: string;
  /**
   * Short name for the agent, used to generate a human-readable ID
   */
  name: string;
  /**
   * Short description of the task
   */
  description?: string;
  /**
   * Optional model override
   */
  model?: string;
}
/**
 * Identifier assigned to the newly started background agent task.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksStartAgentResult".
 */
/** @experimental */
export interface TasksStartAgentResult {
  /**
   * Generated agent ID for the background task
   */
  agentId: string;
}
/**
 * Schema for the `Tool` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "Tool".
 */
export interface Tool {
  /**
   * Tool identifier (e.g., "bash", "grep", "str_replace_editor")
   */
  name: string;
  /**
   * Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP tools)
   */
  namespacedName?: string;
  /**
   * Description of what the tool does
   */
  description: string;
  /**
   * JSON Schema for the tool's input parameters
   */
  parameters?: {
    [k: string]: unknown;
  };
  /**
   * Optional instructions for how to use this tool effectively
   */
  instructions?: string;
}
/**
 * Built-in tools available for the requested model, with their parameters and instructions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolList".
 */
export interface ToolList {
  /**
   * List of available built-in tools with metadata
   */
  tools: Tool[];
}
/**
 * Optional model identifier whose tool overrides should be applied to the listing.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsListRequest".
 */
export interface ToolsListRequest {
  /**
   * Optional model ID — when provided, the returned tool list reflects model-specific overrides
   */
  model?: string;
}
/**
 * Multi-select string field where each option pairs a value with a display label.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationArrayAnyOfField".
 */
export interface UIElicitationArrayAnyOfField {
  /**
   * Type discriminator. Always "array".
   */
  type: "array";
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Minimum number of items the user must select.
   */
  minItems?: number;
  /**
   * Maximum number of items the user may select.
   */
  maxItems?: number;
  items: UIElicitationArrayAnyOfFieldItems;
  /**
   * Default values selected when the form is first shown.
   */
  default?: string[];
}
/**
 * Schema applied to each item in the array.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationArrayAnyOfFieldItems".
 */
export interface UIElicitationArrayAnyOfFieldItems {
  /**
   * Selectable options, each with a value and a display label.
   */
  anyOf: UIElicitationArrayAnyOfFieldItemsAnyOf[];
}
/**
 * Schema for the `UIElicitationArrayAnyOfFieldItemsAnyOf` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationArrayAnyOfFieldItemsAnyOf".
 */
export interface UIElicitationArrayAnyOfFieldItemsAnyOf {
  /**
   * Value submitted when this option is selected.
   */
  const: string;
  /**
   * Display label for this option.
   */
  title: string;
}
/**
 * Multi-select string field whose allowed values are defined inline.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationArrayEnumField".
 */
export interface UIElicitationArrayEnumField {
  /**
   * Type discriminator. Always "array".
   */
  type: "array";
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Minimum number of items the user must select.
   */
  minItems?: number;
  /**
   * Maximum number of items the user may select.
   */
  maxItems?: number;
  items: UIElicitationArrayEnumFieldItems;
  /**
   * Default values selected when the form is first shown.
   */
  default?: string[];
}
/**
 * Schema applied to each item in the array.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationArrayEnumFieldItems".
 */
export interface UIElicitationArrayEnumFieldItems {
  /**
   * Type discriminator. Always "string".
   */
  type: "string";
  /**
   * Allowed string values for each selected item.
   */
  enum: string[];
}
/**
 * Prompt message and JSON schema describing the form fields to elicit from the user.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationRequest".
 */
export interface UIElicitationRequest {
  /**
   * Message describing what information is needed from the user
   */
  message: string;
  requestedSchema: UIElicitationSchema;
}
/**
 * JSON Schema describing the form fields to present to the user
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchema".
 */
export interface UIElicitationSchema {
  /**
   * Schema type indicator (always 'object')
   */
  type: "object";
  /**
   * Form field definitions, keyed by field name
   */
  properties: {
    [k: string]: UIElicitationSchemaProperty;
  };
  /**
   * List of required field names
   */
  required?: string[];
}
/**
 * Single-select string field whose allowed values are defined inline.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationStringEnumField".
 */
export interface UIElicitationStringEnumField {
  /**
   * Type discriminator. Always "string".
   */
  type: "string";
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Allowed string values.
   */
  enum: string[];
  /**
   * Optional display labels for each enum value, in the same order as `enum`.
   */
  enumNames?: string[];
  /**
   * Default value selected when the form is first shown.
   */
  default?: string;
}
/**
 * Single-select string field where each option pairs a value with a display label.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationStringOneOfField".
 */
export interface UIElicitationStringOneOfField {
  /**
   * Type discriminator. Always "string".
   */
  type: "string";
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Selectable options, each with a value and a display label.
   */
  oneOf: UIElicitationStringOneOfFieldOneOf[];
  /**
   * Default value selected when the form is first shown.
   */
  default?: string;
}
/**
 * Schema for the `UIElicitationStringOneOfFieldOneOf` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationStringOneOfFieldOneOf".
 */
export interface UIElicitationStringOneOfFieldOneOf {
  /**
   * Value submitted when this option is selected.
   */
  const: string;
  /**
   * Display label for this option.
   */
  title: string;
}
/**
 * Boolean field rendered as a yes/no toggle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaPropertyBoolean".
 */
export interface UIElicitationSchemaPropertyBoolean {
  /**
   * Type discriminator. Always "boolean".
   */
  type: "boolean";
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Default value selected when the form is first shown.
   */
  default?: boolean;
}
/**
 * Free-text string field with optional length and format constraints.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaPropertyString".
 */
export interface UIElicitationSchemaPropertyString {
  /**
   * Type discriminator. Always "string".
   */
  type: "string";
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Minimum number of characters required.
   */
  minLength?: number;
  /**
   * Maximum number of characters allowed.
   */
  maxLength?: number;
  format?: UIElicitationSchemaPropertyStringFormat;
  /**
   * Default value populated in the input when the form is first shown.
   */
  default?: string;
}
/**
 * Numeric field accepting either a number or an integer.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaPropertyNumber".
 */
export interface UIElicitationSchemaPropertyNumber {
  type: UIElicitationSchemaPropertyNumberType;
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Minimum allowed value (inclusive).
   */
  minimum?: number;
  /**
   * Maximum allowed value (inclusive).
   */
  maximum?: number;
  /**
   * Default value populated in the input when the form is first shown.
   */
  default?: number;
}
/**
 * The elicitation response (accept with form values, decline, or cancel)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationResponse".
 */
export interface UIElicitationResponse {
  action: UIElicitationResponseAction;
  content?: UIElicitationResponseContent;
}
/**
 * The form values submitted by the user (present when action is 'accept')
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationResponseContent".
 */
export interface UIElicitationResponseContent {
  [k: string]: UIElicitationFieldValue;
}
/**
 * Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationResult".
 */
export interface UIElicitationResult {
  /**
   * Whether the response was accepted. False if the request was already resolved by another client.
   */
  success: boolean;
}
/**
 * Pending elicitation request ID and the user's response (accept/decline/cancel + form values).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingElicitationRequest".
 */
export interface UIHandlePendingElicitationRequest {
  /**
   * The unique request ID from the elicitation.requested event
   */
  requestId: string;
  result: UIElicitationResponse;
}
/**
 * Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageGetMetricsResult".
 */
/** @experimental */
export interface UsageGetMetricsResult {
  /**
   * Total user-initiated premium request cost across all models (may be fractional due to multipliers)
   */
  totalPremiumRequestCost: number;
  /**
   * Raw count of user-initiated API requests
   */
  totalUserRequests: number;
  /**
   * Session-wide accumulated nano-AI units cost
   */
  totalNanoAiu?: number;
  /**
   * Session-wide per-token-type accumulated token counts
   */
  tokenDetails?: {
    [k: string]: UsageMetricsTokenDetail;
  };
  /**
   * Total time spent in model API calls (milliseconds)
   */
  totalApiDurationMs: number;
  /**
   * Session start timestamp (epoch milliseconds)
   */
  sessionStartTime: number;
  codeChanges: UsageMetricsCodeChanges;
  /**
   * Per-model token and request metrics, keyed by model identifier
   */
  modelMetrics: {
    [k: string]: UsageMetricsModelMetric;
  };
  /**
   * Currently active model identifier
   */
  currentModel?: string;
  /**
   * Input tokens from the most recent main-agent API call
   */
  lastCallInputTokens: number;
  /**
   * Output tokens from the most recent main-agent API call
   */
  lastCallOutputTokens: number;
}
/**
 * Schema for the `UsageMetricsTokenDetail` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsTokenDetail".
 */
export interface UsageMetricsTokenDetail {
  /**
   * Accumulated token count for this token type
   */
  tokenCount: number;
}
/**
 * Aggregated code change metrics
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsCodeChanges".
 */
export interface UsageMetricsCodeChanges {
  /**
   * Total lines of code added
   */
  linesAdded: number;
  /**
   * Total lines of code removed
   */
  linesRemoved: number;
  /**
   * Number of distinct files modified
   */
  filesModifiedCount: number;
}
/**
 * Schema for the `UsageMetricsModelMetric` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsModelMetric".
 */
export interface UsageMetricsModelMetric {
  requests: UsageMetricsModelMetricRequests;
  usage: UsageMetricsModelMetricUsage;
  /**
   * Accumulated nano-AI units cost for this model
   */
  totalNanoAiu?: number;
  /**
   * Token count details per type
   */
  tokenDetails?: {
    [k: string]: UsageMetricsModelMetricTokenDetail;
  };
}
/**
 * Request count and cost metrics for this model
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsModelMetricRequests".
 */
export interface UsageMetricsModelMetricRequests {
  /**
   * Number of API requests made with this model
   */
  count: number;
  /**
   * User-initiated premium request cost (with multiplier applied)
   */
  cost: number;
}
/**
 * Token usage metrics for this model
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsModelMetricUsage".
 */
export interface UsageMetricsModelMetricUsage {
  /**
   * Total input tokens consumed
   */
  inputTokens: number;
  /**
   * Total output tokens produced
   */
  outputTokens: number;
  /**
   * Total tokens read from prompt cache
   */
  cacheReadTokens: number;
  /**
   * Total tokens written to prompt cache
   */
  cacheWriteTokens: number;
  /**
   * Total output tokens used for reasoning
   */
  reasoningTokens?: number;
}
/**
 * Schema for the `UsageMetricsModelMetricTokenDetail` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsModelMetricTokenDetail".
 */
export interface UsageMetricsModelMetricTokenDetail {
  /**
   * Accumulated token count for this token type
   */
  tokenCount: number;
}
/**
 * Relative path and UTF-8 content for the workspace file to create or overwrite.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesCreateFileRequest".
 */
export interface WorkspacesCreateFileRequest {
  /**
   * Relative path within the workspace files directory
   */
  path: string;
  /**
   * File content to write as a UTF-8 string
   */
  content: string;
}
/**
 * Current workspace metadata for the session, or null when not available.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesGetWorkspaceResult".
 */
export interface WorkspacesGetWorkspaceResult {
  /**
   * Current workspace metadata, or null if not available
   */
  workspace: {
    id: string;
    cwd?: string;
    git_root?: string;
    repository?: string;
    host_type?: "github" | "ado";
    branch?: string;
    name?: string;
    user_named?: boolean;
    summary_count?: number;
    created_at?: string;
    updated_at?: string;
    remote_steerable?: boolean;
    mc_task_id?: string;
    mc_session_id?: string;
    mc_last_event_id?: string;
    chronicle_sync_dismissed?: boolean;
  } | null;
}
/**
 * Relative paths of files stored in the session workspace files directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesListFilesResult".
 */
export interface WorkspacesListFilesResult {
  /**
   * Relative file paths in the workspace files directory
   */
  files: string[];
}
/**
 * Relative path of the workspace file to read.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesReadFileRequest".
 */
export interface WorkspacesReadFileRequest {
  /**
   * Relative path within the workspace files directory
   */
  path: string;
}
/**
 * Contents of the requested workspace file as a UTF-8 string.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesReadFileResult".
 */
export interface WorkspacesReadFileResult {
  /**
   * File content as a UTF-8 string
   */
  content: string;
}
/**
 * Identifies the target session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteExistsRequest".
 */
export interface SessionFsSqliteExistsRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** Create typed server-scoped RPC methods (no session required). */
export function createServerRpc(connection: MessageConnection) {
    return {
        ping: async (params: PingRequest): Promise<PingResult> =>
            connection.sendRequest("ping", params),
        models: {
            list: async (params: ModelsListRequest): Promise<ModelList> =>
                connection.sendRequest("models.list", params),
        },
        tools: {
            list: async (params: ToolsListRequest): Promise<ToolList> =>
                connection.sendRequest("tools.list", params),
        },
        account: {
            getQuota: async (params: AccountGetQuotaRequest): Promise<AccountGetQuotaResult> =>
                connection.sendRequest("account.getQuota", params),
        },
        mcp: {
            config: {
                list: async (): Promise<McpConfigList> =>
                    connection.sendRequest("mcp.config.list", {}),
                add: async (params: McpConfigAddRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.add", params),
                update: async (params: McpConfigUpdateRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.update", params),
                remove: async (params: McpConfigRemoveRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.remove", params),
                enable: async (params: McpConfigEnableRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.enable", params),
                disable: async (params: McpConfigDisableRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.disable", params),
            },
            discover: async (params: McpDiscoverRequest): Promise<McpDiscoverResult> =>
                connection.sendRequest("mcp.discover", params),
        },
        skills: {
            config: {
                setDisabledSkills: async (params: SkillsConfigSetDisabledSkillsRequest): Promise<void> =>
                    connection.sendRequest("skills.config.setDisabledSkills", params),
            },
            discover: async (params: SkillsDiscoverRequest): Promise<ServerSkillList> =>
                connection.sendRequest("skills.discover", params),
        },
        sessionFs: {
            setProvider: async (params: SessionFsSetProviderRequest): Promise<SessionFsSetProviderResult> =>
                connection.sendRequest("sessionFs.setProvider", params),
        },
        /** @experimental */
        sessions: {
            fork: async (params: SessionsForkRequest): Promise<SessionsForkResult> =>
                connection.sendRequest("sessions.fork", params),
        },
    };
}

/**
 * Create typed server-scoped RPC methods that are part of the SDK's internal
 * surface (e.g. handshake helpers). Not exported on the public client API.
 * @internal
 */
export function createInternalServerRpc(connection: MessageConnection) {
    return {
        connect: async (params: ConnectRequest): Promise<ConnectResult> =>
            connection.sendRequest("connect", params),
    };
}

/** Create typed session-scoped RPC methods. */
export function createSessionRpc(connection: MessageConnection, sessionId: string) {
    return {
        suspend: async (): Promise<void> =>
            connection.sendRequest("session.suspend", { sessionId }),
        auth: {
            getStatus: async (): Promise<SessionAuthStatus> =>
                connection.sendRequest("session.auth.getStatus", { sessionId }),
        },
        model: {
            getCurrent: async (): Promise<CurrentModel> =>
                connection.sendRequest("session.model.getCurrent", { sessionId }),
            switchTo: async (params: ModelSwitchToRequest): Promise<ModelSwitchToResult> =>
                connection.sendRequest("session.model.switchTo", { sessionId, ...params }),
        },
        mode: {
            get: async (): Promise<SessionMode> =>
                connection.sendRequest("session.mode.get", { sessionId }),
            set: async (params: ModeSetRequest): Promise<void> =>
                connection.sendRequest("session.mode.set", { sessionId, ...params }),
        },
        name: {
            get: async (): Promise<NameGetResult> =>
                connection.sendRequest("session.name.get", { sessionId }),
            set: async (params: NameSetRequest): Promise<void> =>
                connection.sendRequest("session.name.set", { sessionId, ...params }),
        },
        plan: {
            read: async (): Promise<PlanReadResult> =>
                connection.sendRequest("session.plan.read", { sessionId }),
            update: async (params: PlanUpdateRequest): Promise<void> =>
                connection.sendRequest("session.plan.update", { sessionId, ...params }),
            delete: async (): Promise<void> =>
                connection.sendRequest("session.plan.delete", { sessionId }),
        },
        workspaces: {
            getWorkspace: async (): Promise<WorkspacesGetWorkspaceResult> =>
                connection.sendRequest("session.workspaces.getWorkspace", { sessionId }),
            listFiles: async (): Promise<WorkspacesListFilesResult> =>
                connection.sendRequest("session.workspaces.listFiles", { sessionId }),
            readFile: async (params: WorkspacesReadFileRequest): Promise<WorkspacesReadFileResult> =>
                connection.sendRequest("session.workspaces.readFile", { sessionId, ...params }),
            createFile: async (params: WorkspacesCreateFileRequest): Promise<void> =>
                connection.sendRequest("session.workspaces.createFile", { sessionId, ...params }),
        },
        instructions: {
            getSources: async (): Promise<InstructionsGetSourcesResult> =>
                connection.sendRequest("session.instructions.getSources", { sessionId }),
        },
        /** @experimental */
        fleet: {
            start: async (params: FleetStartRequest): Promise<FleetStartResult> =>
                connection.sendRequest("session.fleet.start", { sessionId, ...params }),
        },
        /** @experimental */
        agent: {
            list: async (): Promise<AgentList> =>
                connection.sendRequest("session.agent.list", { sessionId }),
            getCurrent: async (): Promise<AgentGetCurrentResult> =>
                connection.sendRequest("session.agent.getCurrent", { sessionId }),
            select: async (params: AgentSelectRequest): Promise<AgentSelectResult> =>
                connection.sendRequest("session.agent.select", { sessionId, ...params }),
            deselect: async (): Promise<void> =>
                connection.sendRequest("session.agent.deselect", { sessionId }),
            reload: async (): Promise<AgentReloadResult> =>
                connection.sendRequest("session.agent.reload", { sessionId }),
        },
        /** @experimental */
        tasks: {
            startAgent: async (params: TasksStartAgentRequest): Promise<TasksStartAgentResult> =>
                connection.sendRequest("session.tasks.startAgent", { sessionId, ...params }),
            list: async (): Promise<TaskList> =>
                connection.sendRequest("session.tasks.list", { sessionId }),
            promoteToBackground: async (params: TasksPromoteToBackgroundRequest): Promise<TasksPromoteToBackgroundResult> =>
                connection.sendRequest("session.tasks.promoteToBackground", { sessionId, ...params }),
            cancel: async (params: TasksCancelRequest): Promise<TasksCancelResult> =>
                connection.sendRequest("session.tasks.cancel", { sessionId, ...params }),
            remove: async (params: TasksRemoveRequest): Promise<TasksRemoveResult> =>
                connection.sendRequest("session.tasks.remove", { sessionId, ...params }),
            sendMessage: async (params: TasksSendMessageRequest): Promise<TasksSendMessageResult> =>
                connection.sendRequest("session.tasks.sendMessage", { sessionId, ...params }),
        },
        /** @experimental */
        skills: {
            list: async (): Promise<SkillList> =>
                connection.sendRequest("session.skills.list", { sessionId }),
            enable: async (params: SkillsEnableRequest): Promise<void> =>
                connection.sendRequest("session.skills.enable", { sessionId, ...params }),
            disable: async (params: SkillsDisableRequest): Promise<void> =>
                connection.sendRequest("session.skills.disable", { sessionId, ...params }),
            reload: async (): Promise<SkillsLoadDiagnostics> =>
                connection.sendRequest("session.skills.reload", { sessionId }),
        },
        /** @experimental */
        mcp: {
            list: async (): Promise<McpServerList> =>
                connection.sendRequest("session.mcp.list", { sessionId }),
            enable: async (params: McpEnableRequest): Promise<void> =>
                connection.sendRequest("session.mcp.enable", { sessionId, ...params }),
            disable: async (params: McpDisableRequest): Promise<void> =>
                connection.sendRequest("session.mcp.disable", { sessionId, ...params }),
            reload: async (): Promise<void> =>
                connection.sendRequest("session.mcp.reload", { sessionId }),
            /** @experimental */
            oauth: {
                login: async (params: McpOauthLoginRequest): Promise<McpOauthLoginResult> =>
                    connection.sendRequest("session.mcp.oauth.login", { sessionId, ...params }),
            },
        },
        /** @experimental */
        plugins: {
            list: async (): Promise<PluginList> =>
                connection.sendRequest("session.plugins.list", { sessionId }),
        },
        /** @experimental */
        extensions: {
            list: async (): Promise<ExtensionList> =>
                connection.sendRequest("session.extensions.list", { sessionId }),
            enable: async (params: ExtensionsEnableRequest): Promise<void> =>
                connection.sendRequest("session.extensions.enable", { sessionId, ...params }),
            disable: async (params: ExtensionsDisableRequest): Promise<void> =>
                connection.sendRequest("session.extensions.disable", { sessionId, ...params }),
            reload: async (): Promise<void> =>
                connection.sendRequest("session.extensions.reload", { sessionId }),
        },
        tools: {
            handlePendingToolCall: async (params: HandlePendingToolCallRequest): Promise<HandlePendingToolCallResult> =>
                connection.sendRequest("session.tools.handlePendingToolCall", { sessionId, ...params }),
        },
        commands: {
            list: async (params?: CommandsListRequest): Promise<CommandList> =>
                connection.sendRequest("session.commands.list", { sessionId, ...params }),
            invoke: async (params: CommandsInvokeRequest): Promise<SlashCommandInvocationResult> =>
                connection.sendRequest("session.commands.invoke", { sessionId, ...params }),
            handlePendingCommand: async (params: CommandsHandlePendingCommandRequest): Promise<CommandsHandlePendingCommandResult> =>
                connection.sendRequest("session.commands.handlePendingCommand", { sessionId, ...params }),
            respondToQueuedCommand: async (params: CommandsRespondToQueuedCommandRequest): Promise<CommandsRespondToQueuedCommandResult> =>
                connection.sendRequest("session.commands.respondToQueuedCommand", { sessionId, ...params }),
        },
        ui: {
            elicitation: async (params: UIElicitationRequest): Promise<UIElicitationResponse> =>
                connection.sendRequest("session.ui.elicitation", { sessionId, ...params }),
            handlePendingElicitation: async (params: UIHandlePendingElicitationRequest): Promise<UIElicitationResult> =>
                connection.sendRequest("session.ui.handlePendingElicitation", { sessionId, ...params }),
        },
        permissions: {
            handlePendingPermissionRequest: async (params: PermissionDecisionRequest): Promise<PermissionRequestResult> =>
                connection.sendRequest("session.permissions.handlePendingPermissionRequest", { sessionId, ...params }),
            setApproveAll: async (params: PermissionsSetApproveAllRequest): Promise<PermissionsSetApproveAllResult> =>
                connection.sendRequest("session.permissions.setApproveAll", { sessionId, ...params }),
            resetSessionApprovals: async (): Promise<PermissionsResetSessionApprovalsResult> =>
                connection.sendRequest("session.permissions.resetSessionApprovals", { sessionId }),
        },
        log: async (params: LogRequest): Promise<LogResult> =>
            connection.sendRequest("session.log", { sessionId, ...params }),
        shell: {
            exec: async (params: ShellExecRequest): Promise<ShellExecResult> =>
                connection.sendRequest("session.shell.exec", { sessionId, ...params }),
            kill: async (params: ShellKillRequest): Promise<ShellKillResult> =>
                connection.sendRequest("session.shell.kill", { sessionId, ...params }),
        },
        /** @experimental */
        history: {
            compact: async (): Promise<HistoryCompactResult> =>
                connection.sendRequest("session.history.compact", { sessionId }),
            truncate: async (params: HistoryTruncateRequest): Promise<HistoryTruncateResult> =>
                connection.sendRequest("session.history.truncate", { sessionId, ...params }),
        },
        /** @experimental */
        usage: {
            getMetrics: async (): Promise<UsageGetMetricsResult> =>
                connection.sendRequest("session.usage.getMetrics", { sessionId }),
        },
        /** @experimental */
        remote: {
            enable: async (params: RemoteEnableRequest): Promise<RemoteEnableResult> =>
                connection.sendRequest("session.remote.enable", { sessionId, ...params }),
            disable: async (): Promise<void> =>
                connection.sendRequest("session.remote.disable", { sessionId }),
        },
    };
}

/** Handler for `sessionFs` client session API methods. */
export interface SessionFsHandler {
    readFile(params: SessionFsReadFileRequest): Promise<SessionFsReadFileResult>;
    writeFile(params: SessionFsWriteFileRequest): Promise<SessionFsError | undefined>;
    appendFile(params: SessionFsAppendFileRequest): Promise<SessionFsError | undefined>;
    exists(params: SessionFsExistsRequest): Promise<SessionFsExistsResult>;
    stat(params: SessionFsStatRequest): Promise<SessionFsStatResult>;
    mkdir(params: SessionFsMkdirRequest): Promise<SessionFsError | undefined>;
    readdir(params: SessionFsReaddirRequest): Promise<SessionFsReaddirResult>;
    readdirWithTypes(params: SessionFsReaddirWithTypesRequest): Promise<SessionFsReaddirWithTypesResult>;
    rm(params: SessionFsRmRequest): Promise<SessionFsError | undefined>;
    rename(params: SessionFsRenameRequest): Promise<SessionFsError | undefined>;
    sqliteQuery(params: SessionFsSqliteQueryRequest): Promise<SessionFsSqliteQueryResult>;
    sqliteExists(params: SessionFsSqliteExistsRequest): Promise<SessionFsSqliteExistsResult>;
}

/** All client session API handler groups. */
export interface ClientSessionApiHandlers {
    sessionFs?: SessionFsHandler;
}

/**
 * Register client session API handlers on a JSON-RPC connection.
 * The server calls these methods to delegate work to the client.
 * Each incoming call includes a `sessionId` in the params; the registration
 * function uses `getHandlers` to resolve the session's handlers.
 */
export function registerClientSessionApiHandlers(
    connection: MessageConnection,
    getHandlers: (sessionId: string) => ClientSessionApiHandlers,
): void {
    connection.onRequest("sessionFs.readFile", async (params: SessionFsReadFileRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.readFile(params);
    });
    connection.onRequest("sessionFs.writeFile", async (params: SessionFsWriteFileRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.writeFile(params);
    });
    connection.onRequest("sessionFs.appendFile", async (params: SessionFsAppendFileRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.appendFile(params);
    });
    connection.onRequest("sessionFs.exists", async (params: SessionFsExistsRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.exists(params);
    });
    connection.onRequest("sessionFs.stat", async (params: SessionFsStatRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.stat(params);
    });
    connection.onRequest("sessionFs.mkdir", async (params: SessionFsMkdirRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.mkdir(params);
    });
    connection.onRequest("sessionFs.readdir", async (params: SessionFsReaddirRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.readdir(params);
    });
    connection.onRequest("sessionFs.readdirWithTypes", async (params: SessionFsReaddirWithTypesRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.readdirWithTypes(params);
    });
    connection.onRequest("sessionFs.rm", async (params: SessionFsRmRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.rm(params);
    });
    connection.onRequest("sessionFs.rename", async (params: SessionFsRenameRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.rename(params);
    });
    connection.onRequest("sessionFs.sqliteQuery", async (params: SessionFsSqliteQueryRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.sqliteQuery(params);
    });
    connection.onRequest("sessionFs.sqliteExists", async (params: SessionFsSqliteExistsRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.sqliteExists(params);
    });
}
