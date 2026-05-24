/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: api.schema.json
 */

import type { MessageConnection } from "vscode-jsonrpc/node.js";

import type { AbortReason, EmbeddedBlobResourceContents, EmbeddedTextResourceContents, McpServerSource, McpServerStatus, PermissionPromptRequest, PermissionRule, ReasoningSummary, SessionEvent, SessionMode, ShutdownType, SkillSource, UserToolSessionApproval } from "./session-events.js";

/**
 * Where the agent definition was loaded from
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentInfoSource".
 */
/** @experimental */
export type AgentInfoSource =
  /** Agent loaded from the user's personal agent configuration. */
  | "user"
  /** Agent loaded from the current project's repository configuration. */
  | "project"
  /** Agent inherited from a parent project or workspace. */
  | "inherited"
  /** Agent provided by a remote runtime or service. */
  | "remote"
  /** Agent contributed by an installed plugin. */
  | "plugin"
  /** Agent built into the Copilot runtime. */
  | "builtin";
/**
 * The new auth credentials to install on the session. When omitted or `undefined`, the call is a no-op and the session's existing credentials are preserved. The runtime stores the value verbatim and uses it for outbound model/API requests; it does NOT re-validate or re-fetch the associated Copilot user response. Several variants carry secret material; treat this method's params as containing secrets at rest and in transit.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AuthInfo".
 */
/** @experimental */
export type AuthInfo =
  | HMACAuthInfo
  | EnvAuthInfo
  | TokenAuthInfo
  | CopilotApiTokenAuthInfo
  | UserAuthInfo
  | GhCliAuthInfo
  | ApiKeyAuthInfo;
/**
 * Authentication type
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AuthInfoType".
 */
/** @experimental */
export type AuthInfoType =
  /** Authentication provided by a GitHub App HMAC credential. */
  | "hmac"
  /** Authentication resolved from environment-provided credentials. */
  | "env"
  /** Authentication from an interactive user sign-in. */
  | "user"
  /** Authentication delegated to the GitHub CLI. */
  | "gh-cli"
  /** Authentication from an API key credential. */
  | "api-key"
  /** Authentication from a GitHub token. */
  | "token"
  /** Authentication from a Copilot API token. */
  | "copilot-api-token";
/**
 * Runtime-controlled routing state for an open canvas instance.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasInstanceAvailability".
 */
/** @experimental */
export type CanvasInstanceAvailability =
  /** The owning provider is currently connected and routing calls will be dispatched normally. */
  | "ready"
  /** The owning provider is not currently connected. Routing calls fail with canvas_provider_unavailable until the agent re-issues open_canvas (which rehydrates via a fresh canvas.open) or the provider reconnects. */
  | "stale";
/**
 * Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandKind".
 */
/** @experimental */
export type SlashCommandKind =
  /** Command implemented by the runtime. */
  | "builtin"
  /** Command backed by a skill. */
  | "skill"
  /** Command registered by an SDK client or extension. */
  | "client";
/**
 * Optional completion hint for the input (e.g. 'directory' for filesystem path completion)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandInputCompletion".
 */
/** @experimental */
export type SlashCommandInputCompletion = /** Input should complete filesystem directories. */ "directory";
/**
 * Result of the queued command execution.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuedCommandResult".
 */
/** @experimental */
export type QueuedCommandResult = QueuedCommandHandled | QueuedCommandNotHandled;
/**
 * Neutral SDK discriminator for the connected remote session kind.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectedRemoteSessionMetadataKind".
 */
/** @experimental */
export type ConnectedRemoteSessionMetadataKind =
  /** Remote CLI session. */
  | "remote-session"
  /** GitHub Copilot coding agent session. */
  | "coding-agent";
/**
 * Controls how MCP tool result content is filtered: none leaves content unchanged, markdown sanitizes HTML while preserving Markdown-friendly output, and hidden_characters removes characters that can hide directives.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ContentFilterMode".
 */
export type ContentFilterMode =
  /** Leave MCP tool result content unchanged. */
  | "none"
  /** Sanitize HTML while preserving Markdown-friendly output. */
  | "markdown"
  /** Remove characters that can hide directives. */
  | "hidden_characters";
/**
 * Server transport type: stdio, http, sse (deprecated), or memory
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredMcpServerType".
 */
export type DiscoveredMcpServerType =
  /** Server communicates over stdio with a local child process. */
  | "stdio"
  /** Server communicates over streamable HTTP. */
  | "http"
  /** Server communicates over Server-Sent Events (deprecated). */
  | "sse"
  /** Server is backed by an in-memory runtime implementation. */
  | "memory";
/**
 * Either '*' to receive all event types, or a non-empty list of event types to receive
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventLogTypes".
 */
/** @experimental */
export type EventLogTypes = "*" | [string, ...string[]];
/**
 * Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventsAgentScope".
 */
/** @experimental */
export type EventsAgentScope =
  /** Return main-agent events and typed subagent lifecycle events. */
  | "primary"
  /** Return events from all agents. */
  | "all";
/**
 * Cursor status: 'ok' means the cursor was applied successfully; 'expired' means the cursor referred to an event that no longer exists in history (e.g. truncated or compacted away) and the read started from the beginning of the remaining history.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventsCursorStatus".
 */
/** @experimental */
export type EventsCursorStatus =
  /** The cursor was applied successfully. */
  | "ok"
  /** The cursor referred to history that is no longer available. */
  | "expired";
/**
 * Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionSource".
 */
/** @experimental */
export type ExtensionSource =
  /** Extension discovered from the current project's .github/extensions directory. */
  | "project"
  /** Extension discovered from the user's ~/.copilot/extensions directory. */
  | "user";
/**
 * Current status: running, disabled, failed, or starting
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionStatus".
 */
/** @experimental */
export type ExtensionStatus =
  /** The extension process is running. */
  | "running"
  /** The extension is installed but disabled. */
  | "disabled"
  /** The extension failed to start or crashed. */
  | "failed"
  /** The extension process is starting. */
  | "starting";
/**
 * Tool call result (string or expanded result object)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolResult".
 */
/** @experimental */
export type ExternalToolResult = string | ExternalToolTextResultForLlm;
/**
 * Binary result type discriminator. Use "image" for images and "resource" for other binary data.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmBinaryResultsForLlmType".
 */
/** @experimental */
export type ExternalToolTextResultForLlmBinaryResultsForLlmType =
  /** Binary image data. */
  | "image"
  /** Other binary resource data. */
  | "resource";
/**
 * A content block within a tool result, which may be text, terminal output, image, audio, or a resource
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContent".
 */
/** @experimental */
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
/** @experimental */
export type ExternalToolTextResultForLlmContentResourceLinkIconTheme =
  /** Icon intended for light themes. */
  | "light"
  /** Icon intended for dark themes. */
  | "dark";
/**
 * The embedded resource contents, either text or base64-encoded binary
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentResourceDetails".
 */
/** @experimental */
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
      [k: string]: ContentFilterMode;
    }
  | ContentFilterMode;
/**
 * Source for direct repo installs (when marketplace is empty)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstalledPluginSource".
 */
/** @experimental */
export type InstalledPluginSource =
  | string
  | InstalledPluginSourceGithub
  | InstalledPluginSourceUrl
  | InstalledPluginSourceLocal;
/**
 * Category of instruction source — used for merge logic
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionsSourcesType".
 */
/** @experimental */
export type InstructionsSourcesType =
  /** Instructions loaded from the user's home configuration. */
  | "home"
  /** Instructions loaded from repository-scoped files. */
  | "repo"
  /** Instructions loaded from model-specific files. */
  | "model"
  /** Instructions loaded from VS Code instruction files. */
  | "vscode"
  /** Instructions discovered from nested agent files. */
  | "nested-agents"
  /** Instructions inherited from child instruction files. */
  | "child-instructions"
  /** Instructions supplied by an installed plugin. */
  | "plugin";
/**
 * Where this source lives — used for UI grouping
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionsSourcesLocation".
 */
/** @experimental */
export type InstructionsSourcesLocation =
  /** Instructions live in user-level configuration. */
  | "user"
  /** Instructions live in repository-level configuration. */
  | "repository"
  /** Instructions live under the current working directory. */
  | "working-directory"
  /** Instructions live in plugin-provided configuration. */
  | "plugin";
/**
 * Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLogLevel".
 */
/** @experimental */
export type SessionLogLevel =
  /** Informational message. */
  | "info"
  /** Warning message that may require attention. */
  | "warning"
  /** Error message describing a failure. */
  | "error";
/**
 * UI theme preference per SEP-1865
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsHostContextDetailsTheme".
 */
/** @experimental */
export type McpAppsHostContextDetailsTheme =
  /** Light UI theme */
  | "light"
  /** Dark UI theme */
  | "dark";
/**
 * Current display mode (SEP-1865)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsHostContextDetailsDisplayMode".
 */
/** @experimental */
export type McpAppsHostContextDetailsDisplayMode =
  /** Rendered inline within the host conversation surface */
  | "inline"
  /** Rendered as a fullscreen overlay */
  | "fullscreen"
  /** Rendered as a picture-in-picture floating panel */
  | "pip";
/**
 * Allowed values for the `McpAppsHostContextDetailsAvailableDisplayMode` enumeration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsHostContextDetailsAvailableDisplayMode".
 */
/** @experimental */
export type McpAppsHostContextDetailsAvailableDisplayMode =
  /** Rendered inline within the host conversation surface */
  | "inline"
  /** Rendered as a fullscreen overlay */
  | "fullscreen"
  /** Rendered as a picture-in-picture floating panel */
  | "pip";
/**
 * Platform type for responsive design
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsHostContextDetailsPlatform".
 */
/** @experimental */
export type McpAppsHostContextDetailsPlatform =
  /** Host runs in a web browser */
  | "web"
  /** Host runs as a desktop application */
  | "desktop"
  /** Host runs on a mobile device */
  | "mobile";
/**
 * UI theme preference per SEP-1865
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsSetHostContextDetailsTheme".
 */
/** @experimental */
export type McpAppsSetHostContextDetailsTheme =
  /** Light UI theme */
  | "light"
  /** Dark UI theme */
  | "dark";
/**
 * Current display mode (SEP-1865)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsSetHostContextDetailsDisplayMode".
 */
/** @experimental */
export type McpAppsSetHostContextDetailsDisplayMode =
  /** Rendered inline within the host conversation surface */
  | "inline"
  /** Rendered as a fullscreen overlay */
  | "fullscreen"
  /** Rendered as a picture-in-picture floating panel */
  | "pip";
/**
 * Allowed values for the `McpAppsSetHostContextDetailsAvailableDisplayMode` enumeration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsSetHostContextDetailsAvailableDisplayMode".
 */
/** @experimental */
export type McpAppsSetHostContextDetailsAvailableDisplayMode =
  /** Rendered inline within the host conversation surface */
  | "inline"
  /** Rendered as a fullscreen overlay */
  | "fullscreen"
  /** Rendered as a picture-in-picture floating panel */
  | "pip";
/**
 * Platform type for responsive design
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsSetHostContextDetailsPlatform".
 */
/** @experimental */
export type McpAppsSetHostContextDetailsPlatform =
  /** Host runs in a web browser */
  | "web"
  /** Host runs as a desktop application */
  | "desktop"
  /** Host runs on a mobile device */
  | "mobile";
/**
 * MCP server configuration (stdio process or remote HTTP/SSE)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfig".
 */
export type McpServerConfig = McpServerConfigStdio | McpServerConfigHttp;
/**
 * Remote transport type. Defaults to "http" when omitted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigHttpType".
 */
export type McpServerConfigHttpType =
  /** Streamable HTTP transport. */
  | "http"
  /** Server-Sent Events transport. */
  | "sse";
/**
 * OAuth grant type to use when authenticating to the remote MCP server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigHttpOauthGrantType".
 */
export type McpServerConfigHttpOauthGrantType =
  /** Interactive browser-based authorization code flow with PKCE. */
  | "authorization_code"
  /** Headless client credentials flow using the configured OAuth client. */
  | "client_credentials";
/**
 * Outcome of the sampling inference. 'success' produced a response; 'failure' encountered an error (including agent-side rejection by content filter or criteria); 'cancelled' the caller cancelled this execution via cancelSamplingExecution.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSamplingExecutionAction".
 */
/** @experimental */
export type McpSamplingExecutionAction =
  /** The sampling inference completed and produced a result. */
  | "success"
  /** The sampling inference failed or was rejected. */
  | "failure"
  /** The sampling inference was cancelled before completion. */
  | "cancelled";
/**
 * How environment-variable values supplied to MCP servers are resolved. "direct" passes literal string values; "indirect" treats values as references (e.g. names of environment variables on the host) that the runtime resolves before launch. Defaults to the runtime's startup mode; clients that intentionally launch MCP servers with literal values (e.g. CLI prompt mode and ACP) set this to "direct".
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSetEnvValueModeDetails".
 */
/** @experimental */
export type McpSetEnvValueModeDetails =
  /** Treat MCP server environment values as literal strings. */
  | "direct"
  /** Treat MCP server environment values as host-side references to resolve before launch. */
  | "indirect";
/**
 * Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionContextInfo".
 */
/** @experimental */
export type SessionContextInfo = {
  /**
   * The model used for token counting
   */
  modelName: string;
  /**
   * Tokens consumed by the system prompt
   */
  systemTokens: number;
  /**
   * Tokens consumed by user/assistant/tool messages
   */
  conversationTokens: number;
  /**
   * Tokens consumed by tool definitions sent to the model (excludes deferred tools)
   */
  toolDefinitionsTokens: number;
  /**
   * Sum of system, conversation and tool-definition tokens
   */
  totalTokens: number;
  /**
   * Maximum prompt tokens allowed by the model (or DEFAULT_TOKEN_LIMIT if unspecified)
   */
  promptTokenLimit: number;
  /**
   * Token count at which background compaction starts (configurable percentage of promptTokenLimit)
   */
  compactionThreshold: number;
  /**
   * Total context limit for /context display. promptTokenLimit + min(32k or 64k, outputTokenLimit) depending on model.
   */
  limit: number;
  /**
   * Output reserve plus tokens after the buffer-exhaustion blocking threshold (default 95%)
   */
  bufferTokens: number;
} | null;
/**
 * Hosting platform type of the repository
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionWorkingDirectoryContextHostType".
 */
/** @experimental */
export type SessionWorkingDirectoryContextHostType =
  /** The working directory repository is hosted on GitHub. */
  | "github"
  /** The working directory repository is hosted on Azure DevOps. */
  | "ado";
/**
 * The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot')
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataSnapshotCurrentMode".
 */
/** @experimental */
export type MetadataSnapshotCurrentMode =
  /** The agent is responding interactively to the user. */
  | "interactive"
  /** The agent is preparing a plan before making changes. */
  | "plan"
  /** The agent is working autonomously toward task completion. */
  | "autopilot";
/**
 * Whether the remote task originated from Copilot Coding Agent (cca) or a CLI `--remote` invocation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataSnapshotRemoteMetadataTaskType".
 */
/** @experimental */
export type MetadataSnapshotRemoteMetadataTaskType =
  /** Remote task originated from Copilot Coding Agent. */
  | "cca"
  /** Remote task originated from a CLI remote-session invocation. */
  | "cli";
/**
 * Current policy state for this model
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelPolicyState".
 */
export type ModelPolicyState =
  /** The model is enabled by policy. */
  | "enabled"
  /** The model is disabled by policy. */
  | "disabled"
  /** No explicit policy is configured for the model. */
  | "unconfigured";
/**
 * Model capability category for grouping in the model picker
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelPickerCategory".
 */
export type ModelPickerCategory =
  /** Lightweight model category optimized for faster, lower-cost interactions. */
  | "lightweight"
  /** Versatile model category suitable for a broad range of tasks. */
  | "versatile"
  /** Powerful model category optimized for complex tasks. */
  | "powerful";
/**
 * Relative cost tier for token-based billing users
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelPickerPriceCategory".
 */
export type ModelPickerPriceCategory =
  /** Lowest relative token cost tier. */
  | "low"
  /** Medium relative token cost tier. */
  | "medium"
  /** High relative token cost tier. */
  | "high"
  /** Highest relative token cost tier. */
  | "very_high";
/**
 * How env values are passed to MCP servers (`direct` inlines literal values; `indirect` resolves at launch).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "OptionsUpdateEnvValueMode".
 */
/** @experimental */
export type OptionsUpdateEnvValueMode =
  /** Pass MCP server environment values as literal strings. */
  | "direct"
  /** Resolve MCP server environment values from host-side references. */
  | "indirect";
/**
 * The client's response to the pending permission prompt
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecision".
 */
/** @experimental */
export type PermissionDecision =
  | PermissionDecisionApproveOnce
  | PermissionDecisionApproveForSession
  | PermissionDecisionApproveForLocation
  | PermissionDecisionApprovePermanently
  | PermissionDecisionReject
  | PermissionDecisionUserNotAvailable
  | PermissionDecisionApproved
  | PermissionDecisionApprovedForSession
  | PermissionDecisionApprovedForLocation
  | PermissionDecisionCancelled
  | PermissionDecisionDeniedByRules
  | PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser
  | PermissionDecisionDeniedInteractivelyByUser
  | PermissionDecisionDeniedByContentExclusionPolicy
  | PermissionDecisionDeniedByPermissionRequestHook;
/**
 * Session-scoped approval to remember (tool prompts only; omitted for path/url prompts)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApproval".
 */
/** @experimental */
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
 * Approval to persist for this location
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApproval".
 */
/** @experimental */
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
 * Tool approval to persist and apply
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetails".
 */
/** @experimental */
export type PermissionsLocationsAddToolApprovalDetails =
  | PermissionsLocationsAddToolApprovalDetailsCommands
  | PermissionsLocationsAddToolApprovalDetailsRead
  | PermissionsLocationsAddToolApprovalDetailsWrite
  | PermissionsLocationsAddToolApprovalDetailsMcp
  | PermissionsLocationsAddToolApprovalDetailsMcpSampling
  | PermissionsLocationsAddToolApprovalDetailsMemory
  | PermissionsLocationsAddToolApprovalDetailsCustomTool
  | PermissionsLocationsAddToolApprovalDetailsExtensionManagement
  | PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess;
/**
 * Whether the location is a git repo or directory
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionLocationType".
 */
/** @experimental */
export type PermissionLocationType =
  /** The permission location is persisted at the git repository root. */
  | "repo"
  /** The permission location is persisted at the working directory. */
  | "dir";
/**
 * Allowed values for the `PermissionsConfigureAdditionalContentExclusionPolicyScope` enumeration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsConfigureAdditionalContentExclusionPolicyScope".
 */
/** @experimental */
export type PermissionsConfigureAdditionalContentExclusionPolicyScope =
  /** The content exclusion policy applies to the current repository. */
  | "repo"
  /** The content exclusion policy applies across all repositories. */
  | "all";
/**
 * Whether the change applies to ephemeral session-scoped rules (cleared at session end) or to location-scoped rules persisted via the location-permissions config file.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsModifyRulesScope".
 */
/** @experimental */
export type PermissionsModifyRulesScope =
  /** Apply the rule change only to this session. */
  | "session"
  /** Persist the rule change for this project location. */
  | "location";
/**
 * Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetApproveAllSource".
 */
/** @experimental */
export type PermissionsSetApproveAllSource =
  /** Allow-all was enabled from a CLI command-line flag. */
  | "cli_flag"
  /** Allow-all was enabled by a slash command. */
  | "slash_command"
  /** Allow-all was enabled by confirming autopilot behavior. */
  | "autopilot_confirmation"
  /** Allow-all was enabled through an RPC caller. */
  | "rpc";
/**
 * Whether this item is a queued user message or a queued slash command / model change
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuePendingItemsKind".
 */
/** @experimental */
export type QueuePendingItemsKind =
  /** A queued user message. */
  | "message"
  /** A queued slash command or model-change command. */
  | "command";
/**
 * Per-session remote mode. "off" disables remote, "export" exports session events to GitHub without enabling remote steering, "on" enables both export and remote steering.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteSessionMode".
 */
/** @experimental */
export type RemoteSessionMode =
  /** Disable remote session export and steering. */
  | "off"
  /** Export session events to GitHub without enabling remote steering. */
  | "export"
  /** Enable both remote session export and remote steering. */
  | "on";
/**
 * The UI mode the agent was in when this message was sent. Defaults to the session's current mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAgentMode".
 */
/** @experimental */
export type SendAgentMode =
  /** The agent is responding interactively to the user. */
  | "interactive"
  /** The agent is preparing a plan before making changes. */
  | "plan"
  /** The agent is working autonomously toward task completion. */
  | "autopilot"
  /** The agent is in shell-focused UI mode. */
  | "shell";
/**
 * A user message attachment — a file, directory, code selection, blob, or GitHub reference
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAttachment".
 */
/** @experimental */
export type SendAttachment =
  | SendAttachmentFile
  | SendAttachmentDirectory
  | SendAttachmentSelection
  | SendAttachmentGithubReference
  | SendAttachmentBlob;
/**
 * Type of GitHub reference
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAttachmentGithubReferenceType".
 */
/** @experimental */
export type SendAttachmentGithubReferenceType =
  /** GitHub issue reference. */
  | "issue"
  /** GitHub pull request reference. */
  | "pr"
  /** GitHub discussion reference. */
  | "discussion";
/**
 * How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendMode".
 */
/** @experimental */
export type SendMode =
  /** Append the message to the normal session queue. */
  | "enqueue"
  /** Interject the message during the in-progress turn. */
  | "immediate";
/**
 * Repository host type
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionContextHostType".
 */
/** @experimental */
export type SessionContextHostType =
  /** Session repository is hosted on GitHub. */
  | "github"
  /** Session repository is hosted on Azure DevOps. */
  | "ado";
/**
 * Error classification
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsErrorCode".
 */
export type SessionFsErrorCode =
  /** The requested path does not exist. */
  | "ENOENT"
  /** The filesystem operation failed for an unspecified reason. */
  | "UNKNOWN";
/**
 * Entry type
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirWithTypesEntryType".
 */
export type SessionFsReaddirWithTypesEntryType =
  /** The entry is a file. */
  | "file"
  /** The entry is a directory. */
  | "directory";
/**
 * Path conventions used by this filesystem
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSetProviderConventions".
 */
export type SessionFsSetProviderConventions =
  /** Paths use Windows path conventions. */
  | "windows"
  /** Paths use POSIX path conventions. */
  | "posix";
/**
 * How to execute the query: 'exec' for DDL/multi-statement (no results), 'query' for SELECT (returns rows), 'run' for INSERT/UPDATE/DELETE (returns rowsAffected)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteQueryType".
 */
export type SessionFsSqliteQueryType =
  /** Execute DDL or multi-statement SQL without returning rows. */
  | "exec"
  /** Execute a SELECT-style query and return rows. */
  | "query"
  /** Execute INSERT, UPDATE, or DELETE SQL and return affected-row metadata. */
  | "run";
/**
 * Source descriptor for direct repo installs (when marketplace is empty)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionInstalledPluginSource".
 */
/** @experimental */
export type SessionInstalledPluginSource =
  | string
  | SessionInstalledPluginSourceGithub
  | SessionInstalledPluginSourceUrl
  | SessionInstalledPluginSourceLocal;
/**
 * Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspaceSummary".
 */
/** @experimental */
export type WorkspaceSummary = {
  /**
   * Workspace identifier (1:1 with sessionId)
   */
  id: string;
  /**
   * Current working directory at session start
   */
  cwd?: string;
  /**
   * Resolved git root for cwd, if any
   */
  git_root?: string;
  /**
   * Repository identifier in 'owner/repo' or 'org/project/repo' format, if any
   */
  repository?: string;
  host_type?: WorkspaceSummaryHostType;
  /**
   * Branch checked out at session start, if any
   */
  branch?: string;
  /**
   * Display name for the session, if set
   */
  name?: string;
  /**
   * ISO 8601 timestamp when the workspace was created
   */
  created_at?: string;
  /**
   * ISO 8601 timestamp when the workspace was last updated
   */
  updated_at?: string;
} | null;
/**
 * Repository host type, if known
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspaceSummaryHostType".
 */
/** @experimental */
export type WorkspaceSummaryHostType =
  /** Workspace summary repository is hosted on GitHub. */
  | "github"
  /** Workspace summary repository is hosted on Azure DevOps. */
  | "ado";
/**
 * Signal to send (default: SIGTERM)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellKillSignal".
 */
/** @experimental */
export type ShellKillSignal =
  /** Request graceful process termination. */
  | "SIGTERM"
  /** Forcefully terminate the process. */
  | "SIGKILL"
  /** Send an interrupt signal to the process. */
  | "SIGINT";
/**
 * Result of invoking the slash command (text output, prompt to send to the agent, or completion).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandInvocationResult".
 */
/** @experimental */
export type SlashCommandInvocationResult =
  | SlashCommandTextResult
  | SlashCommandAgentPromptResult
  | SlashCommandCompletedResult
  | SlashCommandSelectSubcommandResult;
/**
 * Current lifecycle status of the task
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskStatus".
 */
/** @experimental */
export type TaskStatus =
  /** The task is actively executing. */
  | "running"
  /** The task is waiting for additional input. */
  | "idle"
  /** The task finished successfully. */
  | "completed"
  /** The task finished with an error. */
  | "failed"
  /** The task was cancelled before completion. */
  | "cancelled";
/**
 * Whether task execution is synchronously awaited or managed in the background
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskExecutionMode".
 */
/** @experimental */
export type TaskExecutionMode =
  /** The task was started with synchronous waiting. */
  | "sync"
  /** The task is managed in the background. */
  | "background";
/**
 * Schema for the `TaskInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskInfo".
 */
/** @experimental */
export type TaskInfo = TaskAgentInfo | TaskShellInfo;
/**
 * Whether the shell runs inside a managed PTY session or as an independent background process
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskShellInfoAttachmentMode".
 */
/** @experimental */
export type TaskShellInfoAttachmentMode =
  /** The shell runs in a managed PTY session. */
  | "attached"
  /** The shell runs as an independent background process. */
  | "detached";
/**
 * Progress information for the task, discriminated by type. Returns null when no task with this ID is currently tracked.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskProgress".
 */
/** @experimental */
export type TaskProgress = (TaskAgentProgress | TaskShellProgress) | null;
/**
 * User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist as setting), or no (decline).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIAutoModeSwitchResponse".
 */
/** @experimental */
export type UIAutoModeSwitchResponse =
  /** Allow the automatic mode switch for this turn. */
  | "yes"
  /** Allow this mode switch and persist the preference. */
  | "yes_always"
  /** Decline the automatic mode switch. */
  | "no";
/**
 * Schema for the `UIElicitationFieldValue` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationFieldValue".
 */
/** @experimental */
export type UIElicitationFieldValue = string | number | boolean | string[];
/**
 * Definition for a single elicitation form field.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaProperty".
 */
/** @experimental */
export type UIElicitationSchemaProperty =
  | (
      | UIElicitationStringEnumField
      | UIElicitationStringOneOfField
      | UIElicitationArrayEnumField
      | UIElicitationArrayAnyOfField
      | UIElicitationSchemaPropertyBoolean
      | UIElicitationSchemaPropertyString
      | UIElicitationSchemaPropertyNumber
    )
  | undefined;
/**
 * Optional format hint that constrains the accepted input.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaPropertyStringFormat".
 */
/** @experimental */
export type UIElicitationSchemaPropertyStringFormat =
  /** Email address string format. */
  | "email"
  /** URI string format. */
  | "uri"
  /** Calendar date string format. */
  | "date"
  /** Date-time string format. */
  | "date-time";
/**
 * Numeric type accepted by the field.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaPropertyNumberType".
 */
/** @experimental */
export type UIElicitationSchemaPropertyNumberType =
  /** Any JSON number. */
  | "number"
  /** Integer JSON number. */
  | "integer";
/**
 * The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationResponseAction".
 */
/** @experimental */
export type UIElicitationResponseAction =
  /** The user submitted the requested form values. */
  | "accept"
  /** The user explicitly declined to provide the requested input. */
  | "decline"
  /** The user dismissed the elicitation request. */
  | "cancel";
/**
 * The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true, otherwise 'interactive'.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIExitPlanModeAction".
 */
/** @experimental */
export type UIExitPlanModeAction =
  /** Exit plan mode without starting implementation. */
  | "exit_only"
  /** Exit plan mode and continue interactively. */
  | "interactive"
  /** Exit plan mode and continue in autopilot mode. */
  | "autopilot"
  /** Exit plan mode and continue in autopilot mode with parallel subagent execution. */
  | "autopilot_fleet";
/**
 * Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesWorkspaceDetailsHostType".
 */
/** @experimental */
export type WorkspacesWorkspaceDetailsHostType =
  /** Workspace repository is hosted on GitHub. */
  | "github"
  /** Workspace repository is hosted on Azure DevOps. */
  | "ado";

/**
 * Parameters for aborting the current turn
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AbortRequest".
 */
/** @experimental */
export interface AbortRequest {
  reason?: AbortReason;
}
/**
 * Result of aborting the current turn
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AbortResult".
 */
/** @experimental */
export interface AbortResult {
  /**
   * Whether the abort completed successfully
   */
  success: boolean;
  /**
   * Error message if the abort failed
   */
  error?: string;
}

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
    [k: string]: AccountQuotaSnapshot | undefined;
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
   * Number of requests included in the entitlement, or -1 for unlimited entitlements
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
   * Number of additional usage requests made this period
   */
  overage: number;
  /**
   * Whether additional usage is allowed when quota is exhausted
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
/** @experimental */
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
  /**
   * Stable identifier for selection. For most agents this is the same as `name`; for plugin/builtin agents it may differ. Always populated; defaults to `name` when no distinct id was assigned.
   */
  id: string;
  source?: AgentInfoSource;
  /**
   * Whether the agent can be selected directly by the user. Agents marked `false` are subagent-only.
   */
  userInvocable?: boolean;
  /**
   * Allowed tool names for this agent. Empty array means none; omitted means inherit defaults.
   */
  tools?: string[];
  /**
   * Preferred model id for this agent. When omitted, inherits the outer agent's model.
   */
  model?: string;
  /**
   * MCP server configurations attached to this agent, keyed by server name. Server config shape mirrors the MCP `mcpServers` schema.
   *
   * @experimental
   */
  mcpServers?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Skill names preloaded into this agent's context. Omitted means none.
   */
  skills?: string[];
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
 * Schema for the `ApiKeyAuthInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ApiKeyAuthInfo".
 */
/** @experimental */
export interface ApiKeyAuthInfo {
  /**
   * API-key authentication for non-GitHub LLM providers (e.g. when running BYOM-style).
   */
  type: "api-key";
  /**
   * The API key. Treat as a secret.
   */
  apiKey: string;
  /**
   * Authentication host.
   */
  host: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotUserResponse".
 */
/** @experimental */
export interface CopilotUserResponse {
  login?: string;
  access_type_sku?: string;
  analytics_tracking_id?: string;
  assigned_date?:
    | (
        | {
            [k: string]: unknown | undefined;
          }
        | string
      )
    | null;
  can_signup_for_limited?: boolean;
  chat_enabled?: boolean;
  copilot_plan?: string;
  copilotignore_enabled?: boolean;
  endpoints?: CopilotUserResponseEndpoints;
  organization_login_list?: string[];
  organization_list?:
    | (
        | {
            [k: string]: unknown | undefined;
          }
        | ({
            login?:
              | (
                  | {
                      [k: string]: unknown | undefined;
                    }
                  | string
                )
              | null;
            name?:
              | (
                  | {
                      [k: string]: unknown | undefined;
                    }
                  | string
                )
              | null;
          } | null)[]
      )
    | null;
  codex_agent_enabled?: boolean;
  is_mcp_enabled?:
    | (
        | {
            [k: string]: unknown | undefined;
          }
        | boolean
      )
    | null;
  quota_reset_date?: string;
  quota_snapshots?: CopilotUserResponseQuotaSnapshots;
  restricted_telemetry?: boolean;
  token_based_billing?: boolean;
  quota_reset_date_utc?: string;
  limited_user_quotas?: {
    [k: string]: number | undefined;
  };
  limited_user_reset_date?: string;
  monthly_quotas?: {
    [k: string]: number | undefined;
  };
  cloud_session_storage_enabled?: boolean;
  cli_remote_control_enabled?: boolean;
}
/**
 * Schema for the `CopilotUserResponseEndpoints` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotUserResponseEndpoints".
 */
/** @experimental */
export interface CopilotUserResponseEndpoints {
  api?: string;
  "origin-tracker"?: string;
  proxy?: string;
  telemetry?: string;
}
/**
 * Schema for the `CopilotUserResponseQuotaSnapshots` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotUserResponseQuotaSnapshots".
 */
/** @experimental */
export interface CopilotUserResponseQuotaSnapshots {
  chat?: CopilotUserResponseQuotaSnapshotsChat;
  completions?: CopilotUserResponseQuotaSnapshotsCompletions;
  premium_interactions?: CopilotUserResponseQuotaSnapshotsPremiumInteractions;
  [k: string]:
    | ({
        entitlement?: number;
        overage_count?: number;
        overage_permitted?: boolean;
        percent_remaining?: number;
        quota_id?: string;
        quota_remaining?: number;
        remaining?: number;
        unlimited?: boolean;
        timestamp_utc?: string;
        has_quota?: boolean;
        quota_reset_at?: number;
        token_based_billing?: boolean;
      } | null)
    | undefined;
}
/**
 * Schema for the `CopilotUserResponseQuotaSnapshotsChat` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotUserResponseQuotaSnapshotsChat".
 */
/** @experimental */
export interface CopilotUserResponseQuotaSnapshotsChat {
  entitlement?: number;
  overage_count?: number;
  overage_permitted?: boolean;
  percent_remaining?: number;
  quota_id?: string;
  quota_remaining?: number;
  remaining?: number;
  unlimited?: boolean;
  timestamp_utc?: string;
  has_quota?: boolean;
  quota_reset_at?: number;
  token_based_billing?: boolean;
}
/**
 * Schema for the `CopilotUserResponseQuotaSnapshotsCompletions` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotUserResponseQuotaSnapshotsCompletions".
 */
/** @experimental */
export interface CopilotUserResponseQuotaSnapshotsCompletions {
  entitlement?: number;
  overage_count?: number;
  overage_permitted?: boolean;
  percent_remaining?: number;
  quota_id?: string;
  quota_remaining?: number;
  remaining?: number;
  unlimited?: boolean;
  timestamp_utc?: string;
  has_quota?: boolean;
  quota_reset_at?: number;
  token_based_billing?: boolean;
}
/**
 * Schema for the `CopilotUserResponseQuotaSnapshotsPremiumInteractions` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotUserResponseQuotaSnapshotsPremiumInteractions".
 */
/** @experimental */
export interface CopilotUserResponseQuotaSnapshotsPremiumInteractions {
  entitlement?: number;
  overage_count?: number;
  overage_permitted?: boolean;
  percent_remaining?: number;
  quota_id?: string;
  quota_remaining?: number;
  remaining?: number;
  unlimited?: boolean;
  timestamp_utc?: string;
  has_quota?: boolean;
  quota_reset_at?: number;
  token_based_billing?: boolean;
}
/**
 * Schema for the `HMACAuthInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HMACAuthInfo".
 */
/** @experimental */
export interface HMACAuthInfo {
  /**
   * HMAC-based authentication used by GitHub-internal services.
   */
  type: "hmac";
  /**
   * Authentication host. HMAC auth always targets the public GitHub host.
   */
  host: "https://github.com";
  /**
   * HMAC secret used to sign requests.
   */
  hmac: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Schema for the `EnvAuthInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EnvAuthInfo".
 */
/** @experimental */
export interface EnvAuthInfo {
  /**
   * Personal access token (PAT) or server-to-server token sourced from an environment variable.
   */
  type: "env";
  /**
   * Authentication host (e.g. https://github.com or a GHES host).
   */
  host: string;
  /**
   * User login associated with the token. Undefined for server-to-server tokens (those starting with `ghs_`).
   */
  login?: string;
  /**
   * The token value itself. Treat as a secret.
   */
  token: string;
  /**
   * Name of the environment variable the token was sourced from.
   */
  envVar: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Schema for the `TokenAuthInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TokenAuthInfo".
 */
/** @experimental */
export interface TokenAuthInfo {
  /**
   * SDK-side token authentication; the host configured the token directly via the SDK.
   */
  type: "token";
  /**
   * Authentication host.
   */
  host: string;
  /**
   * The token value itself. Treat as a secret.
   */
  token: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Schema for the `CopilotApiTokenAuthInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotApiTokenAuthInfo".
 */
/** @experimental */
export interface CopilotApiTokenAuthInfo {
  /**
   * Direct Copilot API authentication via the `GITHUB_COPILOT_API_TOKEN` + `COPILOT_API_URL` environment-variable pair. The token itself is read from the environment by the runtime, not carried in this struct.
   */
  type: "copilot-api-token";
  /**
   * Authentication host (always the public GitHub host).
   */
  host: "https://github.com";
  copilotUser?: CopilotUserResponse;
}
/**
 * Schema for the `UserAuthInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UserAuthInfo".
 */
/** @experimental */
export interface UserAuthInfo {
  /**
   * OAuth user authentication. The token itself is held in the runtime's secret token store (keyed by host+login) and is NOT carried in this struct.
   */
  type: "user";
  /**
   * Authentication host.
   */
  host: string;
  /**
   * OAuth user login.
   */
  login: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Schema for the `GhCliAuthInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "GhCliAuthInfo".
 */
/** @experimental */
export interface GhCliAuthInfo {
  /**
   * Authentication via the `gh` CLI's saved credentials.
   */
  type: "gh-cli";
  /**
   * Authentication host.
   */
  host: string;
  /**
   * User login as reported by `gh auth status`.
   */
  login: string;
  /**
   * The token returned by `gh auth token`. Treat as a secret.
   */
  token: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Canvas action that the agent or host can invoke. To discover the input schema for a particular action, call the list_canvas_capabilities tool.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasAction".
 */
/** @experimental */
export interface CanvasAction {
  /**
   * Action name exposed by the canvas provider
   */
  name: string;
  /**
   * Description of the action
   */
  description?: string;
  inputSchema?: CanvasJsonSchema;
}
/**
 * JSON Schema for canvas open input
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasJsonSchema".
 */
/** @experimental */
export interface CanvasJsonSchema {
  [k: string]: unknown | undefined;
}
/**
 * Canvas close parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasCloseRequest".
 */
/** @experimental */
export interface CanvasCloseRequest {
  /**
   * Open canvas instance identifier
   */
  instanceId: string;
}
/**
 * Canvas action invocation parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasInvokeActionRequest".
 */
/** @experimental */
export interface CanvasInvokeActionRequest {
  /**
   * Open canvas instance identifier
   */
  instanceId: string;
  /**
   * Action name to invoke
   */
  actionName: string;
  /**
   * Action input
   */
  input?: {
    [k: string]: unknown | undefined;
  };
}
/**
 * Canvas action invocation result.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasInvokeActionResult".
 */
/** @experimental */
export interface CanvasInvokeActionResult {
  /**
   * Provider-supplied action result
   */
  result?: {
    [k: string]: unknown | undefined;
  };
}
/**
 * Declared canvases available in this session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasList".
 */
/** @experimental */
export interface CanvasList {
  /**
   * Declared canvases available in this session
   */
  canvases: DiscoveredCanvas[];
}
/**
 * Canvas available in the current session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredCanvas".
 */
/** @experimental */
export interface DiscoveredCanvas {
  /**
   * Human-readable canvas name
   */
  displayName: string;
  /**
   * Short, single-sentence description shown to the agent in canvas catalogs.
   */
  description: string;
  inputSchema?: CanvasJsonSchema;
  /**
   * Actions the agent or host may invoke on an open instance
   */
  actions?: CanvasAction[];
  /**
   * Owning provider identifier
   */
  extensionId: string;
  /**
   * Owning extension display name, when available
   */
  extensionName?: string;
  /**
   * Provider-local canvas identifier
   */
  canvasId: string;
}
/**
 * Live open-canvas snapshot.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasListOpenResult".
 */
/** @experimental */
export interface CanvasListOpenResult {
  /**
   * Currently open canvas instances
   */
  openCanvases: OpenCanvasInstance[];
}
/**
 * Open canvas instance snapshot.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "OpenCanvasInstance".
 */
/** @experimental */
export interface OpenCanvasInstance {
  /**
   * Stable caller-supplied canvas instance identifier
   */
  instanceId: string;
  /**
   * Owning provider identifier
   */
  extensionId: string;
  /**
   * Owning extension display name, when available
   */
  extensionName?: string;
  /**
   * Provider-local canvas identifier
   */
  canvasId: string;
  /**
   * Rendered title
   */
  title?: string;
  /**
   * Provider-supplied status text
   */
  status?: string;
  /**
   * URL for web-rendered canvases
   */
  url?: string;
  /**
   * Input supplied when the instance was opened
   */
  input?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Whether this snapshot came from an idempotent reopen
   */
  reopen: boolean;
  availability: CanvasInstanceAvailability;
}
/**
 * Canvas open parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasOpenRequest".
 */
/** @experimental */
export interface CanvasOpenRequest {
  /**
   * Owning provider identifier. Optional when the canvasId is unique across providers; required to disambiguate when multiple providers register the same canvasId.
   */
  extensionId?: string;
  /**
   * Provider-local canvas identifier
   */
  canvasId: string;
  /**
   * Caller-supplied stable instance identifier
   */
  instanceId: string;
  /**
   * Canvas open input
   */
  input?: {
    [k: string]: unknown | undefined;
  };
}
/**
 * Slash commands available in the session, after applying any include/exclude filters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandList".
 */
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
 * Queued-command request ID and the result indicating whether the host executed it (and whether to stop processing further queued commands).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsRespondToQueuedCommandRequest".
 */
/** @experimental */
export interface CommandsRespondToQueuedCommandRequest {
  /**
   * Request ID from the `command.queued` event the host is responding to.
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
/** @experimental */
export interface QueuedCommandHandled {
  /**
   * The host actually executed the queued command.
   */
  handled: true;
  /**
   * When true, the runtime will not process subsequent queued commands until a new request comes in.
   */
  stopProcessingQueue?: boolean;
}
/**
 * Schema for the `QueuedCommandNotHandled` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuedCommandNotHandled".
 */
/** @experimental */
export interface QueuedCommandNotHandled {
  /**
   * The host did not execute the queued command. Unblocks the queue without claiming the command was processed (e.g. when the handler threw before completing).
   */
  handled: false;
}
/**
 * Indicates whether the queued-command response was matched to a pending request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsRespondToQueuedCommandResult".
 */
/** @experimental */
export interface CommandsRespondToQueuedCommandResult {
  /**
   * Whether a pending queued command with the given request ID was found and resolved. False when the request was already resolved, cancelled, or unknown.
   */
  success: boolean;
}
/**
 * Metadata for a connected remote session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectedRemoteSessionMetadata".
 */
/** @experimental */
export interface ConnectedRemoteSessionMetadata {
  /**
   * SDK session ID for the connected remote session.
   */
  sessionId: string;
  /**
   * Optional friendly session name.
   */
  name?: string;
  /**
   * Optional session summary.
   */
  summary?: string;
  /**
   * Session start time as an ISO 8601 string.
   */
  startTime: string;
  /**
   * Last session update time as an ISO 8601 string.
   */
  modifiedTime: string;
  repository: ConnectedRemoteSessionMetadataRepository;
  /**
   * Pull request number associated with the session.
   */
  pullRequestNumber?: number;
  /**
   * Original remote resource identifier.
   */
  resourceId?: string;
  kind: ConnectedRemoteSessionMetadataKind;
  /**
   * Remote session staleness deadline as an ISO 8601 string.
   */
  staleAt?: string;
  /**
   * Remote session state returned by the backing service.
   */
  state?: string;
}
/**
 * Repository associated with the connected remote session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectedRemoteSessionMetadataRepository".
 */
/** @experimental */
export interface ConnectedRemoteSessionMetadataRepository {
  /**
   * Repository owner or organization login.
   */
  owner: string;
  /**
   * Repository name.
   */
  name: string;
  /**
   * Branch associated with the remote session.
   */
  branch: string;
}
/**
 * Remote session connection parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectRemoteSessionParams".
 */
/** @experimental */
export interface ConnectRemoteSessionParams {
  /**
   * Session ID to connect to.
   */
  sessionId: string;
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
 * The currently selected model and reasoning effort for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CurrentModel".
 */
/** @experimental */
export interface CurrentModel {
  /**
   * Currently active model identifier
   */
  modelId?: string;
  /**
   * Reasoning effort level currently applied to the active model, when one is set. Reads `Session.getReasoningEffort()` synchronously after `getSelectedModel()` resolves so the two values are reported as a snapshot.
   */
  reasoningEffort?: string;
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
  source: McpServerSource;
  /**
   * Whether the server is enabled (not in the disabled list)
   */
  enabled: boolean;
}
/**
 * Slash-prefixed command string to enqueue for FIFO processing.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EnqueueCommandParams".
 */
/** @experimental */
export interface EnqueueCommandParams {
  /**
   * Slash-prefixed command string to enqueue, e.g. '/compact' or '/model gpt-4'. Queued FIFO with any in-flight items; if the session is idle, processing kicks off immediately.
   */
  command: string;
}
/**
 * Indicates whether the command was accepted into the local execution queue.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EnqueueCommandResult".
 */
/** @experimental */
export interface EnqueueCommandResult {
  /**
   * True when the command was accepted into the local execution queue. False when the call targets a session that does not support local command queueing (e.g. remote sessions).
   */
  queued: boolean;
}
/**
 * Cursor, batch size, and optional long-poll/filter parameters for reading session events.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventLogReadRequest".
 */
/** @experimental */
export interface EventLogReadRequest {
  /**
   * Opaque cursor returned by a previous read. Omit on the first call to start from the beginning of the session's persisted history.
   */
  cursor?: string;
  /**
   * Maximum number of events to return in this batch (1–1000, default 200).
   */
  max?: number;
  /**
   * Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen).
   */
  waitMs?: number;
  types?: EventLogTypes;
  agentScope?: EventsAgentScope;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventLogReleaseInterestResult".
 */
/** @experimental */
export interface EventLogReleaseInterestResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Snapshot of the current tail cursor without returning any events. Use this when a consumer wants to subscribe to live events going forward without first paginating through the entire persisted history (which would happen if `read` were called without a cursor on a long-lived session).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventLogTailResult".
 */
/** @experimental */
export interface EventLogTailResult {
  /**
   * Opaque cursor pointing at the current tail of the session's persisted-events history. Pass back to `read` to receive only events that arrive AFTER this snapshot. When the session has no events, this returns the same sentinel as an unset cursor (i.e. equivalent to omitting the cursor on a first read).
   */
  cursor: string;
}
/**
 * Batch of session events returned by a read, with cursor and continuation metadata.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventsReadResult".
 */
/** @experimental */
export interface EventsReadResult {
  /**
   * Events are delivered in two batches per read: persisted events first (in append order), then ephemeral events (in seq order). When `waitMs > 0` and the catch-up batches were empty, post-wait events follow the same two-batch ordering. Persisted and ephemeral events do not interleave within a single read.
   */
  events: SessionEvent[];
  /**
   * Opaque cursor for the next read. Pass back unchanged in the next read.cursor to continue from where this read left off. Always present, even when no events were returned.
   */
  cursor: string;
  /**
   * True when the read returned `max` events and more events are available immediately. When false, the next read with a non-zero `waitMs` will block until a new event arrives or the wait expires.
   */
  hasMore: boolean;
  cursorStatus: EventsCursorStatus;
}
/**
 * Slash command name and argument string to execute synchronously.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExecuteCommandParams".
 */
/** @experimental */
export interface ExecuteCommandParams {
  /**
   * Name of the slash command to invoke (without the leading '/').
   */
  commandName: string;
  /**
   * Argument string to pass to the command (empty string if none).
   */
  args: string;
}
/**
 * Error message produced while executing the command, if any.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExecuteCommandResult".
 */
/** @experimental */
export interface ExecuteCommandResult {
  /**
   * Error message produced while executing the command, if any. Omitted when the handler succeeded.
   */
  error?: string;
}
/**
 * Schema for the `Extension` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "Extension".
 */
/** @experimental */
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
/** @experimental */
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
    [k: string]: unknown | undefined;
  };
  /**
   * Base64-encoded binary results returned to the model
   */
  binaryResultsForLlm?: ExternalToolTextResultForLlmBinaryResultsForLlm[];
  /**
   * Structured content blocks from the tool
   */
  contents?: ExternalToolTextResultForLlmContent[];
  [k: string]: unknown | undefined;
}
/**
 * Binary result returned by a tool for the model
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmBinaryResultsForLlm".
 */
/** @experimental */
export interface ExternalToolTextResultForLlmBinaryResultsForLlm {
  type: ExternalToolTextResultForLlmBinaryResultsForLlmType;
  /**
   * Base64-encoded binary data
   */
  data: string;
  /**
   * MIME type of the binary data
   */
  mimeType: string;
  /**
   * Human-readable description of the binary data
   */
  description?: string;
  /**
   * Optional metadata from the producing tool.
   */
  metadata?: {
    [k: string]: unknown | undefined;
  };
}
/**
 * Plain text content block
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentText".
 */
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
 * Folder path to add to trusted folders.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FolderTrustAddParams".
 */
/** @experimental */
export interface FolderTrustAddParams {
  /**
   * Folder path to mark as trusted
   */
  path: string;
}
/**
 * Folder path to check for trust.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FolderTrustCheckParams".
 */
/** @experimental */
export interface FolderTrustCheckParams {
  /**
   * Folder path to check
   */
  path: string;
}
/**
 * Folder trust check result.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FolderTrustCheckResult".
 */
/** @experimental */
export interface FolderTrustCheckResult {
  /**
   * Whether the folder is trusted
   */
  trusted: boolean;
}
/**
 * Pending external tool call request ID, with the tool result or an error describing why it failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HandlePendingToolCallRequest".
 */
/** @experimental */
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
/** @experimental */
export interface HandlePendingToolCallResult {
  /**
   * Whether the tool call result was handled successfully
   */
  success: boolean;
}
/**
 * Indicates whether an in-progress manual compaction was aborted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryAbortManualCompactionResult".
 */
/** @experimental */
export interface HistoryAbortManualCompactionResult {
  /**
   * Whether an in-progress manual compaction was aborted. False when no manual compaction was running, when its abort controller was already aborted, or when the session is remote.
   */
  aborted: boolean;
}
/**
 * Indicates whether an in-progress background compaction was cancelled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryCancelBackgroundCompactionResult".
 */
/** @experimental */
export interface HistoryCancelBackgroundCompactionResult {
  /**
   * Whether an in-progress background compaction was cancelled. False when no compaction was running, when the session is remote, or when the underlying processor was unavailable.
   */
  cancelled: boolean;
}
/**
 * Post-compaction context window usage breakdown
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryCompactContextWindow".
 */
/** @experimental */
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
 * Optional compaction parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryCompactRequest".
 */
/** @experimental */
export interface HistoryCompactRequest {
  /**
   * Optional user-provided instructions to focus the compaction summary
   */
  customInstructions?: string;
}
/**
 * Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.
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
  /**
   * Summary text produced by compaction. Omitted when compaction did not produce a summary (e.g. failure path).
   */
  summaryContent?: string;
  contextWindow?: HistoryCompactContextWindow;
}
/**
 * Markdown summary of the conversation context (empty when not available).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistorySummarizeForHandoffResult".
 */
/** @experimental */
export interface HistorySummarizeForHandoffResult {
  /**
   * Markdown summary of the conversation context produced by an LLM. Empty string when there are no messages or when the session does not support local summarization.
   */
  summary: string;
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
 * Schema for the `InstalledPlugin` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstalledPlugin".
 */
/** @experimental */
export interface InstalledPlugin {
  /**
   * Plugin name
   */
  name: string;
  /**
   * Marketplace the plugin came from (empty string for direct repo installs)
   */
  marketplace: string;
  /**
   * Version installed (if available)
   */
  version?: string;
  /**
   * Installation timestamp
   */
  installed_at: string;
  /**
   * Whether the plugin is currently enabled
   */
  enabled: boolean;
  /**
   * Path where the plugin is cached locally
   */
  cache_path?: string;
  source?: InstalledPluginSource;
}
/**
 * Schema for the `InstalledPluginSourceGithub` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstalledPluginSourceGithub".
 */
/** @experimental */
export interface InstalledPluginSourceGithub {
  /**
   * Constant value. Always "github".
   */
  source: "github";
  repo: string;
  ref?: string;
  path?: string;
}
/**
 * Schema for the `InstalledPluginSourceUrl` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstalledPluginSourceUrl".
 */
/** @experimental */
export interface InstalledPluginSourceUrl {
  /**
   * Constant value. Always "url".
   */
  source: "url";
  url: string;
  ref?: string;
  path?: string;
}
/**
 * Schema for the `InstalledPluginSourceLocal` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstalledPluginSourceLocal".
 */
/** @experimental */
export interface InstalledPluginSourceLocal {
  /**
   * Constant value. Always "local".
   */
  source: "local";
  path: string;
}
/**
 * Instruction sources loaded for the session, in merge order.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionsGetSourcesResult".
 */
/** @experimental */
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
/** @experimental */
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
   * Glob pattern(s) from frontmatter — when set, this instruction applies only to matching files
   */
  applyTo?: string[];
  /**
   * Short description (body after frontmatter) for use in instruction tables
   */
  description?: string;
  /**
   * When true, this source starts disabled and must be toggled on by the user
   */
  defaultDisabled?: boolean;
}
/**
 * Message text, optional severity level, persistence flag, optional follow-up URL, and optional tip.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LogRequest".
 */
/** @experimental */
export interface LogRequest {
  /**
   * Human-readable message
   */
  message: string;
  level?: SessionLogLevel;
  /**
   * Domain category for this log entry (e.g., "mcp", "subscription", "policy", "model"). Maps to `infoType`/`warningType`/`errorType` on the emitted event. Defaults to "notification".
   */
  type?: string;
  /**
   * When true, the message is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Optional URL the user can open in their browser for more details
   */
  url?: string;
  /**
   * Optional actionable tip displayed alongside the message. Only honored on `level: "info"`.
   */
  tip?: string;
}
/**
 * Identifier of the session event that was emitted for the log message.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LogResult".
 */
/** @experimental */
export interface LogResult {
  /**
   * The unique identifier of the emitted session event
   */
  eventId: string;
}
/**
 * Parameters for (re)loading the merged LSP configuration set.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LspInitializeRequest".
 */
/** @experimental */
export interface LspInitializeRequest {
  /**
   * Working directory used to load project-level LSP configs. Defaults to the session working directory when omitted.
   */
  workingDirectory?: string;
  /**
   * Git root used as the boundary when traversing for project-level LSP configs (supports monorepos).
   */
  gitRoot?: string;
  /**
   * Force re-initialization even when LSP configs were already loaded for the working directory.
   */
  force?: boolean;
}
/**
 * MCP server, tool name, and arguments to invoke from an MCP App view.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsCallToolRequest".
 */
/** @experimental */
export interface McpAppsCallToolRequest {
  /**
   * MCP server hosting the tool
   */
  serverName: string;
  /**
   * MCP tool name
   */
  toolName: string;
  /**
   * Tool arguments
   */
  arguments?: {
    [k: string]: unknown | undefined;
  };
  /**
   * **Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.
   */
  originServerName: string;
}
/**
 * Capability negotiation snapshot
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsDiagnoseCapability".
 */
/** @experimental */
export interface McpAppsDiagnoseCapability {
  /**
   * Whether the session has the `mcp-apps` capability
   */
  sessionHasMcpApps: boolean;
  /**
   * Whether the MCP_APPS feature flag (or COPILOT_MCP_APPS env override) is on
   */
  featureFlagEnabled: boolean;
  /**
   * Whether the runtime advertises `extensions.io.modelcontextprotocol/ui` to MCP servers
   */
  advertised: boolean;
}
/**
 * MCP server to diagnose MCP Apps wiring for.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsDiagnoseRequest".
 */
/** @experimental */
export interface McpAppsDiagnoseRequest {
  /**
   * MCP server to probe
   */
  serverName: string;
}
/**
 * Diagnostic snapshot of MCP Apps wiring for the named server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsDiagnoseResult".
 */
/** @experimental */
export interface McpAppsDiagnoseResult {
  capability: McpAppsDiagnoseCapability;
  server: McpAppsDiagnoseServer;
}
/**
 * What the server returned for this session
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsDiagnoseServer".
 */
/** @experimental */
export interface McpAppsDiagnoseServer {
  /**
   * Whether the named server is currently connected
   */
  connected: boolean;
  /**
   * Total tools returned by the server's tools/list
   */
  toolCount: number;
  /**
   * Tools whose `_meta.ui` is populated (resourceUri and/or visibility set)
   */
  toolsWithUiMeta: number;
  /**
   * Up to 5 tool names with `_meta.ui` for quick inspection
   */
  sampleToolNames: string[];
}
/**
 * Current host context advertised to MCP App guests.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsHostContext".
 */
/** @experimental */
export interface McpAppsHostContext {
  context: McpAppsHostContextDetails;
}
/**
 * Current host context
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsHostContextDetails".
 */
/** @experimental */
export interface McpAppsHostContextDetails {
  theme?: McpAppsHostContextDetailsTheme;
  /**
   * BCP-47 locale, e.g. 'en-US'
   */
  locale?: string;
  /**
   * IANA timezone, e.g. 'America/New_York'
   */
  timeZone?: string;
  displayMode?: McpAppsHostContextDetailsDisplayMode;
  /**
   * Display modes the host supports
   */
  availableDisplayModes?: McpAppsHostContextDetailsAvailableDisplayMode[];
  platform?: McpAppsHostContextDetailsPlatform;
  /**
   * Host application identifier
   */
  userAgent?: string;
  [k: string]: unknown | undefined;
}
/**
 * MCP server to list app-callable tools for.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsListToolsRequest".
 */
/** @experimental */
export interface McpAppsListToolsRequest {
  /**
   * MCP server hosting the app
   */
  serverName: string;
  /**
   * **Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.
   */
  originServerName: string;
}
/**
 * App-callable tools from the named MCP server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsListToolsResult".
 */
/** @experimental */
export interface McpAppsListToolsResult {
  /**
   * App-callable tools from the server
   */
  tools: {
    [k: string]: unknown | undefined;
  }[];
}
/**
 * MCP server and resource URI to fetch.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsReadResourceRequest".
 */
/** @experimental */
export interface McpAppsReadResourceRequest {
  /**
   * Name of the MCP server hosting the resource
   */
  serverName: string;
  /**
   * Resource URI (typically ui://...)
   */
  uri: string;
}
/**
 * Resource contents returned by the MCP server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsReadResourceResult".
 */
/** @experimental */
export interface McpAppsReadResourceResult {
  /**
   * Resource contents returned by the server
   */
  contents: McpAppsResourceContent[];
}
/**
 * Schema for the `McpAppsResourceContent` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsResourceContent".
 */
/** @experimental */
export interface McpAppsResourceContent {
  /**
   * The resource URI (typically ui://...)
   */
  uri: string;
  /**
   * MIME type of the content
   */
  mimeType?: string;
  /**
   * Text content (e.g. HTML)
   */
  text?: string;
  /**
   * Base64-encoded binary content
   */
  blob?: string;
  /**
   * Resource-level metadata (CSP, permissions, etc.)
   */
  _meta?: {
    [k: string]: unknown | undefined;
  };
}
/**
 * Host context advertised to MCP App guests
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsSetHostContextDetails".
 */
/** @experimental */
export interface McpAppsSetHostContextDetails {
  theme?: McpAppsSetHostContextDetailsTheme;
  /**
   * BCP-47 locale, e.g. 'en-US'
   */
  locale?: string;
  /**
   * IANA timezone, e.g. 'America/New_York'
   */
  timeZone?: string;
  displayMode?: McpAppsSetHostContextDetailsDisplayMode;
  /**
   * Display modes the host supports
   */
  availableDisplayModes?: McpAppsSetHostContextDetailsAvailableDisplayMode[];
  platform?: McpAppsSetHostContextDetailsPlatform;
  /**
   * Host application identifier
   */
  userAgent?: string;
  [k: string]: unknown | undefined;
}
/**
 * Host context to advertise to MCP App guests.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsSetHostContextRequest".
 */
/** @experimental */
export interface McpAppsSetHostContextRequest {
  context: McpAppsSetHostContextDetails;
}
/**
 * The requestId previously passed to executeSampling that should be cancelled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpCancelSamplingExecutionParams".
 */
/** @experimental */
export interface McpCancelSamplingExecutionParams {
  /**
   * The requestId previously passed to executeSampling that should be cancelled
   */
  requestId: string;
}
/**
 * Indicates whether an in-flight sampling execution with the given requestId was found and cancelled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpCancelSamplingExecutionResult".
 */
/** @experimental */
export interface McpCancelSamplingExecutionResult {
  /**
   * True if an in-flight execution with the given requestId was found and signalled to cancel. False when no such execution is in flight (already completed, never started, or cancelled by another caller).
   */
  cancelled: boolean;
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
 * Stdio MCP server configuration launched as a child process.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigStdio".
 */
export interface McpServerConfigStdio {
  /**
   * Tools to include. Defaults to all tools if not specified.
   */
  tools?: string[];
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
   * Executable command used to start the Stdio MCP server process.
   */
  command: string;
  /**
   * Command-line arguments passed to the Stdio MCP server process.
   */
  args?: string[];
  /**
   * Working directory for the Stdio MCP server process.
   */
  cwd?: string;
  /**
   * Environment variables to pass to the Stdio MCP server process.
   */
  env?: {
    [k: string]: string | undefined;
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
    [k: string]: string | undefined;
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
  auth?: McpServerConfigHttpAuth;
}
/**
 * Additional authentication configuration for this server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigHttpAuth".
 */
export interface McpServerConfigHttpAuth {
  /**
   * Fixed port for the OAuth redirect callback server.
   */
  redirectPort?: number;
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
 * Identifiers and raw MCP CreateMessageRequest params used to run a sampling inference.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpExecuteSamplingParams".
 */
/** @experimental */
export interface McpExecuteSamplingParams {
  /**
   * Caller-provided unique identifier for this sampling execution. Use this same ID with cancelSamplingExecution to cancel the in-flight call. Must be unique within the session for the lifetime of the call.
   */
  requestId: string;
  /**
   * Name of the MCP server that initiated the sampling request
   */
  serverName: string;
  /**
   * The original MCP JSON-RPC request ID (string or number). Used by the runtime to correlate the inference with the originating MCP request for telemetry; this is distinct from `requestId` (which is the schema-level cancellation handle).
   */
  mcpRequestId: string | number;
  request: McpExecuteSamplingRequest;
}
/**
 * Raw MCP CreateMessageRequest params, as received in the `sampling.requested` event. Treated as opaque at the schema layer; the runtime converts the embedded MCP messages into the OpenAI chat-completion shape internally.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpExecuteSamplingRequest".
 */
/** @experimental */
export interface McpExecuteSamplingRequest {
  [k: string]: unknown | undefined;
}
/**
 * MCP CreateMessageResult payload (with optional 'tools' extension), present when action='success'. Treated as opaque at the schema layer; consumers should construct/consume it per the MCP CreateMessageResult shape.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpExecuteSamplingResult".
 */
/** @experimental */
export interface McpExecuteSamplingResult {
  [k: string]: unknown | undefined;
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
 * Indicates whether the auto-managed `github` MCP server was removed (false when nothing to remove).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpRemoveGitHubResult".
 */
/** @experimental */
export interface McpRemoveGitHubResult {
  /**
   * True when the auto-managed `github` MCP server was removed; false when no removal happened (e.g. user has explicitly configured a `github` server, or the server was not registered).
   */
  removed: boolean;
}
/**
 * Outcome of an MCP sampling execution: success result, failure error, or cancellation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSamplingExecutionResult".
 */
/** @experimental */
export interface McpSamplingExecutionResult {
  action: McpSamplingExecutionAction;
  result?: McpExecuteSamplingResult;
  /**
   * Error description, present when action='failure'.
   */
  error?: string;
}
/**
 * Schema for the `McpServer` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServer".
 */
/** @experimental */
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
 * Mode controlling how MCP server env values are resolved (`direct` or `indirect`).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSetEnvValueModeParams".
 */
/** @experimental */
export interface McpSetEnvValueModeParams {
  mode: McpSetEnvValueModeDetails;
}
/**
 * Env-value mode recorded on the session after the update.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSetEnvValueModeResult".
 */
/** @experimental */
export interface McpSetEnvValueModeResult {
  mode: McpSetEnvValueModeDetails;
}
/**
 * Model identifier and token limits used to compute the context-info breakdown.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataContextInfoRequest".
 */
/** @experimental */
export interface MetadataContextInfoRequest {
  /**
   * Maximum prompt tokens allowed by the target model. Pass 0 to use the runtime default.
   */
  promptTokenLimit: number;
  /**
   * Maximum output tokens allowed by the target model. Pass 0 if unknown.
   */
  outputTokenLimit: number;
  /**
   * Model identifier used for tokenization. Omit to use the session default. Used both for token counting and to compute display values.
   */
  selectedModel?: string;
}
/**
 * Token breakdown for the session's current context window, or null if uninitialized.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataContextInfoResult".
 */
/** @experimental */
export interface MetadataContextInfoResult {
  /**
   * Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
   */
  contextInfo?: SessionContextInfo | null;
}
/**
 * Indicates whether the local session is currently processing a turn or background continuation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataIsProcessingResult".
 */
/** @experimental */
export interface MetadataIsProcessingResult {
  /**
   * Whether the session is currently processing user/agent messages. False for non-local sessions (which don't run a local agentic loop). Reflects an in-flight turn or background continuation.
   */
  processing: boolean;
}
/**
 * Model identifier to use when re-tokenizing the session's existing messages.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataRecomputeContextTokensRequest".
 */
/** @experimental */
export interface MetadataRecomputeContextTokensRequest {
  /**
   * Model identifier used for tokenization. The runtime token-counts both chat-context and system-context messages against this model.
   */
  modelId: string;
}
/**
 * Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataRecomputeContextTokensResult".
 */
/** @experimental */
export interface MetadataRecomputeContextTokensResult {
  /**
   * Sum of tokens across chat-context and system-context messages currently held by the session.
   */
  totalTokens: number;
  /**
   * Tokens contributed by user/assistant/tool messages (excludes system/developer prompts).
   */
  messagesTokenCount: number;
  /**
   * Tokens contributed by system/developer prompt snapshots.
   */
  systemTokenCount: number;
}
/**
 * Updated working-directory/git context to record on the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataRecordContextChangeRequest".
 */
/** @experimental */
export interface MetadataRecordContextChangeRequest {
  context: SessionWorkingDirectoryContext;
}
/**
 * Updated working directory and git context. Emitted as the new payload of `session.context_changed`.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionWorkingDirectoryContext".
 */
/** @experimental */
export interface SessionWorkingDirectoryContext {
  /**
   * Current working directory path
   */
  cwd: string;
  /**
   * Root directory of the git repository, resolved via git rev-parse
   */
  gitRoot?: string;
  /**
   * Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
   */
  repository?: string;
  hostType?: SessionWorkingDirectoryContextHostType;
  /**
   * Raw host string from the git remote URL (e.g. "github.com", "dev.azure.com")
   */
  repositoryHost?: string;
  /**
   * Current git branch name
   */
  branch?: string;
  /**
   * Head commit of the current git branch
   */
  headCommit?: string;
  /**
   * Merge-base commit SHA (fork point from the remote default branch)
   */
  baseCommit?: string;
}
/**
 * Notify the session that its working directory context has changed. Emits a `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline UI) can react. Use this when the host has detected a cwd/branch/repo change outside the session's normal lifecycle (e.g., after a shell command in interactive mode).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataRecordContextChangeResult".
 */
/** @experimental */
export interface MetadataRecordContextChangeResult {}
/**
 * Absolute path to set as the session's new working directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataSetWorkingDirectoryRequest".
 */
/** @experimental */
export interface MetadataSetWorkingDirectoryRequest {
  /**
   * Absolute path to set as the session's working directory. The runtime updates the session's recorded cwd so subsequent operations (shell tools, file lookups, telemetry) anchor to it.
   */
  workingDirectory: string;
}
/**
 * Update the session's working directory. Used by the host when the user explicitly changes cwd (e.g., the `/cd` slash command). The host is responsible for `process.chdir` and any related side-effects (file index, etc.); this method only updates the session's own recorded path.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataSetWorkingDirectoryResult".
 */
/** @experimental */
export interface MetadataSetWorkingDirectoryResult {
  /**
   * Working directory after the update
   */
  workingDirectory: string;
}
/**
 * Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataSnapshotRemoteMetadata".
 */
/** @experimental */
export interface MetadataSnapshotRemoteMetadata {
  /**
   * The original resource identifier (task ID or PR node ID), preserved across event-replay reconstructions. Falls back to `sessionId` when absent.
   */
  resourceId?: string;
  repository: MetadataSnapshotRemoteMetadataRepository;
  /**
   * The pull request number the remote session is associated with, if any.
   */
  pullRequestNumber?: number;
  taskType?: MetadataSnapshotRemoteMetadataTaskType;
}
/**
 * The repository the remote session targets.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataSnapshotRemoteMetadataRepository".
 */
/** @experimental */
export interface MetadataSnapshotRemoteMetadataRepository {
  /**
   * The GitHub owner (user or organization) of the target repository.
   */
  owner: string;
  /**
   * The GitHub repository name (without owner).
   */
  name: string;
  /**
   * The branch the remote session is operating on.
   */
  branch: string;
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
  state: ModelPolicyState;
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
   * AI Credits cost per billing batch of input tokens
   */
  inputPrice?: number;
  /**
   * AI Credits cost per billing batch of output tokens
   */
  outputPrice?: number;
  /**
   * AI Credits cost per billing batch of cached tokens
   */
  cachePrice?: number;
  /**
   * Number of tokens per standard billing batch
   */
  batchSize?: number;
  /**
   * Maximum context window tokens for the default tier
   */
  contextMax?: number;
  longContext?: ModelBillingTokenPricesLongContext;
}
/**
 * Long context tier pricing (available for models with extended context windows)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelBillingTokenPricesLongContext".
 */
export interface ModelBillingTokenPricesLongContext {
  /**
   * AI Credits cost per billing batch of input tokens
   */
  inputPrice?: number;
  /**
   * AI Credits cost per billing batch of output tokens
   */
  outputPrice?: number;
  /**
   * AI Credits cost per billing batch of cached tokens
   */
  cachePrice?: number;
  /**
   * Maximum context window tokens for the long context tier
   */
  contextMax?: number;
}
/**
 * Override individual model capabilities resolved by the runtime
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesOverride".
 */
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/**
 * Reasoning effort level to apply to the currently selected model.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelSetReasoningEffortRequest".
 */
/** @experimental */
export interface ModelSetReasoningEffortRequest {
  /**
   * Reasoning effort level to apply to the currently selected model. The host is responsible for validating the value against the model's supported levels before calling.
   */
  reasoningEffort: string;
}
/**
 * Update the session's reasoning effort without changing the selected model. Use `switchTo` instead when you also need to change the model. The runtime stores the effort on the session and applies it to subsequent turns.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelSetReasoningEffortResult".
 */
/** @experimental */
export interface ModelSetReasoningEffortResult {
  /**
   * Reasoning effort level recorded on the session after the update
   */
  reasoningEffort: string;
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
export interface ModeSetRequest {
  mode: SessionMode;
}
/**
 * The session's friendly name, or null when not yet set.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "NameGetResult".
 */
/** @experimental */
export interface NameGetResult {
  /**
   * The session name (user-set or auto-generated), or null if not yet set
   */
  name: string | null;
}
/**
 * Auto-generated session summary to apply as the session's name when no user-set name exists.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "NameSetAutoRequest".
 */
/** @experimental */
export interface NameSetAutoRequest {
  /**
   * Auto-generated session summary. Empty/whitespace-only values are ignored; values are trimmed before persisting.
   */
  summary: string;
}
/**
 * Indicates whether the auto-generated summary was applied as the session's name.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "NameSetAutoResult".
 */
/** @experimental */
export interface NameSetAutoResult {
  /**
   * Whether the auto-generated summary was persisted. False if the session already has a user-set name, the summary normalized to empty, or the session does not have a workspace.
   */
  applied: boolean;
}
/**
 * New friendly name to apply to the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "NameSetRequest".
 */
/** @experimental */
export interface NameSetRequest {
  /**
   * New session name (1–100 characters, trimmed of leading/trailing whitespace)
   */
  name: string;
}
/**
 * Schema for the `PendingPermissionRequest` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PendingPermissionRequest".
 */
/** @experimental */
export interface PendingPermissionRequest {
  /**
   * Unique identifier for the pending permission request
   */
  requestId: string;
  request: PermissionPromptRequest;
}
/**
 * List of pending permission requests reconstructed from event history.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PendingPermissionRequestList".
 */
/** @experimental */
export interface PendingPermissionRequestList {
  /**
   * Pending permission prompts reconstructed from the session's event history. Equivalent to the set of `permission.requested` events that have not yet been followed by a matching `permission.completed` event. Used by clients (e.g. the CLI) to hydrate UI for prompts that were emitted before the client attached to the session.
   */
  items: PendingPermissionRequest[];
}
/**
 * Schema for the `PermissionDecisionApproveOnce` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveOnce".
 */
/** @experimental */
export interface PermissionDecisionApproveOnce {
  /**
   * Approve this single request only
   */
  kind: "approve-once";
}
/**
 * Schema for the `PermissionDecisionApproveForSession` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSession".
 */
/** @experimental */
export interface PermissionDecisionApproveForSession {
  /**
   * Approve and remember for the rest of the session
   */
  kind: "approve-for-session";
  approval?: PermissionDecisionApproveForSessionApproval;
  /**
   * URL domain to approve for the rest of the session (URL prompts only)
   */
  domain?: string;
}
/**
 * Schema for the `PermissionDecisionApproveForSessionApprovalCommands` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalCommands".
 */
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
export interface PermissionDecisionApproveForLocation {
  /**
   * Approve and persist for this project location
   */
  kind: "approve-for-location";
  approval: PermissionDecisionApproveForLocationApproval;
  /**
   * Location key (git root or cwd) to persist the approval to
   */
  locationKey: string;
}
/**
 * Schema for the `PermissionDecisionApproveForLocationApprovalCommands` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalCommands".
 */
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
export interface PermissionDecisionApprovePermanently {
  /**
   * Approve and persist across sessions (URL prompts only)
   */
  kind: "approve-permanently";
  /**
   * URL domain to approve permanently
   */
  domain: string;
}
/**
 * Schema for the `PermissionDecisionReject` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionReject".
 */
/** @experimental */
export interface PermissionDecisionReject {
  /**
   * Reject the request
   */
  kind: "reject";
  /**
   * Optional feedback explaining the rejection
   */
  feedback?: string;
}
/**
 * Schema for the `PermissionDecisionUserNotAvailable` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionUserNotAvailable".
 */
/** @experimental */
export interface PermissionDecisionUserNotAvailable {
  /**
   * No user is available to confirm the request
   */
  kind: "user-not-available";
}
/**
 * Schema for the `PermissionDecisionApproved` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproved".
 */
/** @experimental */
export interface PermissionDecisionApproved {
  /**
   * The permission request was approved
   */
  kind: "approved";
}
/**
 * Schema for the `PermissionDecisionApprovedForSession` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApprovedForSession".
 */
/** @experimental */
export interface PermissionDecisionApprovedForSession {
  /**
   * Approved and remembered for the rest of the session
   */
  kind: "approved-for-session";
  approval: UserToolSessionApproval;
}
/**
 * Schema for the `PermissionDecisionApprovedForLocation` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApprovedForLocation".
 */
/** @experimental */
export interface PermissionDecisionApprovedForLocation {
  /**
   * Approved and persisted for this project location
   */
  kind: "approved-for-location";
  approval: UserToolSessionApproval;
  /**
   * The location key (git root or cwd) to persist the approval to
   */
  locationKey: string;
}
/**
 * Schema for the `PermissionDecisionCancelled` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionCancelled".
 */
/** @experimental */
export interface PermissionDecisionCancelled {
  /**
   * The permission request was cancelled before a response was used
   */
  kind: "cancelled";
  /**
   * Optional explanation of why the request was cancelled
   */
  reason?: string;
}
/**
 * Schema for the `PermissionDecisionDeniedByRules` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionDeniedByRules".
 */
/** @experimental */
export interface PermissionDecisionDeniedByRules {
  /**
   * Denied because approval rules explicitly blocked it
   */
  kind: "denied-by-rules";
  /**
   * Rules that denied the request
   */
  rules: PermissionRule[];
}
/**
 * Schema for the `PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser".
 */
/** @experimental */
export interface PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser {
  /**
   * Denied because no approval rule matched and user confirmation was unavailable
   */
  kind: "denied-no-approval-rule-and-could-not-request-from-user";
}
/**
 * Schema for the `PermissionDecisionDeniedInteractivelyByUser` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionDeniedInteractivelyByUser".
 */
/** @experimental */
export interface PermissionDecisionDeniedInteractivelyByUser {
  /**
   * Denied by the user during an interactive prompt
   */
  kind: "denied-interactively-by-user";
  /**
   * Optional feedback from the user explaining the denial
   */
  feedback?: string;
  /**
   * Whether to force-reject the current agent turn
   */
  forceReject?: boolean;
}
/**
 * Schema for the `PermissionDecisionDeniedByContentExclusionPolicy` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionDeniedByContentExclusionPolicy".
 */
/** @experimental */
export interface PermissionDecisionDeniedByContentExclusionPolicy {
  /**
   * Denied by the organization's content exclusion policy
   */
  kind: "denied-by-content-exclusion-policy";
  /**
   * File path that triggered the exclusion
   */
  path: string;
  /**
   * Human-readable explanation of why the path was excluded
   */
  message: string;
}
/**
 * Schema for the `PermissionDecisionDeniedByPermissionRequestHook` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionDeniedByPermissionRequestHook".
 */
/** @experimental */
export interface PermissionDecisionDeniedByPermissionRequestHook {
  /**
   * Denied by a permission request hook registered by an extension or plugin
   */
  kind: "denied-by-permission-request-hook";
  /**
   * Optional message from the hook explaining the denial
   */
  message?: string;
  /**
   * Whether to interrupt the current agent turn
   */
  interrupt?: boolean;
}
/**
 * Pending permission request ID and the decision to apply (approve/reject and scope).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionRequest".
 */
/** @experimental */
export interface PermissionDecisionRequest {
  /**
   * Request ID of the pending permission request
   */
  requestId: string;
  result: PermissionDecision;
}
/**
 * Location-scoped tool approval to persist.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionLocationAddToolApprovalParams".
 */
/** @experimental */
export interface PermissionLocationAddToolApprovalParams {
  /**
   * Location key (git root or cwd) to persist the approval to
   */
  locationKey: string;
  approval: PermissionsLocationsAddToolApprovalDetails;
}
/**
 * Schema for the `PermissionsLocationsAddToolApprovalDetailsCommands` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsCommands".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsCommands {
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
 * Schema for the `PermissionsLocationsAddToolApprovalDetailsRead` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsRead".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsRead {
  /**
   * Approval covering read-only filesystem operations.
   */
  kind: "read";
}
/**
 * Schema for the `PermissionsLocationsAddToolApprovalDetailsWrite` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsWrite".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsWrite {
  /**
   * Approval covering filesystem write operations.
   */
  kind: "write";
}
/**
 * Schema for the `PermissionsLocationsAddToolApprovalDetailsMcp` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsMcp".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsMcp {
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
 * Schema for the `PermissionsLocationsAddToolApprovalDetailsMcpSampling` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsMcpSampling".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsMcpSampling {
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
 * Schema for the `PermissionsLocationsAddToolApprovalDetailsMemory` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsMemory".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsMemory {
  /**
   * Approval covering writes to long-term memory.
   */
  kind: "memory";
}
/**
 * Schema for the `PermissionsLocationsAddToolApprovalDetailsCustomTool` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsCustomTool".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsCustomTool {
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
 * Schema for the `PermissionsLocationsAddToolApprovalDetailsExtensionManagement` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsExtensionManagement".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsExtensionManagement {
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
 * Schema for the `PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess {
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
 * Working directory to load persisted location permissions for.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionLocationApplyParams".
 */
/** @experimental */
export interface PermissionLocationApplyParams {
  /**
   * Working directory whose persisted location permissions should be applied
   */
  workingDirectory: string;
}
/**
 * Summary of persisted location permissions applied to the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionLocationApplyResult".
 */
/** @experimental */
export interface PermissionLocationApplyResult {
  /**
   * Location key used in the location-permissions store
   */
  locationKey: string;
  locationType: PermissionLocationType;
  /**
   * Whether a different location was applied since the previous apply call
   */
  changed: boolean;
  /**
   * Number of location-scoped rules added to the live permission service
   */
  appliedRuleCount: number;
  /**
   * Number of persisted allowed directories added to the live path manager
   */
  appliedDirectoryCount: number;
  /**
   * Location-scoped rules applied to the live permission service
   */
  appliedRules: PermissionRule[];
}
/**
 * Working directory to resolve into a location-permissions key.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionLocationResolveParams".
 */
/** @experimental */
export interface PermissionLocationResolveParams {
  /**
   * Working directory whose permission location should be resolved
   */
  workingDirectory: string;
}
/**
 * Resolved location-permissions key and type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionLocationResolveResult".
 */
/** @experimental */
export interface PermissionLocationResolveResult {
  /**
   * Location key used in the location-permissions store
   */
  locationKey: string;
  locationType: PermissionLocationType;
}
/**
 * Directory path to add to the session's allowed directories.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsAddParams".
 */
/** @experimental */
export interface PermissionPathsAddParams {
  /**
   * Directory to add to the allow-list. The runtime resolves and validates the path before adding.
   */
  path: string;
}
/**
 * Path to evaluate against the session's allowed directories.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsAllowedCheckParams".
 */
/** @experimental */
export interface PermissionPathsAllowedCheckParams {
  /**
   * Path to check against the session's allowed directories
   */
  path: string;
}
/**
 * Indicates whether the supplied path is within the session's allowed directories.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsAllowedCheckResult".
 */
/** @experimental */
export interface PermissionPathsAllowedCheckResult {
  /**
   * Whether the path is within the session's allowed directories
   */
  allowed: boolean;
}
/**
 * If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsConfig".
 */
/** @experimental */
export interface PermissionPathsConfig {
  /**
   * If true, the runtime allows access to all paths without prompting. Equivalent to constructing an UnrestrictedPathManager.
   */
  unrestricted?: boolean;
  /**
   * Additional directories to allow tool access to (in addition to the session's working directory). When `unrestricted` is true, these are still pre-populated on the UnrestrictedPathManager so they remain visible via getDirectories() (e.g. for @-mention completion).
   */
  additionalDirectories?: string[];
  /**
   * Whether to include the system temp directory in the allowed list (defaults to true). Ignored when `unrestricted` is true.
   */
  includeTempDirectory?: boolean;
  /**
   * Workspace root path (special-cased to be allowed even before the directory exists). Ignored when `unrestricted` is true.
   */
  workspacePath?: string;
}
/**
 * Snapshot of the session's allow-listed directories and primary working directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsList".
 */
/** @experimental */
export interface PermissionPathsList {
  /**
   * All directories currently allowed for tool access on this session.
   */
  directories: string[];
  /**
   * The primary working directory for this session.
   */
  primary: string;
}
/**
 * Directory path to set as the session's new primary working directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsUpdatePrimaryParams".
 */
/** @experimental */
export interface PermissionPathsUpdatePrimaryParams {
  /**
   * Directory to set as the new primary working directory for the session's permission policy.
   */
  path: string;
}
/**
 * Path to evaluate against the session's workspace (primary) directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsWorkspaceCheckParams".
 */
/** @experimental */
export interface PermissionPathsWorkspaceCheckParams {
  /**
   * Path to check against the session workspace directory
   */
  path: string;
}
/**
 * Indicates whether the supplied path is within the session's workspace directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsWorkspaceCheckResult".
 */
/** @experimental */
export interface PermissionPathsWorkspaceCheckResult {
  /**
   * Whether the path is within the session workspace directory
   */
  allowed: boolean;
}
/**
 * Notification payload describing the permission prompt that the client just rendered.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPromptShownNotification".
 */
/** @experimental */
export interface PermissionPromptShownNotification {
  /**
   * Human-readable description of the prompt the user is being asked to approve. Used by the runtime to fire the registered `permission_prompt` notification hook (e.g. terminal bell, desktop notification).
   */
  message: string;
}
/**
 * Indicates whether the permission decision was applied; false when the request was already resolved.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionRequestResult".
 */
/** @experimental */
export interface PermissionRequestResult {
  /**
   * Whether the permission request was handled successfully
   */
  success: boolean;
}
/**
 * If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionRulesSet".
 */
/** @experimental */
export interface PermissionRulesSet {
  /**
   * Rules that auto-approve matching requests
   */
  approved: PermissionRule[];
  /**
   * Rules that auto-deny matching requests
   */
  denied: PermissionRule[];
}
/**
 * Schema for the `PermissionsConfigureAdditionalContentExclusionPolicy` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsConfigureAdditionalContentExclusionPolicy".
 */
/** @experimental */
export interface PermissionsConfigureAdditionalContentExclusionPolicy {
  rules: PermissionsConfigureAdditionalContentExclusionPolicyRule[];
  last_updated_at: string | number;
  scope: PermissionsConfigureAdditionalContentExclusionPolicyScope;
  [k: string]: unknown | undefined;
}
/**
 * Schema for the `PermissionsConfigureAdditionalContentExclusionPolicyRule` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsConfigureAdditionalContentExclusionPolicyRule".
 */
/** @experimental */
export interface PermissionsConfigureAdditionalContentExclusionPolicyRule {
  paths: string[];
  ifAnyMatch?: string[];
  ifNoneMatch?: string[];
  source: PermissionsConfigureAdditionalContentExclusionPolicyRuleSource;
  [k: string]: unknown | undefined;
}
/**
 * Schema for the `PermissionsConfigureAdditionalContentExclusionPolicyRuleSource` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsConfigureAdditionalContentExclusionPolicyRuleSource".
 */
/** @experimental */
export interface PermissionsConfigureAdditionalContentExclusionPolicyRuleSource {
  name: string;
  type: string;
}
/**
 * Patch of permission policy fields to apply (omit a field to leave it unchanged).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsConfigureParams".
 */
/** @experimental */
export interface PermissionsConfigureParams {
  /**
   * If specified, sets whether tool permission requests are auto-approved without prompting. Omit to leave the current value unchanged.
   */
  approveAllToolPermissionRequests?: boolean;
  /**
   * If specified, sets whether path/URL read permission requests are auto-approved. Omit to leave the current value unchanged.
   */
  approveAllReadPermissionRequests?: boolean;
  rules?: PermissionRulesSet;
  paths?: PermissionPathsConfig;
  urls?: PermissionUrlsConfig;
  /**
   * If specified, replaces the host-supplied GitHub Content Exclusion policies on the session (combined with natively-discovered policies when evaluating tool/file access). Omit to leave the current policies unchanged.
   */
  additionalContentExclusionPolicies?: PermissionsConfigureAdditionalContentExclusionPolicy[];
}
/**
 * If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionUrlsConfig".
 */
/** @experimental */
export interface PermissionUrlsConfig {
  /**
   * If true, the runtime allows access to all URLs without prompting. Initial allow-list is ignored when this is true.
   */
  unrestricted?: boolean;
  /**
   * Initial list of allowed URL/domain patterns. Patterns may include path components. Ignored when `unrestricted` is true.
   */
  initialAllowed?: string[];
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsConfigureResult".
 */
/** @experimental */
export interface PermissionsConfigureResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsFolderTrustAddTrustedResult".
 */
/** @experimental */
export interface PermissionsFolderTrustAddTrustedResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalResult".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Scope and add/remove instructions for modifying session- or location-scoped permission rules.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsModifyRulesParams".
 */
/** @experimental */
export interface PermissionsModifyRulesParams {
  scope: PermissionsModifyRulesScope;
  /**
   * Rules to add to the scope. Applied before `remove`/`removeAll`.
   */
  add?: PermissionRule[];
  /**
   * Specific rules to remove from the scope. Ignored when `removeAll` is true.
   */
  remove?: PermissionRule[];
  /**
   * When true, removes every rule currently in the scope (after any `add` is applied). Useful for clearing the location scope wholesale.
   */
  removeAll?: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsModifyRulesResult".
 */
/** @experimental */
export interface PermissionsModifyRulesResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsNotifyPromptShownResult".
 */
/** @experimental */
export interface PermissionsNotifyPromptShownResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsPathsAddResult".
 */
/** @experimental */
export interface PermissionsPathsAddResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * No parameters; returns the session's allow-listed directories.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsPathsListRequest".
 */
export interface PermissionsPathsListRequest {}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsPathsUpdatePrimaryResult".
 */
/** @experimental */
export interface PermissionsPathsUpdatePrimaryResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * No parameters; returns currently-pending permission requests for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsPendingRequestsRequest".
 */
export interface PermissionsPendingRequestsRequest {}
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
/** @experimental */
export interface PermissionsResetSessionApprovalsResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Allow-all toggle for tool permission requests, with an optional telemetry source.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetApproveAllRequest".
 */
/** @experimental */
export interface PermissionsSetApproveAllRequest {
  /**
   * Whether to auto-approve all tool permission requests
   */
  enabled: boolean;
  source?: PermissionsSetApproveAllSource;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetApproveAllResult".
 */
/** @experimental */
export interface PermissionsSetApproveAllResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Toggles whether permission prompts should be bridged into session events for this client.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetRequiredRequest".
 */
/** @experimental */
export interface PermissionsSetRequiredRequest {
  /**
   * Whether the client wants `permission.requested` events bridged from the session-owned permission service. CLI clients that render prompt UI set this to `true` for as long as their listener is mounted; headless callers leave it unset (the default is `false`).
   */
  required: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetRequiredResult".
 */
/** @experimental */
export interface PermissionsSetRequiredResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsUrlsSetUnrestrictedModeResult".
 */
/** @experimental */
export interface PermissionsUrlsSetUnrestrictedModeResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Whether the URL-permission policy should run in unrestricted mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionUrlsSetUnrestrictedModeParams".
 */
/** @experimental */
export interface PermissionUrlsSetUnrestrictedModeParams {
  /**
   * Whether to allow access to all URLs without prompting. Toggles the runtime's URL-permission policy in place.
   */
  enabled: boolean;
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
 * Server liveness response, including the echoed message, current server timestamp, and protocol version.
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
   * ISO 8601 timestamp when the server handled the ping
   */
  timestamp: string;
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
 * Schema for the `QueuePendingItems` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuePendingItems".
 */
/** @experimental */
export interface QueuePendingItems {
  kind: QueuePendingItemsKind;
  /**
   * Human-readable text to display for this queue entry in the UI
   */
  displayText: string;
}
/**
 * Snapshot of the session's pending queued items and immediate-steering messages.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuePendingItemsResult".
 */
/** @experimental */
export interface QueuePendingItemsResult {
  /**
   * Pending queued items in submission order. Includes user messages, queued slash commands, and queued model changes; omits internal system items.
   */
  items: QueuePendingItems[];
  /**
   * Display text for messages currently in the immediate steering queue (interjections sent during a running turn).
   */
  steeringMessages: string[];
}
/**
 * Indicates whether a user-facing pending item was removed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueRemoveMostRecentResult".
 */
/** @experimental */
export interface QueueRemoveMostRecentResult {
  /**
   * True if a user-facing pending item was removed (LIFO across both queues); false when no removable items remained.
   */
  removed: boolean;
}
/**
 * Event type to register consumer interest for, used by runtime gating logic.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RegisterEventInterestParams".
 */
/** @experimental */
export interface RegisterEventInterestParams {
  /**
   * The event type the consumer wants the runtime to treat as 'observed' for behavior-switching gating. Some runtime code paths inspect whether any consumer is interested in a specific event type and choose a different implementation accordingly (e.g. `mcp.oauth_required`: when interest is registered the runtime delegates the full interactive OAuth flow to the consumer; when no interest is registered the runtime installs a browserless fallback that silently reuses cached tokens). SDK clients that long-poll events do NOT automatically appear as listeners to these gating checks — they must explicitly call `registerInterest` for each event type they want the runtime to count as having a consumer. Multiple registrations for the same event type from the same or different consumers are tracked independently and must each be released. See: `mcp.oauth_required`, `sampling.requested`, `auto_mode_switch.requested`, `user_input.requested`, `elicitation.requested`, `command.queued`, `exit_plan_mode.requested`.
   */
  eventType: string;
}
/**
 * Opaque handle representing an event-type interest registration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RegisterEventInterestResult".
 */
/** @experimental */
export interface RegisterEventInterestResult {
  /**
   * Opaque handle for this registration. Pass to releaseInterest to release. Each call to registerInterest produces a fresh handle, even when the same eventType is registered multiple times.
   */
  handle: string;
}
/**
 * Opaque handle previously returned by `registerInterest` to release.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ReleaseEventInterestParams".
 */
/** @experimental */
export interface ReleaseEventInterestParams {
  /**
   * Handle returned by a previous `registerInterest` call. Idempotent: releasing an unknown or already-released handle is a no-op (returns success). When the last outstanding handle for an event type is released, the runtime reverts to its 'no consumer' code path for that event type.
   */
  handle: string;
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
 * New remote-steerability state to persist as a `session.remote_steerable_changed` event.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteNotifySteerableChangedRequest".
 */
/** @experimental */
export interface RemoteNotifySteerableChangedRequest {
  /**
   * Whether the session now supports remote steering via GitHub. The runtime persists this as a `session.remote_steerable_changed` event so resume/replay sees the up-to-date capability.
   */
  remoteSteerable: boolean;
}
/**
 * Persist a steerability change as a `session.remote_steerable_changed` event. Used by the host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a remote exporter that the runtime does not directly own.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteNotifySteerableChangedResult".
 */
/** @experimental */
export interface RemoteNotifySteerableChangedResult {}
/**
 * Remote session connection result.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteSessionConnectionResult".
 */
/** @experimental */
export interface RemoteSessionConnectionResult {
  /**
   * SDK session ID for the connected remote session.
   */
  sessionId: string;
  metadata: ConnectedRemoteSessionMetadata;
}
/**
 * Schema for the `ScheduleEntry` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleEntry".
 */
/** @experimental */
export interface ScheduleEntry {
  /**
   * Sequential id assigned by the runtime within the session. Stable across resumes (rebuilt from the event log).
   */
  id: number;
  /**
   * Interval between scheduled ticks, in milliseconds.
   */
  intervalMs: number;
  /**
   * Prompt text that gets enqueued on every tick.
   */
  prompt: string;
  /**
   * Whether the schedule re-arms after each tick (`/every`) or fires once (`/after`).
   */
  recurring: boolean;
  /**
   * Display-only label for the prompt as shown in the UI (e.g. `/skill-name` for a skill-invocation schedule). The actual enqueued prompt is `prompt`.
   */
  displayPrompt?: string;
  /**
   * ISO 8601 timestamp when the next tick is scheduled to fire.
   */
  nextRunAt: string;
}
/**
 * Snapshot of the currently active recurring prompts for this session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleList".
 */
/** @experimental */
export interface ScheduleList {
  /**
   * Active scheduled prompts, ordered by id.
   */
  entries: ScheduleEntry[];
}
/**
 * Identifier of the scheduled prompt to remove.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleStopRequest".
 */
/** @experimental */
export interface ScheduleStopRequest {
  /**
   * Id of the scheduled prompt to remove.
   */
  id: number;
}
/**
 * Remove a scheduled prompt by id. The result entry is omitted if the id was unknown.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleStopResult".
 */
/** @experimental */
export interface ScheduleStopResult {
  entry?: ScheduleEntry;
}
/**
 * Secret values to add to the redaction filter.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SecretsAddFilterValuesRequest".
 */
export interface SecretsAddFilterValuesRequest {
  /**
   * Raw secret values to register for redaction
   */
  values: string[];
}
/**
 * Confirmation that the secret values were registered.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SecretsAddFilterValuesResult".
 */
export interface SecretsAddFilterValuesResult {
  /**
   * Whether the values were successfully registered
   */
  ok: true;
}
/**
 * File attachment
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAttachmentFile".
 */
/** @experimental */
export interface SendAttachmentFile {
  /**
   * Attachment type discriminator
   */
  type: "file";
  /**
   * Absolute file path
   */
  path: string;
  /**
   * User-facing display name for the attachment
   */
  displayName: string;
  lineRange?: SendAttachmentFileLineRange;
}
/**
 * Optional line range to scope the attachment to a specific section of the file
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAttachmentFileLineRange".
 */
/** @experimental */
export interface SendAttachmentFileLineRange {
  /**
   * Start line number (1-based)
   */
  start: number;
  /**
   * End line number (1-based, inclusive)
   */
  end: number;
}
/**
 * Directory attachment
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAttachmentDirectory".
 */
/** @experimental */
export interface SendAttachmentDirectory {
  /**
   * Attachment type discriminator
   */
  type: "directory";
  /**
   * Absolute directory path
   */
  path: string;
  /**
   * User-facing display name for the attachment
   */
  displayName: string;
}
/**
 * Code selection attachment from an editor
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAttachmentSelection".
 */
/** @experimental */
export interface SendAttachmentSelection {
  /**
   * Attachment type discriminator
   */
  type: "selection";
  /**
   * Absolute path to the file containing the selection
   */
  filePath: string;
  /**
   * User-facing display name for the selection
   */
  displayName: string;
  /**
   * The selected text content
   */
  text: string;
  selection: SendAttachmentSelectionDetails;
}
/**
 * Position range of the selection within the file
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAttachmentSelectionDetails".
 */
/** @experimental */
export interface SendAttachmentSelectionDetails {
  start: SendAttachmentSelectionDetailsStart;
  end: SendAttachmentSelectionDetailsEnd;
}
/**
 * Start position of the selection
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAttachmentSelectionDetailsStart".
 */
/** @experimental */
export interface SendAttachmentSelectionDetailsStart {
  /**
   * Start line number (0-based)
   */
  line: number;
  /**
   * Start character offset within the line (0-based)
   */
  character: number;
}
/**
 * End position of the selection
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAttachmentSelectionDetailsEnd".
 */
/** @experimental */
export interface SendAttachmentSelectionDetailsEnd {
  /**
   * End line number (0-based)
   */
  line: number;
  /**
   * End character offset within the line (0-based)
   */
  character: number;
}
/**
 * GitHub issue, pull request, or discussion reference
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAttachmentGithubReference".
 */
/** @experimental */
export interface SendAttachmentGithubReference {
  /**
   * Attachment type discriminator
   */
  type: "github_reference";
  /**
   * Issue, pull request, or discussion number
   */
  number: number;
  /**
   * Title of the referenced item
   */
  title: string;
  referenceType: SendAttachmentGithubReferenceType;
  /**
   * Current state of the referenced item (e.g., open, closed, merged)
   */
  state: string;
  /**
   * URL to the referenced item on GitHub
   */
  url: string;
}
/**
 * Blob attachment with inline base64-encoded data
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAttachmentBlob".
 */
/** @experimental */
export interface SendAttachmentBlob {
  /**
   * Attachment type discriminator
   */
  type: "blob";
  /**
   * Base64-encoded content
   */
  data: string;
  /**
   * MIME type of the inline data
   */
  mimeType: string;
  /**
   * User-facing display name for the attachment
   */
  displayName?: string;
}
/**
 * Parameters for sending a user message to the session
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendRequest".
 */
/** @experimental */
export interface SendRequest {
  /**
   * The user message text
   */
  prompt: string;
  /**
   * If provided, this is shown in the timeline instead of `prompt`
   */
  displayPrompt?: string;
  /**
   * Optional attachments (files, directories, selections, blobs, GitHub references) to include with the message
   */
  attachments?: SendAttachment[];
  mode?: SendMode;
  /**
   * If true, adds the message to the front of the queue instead of the end
   */
  prepend?: boolean;
  /**
   * If false, this message will not trigger a Premium Request Unit charge. User messages default to billable.
   */
  billable?: boolean;
  /**
   * If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange
   */
  requiredTool?: string;
  /**
   * Optional provenance tag copied to the resulting user.message event. Supported values are `system`, `command-*`, and `schedule-*`.
   *
   * @internal
   */
  source?: {
    [k: string]: unknown | undefined;
  };
  agentMode?: SendAgentMode;
  /**
   * Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key.
   */
  requestHeaders?: {
    [k: string]: string | undefined;
  };
  /**
   * W3C Trace Context traceparent header for distributed tracing of this agent turn
   */
  traceparent?: string;
  /**
   * W3C Trace Context tracestate header for distributed tracing
   */
  tracestate?: string;
  /**
   * If true, await completion of the agentic loop for this message before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageId`; the caller can rely on the agent having processed the message before the call resolves.
   */
  wait?: boolean;
}
/**
 * Result of sending a user message
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendResult".
 */
/** @experimental */
export interface SendResult {
  /**
   * Unique identifier assigned to the message
   */
  messageId: string;
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
  source: SkillSource;
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
/** @experimental */
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
 * Map of sessionId -> bytes freed by removing the session's workspace directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionBulkDeleteResult".
 */
/** @experimental */
export interface SessionBulkDeleteResult {
  /**
   * Map of sessionId -> bytes freed by removing the session's workspace directory. Sessions whose deletion failed are omitted from this map (failures are logged on the server but not surfaced per-id; check the map for absent IDs to detect them).
   */
  freedBytes: {
    [k: string]: number | undefined;
  };
}
/**
 * Schema for the `SessionContext` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionContext".
 */
/** @experimental */
export interface SessionContext {
  /**
   * Most recent working directory for this session
   */
  cwd: string;
  /**
   * Git repository root, if the cwd was inside a git repo
   */
  gitRoot?: string;
  /**
   * Repository slug in `owner/name` form, when known
   */
  repository?: string;
  hostType?: SessionContextHostType;
  /**
   * Active git branch
   */
  branch?: string;
}
/**
 * The same metadata records, with summary and context fields backfilled where available.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionEnrichMetadataResult".
 */
/** @experimental */
export interface SessionEnrichMetadataResult {
  /**
   * Same records, with summary and context backfilled
   */
  sessions: SessionMetadata[];
}
/**
 * Schema for the `SessionMetadata` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionMetadata".
 */
/** @experimental */
export interface SessionMetadata {
  /**
   * Stable session identifier
   */
  sessionId: string;
  /**
   * Session creation time as an ISO 8601 timestamp
   */
  startTime: string;
  /**
   * Last-modified time of the session's persisted state, as ISO 8601
   */
  modifiedTime: string;
  /**
   * Short summary of the session, when one has been derived
   */
  summary?: string;
  /**
   * Optional human-friendly name set via /rename
   */
  name?: string;
  /**
   * True for remote (GitHub) sessions; false for local
   */
  isRemote: boolean;
  context?: SessionContext;
  /**
   * GitHub task ID, when this local session is bound to one. Only present for local sessions exported to remote control.
   */
  mcTaskId?: string;
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
 * Optional capabilities declared by the provider
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSetProviderCapabilities".
 */
export interface SessionFsSetProviderCapabilities {
  /**
   * Whether the provider supports SQLite query/exists operations
   */
  sqlite?: boolean;
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
  capabilities?: SessionFsSetProviderCapabilities;
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
    [k: string]: (string | number | null) | undefined;
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
    [k: string]: unknown | undefined;
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
   * SQLite last_insert_rowid() value for INSERT.
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
 * Schema for the `SessionInstalledPlugin` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionInstalledPlugin".
 */
/** @experimental */
export interface SessionInstalledPlugin {
  /**
   * Plugin name
   */
  name: string;
  /**
   * Marketplace the plugin came from (empty string for direct repo installs)
   */
  marketplace: string;
  /**
   * Installed version, if known
   */
  version?: string;
  /**
   * Installation timestamp (ISO-8601)
   */
  installed_at: string;
  /**
   * Whether the plugin is currently enabled
   */
  enabled: boolean;
  /**
   * Path where the plugin is cached locally
   */
  cache_path?: string;
  source?: SessionInstalledPluginSource;
}
/**
 * Schema for the `SessionInstalledPluginSourceGithub` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionInstalledPluginSourceGithub".
 */
/** @experimental */
export interface SessionInstalledPluginSourceGithub {
  /**
   * Constant value. Always "github".
   */
  source: "github";
  repo: string;
  ref?: string;
  path?: string;
}
/**
 * Schema for the `SessionInstalledPluginSourceUrl` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionInstalledPluginSourceUrl".
 */
/** @experimental */
export interface SessionInstalledPluginSourceUrl {
  /**
   * Constant value. Always "url".
   */
  source: "url";
  url: string;
  ref?: string;
  path?: string;
}
/**
 * Schema for the `SessionInstalledPluginSourceLocal` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionInstalledPluginSourceLocal".
 */
/** @experimental */
export interface SessionInstalledPluginSourceLocal {
  /**
   * Constant value. Always "local".
   */
  source: "local";
  path: string;
}
/**
 * Persisted sessions matching the filter, ordered most-recently-modified first.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionList".
 */
/** @experimental */
export interface SessionList {
  /**
   * Sessions ordered most-recently-modified first
   */
  sessions: SessionMetadata[];
}
/**
 * Optional filter applied to the returned sessions
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionListFilter".
 */
/** @experimental */
export interface SessionListFilter {
  /**
   * Match sessions whose context.cwd equals this value
   */
  cwd?: string;
  /**
   * Match sessions whose context.gitRoot equals this value
   */
  gitRoot?: string;
  /**
   * Match sessions whose context.repository equals this value
   */
  repository?: string;
  /**
   * Match sessions whose context.branch equals this value
   */
  branch?: string;
}
/**
 * Queued repo-level startup prompts and the total hook command count after loading.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLoadDeferredRepoHooksResult".
 */
/** @experimental */
export interface SessionLoadDeferredRepoHooksResult {
  /**
   * Repo-level startup prompts queued from repo hook configs. Empty on resume, when no repo configs were pending, or when disableAllHooks is set.
   */
  startupPrompts: string[];
  /**
   * Total hook command count (user + plugin + repo) loaded for the session by this call. Captured atomically with startupPrompts so callers don't need to read a separate counter.
   */
  hookCount: number;
}
/**
 * Point-in-time snapshot of slow-changing session identifier and state fields
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionMetadataSnapshot".
 */
/** @experimental */
export interface SessionMetadataSnapshot {
  /**
   * The unique identifier of the session
   */
  sessionId: string;
  /**
   * ISO 8601 timestamp of when the session started
   */
  startTime: string;
  /**
   * ISO 8601 timestamp of when the session's persisted state was last modified on disk. For new sessions, equals startTime. For resumed sessions, reflects the previous modification time at construction.
   */
  modifiedTime: string;
  /**
   * Whether this is a remote session (i.e., one whose runtime executes elsewhere and is steered through this process)
   */
  isRemote: boolean;
  /**
   * True when the session was detected to be in use by another process at construction time. Local consumers may surface a confirmation prompt before fully attaching. Always false for new sessions.
   */
  alreadyInUse: boolean;
  /**
   * Absolute path to the session's workspace directory on disk, or null if the session has no associated workspace
   */
  workspacePath: string | null;
  /**
   * User-provided name supplied at session construction (via `--name`), if any. Immutable after construction.
   */
  initialName?: string;
  remoteMetadata?: MetadataSnapshotRemoteMetadata;
  /**
   * Short human-readable summary of the session, if known. Omitted when no summary has been generated.
   */
  summary?: string;
  /**
   * Absolute path to the session's current working directory
   */
  workingDirectory: string;
  currentMode: MetadataSnapshotCurrentMode;
  /**
   * Currently selected model identifier, if any
   */
  selectedModel?: string;
  /**
   * Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags).
   */
  workspace?: WorkspaceSummary | null;
}
/**
 * Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionPruneResult".
 */
/** @experimental */
export interface SessionPruneResult {
  /**
   * Session IDs that were deleted (always empty in dry-run mode)
   */
  deleted: string[];
  /**
   * Session IDs that would be deleted in dry-run mode (always empty otherwise)
   */
  candidates: string[];
  /**
   * Session IDs that were skipped (e.g., named sessions)
   */
  skipped: string[];
  /**
   * Total bytes freed (actual when not dry-run, projected when dry-run)
   */
  freedBytes: number;
  /**
   * True when no deletions were actually performed
   */
  dryRun: boolean;
}
/**
 * Session IDs to close, deactivate, and delete from disk.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsBulkDeleteRequest".
 */
/** @experimental */
export interface SessionsBulkDeleteRequest {
  /**
   * Session IDs to close, deactivate, and delete from disk
   */
  sessionIds: string[];
}
/**
 * Session IDs to test for live in-use locks.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsCheckInUseRequest".
 */
/** @experimental */
export interface SessionsCheckInUseRequest {
  /**
   * Session IDs to test for live in-use locks
   */
  sessionIds: string[];
}
/**
 * Session IDs from the input set that are currently in use by another process.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsCheckInUseResult".
 */
/** @experimental */
export interface SessionsCheckInUseResult {
  /**
   * Session IDs from the input set that are currently held by another running process via an alive lock file
   */
  inUse: string[];
}
/**
 * Session ID to close.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsCloseRequest".
 */
/** @experimental */
export interface SessionsCloseRequest {
  /**
   * Session ID to close
   */
  sessionId: string;
}
/**
 * Closes a session: emits shutdown, flushes pending events to disk, releases the in-use lock, disposes the active session. Idempotent: succeeds even if the session is not currently active.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsCloseResult".
 */
/** @experimental */
export interface SessionsCloseResult {}
/**
 * Session metadata records to enrich with summary and context information.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsEnrichMetadataRequest".
 */
/** @experimental */
export interface SessionsEnrichMetadataRequest {
  /**
   * Session metadata records to enrich. Records that already have summary and context are returned unchanged.
   */
  sessions: SessionMetadata[];
}
/**
 * New auth credentials to install on the session. Omit to leave credentials unchanged.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSetCredentialsParams".
 */
/** @experimental */
export interface SessionSetCredentialsParams {
  credentials?: AuthInfo;
}
/**
 * Indicates whether the credential update succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSetCredentialsResult".
 */
/** @experimental */
export interface SessionSetCredentialsResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * UUID prefix to resolve to a unique session ID.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsFindByPrefixRequest".
 */
/** @experimental */
export interface SessionsFindByPrefixRequest {
  /**
   * UUID prefix (>=7 hex chars, <36 chars). Returns the unique session ID, or undefined when there is no match or the prefix matches multiple sessions.
   */
  prefix: string;
}
/**
 * Session ID matching the prefix, omitted when no unique match exists.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsFindByPrefixResult".
 */
/** @experimental */
export interface SessionsFindByPrefixResult {
  /**
   * Omitted when no unique session matches the prefix (no match or ambiguous)
   */
  sessionId?: string;
}
/**
 * GitHub task ID to look up.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsFindByTaskIDRequest".
 */
/** @experimental */
export interface SessionsFindByTaskIDRequest {
  /**
   * GitHub task ID to look up
   */
  taskId: string;
}
/**
 * ID of the local session bound to the given GitHub task, or omitted when none.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsFindByTaskIDResult".
 */
/** @experimental */
export interface SessionsFindByTaskIDResult {
  /**
   * Omitted when no local session is bound to that GitHub task
   */
  sessionId?: string;
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
 * Session ID whose event-log file path to compute.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetEventFilePathRequest".
 */
/** @experimental */
export interface SessionsGetEventFilePathRequest {
  /**
   * Session ID whose event-log file path to compute
   */
  sessionId: string;
}
/**
 * Absolute path to the session's events.jsonl file on disk.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetEventFilePathResult".
 */
/** @experimental */
export interface SessionsGetEventFilePathResult {
  /**
   * Absolute path to the session's events.jsonl file
   */
  filePath: string;
}
/**
 * Optional working-directory context used to score session relevance.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetLastForContextRequest".
 */
/** @experimental */
export interface SessionsGetLastForContextRequest {
  context?: SessionContext;
}
/**
 * Most-relevant session ID for the supplied context, or omitted when no sessions exist.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetLastForContextResult".
 */
/** @experimental */
export interface SessionsGetLastForContextResult {
  /**
   * Most-relevant session ID for the supplied context, or omitted when no sessions exist
   */
  sessionId?: string;
}
/**
 * Session ID to look up the persisted remote-steerable flag for.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetPersistedRemoteSteerableRequest".
 */
/** @experimental */
export interface SessionsGetPersistedRemoteSteerableRequest {
  /**
   * Session ID to look up the persisted remote-steerable flag for
   */
  sessionId: string;
}
/**
 * The session's persisted remote-steerable flag, or omitted when no value has been persisted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetPersistedRemoteSteerableResult".
 */
/** @experimental */
export interface SessionsGetPersistedRemoteSteerableResult {
  /**
   * The session's persisted remote-steerable flag if recorded; omitted when no value has been persisted
   */
  remoteSteerable?: boolean;
}
/**
 * Map of sessionId -> on-disk size in bytes for each session's workspace directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSizes".
 */
/** @experimental */
export interface SessionSizes {
  /**
   * Map of sessionId -> on-disk size in bytes for the session's workspace directory
   */
  sizes: {
    [k: string]: number | undefined;
  };
}
/**
 * Optional metadata-load limit and context filter applied to the returned sessions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsListRequest".
 */
/** @experimental */
export interface SessionsListRequest {
  /**
   * When provided, only the first N sessions (sorted by modification time, newest first) load full metadata; remaining sessions return basic info only. Use 0 to return only basic info for every session.
   */
  metadataLimit?: number;
  filter?: SessionListFilter;
}
/**
 * Active session ID whose deferred repo-level hooks should be loaded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsLoadDeferredRepoHooksRequest".
 */
/** @experimental */
export interface SessionsLoadDeferredRepoHooksRequest {
  /**
   * Active session ID whose deferred repo-level hooks should be loaded
   */
  sessionId: string;
}
/**
 * Age threshold and optional flags controlling which old sessions are pruned (or simulated when dryRun is true).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsPruneOldRequest".
 */
/** @experimental */
export interface SessionsPruneOldRequest {
  /**
   * Delete sessions whose modifiedTime is at least this many days old
   */
  olderThanDays: number;
  /**
   * When true, only report what would be deleted without performing any deletion
   */
  dryRun?: boolean;
  /**
   * When true, named sessions (set via /rename) are also eligible for pruning
   */
  includeNamed?: boolean;
  /**
   * Session IDs that should never be considered for pruning
   */
  excludeSessionIds?: string[];
}
/**
 * Session ID whose in-use lock should be released.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsReleaseLockRequest".
 */
/** @experimental */
export interface SessionsReleaseLockRequest {
  /**
   * Session ID whose in-use lock should be released
   */
  sessionId: string;
}
/**
 * Release the in-use lock held by this process for the given session. No-op when this process does not currently hold a lock for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsReleaseLockResult".
 */
/** @experimental */
export interface SessionsReleaseLockResult {}
/**
 * Active session ID and an optional flag for deferring repo-level hooks until folder trust.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsReloadPluginHooksRequest".
 */
/** @experimental */
export interface SessionsReloadPluginHooksRequest {
  /**
   * Active session ID to reload hooks for
   */
  sessionId: string;
  /**
   * When true, skip repo-level hooks. Use before folder trust is confirmed; loadDeferredRepoHooks loads them post-trust.
   */
  deferRepoHooks?: boolean;
}
/**
 * Reload all hooks (user, plugin, optionally repo) and apply them to the active session. Call after installing or removing plugins so their hooks take effect immediately. No-op when no active session matches the given sessionId.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsReloadPluginHooksResult".
 */
/** @experimental */
export interface SessionsReloadPluginHooksResult {}
/**
 * Session ID whose pending events should be flushed to disk.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsSaveRequest".
 */
/** @experimental */
export interface SessionsSaveRequest {
  /**
   * Session ID whose pending events should be flushed to disk
   */
  sessionId: string;
}
/**
 * Flush a session's pending events to disk. No-op when no writer exists for the session (e.g., already closed).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsSaveResult".
 */
/** @experimental */
export interface SessionsSaveResult {}
/**
 * Manager-wide additional plugins to register; replaces any previously-configured set.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsSetAdditionalPluginsRequest".
 */
/** @experimental */
export interface SessionsSetAdditionalPluginsRequest {
  /**
   * Manager-wide additional plugins to register. Replaces any previously-configured set. Pass an empty array to clear.
   */
  plugins: InstalledPlugin[];
}
/**
 * Replace the manager-wide additional plugins. New session creations and subsequent hook reloads see the new set; already-running sessions keep their existing hook installation until the next reload.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsSetAdditionalPluginsResult".
 */
/** @experimental */
export interface SessionsSetAdditionalPluginsResult {}
/**
 * Patch of mutable session options to apply to the running session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionUpdateOptionsParams".
 */
/** @experimental */
export interface SessionUpdateOptionsParams {
  /**
   * The model ID to use for assistant turns.
   */
  model?: string;
  /**
   * Reasoning effort for the selected model (model-defined enum).
   */
  reasoningEffort?: string;
  /**
   * Identifier of the client driving the session.
   */
  clientName?: string;
  /**
   * Identifier sent to LSP-style integrations.
   */
  lspClientName?: string;
  /**
   * Stable integration identifier used for analytics and rate-limit attribution.
   */
  integrationId?: string;
  /**
   * Map of feature-flag IDs to their boolean enabled state.
   */
  featureFlags?: {
    [k: string]: boolean | undefined;
  };
  /**
   * Whether experimental capabilities are enabled.
   */
  isExperimentalMode?: boolean;
  /**
   * Custom model-provider configuration (BYOK). Opaque shape; see `ProviderConfig` in the runtime.
   *
   * @experimental
   */
  provider?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Absolute working-directory path for shell tools.
   */
  workingDirectory?: string;
  /**
   * Allowlist of tool names available to this session.
   */
  availableTools?: string[];
  /**
   * Denylist of tool names for this session.
   */
  excludedTools?: string[];
  /**
   * Whether shell-script safety heuristics are enabled.
   */
  enableScriptSafety?: boolean;
  /**
   * Shell init profile (`None` or `NonInteractive`).
   */
  shellInitProfile?: string;
  /**
   * Per-shell process flags (e.g., `pwsh` arguments).
   */
  shellProcessFlags?: string[];
  /**
   * Sandbox configuration shape; opaque to SDK consumers. See `SandboxConfig` in the runtime.
   *
   * @experimental
   */
  sandboxConfig?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Whether interactive shell sessions are logged.
   */
  logInteractiveShells?: boolean;
  envValueMode?: OptionsUpdateEnvValueMode;
  /**
   * Additional directories to search for skills.
   */
  skillDirectories?: string[];
  /**
   * Skill IDs that should be excluded from this session.
   */
  disabledSkills?: string[];
  /**
   * Whether to discover custom instructions on demand after successful file views (AGENTS.md / CLAUDE.md / .github/copilot-instructions.md surfacing). Combined with `skipCustomInstructions` and the runtime-side `ON_DEMAND_INSTRUCTIONS` feature flag.
   */
  enableOnDemandInstructionDiscovery?: boolean;
  /**
   * Full set of installed plugins for the session. Replaces the existing list; the runtime invalidates the skills cache only when the list materially changes.
   */
  installedPlugins?: SessionInstalledPlugin[];
  /**
   * Whether to default custom agents to local-only execution.
   */
  customAgentsLocalOnly?: boolean;
  /**
   * Whether to skip loading custom instruction sources.
   */
  skipCustomInstructions?: boolean;
  /**
   * Instruction source IDs to exclude from the system prompt.
   */
  disabledInstructionSources?: string[];
  /**
   * Whether to include the `Co-authored-by` trailer in commit messages.
   */
  coauthorEnabled?: boolean;
  /**
   * Optional path for trajectory output.
   */
  trajectoryFile?: string;
  /**
   * Whether to stream model responses.
   */
  enableStreaming?: boolean;
  /**
   * Override URL for the Copilot API endpoint.
   */
  copilotUrl?: string;
  /**
   * Whether to disable the `ask_user` tool (encourages autonomous behavior).
   */
  askUserDisabled?: boolean;
  /**
   * Whether to allow auto-mode continuation across turns.
   */
  continueOnAutoMode?: boolean;
  /**
   * Whether the session is running in an interactive UI.
   */
  runningInInteractiveMode?: boolean;
  /**
   * Whether to surface reasoning-summary events from the model.
   */
  enableReasoningSummaries?: boolean;
  /**
   * Runtime context discriminator (e.g., `cli`, `actions`).
   */
  agentContext?: string;
  /**
   * Override directory for the session-events log. When unset, the runtime's default events log directory is used.
   */
  eventsLogDirectory?: string;
  /**
   * Additional content-exclusion policies to merge into the session's policy set. Opaque shape; see `ContentExclusionApiResponse` in the runtime.
   *
   * @experimental
   */
  additionalContentExclusionPolicies?: unknown[];
  /**
   * Whether to expose the `manage_schedule` tool to the agent. The runtime always owns the per-session schedule registry; this flag only controls tool exposure (typically gated to staff users).
   */
  manageScheduleEnabled?: boolean;
}
/**
 * Indicates whether the session options patch was applied successfully.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionUpdateOptionsResult".
 */
/** @experimental */
export interface SessionUpdateOptionsResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Shell command to run, with optional working directory and timeout in milliseconds.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellExecRequest".
 */
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
export interface ShellKillResult {
  /**
   * Whether the signal was sent successfully
   */
  killed: boolean;
}
/**
 * Parameters for shutting down the session
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShutdownRequest".
 */
/** @experimental */
export interface ShutdownRequest {
  type?: ShutdownType;
  /**
   * Optional human-readable reason. Typically the message of the error that triggered shutdown when type is 'error'.
   */
  reason?: string;
}
/**
 * Schema for the `Skill` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "Skill".
 */
/** @experimental */
export interface Skill {
  /**
   * Unique identifier for the skill
   */
  name: string;
  /**
   * Description of what the skill does
   */
  description: string;
  source: SkillSource;
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
  /**
   * Name of the plugin that provides the skill, when source is 'plugin'
   */
  pluginName?: string;
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
 * Skills invoked during this session, ordered by invocation time (most recent last).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsGetInvokedResult".
 */
/** @experimental */
export interface SkillsGetInvokedResult {
  /**
   * Skills invoked during this session, ordered by invocation time (most recent last)
   */
  skills: SkillsInvokedSkill[];
}
/**
 * Schema for the `SkillsInvokedSkill` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsInvokedSkill".
 */
/** @experimental */
export interface SkillsInvokedSkill {
  /**
   * Unique identifier for the skill
   */
  name: string;
  /**
   * Path to the SKILL.md file
   */
  path: string;
  /**
   * Full content of the skill file
   */
  content: string;
  /**
   * Tools that should be auto-approved when this skill is active, captured at invocation time
   */
  allowedTools?: string[];
  /**
   * Turn number when the skill was invoked
   */
  invokedAtTurn: number;
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
/** @experimental */
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
  mode?: SessionMode;
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
/** @experimental */
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
/** @experimental */
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
 * Schema for the `SlashCommandSelectSubcommandResult` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandSelectSubcommandResult".
 */
/** @experimental */
export interface SlashCommandSelectSubcommandResult {
  /**
   * Select subcommand result discriminator
   */
  kind: "select-subcommand";
  /**
   * Parent command name that requires subcommand selection
   */
  command: string;
  /**
   * Human-readable title for the selection UI
   */
  title: string;
  /**
   * Available subcommand options for the client to present
   */
  options: SlashCommandSelectSubcommandOption[];
  /**
   * True when the invocation mutated user runtime settings; consumers caching settings should refresh
   */
  runtimeSettingsChanged?: boolean;
}
/**
 * Schema for the `SlashCommandSelectSubcommandOption` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandSelectSubcommandOption".
 */
/** @experimental */
export interface SlashCommandSelectSubcommandOption {
  /**
   * Subcommand name to invoke
   */
  name: string;
  /**
   * Human-readable description of the subcommand
   */
  description: string;
  /**
   * Optional group label for organizing options
   */
  group?: string;
}
/**
 * Schema for the `TaskAgentInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskAgentInfo".
 */
/** @experimental */
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
  status: TaskStatus;
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
  executionMode?: TaskExecutionMode;
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
 * Schema for the `TaskAgentProgress` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskAgentProgress".
 */
/** @experimental */
export interface TaskAgentProgress {
  /**
   * Progress kind
   */
  type: "agent";
  /**
   * Recent tool execution events converted to display lines
   */
  recentActivity: TaskProgressLine[];
  /**
   * The most recent intent reported by the agent
   */
  latestIntent?: string;
}
/**
 * Schema for the `TaskProgressLine` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskProgressLine".
 */
/** @experimental */
export interface TaskProgressLine {
  /**
   * Display message, e.g., "▸ bash", "✓ edit src/foo.ts"
   */
  message: string;
  /**
   * ISO 8601 timestamp when this event occurred
   */
  timestamp: string;
}
/**
 * Schema for the `TaskShellInfo` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskShellInfo".
 */
/** @experimental */
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
  status: TaskStatus;
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
  executionMode?: TaskExecutionMode;
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
 * Schema for the `TaskShellProgress` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskShellProgress".
 */
/** @experimental */
export interface TaskShellProgress {
  /**
   * Progress kind
   */
  type: "shell";
  /**
   * Recent stdout/stderr lines from the running shell command
   */
  recentOutput: string;
  /**
   * Process ID when available
   */
  pid?: number;
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
 * The first sync-waiting task that can currently be promoted to background mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksGetCurrentPromotableResult".
 */
/** @experimental */
export interface TasksGetCurrentPromotableResult {
  task?: TaskInfo;
}
/**
 * Identifier of the background task to fetch progress for.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksGetProgressRequest".
 */
/** @experimental */
export interface TasksGetProgressRequest {
  /**
   * Task identifier (agent ID or shell ID)
   */
  id: string;
}
/**
 * Progress information for the task, or null when no task with that ID is tracked.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksGetProgressResult".
 */
/** @experimental */
export interface TasksGetProgressResult {
  /**
   * Progress information for the task, discriminated by type. Returns null when no task with this ID is currently tracked.
   */
  progress?: TaskProgress | null;
}
/**
 * The promoted task as it now exists in background mode, omitted if no promotable task was waiting.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksPromoteCurrentToBackgroundResult".
 */
/** @experimental */
export interface TasksPromoteCurrentToBackgroundResult {
  task?: TaskInfo;
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
 * Refresh metadata for any detached background shells the runtime knows about. Use after a long pause to pick up exit/output state for shells running outside the agent loop.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksRefreshResult".
 */
/** @experimental */
export interface TasksRefreshResult {}
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
 * Wait until all in-flight background tasks (agents + shells) and any follow-up turns scheduled by their completions have settled. Returns when the runtime is fully drained or after an internal timeout (default 10 minutes; configurable via COPILOT_TASK_WAIT_TIMEOUT_SECONDS).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksWaitForPendingResult".
 */
/** @experimental */
export interface TasksWaitForPendingResult {}
/**
 * Feature override key/value pairs to attach to subsequent telemetry events from this session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TelemetrySetFeatureOverridesRequest".
 */
/** @experimental */
export interface TelemetrySetFeatureOverridesRequest {
  /**
   * Override key/value pairs to attach to subsequent telemetry events from this session. Replaces any previously-set overrides.
   */
  features: {
    [k: string]: string | undefined;
  };
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
    [k: string]: unknown | undefined;
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
 * Resolve, build, and validate the runtime tool list for this session. Subagent sessions and consumer flows that need an initialized tool set before `send` invoke this. Default base-class implementation is a no-op for sessions that don't support tool validation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsInitializeAndValidateResult".
 */
/** @experimental */
export interface ToolsInitializeAndValidateResult {}
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
export interface UIElicitationSchema {
  /**
   * Schema type indicator (always 'object')
   */
  type: "object";
  /**
   * Form field definitions, keyed by field name
   */
  properties: {
    [k: string]: UIElicitationSchemaProperty | undefined;
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
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
/** @experimental */
export interface UIElicitationResponseContent {
  [k: string]: UIElicitationFieldValue;
}
/**
 * Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationResult".
 */
/** @experimental */
export interface UIElicitationResult {
  /**
   * Whether the response was accepted. False if the request was already resolved by another client.
   */
  success: boolean;
}
/**
 * Schema for the `UIExitPlanModeResponse` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIExitPlanModeResponse".
 */
/** @experimental */
export interface UIExitPlanModeResponse {
  /**
   * Whether the plan was approved.
   */
  approved: boolean;
  selectedAction?: UIExitPlanModeAction;
  /**
   * Whether subsequent edits should be auto-approved without confirmation.
   */
  autoApproveEdits?: boolean;
  /**
   * Feedback from the user when they declined the plan or requested changes.
   */
  feedback?: string;
}
/**
 * Request ID of a pending `auto_mode_switch.requested` event and the user's response.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingAutoModeSwitchRequest".
 */
/** @experimental */
export interface UIHandlePendingAutoModeSwitchRequest {
  /**
   * The unique request ID from the auto_mode_switch.requested event
   */
  requestId: string;
  response: UIAutoModeSwitchResponse;
}
/**
 * Pending elicitation request ID and the user's response (accept/decline/cancel + form values).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingElicitationRequest".
 */
/** @experimental */
export interface UIHandlePendingElicitationRequest {
  /**
   * The unique request ID from the elicitation.requested event
   */
  requestId: string;
  result: UIElicitationResponse;
}
/**
 * Request ID of a pending `exit_plan_mode.requested` event and the user's response.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingExitPlanModeRequest".
 */
/** @experimental */
export interface UIHandlePendingExitPlanModeRequest {
  /**
   * The unique request ID from the exit_plan_mode.requested event
   */
  requestId: string;
  response: UIExitPlanModeResponse;
}
/**
 * Indicates whether the pending UI request was resolved by this call.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingResult".
 */
/** @experimental */
export interface UIHandlePendingResult {
  /**
   * True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
   */
  success: boolean;
}
/**
 * Request ID of a pending `sampling.requested` event and an optional sampling result payload (omit to reject).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingSamplingRequest".
 */
/** @experimental */
export interface UIHandlePendingSamplingRequest {
  /**
   * The unique request ID from the sampling.requested event
   */
  requestId: string;
  response?: UIHandlePendingSamplingResponse;
}
/**
 * Optional sampling result payload. Omit to reject/cancel the sampling request without providing a result.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingSamplingResponse".
 */
/** @experimental */
export interface UIHandlePendingSamplingResponse {
  [k: string]: unknown | undefined;
}
/**
 * Request ID of a pending `user_input.requested` event and the user's response.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingUserInputRequest".
 */
/** @experimental */
export interface UIHandlePendingUserInputRequest {
  /**
   * The unique request ID from the user_input.requested event
   */
  requestId: string;
  response: UIUserInputResponse;
}
/**
 * Schema for the `UIUserInputResponse` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIUserInputResponse".
 */
/** @experimental */
export interface UIUserInputResponse {
  /**
   * The user's answer text
   */
  answer: string;
  /**
   * True if the user typed a freeform response, false if they selected a presented choice. Used by telemetry to differentiate between free text input and choice selection.
   */
  wasFreeform: boolean;
}
/**
 * Register an in-process handler for `auto_mode_switch.requested` events. The caller still attaches the actual listener via the standard event-subscription mechanism; this registration solely tells the server bridge to skip its own dispatch (so a remote client doesn't race the in-process handler for the same requestId).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIRegisterDirectAutoModeSwitchHandlerResult".
 */
/** @experimental */
export interface UIRegisterDirectAutoModeSwitchHandlerResult {
  /**
   * Opaque handle representing the registration. Pass this same handle to `unregisterDirectAutoModeSwitchHandler` when the in-process handler is no longer active. Multiple registrations are reference-counted; the server bridge will only dispatch auto-mode-switch requests when no handles are active.
   */
  handle: string;
}
/**
 * Opaque handle previously returned by `registerDirectAutoModeSwitchHandler` to release.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIUnregisterDirectAutoModeSwitchHandlerRequest".
 */
/** @experimental */
export interface UIUnregisterDirectAutoModeSwitchHandlerRequest {
  /**
   * Handle previously returned by `registerDirectAutoModeSwitchHandler`
   */
  handle: string;
}
/**
 * Indicates whether the handle was active and the registration count was decremented.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIUnregisterDirectAutoModeSwitchHandlerResult".
 */
/** @experimental */
export interface UIUnregisterDirectAutoModeSwitchHandlerResult {
  /**
   * True if the handle was active and decremented the counter; false if the handle was unknown.
   */
  unregistered: boolean;
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
    [k: string]: UsageMetricsTokenDetail | undefined;
  };
  /**
   * Total time spent in model API calls (milliseconds)
   */
  totalApiDurationMs: number;
  /**
   * ISO 8601 timestamp when the session started
   */
  sessionStartTime: string;
  codeChanges: UsageMetricsCodeChanges;
  /**
   * Per-model token and request metrics, keyed by model identifier
   */
  modelMetrics: {
    [k: string]: UsageMetricsModelMetric | undefined;
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
/** @experimental */
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
/** @experimental */
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
  /**
   * Distinct file paths modified during the session
   */
  filesModified: string[];
}
/**
 * Schema for the `UsageMetricsModelMetric` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsModelMetric".
 */
/** @experimental */
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
    [k: string]: UsageMetricsModelMetricTokenDetail | undefined;
  };
}
/**
 * Request count and cost metrics for this model
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsModelMetricRequests".
 */
/** @experimental */
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
/** @experimental */
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
/** @experimental */
export interface UsageMetricsModelMetricTokenDetail {
  /**
   * Accumulated token count for this token type
   */
  tokenCount: number;
}
/**
 * Schema for the `WorkspacesCheckpoints` type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesCheckpoints".
 */
/** @experimental */
export interface WorkspacesCheckpoints {
  /**
   * Checkpoint number assigned by the workspace manager
   */
  number: number;
  /**
   * Human-readable checkpoint title
   */
  title: string;
  /**
   * Filename of the checkpoint within the workspace checkpoints directory
   */
  filename: string;
}
/**
 * Relative path and UTF-8 content for the workspace file to create or overwrite.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesCreateFileRequest".
 */
/** @experimental */
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
 * Current workspace metadata for the session, including its absolute filesystem path when available.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesGetWorkspaceResult".
 */
/** @experimental */
export interface WorkspacesGetWorkspaceResult {
  /**
   * Current workspace metadata, or null if not available
   */
  workspace: {
    id: string;
    cwd?: string;
    git_root?: string;
    repository?: string;
    host_type?: WorkspacesWorkspaceDetailsHostType;
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
  /**
   * Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions).
   */
  path?: string;
}
/**
 * Workspace checkpoints in chronological order; empty when the workspace is not enabled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesListCheckpointsResult".
 */
/** @experimental */
export interface WorkspacesListCheckpointsResult {
  /**
   * Workspace checkpoints in chronological order. Empty when workspace is not enabled.
   */
  checkpoints: WorkspacesCheckpoints[];
}
/**
 * Relative paths of files stored in the session workspace files directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesListFilesResult".
 */
/** @experimental */
export interface WorkspacesListFilesResult {
  /**
   * Relative file paths in the workspace files directory
   */
  files: string[];
}
/**
 * Checkpoint number to read.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesReadCheckpointRequest".
 */
/** @experimental */
export interface WorkspacesReadCheckpointRequest {
  /**
   * Checkpoint number to read
   */
  number: number;
}
/**
 * Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesReadCheckpointResult".
 */
/** @experimental */
export interface WorkspacesReadCheckpointResult {
  /**
   * Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing
   */
  content: string | null;
}
/**
 * Relative path of the workspace file to read.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesReadFileRequest".
 */
/** @experimental */
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
/** @experimental */
export interface WorkspacesReadFileResult {
  /**
   * File content as a UTF-8 string
   */
  content: string;
}
/**
 * Pasted content to save as a UTF-8 file in the session workspace.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesSaveLargePasteRequest".
 */
/** @experimental */
export interface WorkspacesSaveLargePasteRequest {
  /**
   * Pasted content to save as a UTF-8 file
   */
  content: string;
}
/**
 * Descriptor for the saved paste file, or null when the workspace is unavailable.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesSaveLargePasteResult".
 */
/** @experimental */
export interface WorkspacesSaveLargePasteResult {
  /**
   * Saved-paste descriptor, or null when the workspace is unavailable (e.g. CCA runtime, non-infinite sessions, remote sessions)
   */
  saved: {
    /**
     * Absolute filesystem path to the saved paste file
     */
    filePath: string;
    /**
     * Filename within the workspace files directory
     */
    filename: string;
    /**
     * Size of the saved file in bytes
     */
    sizeBytes: number;
  } | null;
}
/**
 * Standard MCP CallToolResult
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionMcpAppsCallToolResult".
 */
/** @experimental */
export interface SessionMcpAppsCallToolResult {
  [k: string]: unknown | undefined;
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
        /**
         * Checks server responsiveness and returns protocol information.
         *
         * @param params Optional message to echo back to the caller.
         *
         * @returns Server liveness response, including the echoed message, current server timestamp, and protocol version.
         */
        ping: async (params: PingRequest): Promise<PingResult> =>
            connection.sendRequest("ping", params),
        models: {
            /**
             * Lists Copilot models available to the authenticated user.
             *
             * @param params Optional GitHub token used to list models for a specific user instead of the global auth context.
             *
             * @returns List of Copilot models available to the resolved user, including capabilities and billing metadata.
             */
            list: async (params: ModelsListRequest): Promise<ModelList> =>
                connection.sendRequest("models.list", params),
        },
        tools: {
            /**
             * Lists built-in tools available for a model.
             *
             * @param params Optional model identifier whose tool overrides should be applied to the listing.
             *
             * @returns Built-in tools available for the requested model, with their parameters and instructions.
             */
            list: async (params: ToolsListRequest): Promise<ToolList> =>
                connection.sendRequest("tools.list", params),
        },
        account: {
            /**
             * Gets Copilot quota usage for the authenticated user or supplied GitHub token.
             *
             * @param params Optional GitHub token used to look up quota for a specific user instead of the global auth context.
             *
             * @returns Quota usage snapshots for the resolved user, keyed by quota type.
             */
            getQuota: async (params: AccountGetQuotaRequest): Promise<AccountGetQuotaResult> =>
                connection.sendRequest("account.getQuota", params),
        },
        secrets: {
            /**
             * Registers secret values for redaction in session logs and exports. The SDK calls this to inject dynamically generated secret values (e.g., OIDC tokens).
             *
             * @param params Secret values to add to the redaction filter.
             *
             * @returns Confirmation that the secret values were registered.
             */
            addFilterValues: async (params: SecretsAddFilterValuesRequest): Promise<SecretsAddFilterValuesResult> =>
                connection.sendRequest("secrets.addFilterValues", params),
        },
        mcp: {
            config: {
                /**
                 * Lists MCP servers from user configuration.
                 *
                 * @returns User-configured MCP servers, keyed by server name.
                 */
                list: async (): Promise<McpConfigList> =>
                    connection.sendRequest("mcp.config.list", {}),
                /**
                 * Adds an MCP server to user configuration.
                 *
                 * @param params MCP server name and configuration to add to user configuration.
                 */
                add: async (params: McpConfigAddRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.add", params),
                /**
                 * Updates an MCP server in user configuration.
                 *
                 * @param params MCP server name and replacement configuration to write to user configuration.
                 */
                update: async (params: McpConfigUpdateRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.update", params),
                /**
                 * Removes an MCP server from user configuration.
                 *
                 * @param params MCP server name to remove from user configuration.
                 */
                remove: async (params: McpConfigRemoveRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.remove", params),
                /**
                 * Enables MCP servers in user configuration for new sessions.
                 *
                 * @param params MCP server names to enable for new sessions.
                 */
                enable: async (params: McpConfigEnableRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.enable", params),
                /**
                 * Disables MCP servers in user configuration for new sessions.
                 *
                 * @param params MCP server names to disable for new sessions.
                 */
                disable: async (params: McpConfigDisableRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.disable", params),
            },
            /**
             * Discovers MCP servers from user, workspace, plugin, and builtin sources.
             *
             * @param params Optional working directory used as context for MCP server discovery.
             *
             * @returns MCP servers discovered from user, workspace, plugin, and built-in sources.
             */
            discover: async (params: McpDiscoverRequest): Promise<McpDiscoverResult> =>
                connection.sendRequest("mcp.discover", params),
        },
        skills: {
            config: {
                /**
                 * Replaces the global list of disabled skills.
                 *
                 * @param params Skill names to mark as disabled in global configuration, replacing any previous list.
                 */
                setDisabledSkills: async (params: SkillsConfigSetDisabledSkillsRequest): Promise<void> =>
                    connection.sendRequest("skills.config.setDisabledSkills", params),
            },
            /**
             * Discovers skills across global and project sources.
             *
             * @param params Optional project paths and additional skill directories to include in discovery.
             *
             * @returns Skills discovered across global and project sources.
             */
            discover: async (params: SkillsDiscoverRequest): Promise<ServerSkillList> =>
                connection.sendRequest("skills.discover", params),
        },
        sessionFs: {
            /**
             * Registers an SDK client as the session filesystem provider.
             *
             * @param params Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider.
             *
             * @returns Indicates whether the calling client was registered as the session filesystem provider.
             */
            setProvider: async (params: SessionFsSetProviderRequest): Promise<SessionFsSetProviderResult> =>
                connection.sendRequest("sessionFs.setProvider", params),
        },
        /** @experimental */
        sessions: {
            /**
             * Creates a new session by forking persisted history from an existing session.
             *
             * @param params Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.
             *
             * @returns Identifier and optional friendly name assigned to the newly forked session.
             */
            fork: async (params: SessionsForkRequest): Promise<SessionsForkResult> =>
                connection.sendRequest("sessions.fork", params),
            /**
             * Connects to an existing remote session and exposes it as an SDK session.
             *
             * @param params Remote session connection parameters.
             *
             * @returns Remote session connection result.
             */
            connect: async (params: ConnectRemoteSessionParams): Promise<RemoteSessionConnectionResult> =>
                connection.sendRequest("sessions.connect", params),
            /**
             * Lists persisted sessions, optionally filtered by working-directory context.
             *
             * @param params Optional metadata-load limit and context filter applied to the returned sessions.
             *
             * @returns Persisted sessions matching the filter, ordered most-recently-modified first.
             */
            list: async (params: SessionsListRequest): Promise<SessionList> =>
                connection.sendRequest("sessions.list", params),
            /**
             * Finds the local session bound to a GitHub task ID, if any.
             *
             * @param params GitHub task ID to look up.
             *
             * @returns ID of the local session bound to the given GitHub task, or omitted when none.
             */
            findByTaskId: async (params: SessionsFindByTaskIDRequest): Promise<SessionsFindByTaskIDResult> =>
                connection.sendRequest("sessions.findByTaskId", params),
            /**
             * Resolves a UUID prefix to a unique session ID, if exactly one session matches.
             *
             * @param params UUID prefix to resolve to a unique session ID.
             *
             * @returns Session ID matching the prefix, omitted when no unique match exists.
             */
            findByPrefix: async (params: SessionsFindByPrefixRequest): Promise<SessionsFindByPrefixResult> =>
                connection.sendRequest("sessions.findByPrefix", params),
            /**
             * Returns the most-relevant prior session for a given working-directory context.
             *
             * @param params Optional working-directory context used to score session relevance.
             *
             * @returns Most-relevant session ID for the supplied context, or omitted when no sessions exist.
             */
            getLastForContext: async (params: SessionsGetLastForContextRequest): Promise<SessionsGetLastForContextResult> =>
                connection.sendRequest("sessions.getLastForContext", params),
            /**
             * Computes the absolute path to a session's persisted events.jsonl file.
             *
             * @param params Session ID whose event-log file path to compute.
             *
             * @returns Absolute path to the session's events.jsonl file on disk.
             */
            getEventFilePath: async (params: SessionsGetEventFilePathRequest): Promise<SessionsGetEventFilePathResult> =>
                connection.sendRequest("sessions.getEventFilePath", params),
            /**
             * Returns the on-disk byte size of each session's workspace directory.
             *
             * @returns Map of sessionId -> on-disk size in bytes for each session's workspace directory.
             */
            getSizes: async (): Promise<SessionSizes> =>
                connection.sendRequest("sessions.getSizes", {}),
            /**
             * Returns the subset of the supplied session IDs that are currently held by another running process.
             *
             * @param params Session IDs to test for live in-use locks.
             *
             * @returns Session IDs from the input set that are currently in use by another process.
             */
            checkInUse: async (params: SessionsCheckInUseRequest): Promise<SessionsCheckInUseResult> =>
                connection.sendRequest("sessions.checkInUse", params),
            /**
             * Returns a session's persisted remote-steerable flag, if any has been recorded.
             *
             * @param params Session ID to look up the persisted remote-steerable flag for.
             *
             * @returns The session's persisted remote-steerable flag, or omitted when no value has been persisted.
             */
            getPersistedRemoteSteerable: async (params: SessionsGetPersistedRemoteSteerableRequest): Promise<SessionsGetPersistedRemoteSteerableResult> =>
                connection.sendRequest("sessions.getPersistedRemoteSteerable", params),
            /**
             * Closes a session: emits shutdown, flushes pending events, releases the in-use lock, and disposes the active session.
             *
             * @param params Session ID to close.
             *
             * @returns Closes a session: emits shutdown, flushes pending events to disk, releases the in-use lock, disposes the active session. Idempotent: succeeds even if the session is not currently active.
             */
            close: async (params: SessionsCloseRequest): Promise<SessionsCloseResult> =>
                connection.sendRequest("sessions.close", params),
            /**
             * Closes, deactivates, and deletes a set of sessions, returning the bytes freed per session.
             *
             * @param params Session IDs to close, deactivate, and delete from disk.
             *
             * @returns Map of sessionId -> bytes freed by removing the session's workspace directory.
             */
            bulkDelete: async (params: SessionsBulkDeleteRequest): Promise<SessionBulkDeleteResult> =>
                connection.sendRequest("sessions.bulkDelete", params),
            /**
             * Deletes sessions older than the given threshold, with optional dry-run and exclusion list.
             *
             * @param params Age threshold and optional flags controlling which old sessions are pruned (or simulated when dryRun is true).
             *
             * @returns Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.
             */
            pruneOld: async (params: SessionsPruneOldRequest): Promise<SessionPruneResult> =>
                connection.sendRequest("sessions.pruneOld", params),
            /**
             * Flushes a session's pending events to disk.
             *
             * @param params Session ID whose pending events should be flushed to disk.
             *
             * @returns Flush a session's pending events to disk. No-op when no writer exists for the session (e.g., already closed).
             */
            save: async (params: SessionsSaveRequest): Promise<SessionsSaveResult> =>
                connection.sendRequest("sessions.save", params),
            /**
             * Releases the in-use lock held by this process for a session.
             *
             * @param params Session ID whose in-use lock should be released.
             *
             * @returns Release the in-use lock held by this process for the given session. No-op when this process does not currently hold a lock for the session.
             */
            releaseLock: async (params: SessionsReleaseLockRequest): Promise<SessionsReleaseLockResult> =>
                connection.sendRequest("sessions.releaseLock", params),
            /**
             * Backfills missing summary and context fields on the supplied session metadata records.
             *
             * @param params Session metadata records to enrich with summary and context information.
             *
             * @returns The same metadata records, with summary and context fields backfilled where available.
             */
            enrichMetadata: async (params: SessionsEnrichMetadataRequest): Promise<SessionEnrichMetadataResult> =>
                connection.sendRequest("sessions.enrichMetadata", params),
            /**
             * Reloads user, plugin, and (optionally) repo hooks on the active session.
             *
             * @param params Active session ID and an optional flag for deferring repo-level hooks until folder trust.
             *
             * @returns Reload all hooks (user, plugin, optionally repo) and apply them to the active session. Call after installing or removing plugins so their hooks take effect immediately. No-op when no active session matches the given sessionId.
             */
            reloadPluginHooks: async (params: SessionsReloadPluginHooksRequest): Promise<SessionsReloadPluginHooksResult> =>
                connection.sendRequest("sessions.reloadPluginHooks", params),
            /**
             * Loads previously-deferred repo-level hooks on the active session, returning queued startup prompts.
             *
             * @param params Active session ID whose deferred repo-level hooks should be loaded.
             *
             * @returns Queued repo-level startup prompts and the total hook command count after loading.
             */
            loadDeferredRepoHooks: async (params: SessionsLoadDeferredRepoHooksRequest): Promise<SessionLoadDeferredRepoHooksResult> =>
                connection.sendRequest("sessions.loadDeferredRepoHooks", params),
            /**
             * Replaces the manager-wide additional plugins registered with the session manager.
             *
             * @param params Manager-wide additional plugins to register; replaces any previously-configured set.
             *
             * @returns Replace the manager-wide additional plugins. New session creations and subsequent hook reloads see the new set; already-running sessions keep their existing hook installation until the next reload.
             */
            setAdditionalPlugins: async (params: SessionsSetAdditionalPluginsRequest): Promise<SessionsSetAdditionalPluginsResult> =>
                connection.sendRequest("sessions.setAdditionalPlugins", params),
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
        /**
         * Performs the SDK server connection handshake and validates the optional connection token.
         *
         * @param params Optional connection token presented by the SDK client during the handshake.
         *
         * @returns Handshake result reporting the server's protocol version and package version on success.
         */
        connect: async (params: ConnectRequest): Promise<ConnectResult> =>
            connection.sendRequest("connect", params),
    };
}

/** Create typed session-scoped RPC methods. */
export function createSessionRpc(connection: MessageConnection, sessionId: string) {
    return {
        /**
         * Suspends the session while preserving persisted state for later resume.
         *
         * @experimental
         */
        suspend: async (): Promise<void> =>
            connection.sendRequest("session.suspend", { sessionId }),
        /**
         * Sends a user message to the session and returns its message ID.
         *
         * @param params Parameters for sending a user message to the session
         *
         * @returns Result of sending a user message
         *
         * @experimental
         */
        send: async (params: SendRequest): Promise<SendResult> =>
            connection.sendRequest("session.send", { sessionId, ...params }),
        /**
         * Aborts the current agent turn.
         *
         * @param params Parameters for aborting the current turn
         *
         * @returns Result of aborting the current turn
         *
         * @experimental
         */
        abort: async (params: AbortRequest): Promise<AbortResult> =>
            connection.sendRequest("session.abort", { sessionId, ...params }),
        /**
         * Shuts down the session and persists its final state. Awaits any deferred sessionEnd hooks before resolving so user-supplied hook scripts complete before the runtime tears down.
         *
         * @param params Parameters for shutting down the session
         *
         * @experimental
         */
        shutdown: async (params: ShutdownRequest): Promise<void> =>
            connection.sendRequest("session.shutdown", { sessionId, ...params }),
        /** @experimental */
        auth: {
            /**
             * Gets authentication status and account metadata for the session.
             *
             * @returns Authentication status and account metadata for the session.
             */
            getStatus: async (): Promise<SessionAuthStatus> =>
                connection.sendRequest("session.auth.getStatus", { sessionId }),
            /**
             * Updates the session's auth credentials used for outbound model and API requests.
             *
             * @param params New auth credentials to install on the session. Omit to leave credentials unchanged.
             *
             * @returns Indicates whether the credential update succeeded.
             */
            setCredentials: async (params: SessionSetCredentialsParams): Promise<SessionSetCredentialsResult> =>
                connection.sendRequest("session.auth.setCredentials", { sessionId, ...params }),
        },
        /** @experimental */
        canvas: {
            /**
             * Lists canvases declared for the session.
             *
             * @returns Declared canvases available in this session.
             */
            list: async (): Promise<CanvasList> =>
                connection.sendRequest("session.canvas.list", { sessionId }),
            /**
             * Lists currently open canvas instances for the live session.
             *
             * @returns Live open-canvas snapshot.
             */
            listOpen: async (): Promise<CanvasListOpenResult> =>
                connection.sendRequest("session.canvas.listOpen", { sessionId }),
            /**
             * Opens or focuses a canvas instance.
             *
             * @param params Canvas open parameters.
             *
             * @returns Open canvas instance snapshot.
             */
            open: async (params: CanvasOpenRequest): Promise<OpenCanvasInstance> =>
                connection.sendRequest("session.canvas.open", { sessionId, ...params }),
            /**
             * Closes an open canvas instance.
             *
             * @param params Canvas close parameters.
             */
            close: async (params: CanvasCloseRequest): Promise<void> =>
                connection.sendRequest("session.canvas.close", { sessionId, ...params }),
            /**
             * Invokes an action on an open canvas instance.
             *
             * @param params Canvas action invocation parameters.
             *
             * @returns Canvas action invocation result.
             */
            invokeAction: async (params: CanvasInvokeActionRequest): Promise<CanvasInvokeActionResult> =>
                connection.sendRequest("session.canvas.invokeAction", { sessionId, ...params }),
        },
        /** @experimental */
        model: {
            /**
             * Gets the currently selected model for the session.
             *
             * @returns The currently selected model and reasoning effort for the session.
             */
            getCurrent: async (): Promise<CurrentModel> =>
                connection.sendRequest("session.model.getCurrent", { sessionId }),
            /**
             * Switches the session to a model and optional reasoning configuration.
             *
             * @param params Target model identifier and optional reasoning effort, summary, and capability overrides.
             *
             * @returns The model identifier active on the session after the switch.
             */
            switchTo: async (params: ModelSwitchToRequest): Promise<ModelSwitchToResult> =>
                connection.sendRequest("session.model.switchTo", { sessionId, ...params }),
            /**
             * Updates the session's reasoning effort without changing the selected model.
             *
             * @param params Reasoning effort level to apply to the currently selected model.
             *
             * @returns Update the session's reasoning effort without changing the selected model. Use `switchTo` instead when you also need to change the model. The runtime stores the effort on the session and applies it to subsequent turns.
             */
            setReasoningEffort: async (params: ModelSetReasoningEffortRequest): Promise<ModelSetReasoningEffortResult> =>
                connection.sendRequest("session.model.setReasoningEffort", { sessionId, ...params }),
        },
        /** @experimental */
        mode: {
            /**
             * Gets the current agent interaction mode.
             *
             * @returns The session mode the agent is operating in
             */
            get: async (): Promise<SessionMode> =>
                connection.sendRequest("session.mode.get", { sessionId }),
            /**
             * Sets the current agent interaction mode.
             *
             * @param params Agent interaction mode to apply to the session.
             */
            set: async (params: ModeSetRequest): Promise<void> =>
                connection.sendRequest("session.mode.set", { sessionId, ...params }),
        },
        /** @experimental */
        name: {
            /**
             * Gets the session's friendly name.
             *
             * @returns The session's friendly name, or null when not yet set.
             */
            get: async (): Promise<NameGetResult> =>
                connection.sendRequest("session.name.get", { sessionId }),
            /**
             * Sets the session's friendly name.
             *
             * @param params New friendly name to apply to the session.
             */
            set: async (params: NameSetRequest): Promise<void> =>
                connection.sendRequest("session.name.set", { sessionId, ...params }),
            /**
             * Persists an auto-generated session summary as the session's name when no user-set name exists.
             *
             * @param params Auto-generated session summary to apply as the session's name when no user-set name exists.
             *
             * @returns Indicates whether the auto-generated summary was applied as the session's name.
             */
            setAuto: async (params: NameSetAutoRequest): Promise<NameSetAutoResult> =>
                connection.sendRequest("session.name.setAuto", { sessionId, ...params }),
        },
        /** @experimental */
        plan: {
            /**
             * Reads the session plan file from the workspace.
             *
             * @returns Existence, contents, and resolved path of the session plan file.
             */
            read: async (): Promise<PlanReadResult> =>
                connection.sendRequest("session.plan.read", { sessionId }),
            /**
             * Writes new content to the session plan file.
             *
             * @param params Replacement contents to write to the session plan file.
             */
            update: async (params: PlanUpdateRequest): Promise<void> =>
                connection.sendRequest("session.plan.update", { sessionId, ...params }),
            /**
             * Deletes the session plan file from the workspace.
             */
            delete: async (): Promise<void> =>
                connection.sendRequest("session.plan.delete", { sessionId }),
        },
        /** @experimental */
        workspaces: {
            /**
             * Gets current workspace metadata for the session.
             *
             * @returns Current workspace metadata for the session, including its absolute filesystem path when available.
             */
            getWorkspace: async (): Promise<WorkspacesGetWorkspaceResult> =>
                connection.sendRequest("session.workspaces.getWorkspace", { sessionId }),
            /**
             * Lists files stored in the session workspace files directory.
             *
             * @returns Relative paths of files stored in the session workspace files directory.
             */
            listFiles: async (): Promise<WorkspacesListFilesResult> =>
                connection.sendRequest("session.workspaces.listFiles", { sessionId }),
            /**
             * Reads a file from the session workspace files directory.
             *
             * @param params Relative path of the workspace file to read.
             *
             * @returns Contents of the requested workspace file as a UTF-8 string.
             */
            readFile: async (params: WorkspacesReadFileRequest): Promise<WorkspacesReadFileResult> =>
                connection.sendRequest("session.workspaces.readFile", { sessionId, ...params }),
            /**
             * Creates or overwrites a file in the session workspace files directory.
             *
             * @param params Relative path and UTF-8 content for the workspace file to create or overwrite.
             */
            createFile: async (params: WorkspacesCreateFileRequest): Promise<void> =>
                connection.sendRequest("session.workspaces.createFile", { sessionId, ...params }),
            /**
             * Lists workspace checkpoints in chronological order.
             *
             * @returns Workspace checkpoints in chronological order; empty when the workspace is not enabled.
             */
            listCheckpoints: async (): Promise<WorkspacesListCheckpointsResult> =>
                connection.sendRequest("session.workspaces.listCheckpoints", { sessionId }),
            /**
             * Reads the content of a workspace checkpoint by number.
             *
             * @param params Checkpoint number to read.
             *
             * @returns Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing.
             */
            readCheckpoint: async (params: WorkspacesReadCheckpointRequest): Promise<WorkspacesReadCheckpointResult> =>
                connection.sendRequest("session.workspaces.readCheckpoint", { sessionId, ...params }),
            /**
             * Saves pasted content as a UTF-8 file in the session workspace.
             *
             * @param params Pasted content to save as a UTF-8 file in the session workspace.
             *
             * @returns Descriptor for the saved paste file, or null when the workspace is unavailable.
             */
            saveLargePaste: async (params: WorkspacesSaveLargePasteRequest): Promise<WorkspacesSaveLargePasteResult> =>
                connection.sendRequest("session.workspaces.saveLargePaste", { sessionId, ...params }),
        },
        /** @experimental */
        instructions: {
            /**
             * Gets instruction sources loaded for the session.
             *
             * @returns Instruction sources loaded for the session, in merge order.
             */
            getSources: async (): Promise<InstructionsGetSourcesResult> =>
                connection.sendRequest("session.instructions.getSources", { sessionId }),
        },
        /** @experimental */
        fleet: {
            /**
             * Starts fleet mode by submitting the fleet orchestration prompt to the session.
             *
             * @param params Optional user prompt to combine with the fleet orchestration instructions.
             *
             * @returns Indicates whether fleet mode was successfully activated.
             */
            start: async (params: FleetStartRequest): Promise<FleetStartResult> =>
                connection.sendRequest("session.fleet.start", { sessionId, ...params }),
        },
        /** @experimental */
        agent: {
            /**
             * Lists custom agents available to the session.
             *
             * @returns Custom agents available to the session.
             */
            list: async (): Promise<AgentList> =>
                connection.sendRequest("session.agent.list", { sessionId }),
            /**
             * Gets the currently selected custom agent for the session.
             *
             * @returns The currently selected custom agent, or null when using the default agent.
             */
            getCurrent: async (): Promise<AgentGetCurrentResult> =>
                connection.sendRequest("session.agent.getCurrent", { sessionId }),
            /**
             * Selects a custom agent for subsequent turns in the session.
             *
             * @param params Name of the custom agent to select for subsequent turns.
             *
             * @returns The newly selected custom agent.
             */
            select: async (params: AgentSelectRequest): Promise<AgentSelectResult> =>
                connection.sendRequest("session.agent.select", { sessionId, ...params }),
            /**
             * Clears the selected custom agent and returns the session to the default agent.
             */
            deselect: async (): Promise<void> =>
                connection.sendRequest("session.agent.deselect", { sessionId }),
            /**
             * Reloads custom agent definitions and returns the refreshed list.
             *
             * @returns Custom agents available to the session after reloading definitions from disk.
             */
            reload: async (): Promise<AgentReloadResult> =>
                connection.sendRequest("session.agent.reload", { sessionId }),
        },
        /** @experimental */
        tasks: {
            /**
             * Starts a background agent task in the session.
             *
             * @param params Agent type, prompt, name, and optional description and model override for the new task.
             *
             * @returns Identifier assigned to the newly started background agent task.
             */
            startAgent: async (params: TasksStartAgentRequest): Promise<TasksStartAgentResult> =>
                connection.sendRequest("session.tasks.startAgent", { sessionId, ...params }),
            /**
             * Lists background tasks tracked by the session.
             *
             * @returns Background tasks currently tracked by the session.
             */
            list: async (): Promise<TaskList> =>
                connection.sendRequest("session.tasks.list", { sessionId }),
            /**
             * Refreshes metadata for any detached background shells the runtime knows about.
             *
             * @returns Refresh metadata for any detached background shells the runtime knows about. Use after a long pause to pick up exit/output state for shells running outside the agent loop.
             */
            refresh: async (): Promise<TasksRefreshResult> =>
                connection.sendRequest("session.tasks.refresh", { sessionId }),
            /**
             * Waits for all in-flight background tasks and any follow-up turns to settle.
             *
             * @returns Wait until all in-flight background tasks (agents + shells) and any follow-up turns scheduled by their completions have settled. Returns when the runtime is fully drained or after an internal timeout (default 10 minutes; configurable via COPILOT_TASK_WAIT_TIMEOUT_SECONDS).
             */
            waitForPending: async (): Promise<TasksWaitForPendingResult> =>
                connection.sendRequest("session.tasks.waitForPending", { sessionId }),
            /**
             * Returns progress information for a background task by ID.
             *
             * @param params Identifier of the background task to fetch progress for.
             *
             * @returns Progress information for the task, or null when no task with that ID is tracked.
             */
            getProgress: async (params: TasksGetProgressRequest): Promise<TasksGetProgressResult> =>
                connection.sendRequest("session.tasks.getProgress", { sessionId, ...params }),
            /**
             * Returns the first sync-waiting task that can currently be promoted to background mode.
             *
             * @returns The first sync-waiting task that can currently be promoted to background mode.
             */
            getCurrentPromotable: async (): Promise<TasksGetCurrentPromotableResult> =>
                connection.sendRequest("session.tasks.getCurrentPromotable", { sessionId }),
            /**
             * Promotes an eligible synchronously-waited task so it continues running in the background.
             *
             * @param params Identifier of the task to promote to background mode.
             *
             * @returns Indicates whether the task was successfully promoted to background mode.
             */
            promoteToBackground: async (params: TasksPromoteToBackgroundRequest): Promise<TasksPromoteToBackgroundResult> =>
                connection.sendRequest("session.tasks.promoteToBackground", { sessionId, ...params }),
            /**
             * Atomically promotes the first promotable sync-waiting task to background mode and returns it.
             *
             * @returns The promoted task as it now exists in background mode, omitted if no promotable task was waiting.
             */
            promoteCurrentToBackground: async (): Promise<TasksPromoteCurrentToBackgroundResult> =>
                connection.sendRequest("session.tasks.promoteCurrentToBackground", { sessionId }),
            /**
             * Cancels a background task.
             *
             * @param params Identifier of the background task to cancel.
             *
             * @returns Indicates whether the background task was successfully cancelled.
             */
            cancel: async (params: TasksCancelRequest): Promise<TasksCancelResult> =>
                connection.sendRequest("session.tasks.cancel", { sessionId, ...params }),
            /**
             * Removes a completed or cancelled background task from tracking.
             *
             * @param params Identifier of the completed or cancelled task to remove from tracking.
             *
             * @returns Indicates whether the task was removed. False when the task does not exist or is still running/idle.
             */
            remove: async (params: TasksRemoveRequest): Promise<TasksRemoveResult> =>
                connection.sendRequest("session.tasks.remove", { sessionId, ...params }),
            /**
             * Sends a message to a background agent task.
             *
             * @param params Identifier of the target agent task, message content, and optional sender agent ID.
             *
             * @returns Indicates whether the message was delivered, with an error message when delivery failed.
             */
            sendMessage: async (params: TasksSendMessageRequest): Promise<TasksSendMessageResult> =>
                connection.sendRequest("session.tasks.sendMessage", { sessionId, ...params }),
        },
        /** @experimental */
        skills: {
            /**
             * Lists skills available to the session.
             *
             * @returns Skills available to the session, with their enabled state.
             */
            list: async (): Promise<SkillList> =>
                connection.sendRequest("session.skills.list", { sessionId }),
            /**
             * Returns the skills that have been invoked during this session.
             *
             * @returns Skills invoked during this session, ordered by invocation time (most recent last).
             */
            getInvoked: async (): Promise<SkillsGetInvokedResult> =>
                connection.sendRequest("session.skills.getInvoked", { sessionId }),
            /**
             * Enables a skill for the session.
             *
             * @param params Name of the skill to enable for the session.
             */
            enable: async (params: SkillsEnableRequest): Promise<void> =>
                connection.sendRequest("session.skills.enable", { sessionId, ...params }),
            /**
             * Disables a skill for the session.
             *
             * @param params Name of the skill to disable for the session.
             */
            disable: async (params: SkillsDisableRequest): Promise<void> =>
                connection.sendRequest("session.skills.disable", { sessionId, ...params }),
            /**
             * Reloads skill definitions for the session.
             *
             * @returns Diagnostics from reloading skill definitions, with warnings and errors as separate lists.
             */
            reload: async (): Promise<SkillsLoadDiagnostics> =>
                connection.sendRequest("session.skills.reload", { sessionId }),
            /**
             * Ensures the session's skill definitions have been loaded from disk.
             */
            ensureLoaded: async (): Promise<void> =>
                connection.sendRequest("session.skills.ensureLoaded", { sessionId }),
        },
        /** @experimental */
        mcp: {
            /**
             * Lists MCP servers configured for the session and their connection status.
             *
             * @returns MCP servers configured for the session, with their connection status.
             */
            list: async (): Promise<McpServerList> =>
                connection.sendRequest("session.mcp.list", { sessionId }),
            /**
             * Enables an MCP server for the session.
             *
             * @param params Name of the MCP server to enable for the session.
             */
            enable: async (params: McpEnableRequest): Promise<void> =>
                connection.sendRequest("session.mcp.enable", { sessionId, ...params }),
            /**
             * Disables an MCP server for the session.
             *
             * @param params Name of the MCP server to disable for the session.
             */
            disable: async (params: McpDisableRequest): Promise<void> =>
                connection.sendRequest("session.mcp.disable", { sessionId, ...params }),
            /**
             * Reloads MCP server connections for the session.
             */
            reload: async (): Promise<void> =>
                connection.sendRequest("session.mcp.reload", { sessionId }),
            /**
             * Runs an MCP sampling inference on behalf of an MCP server.
             *
             * @param params Identifiers and raw MCP CreateMessageRequest params used to run a sampling inference.
             *
             * @returns Outcome of an MCP sampling execution: success result, failure error, or cancellation.
             */
            executeSampling: async (params: McpExecuteSamplingParams): Promise<McpSamplingExecutionResult> =>
                connection.sendRequest("session.mcp.executeSampling", { sessionId, ...params }),
            /**
             * Cancels an in-flight MCP sampling execution by request ID.
             *
             * @param params The requestId previously passed to executeSampling that should be cancelled.
             *
             * @returns Indicates whether an in-flight sampling execution with the given requestId was found and cancelled.
             */
            cancelSamplingExecution: async (params: McpCancelSamplingExecutionParams): Promise<McpCancelSamplingExecutionResult> =>
                connection.sendRequest("session.mcp.cancelSamplingExecution", { sessionId, ...params }),
            /**
             * Sets how environment-variable values supplied to MCP servers are resolved (direct or indirect).
             *
             * @param params Mode controlling how MCP server env values are resolved (`direct` or `indirect`).
             *
             * @returns Env-value mode recorded on the session after the update.
             */
            setEnvValueMode: async (params: McpSetEnvValueModeParams): Promise<McpSetEnvValueModeResult> =>
                connection.sendRequest("session.mcp.setEnvValueMode", { sessionId, ...params }),
            /**
             * Removes the auto-managed `github` MCP server when present.
             *
             * @returns Indicates whether the auto-managed `github` MCP server was removed (false when nothing to remove).
             */
            removeGitHub: async (): Promise<McpRemoveGitHubResult> =>
                connection.sendRequest("session.mcp.removeGitHub", { sessionId }),
            /** @experimental */
            oauth: {
                /**
                 * Starts OAuth authentication for a remote MCP server.
                 *
                 * @param params Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, and the callback success-page copy.
                 *
                 * @returns OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.
                 */
                login: async (params: McpOauthLoginRequest): Promise<McpOauthLoginResult> =>
                    connection.sendRequest("session.mcp.oauth.login", { sessionId, ...params }),
            },
            /** @experimental */
            apps: {
                /**
                 * Fetch an MCP resource (typically a `ui://` MCP App bundle, per SEP-1865) from a connected server. Requires the `mcp-apps` session capability.
                 *
                 * @param params MCP server and resource URI to fetch.
                 *
                 * @returns Resource contents returned by the MCP server.
                 */
                readResource: async (params: McpAppsReadResourceRequest): Promise<McpAppsReadResourceResult> =>
                    connection.sendRequest("session.mcp.apps.readResource", { sessionId, ...params }),
                /**
                 * List tools that an MCP App view is allowed to call (SEP-1865 visibility filter). Returns tools whose `_meta.ui.visibility` is unset (default `["model","app"]`) or includes `"app"`.
                 *
                 * @param params MCP server to list app-callable tools for.
                 *
                 * @returns App-callable tools from the named MCP server.
                 */
                listTools: async (params: McpAppsListToolsRequest): Promise<McpAppsListToolsResult> =>
                    connection.sendRequest("session.mcp.apps.listTools", { sessionId, ...params }),
                /**
                 * Call an MCP tool from an MCP App view (SEP-1865). Enforces the visibility check that prevents an app iframe from invoking model-only tools. Returns the standard MCP `CallToolResult`.
                 *
                 * @param params MCP server, tool name, and arguments to invoke from an MCP App view.
                 *
                 * @returns Standard MCP CallToolResult
                 */
                callTool: async (params: McpAppsCallToolRequest): Promise<SessionMcpAppsCallToolResult> =>
                    connection.sendRequest("session.mcp.apps.callTool", { sessionId, ...params }),
                /**
                 * Replace the host context returned to MCP App guests on `ui/initialize`. Hosts use this to advertise theme, locale, or other metadata to the guest UI.
                 *
                 * @param params Host context to advertise to MCP App guests.
                 */
                setHostContext: async (params: McpAppsSetHostContextRequest): Promise<void> =>
                    connection.sendRequest("session.mcp.apps.setHostContext", { sessionId, ...params }),
                /**
                 * Read the current host context advertised to MCP App guests.
                 *
                 * @returns Current host context advertised to MCP App guests.
                 */
                getHostContext: async (): Promise<McpAppsHostContext> =>
                    connection.sendRequest("session.mcp.apps.getHostContext", { sessionId }),
                /**
                 * Diagnose MCP Apps wiring for a specific MCP server. Reports the session capability, feature-flag state, advertised extension, and how many tools have `_meta.ui` populated.
                 *
                 * @param params MCP server to diagnose MCP Apps wiring for.
                 *
                 * @returns Diagnostic snapshot of MCP Apps wiring for the named server.
                 */
                diagnose: async (params: McpAppsDiagnoseRequest): Promise<McpAppsDiagnoseResult> =>
                    connection.sendRequest("session.mcp.apps.diagnose", { sessionId, ...params }),
            },
        },
        /** @experimental */
        plugins: {
            /**
             * Lists plugins installed for the session.
             *
             * @returns Plugins installed for the session, with their enabled state and version metadata.
             */
            list: async (): Promise<PluginList> =>
                connection.sendRequest("session.plugins.list", { sessionId }),
        },
        /** @experimental */
        options: {
            /**
             * Patches the genuinely-mutable subset of session options.
             *
             * @param params Patch of mutable session options to apply to the running session.
             *
             * @returns Indicates whether the session options patch was applied successfully.
             */
            update: async (params: SessionUpdateOptionsParams): Promise<SessionUpdateOptionsResult> =>
                connection.sendRequest("session.options.update", { sessionId, ...params }),
        },
        /** @experimental */
        lsp: {
            /**
             * Loads the merged LSP configuration set for the session's working directory.
             *
             * @param params Parameters for (re)loading the merged LSP configuration set.
             */
            initialize: async (params: LspInitializeRequest): Promise<void> =>
                connection.sendRequest("session.lsp.initialize", { sessionId, ...params }),
        },
        /** @experimental */
        extensions: {
            /**
             * Lists extensions discovered for the session and their current status.
             *
             * @returns Extensions discovered for the session, with their current status.
             */
            list: async (): Promise<ExtensionList> =>
                connection.sendRequest("session.extensions.list", { sessionId }),
            /**
             * Enables an extension for the session.
             *
             * @param params Source-qualified extension identifier to enable for the session.
             */
            enable: async (params: ExtensionsEnableRequest): Promise<void> =>
                connection.sendRequest("session.extensions.enable", { sessionId, ...params }),
            /**
             * Disables an extension for the session.
             *
             * @param params Source-qualified extension identifier to disable for the session.
             */
            disable: async (params: ExtensionsDisableRequest): Promise<void> =>
                connection.sendRequest("session.extensions.disable", { sessionId, ...params }),
            /**
             * Reloads extension definitions and processes for the session.
             */
            reload: async (): Promise<void> =>
                connection.sendRequest("session.extensions.reload", { sessionId }),
        },
        /** @experimental */
        tools: {
            /**
             * Provides the result for a pending external tool call.
             *
             * @param params Pending external tool call request ID, with the tool result or an error describing why it failed.
             *
             * @returns Indicates whether the external tool call result was handled successfully.
             */
            handlePendingToolCall: async (params: HandlePendingToolCallRequest): Promise<HandlePendingToolCallResult> =>
                connection.sendRequest("session.tools.handlePendingToolCall", { sessionId, ...params }),
            /**
             * Resolves, builds, and validates the runtime tool list for the session.
             *
             * @returns Resolve, build, and validate the runtime tool list for this session. Subagent sessions and consumer flows that need an initialized tool set before `send` invoke this. Default base-class implementation is a no-op for sessions that don't support tool validation.
             */
            initializeAndValidate: async (): Promise<ToolsInitializeAndValidateResult> =>
                connection.sendRequest("session.tools.initializeAndValidate", { sessionId }),
        },
        /** @experimental */
        commands: {
            /**
             * Lists slash commands available in the session.
             *
             * @param params Optional filters controlling which command sources to include in the listing.
             *
             * @returns Slash commands available in the session, after applying any include/exclude filters.
             */
            list: async (params?: CommandsListRequest): Promise<CommandList> =>
                connection.sendRequest("session.commands.list", { sessionId, ...params }),
            /**
             * Invokes a slash command in the session.
             *
             * @param params Slash command name and optional raw input string to invoke.
             *
             * @returns Result of invoking the slash command (text output, prompt to send to the agent, or completion).
             */
            invoke: async (params: CommandsInvokeRequest): Promise<SlashCommandInvocationResult> =>
                connection.sendRequest("session.commands.invoke", { sessionId, ...params }),
            /**
             * Reports completion of a pending client-handled slash command.
             *
             * @param params Pending command request ID and an optional error if the client handler failed.
             *
             * @returns Indicates whether the pending client-handled command was completed successfully.
             */
            handlePendingCommand: async (params: CommandsHandlePendingCommandRequest): Promise<CommandsHandlePendingCommandResult> =>
                connection.sendRequest("session.commands.handlePendingCommand", { sessionId, ...params }),
            /**
             * Executes a slash command synchronously and returns any error.
             *
             * @param params Slash command name and argument string to execute synchronously.
             *
             * @returns Error message produced while executing the command, if any.
             */
            execute: async (params: ExecuteCommandParams): Promise<ExecuteCommandResult> =>
                connection.sendRequest("session.commands.execute", { sessionId, ...params }),
            /**
             * Enqueues a slash command for FIFO processing on the local session.
             *
             * @param params Slash-prefixed command string to enqueue for FIFO processing.
             *
             * @returns Indicates whether the command was accepted into the local execution queue.
             */
            enqueue: async (params: EnqueueCommandParams): Promise<EnqueueCommandResult> =>
                connection.sendRequest("session.commands.enqueue", { sessionId, ...params }),
            /**
             * Reports whether the host actually executed a queued command and whether to continue processing.
             *
             * @param params Queued-command request ID and the result indicating whether the host executed it (and whether to stop processing further queued commands).
             *
             * @returns Indicates whether the queued-command response was matched to a pending request.
             */
            respondToQueuedCommand: async (params: CommandsRespondToQueuedCommandRequest): Promise<CommandsRespondToQueuedCommandResult> =>
                connection.sendRequest("session.commands.respondToQueuedCommand", { sessionId, ...params }),
        },
        /** @experimental */
        telemetry: {
            /**
             * Sets feature override key/value pairs to attach to subsequent telemetry events for the session.
             *
             * @param params Feature override key/value pairs to attach to subsequent telemetry events from this session.
             */
            setFeatureOverrides: async (params: TelemetrySetFeatureOverridesRequest): Promise<void> =>
                connection.sendRequest("session.telemetry.setFeatureOverrides", { sessionId, ...params }),
        },
        /** @experimental */
        ui: {
            /**
             * Requests structured input from a UI-capable client.
             *
             * @param params Prompt message and JSON schema describing the form fields to elicit from the user.
             *
             * @returns The elicitation response (accept with form values, decline, or cancel)
             */
            elicitation: async (params: UIElicitationRequest): Promise<UIElicitationResponse> =>
                connection.sendRequest("session.ui.elicitation", { sessionId, ...params }),
            /**
             * Provides the user response for a pending elicitation request.
             *
             * @param params Pending elicitation request ID and the user's response (accept/decline/cancel + form values).
             *
             * @returns Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
             */
            handlePendingElicitation: async (params: UIHandlePendingElicitationRequest): Promise<UIElicitationResult> =>
                connection.sendRequest("session.ui.handlePendingElicitation", { sessionId, ...params }),
            /**
             * Resolves a pending `user_input.requested` event with the user's response.
             *
             * @param params Request ID of a pending `user_input.requested` event and the user's response.
             *
             * @returns Indicates whether the pending UI request was resolved by this call.
             */
            handlePendingUserInput: async (params: UIHandlePendingUserInputRequest): Promise<UIHandlePendingResult> =>
                connection.sendRequest("session.ui.handlePendingUserInput", { sessionId, ...params }),
            /**
             * Resolves a pending `sampling.requested` event with a sampling result, or rejects it.
             *
             * @param params Request ID of a pending `sampling.requested` event and an optional sampling result payload (omit to reject).
             *
             * @returns Indicates whether the pending UI request was resolved by this call.
             */
            handlePendingSampling: async (params: UIHandlePendingSamplingRequest): Promise<UIHandlePendingResult> =>
                connection.sendRequest("session.ui.handlePendingSampling", { sessionId, ...params }),
            /**
             * Resolves a pending `auto_mode_switch.requested` event with the user's accept/decline decision.
             *
             * @param params Request ID of a pending `auto_mode_switch.requested` event and the user's response.
             *
             * @returns Indicates whether the pending UI request was resolved by this call.
             */
            handlePendingAutoModeSwitch: async (params: UIHandlePendingAutoModeSwitchRequest): Promise<UIHandlePendingResult> =>
                connection.sendRequest("session.ui.handlePendingAutoModeSwitch", { sessionId, ...params }),
            /**
             * Resolves a pending `exit_plan_mode.requested` event with the user's response.
             *
             * @param params Request ID of a pending `exit_plan_mode.requested` event and the user's response.
             *
             * @returns Indicates whether the pending UI request was resolved by this call.
             */
            handlePendingExitPlanMode: async (params: UIHandlePendingExitPlanModeRequest): Promise<UIHandlePendingResult> =>
                connection.sendRequest("session.ui.handlePendingExitPlanMode", { sessionId, ...params }),
            /**
             * Registers an in-process handler for auto-mode-switch requests so the server bridge skips dispatch.
             *
             * @returns Register an in-process handler for `auto_mode_switch.requested` events. The caller still attaches the actual listener via the standard event-subscription mechanism; this registration solely tells the server bridge to skip its own dispatch (so a remote client doesn't race the in-process handler for the same requestId).
             */
            registerDirectAutoModeSwitchHandler: async (): Promise<UIRegisterDirectAutoModeSwitchHandlerResult> =>
                connection.sendRequest("session.ui.registerDirectAutoModeSwitchHandler", { sessionId }),
            /**
             * Unregisters a previously-registered in-process auto-mode-switch handler by its opaque handle.
             *
             * @param params Opaque handle previously returned by `registerDirectAutoModeSwitchHandler` to release.
             *
             * @returns Indicates whether the handle was active and the registration count was decremented.
             */
            unregisterDirectAutoModeSwitchHandler: async (params: UIUnregisterDirectAutoModeSwitchHandlerRequest): Promise<UIUnregisterDirectAutoModeSwitchHandlerResult> =>
                connection.sendRequest("session.ui.unregisterDirectAutoModeSwitchHandler", { sessionId, ...params }),
        },
        /** @experimental */
        permissions: {
            /**
             * Replaces selected permission policy fields (rules, paths, URLs, exclusions, allow-all flags) on the session.
             *
             * @param params Patch of permission policy fields to apply (omit a field to leave it unchanged).
             *
             * @returns Indicates whether the operation succeeded.
             */
            configure: async (params: PermissionsConfigureParams): Promise<PermissionsConfigureResult> =>
                connection.sendRequest("session.permissions.configure", { sessionId, ...params }),
            /**
             * Provides a decision for a pending tool permission request.
             *
             * @param params Pending permission request ID and the decision to apply (approve/reject and scope).
             *
             * @returns Indicates whether the permission decision was applied; false when the request was already resolved.
             */
            handlePendingPermissionRequest: async (params: PermissionDecisionRequest): Promise<PermissionRequestResult> =>
                connection.sendRequest("session.permissions.handlePendingPermissionRequest", { sessionId, ...params }),
            /**
             * Reconstructs the set of pending tool permission requests from the session's event history.
             *
             * @returns List of pending permission requests reconstructed from event history.
             */
            pendingRequests: async (): Promise<PendingPermissionRequestList> =>
                connection.sendRequest("session.permissions.pendingRequests", { sessionId }),
            /**
             * Enables or disables automatic approval of tool permission requests for the session.
             *
             * @param params Allow-all toggle for tool permission requests, with an optional telemetry source.
             *
             * @returns Indicates whether the operation succeeded.
             */
            setApproveAll: async (params: PermissionsSetApproveAllRequest): Promise<PermissionsSetApproveAllResult> =>
                connection.sendRequest("session.permissions.setApproveAll", { sessionId, ...params }),
            /**
             * Adds or removes session-scoped or location-scoped permission rules.
             *
             * @param params Scope and add/remove instructions for modifying session- or location-scoped permission rules.
             *
             * @returns Indicates whether the operation succeeded.
             */
            modifyRules: async (params: PermissionsModifyRulesParams): Promise<PermissionsModifyRulesResult> =>
                connection.sendRequest("session.permissions.modifyRules", { sessionId, ...params }),
            /**
             * Sets whether the client wants permission prompts bridged into session events.
             *
             * @param params Toggles whether permission prompts should be bridged into session events for this client.
             *
             * @returns Indicates whether the operation succeeded.
             */
            setRequired: async (params: PermissionsSetRequiredRequest): Promise<PermissionsSetRequiredResult> =>
                connection.sendRequest("session.permissions.setRequired", { sessionId, ...params }),
            /**
             * Clears session-scoped tool permission approvals.
             *
             * @returns Indicates whether the operation succeeded.
             */
            resetSessionApprovals: async (): Promise<PermissionsResetSessionApprovalsResult> =>
                connection.sendRequest("session.permissions.resetSessionApprovals", { sessionId }),
            /**
             * Notifies the runtime that a permission prompt UI has been shown to the user.
             *
             * @param params Notification payload describing the permission prompt that the client just rendered.
             *
             * @returns Indicates whether the operation succeeded.
             */
            notifyPromptShown: async (params: PermissionPromptShownNotification): Promise<PermissionsNotifyPromptShownResult> =>
                connection.sendRequest("session.permissions.notifyPromptShown", { sessionId, ...params }),
            /** @experimental */
            paths: {
                /**
                 * Returns the session's allowed directories and primary working directory.
                 *
                 * @returns Snapshot of the session's allow-listed directories and primary working directory.
                 */
                list: async (): Promise<PermissionPathsList> =>
                    connection.sendRequest("session.permissions.paths.list", { sessionId }),
                /**
                 * Adds a directory to the session's allow-list.
                 *
                 * @param params Directory path to add to the session's allowed directories.
                 *
                 * @returns Indicates whether the operation succeeded.
                 */
                add: async (params: PermissionPathsAddParams): Promise<PermissionsPathsAddResult> =>
                    connection.sendRequest("session.permissions.paths.add", { sessionId, ...params }),
                /**
                 * Updates the session's primary working directory used by the permission policy.
                 *
                 * @param params Directory path to set as the session's new primary working directory.
                 *
                 * @returns Indicates whether the operation succeeded.
                 */
                updatePrimary: async (params: PermissionPathsUpdatePrimaryParams): Promise<PermissionsPathsUpdatePrimaryResult> =>
                    connection.sendRequest("session.permissions.paths.updatePrimary", { sessionId, ...params }),
                /**
                 * Reports whether a path falls within any of the session's allowed directories.
                 *
                 * @param params Path to evaluate against the session's allowed directories.
                 *
                 * @returns Indicates whether the supplied path is within the session's allowed directories.
                 */
                isPathWithinAllowedDirectories: async (params: PermissionPathsAllowedCheckParams): Promise<PermissionPathsAllowedCheckResult> =>
                    connection.sendRequest("session.permissions.paths.isPathWithinAllowedDirectories", { sessionId, ...params }),
                /**
                 * Reports whether a path falls within the session's workspace (primary) directory.
                 *
                 * @param params Path to evaluate against the session's workspace (primary) directory.
                 *
                 * @returns Indicates whether the supplied path is within the session's workspace directory.
                 */
                isPathWithinWorkspace: async (params: PermissionPathsWorkspaceCheckParams): Promise<PermissionPathsWorkspaceCheckResult> =>
                    connection.sendRequest("session.permissions.paths.isPathWithinWorkspace", { sessionId, ...params }),
            },
            /** @experimental */
            locations: {
                /**
                 * Resolves the permission location key and type for a working directory.
                 *
                 * @param params Working directory to resolve into a location-permissions key.
                 *
                 * @returns Resolved location-permissions key and type.
                 */
                resolve: async (params: PermissionLocationResolveParams): Promise<PermissionLocationResolveResult> =>
                    connection.sendRequest("session.permissions.locations.resolve", { sessionId, ...params }),
                /**
                 * Applies persisted location-scoped tool approvals and allowed directories for a working directory to this session's permission service.
                 *
                 * @param params Working directory to load persisted location permissions for.
                 *
                 * @returns Summary of persisted location permissions applied to the session.
                 */
                apply: async (params: PermissionLocationApplyParams): Promise<PermissionLocationApplyResult> =>
                    connection.sendRequest("session.permissions.locations.apply", { sessionId, ...params }),
                /**
                 * Persists a tool approval for a permission location and applies its rules to this session's live permission service.
                 *
                 * @param params Location-scoped tool approval to persist.
                 *
                 * @returns Indicates whether the operation succeeded.
                 */
                addToolApproval: async (params: PermissionLocationAddToolApprovalParams): Promise<PermissionsLocationsAddToolApprovalResult> =>
                    connection.sendRequest("session.permissions.locations.addToolApproval", { sessionId, ...params }),
            },
            /** @experimental */
            folderTrust: {
                /**
                 * Reports whether a folder is trusted according to the user's folder trust state.
                 *
                 * @param params Folder path to check for trust.
                 *
                 * @returns Folder trust check result.
                 */
                isTrusted: async (params: FolderTrustCheckParams): Promise<FolderTrustCheckResult> =>
                    connection.sendRequest("session.permissions.folderTrust.isTrusted", { sessionId, ...params }),
                /**
                 * Adds a folder to the user's trusted folders list.
                 *
                 * @param params Folder path to add to trusted folders.
                 *
                 * @returns Indicates whether the operation succeeded.
                 */
                addTrusted: async (params: FolderTrustAddParams): Promise<PermissionsFolderTrustAddTrustedResult> =>
                    connection.sendRequest("session.permissions.folderTrust.addTrusted", { sessionId, ...params }),
            },
            /** @experimental */
            urls: {
                /**
                 * Toggles the runtime's URL-permission policy between unrestricted and restricted modes.
                 *
                 * @param params Whether the URL-permission policy should run in unrestricted mode.
                 *
                 * @returns Indicates whether the operation succeeded.
                 */
                setUnrestrictedMode: async (params: PermissionUrlsSetUnrestrictedModeParams): Promise<PermissionsUrlsSetUnrestrictedModeResult> =>
                    connection.sendRequest("session.permissions.urls.setUnrestrictedMode", { sessionId, ...params }),
            },
        },
        /**
         * Emits a user-visible session log event.
         *
         * @param params Message text, optional severity level, persistence flag, optional follow-up URL, and optional tip.
         *
         * @returns Identifier of the session event that was emitted for the log message.
         *
         * @experimental
         */
        log: async (params: LogRequest): Promise<LogResult> =>
            connection.sendRequest("session.log", { sessionId, ...params }),
        /** @experimental */
        metadata: {
            /**
             * Returns a snapshot of the session's identifying metadata, mode, agent, and remote info.
             *
             * @returns Point-in-time snapshot of slow-changing session identifier and state fields
             */
            snapshot: async (): Promise<SessionMetadataSnapshot> =>
                connection.sendRequest("session.metadata.snapshot", { sessionId }),
            /**
             * Reports whether the local session is currently processing user/agent messages.
             *
             * @returns Indicates whether the local session is currently processing a turn or background continuation.
             */
            isProcessing: async (): Promise<MetadataIsProcessingResult> =>
                connection.sendRequest("session.metadata.isProcessing", { sessionId }),
            /**
             * Returns the token breakdown for the session's current context window for a given model.
             *
             * @param params Model identifier and token limits used to compute the context-info breakdown.
             *
             * @returns Token breakdown for the session's current context window, or null if uninitialized.
             */
            contextInfo: async (params: MetadataContextInfoRequest): Promise<MetadataContextInfoResult> =>
                connection.sendRequest("session.metadata.contextInfo", { sessionId, ...params }),
            /**
             * Records a working-directory/git context change and emits a `session.context_changed` event.
             *
             * @param params Updated working-directory/git context to record on the session.
             *
             * @returns Notify the session that its working directory context has changed. Emits a `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline UI) can react. Use this when the host has detected a cwd/branch/repo change outside the session's normal lifecycle (e.g., after a shell command in interactive mode).
             */
            recordContextChange: async (params: MetadataRecordContextChangeRequest): Promise<MetadataRecordContextChangeResult> =>
                connection.sendRequest("session.metadata.recordContextChange", { sessionId, ...params }),
            /**
             * Updates the session's recorded working directory.
             *
             * @param params Absolute path to set as the session's new working directory.
             *
             * @returns Update the session's working directory. Used by the host when the user explicitly changes cwd (e.g., the `/cd` slash command). The host is responsible for `process.chdir` and any related side-effects (file index, etc.); this method only updates the session's own recorded path.
             */
            setWorkingDirectory: async (params: MetadataSetWorkingDirectoryRequest): Promise<MetadataSetWorkingDirectoryResult> =>
                connection.sendRequest("session.metadata.setWorkingDirectory", { sessionId, ...params }),
            /**
             * Re-tokenizes the session's existing messages against a model and returns aggregate token totals.
             *
             * @param params Model identifier to use when re-tokenizing the session's existing messages.
             *
             * @returns Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.
             */
            recomputeContextTokens: async (params: MetadataRecomputeContextTokensRequest): Promise<MetadataRecomputeContextTokensResult> =>
                connection.sendRequest("session.metadata.recomputeContextTokens", { sessionId, ...params }),
        },
        /** @experimental */
        shell: {
            /**
             * Starts a shell command and streams output through session notifications.
             *
             * @param params Shell command to run, with optional working directory and timeout in milliseconds.
             *
             * @returns Identifier of the spawned process, used to correlate streamed output and exit notifications.
             */
            exec: async (params: ShellExecRequest): Promise<ShellExecResult> =>
                connection.sendRequest("session.shell.exec", { sessionId, ...params }),
            /**
             * Sends a signal to a shell process previously started via "shell.exec".
             *
             * @param params Identifier of a process previously returned by "shell.exec" and the signal to send.
             *
             * @returns Indicates whether the signal was delivered; false if the process was unknown or already exited.
             */
            kill: async (params: ShellKillRequest): Promise<ShellKillResult> =>
                connection.sendRequest("session.shell.kill", { sessionId, ...params }),
        },
        /** @experimental */
        history: {
            /**
             * Compacts the session history to reduce context usage.
             *
             * @param params Optional compaction parameters.
             *
             * @returns Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.
             */
            compact: async (params?: HistoryCompactRequest): Promise<HistoryCompactResult> =>
                connection.sendRequest("session.history.compact", { sessionId, ...params }),
            /**
             * Truncates persisted session history to a specific event.
             *
             * @param params Identifier of the event to truncate to; this event and all later events are removed.
             *
             * @returns Number of events that were removed by the truncation.
             */
            truncate: async (params: HistoryTruncateRequest): Promise<HistoryTruncateResult> =>
                connection.sendRequest("session.history.truncate", { sessionId, ...params }),
            /**
             * Cancels any in-progress background compaction on a local session.
             *
             * @returns Indicates whether an in-progress background compaction was cancelled.
             */
            cancelBackgroundCompaction: async (): Promise<HistoryCancelBackgroundCompactionResult> =>
                connection.sendRequest("session.history.cancelBackgroundCompaction", { sessionId }),
            /**
             * Aborts any in-progress manual compaction on a local session.
             *
             * @returns Indicates whether an in-progress manual compaction was aborted.
             */
            abortManualCompaction: async (): Promise<HistoryAbortManualCompactionResult> =>
                connection.sendRequest("session.history.abortManualCompaction", { sessionId }),
            /**
             * Produces a markdown summary of the session's conversation context for hand-off scenarios.
             *
             * @returns Markdown summary of the conversation context (empty when not available).
             */
            summarizeForHandoff: async (): Promise<HistorySummarizeForHandoffResult> =>
                connection.sendRequest("session.history.summarizeForHandoff", { sessionId }),
        },
        /** @experimental */
        queue: {
            /**
             * Returns the local session's pending user-facing queued items and steering messages.
             *
             * @returns Snapshot of the session's pending queued items and immediate-steering messages.
             */
            pendingItems: async (): Promise<QueuePendingItemsResult> =>
                connection.sendRequest("session.queue.pendingItems", { sessionId }),
            /**
             * Removes the most recently queued user-facing item (LIFO).
             *
             * @returns Indicates whether a user-facing pending item was removed.
             */
            removeMostRecent: async (): Promise<QueueRemoveMostRecentResult> =>
                connection.sendRequest("session.queue.removeMostRecent", { sessionId }),
            /**
             * Clears all pending queued items on the local session.
             */
            clear: async (): Promise<void> =>
                connection.sendRequest("session.queue.clear", { sessionId }),
        },
        /** @experimental */
        eventLog: {
            /**
             * Reads a batch of session events from a cursor, optionally waiting for new events.
             *
             * @param params Cursor, batch size, and optional long-poll/filter parameters for reading session events.
             *
             * @returns Batch of session events returned by a read, with cursor and continuation metadata.
             */
            read: async (params: EventLogReadRequest): Promise<EventsReadResult> =>
                connection.sendRequest("session.eventLog.read", { sessionId, ...params }),
            /**
             * Returns a snapshot of the current tail cursor without consuming events.
             *
             * @returns Snapshot of the current tail cursor without returning any events. Use this when a consumer wants to subscribe to live events going forward without first paginating through the entire persisted history (which would happen if `read` were called without a cursor on a long-lived session).
             */
            tail: async (): Promise<EventLogTailResult> =>
                connection.sendRequest("session.eventLog.tail", { sessionId }),
            /**
             * Registers consumer interest in an event type for runtime gating purposes.
             *
             * @param params Event type to register consumer interest for, used by runtime gating logic.
             *
             * @returns Opaque handle representing an event-type interest registration.
             */
            registerInterest: async (params: RegisterEventInterestParams): Promise<RegisterEventInterestResult> =>
                connection.sendRequest("session.eventLog.registerInterest", { sessionId, ...params }),
            /**
             * Releases a consumer's previously-registered interest in an event type.
             *
             * @param params Opaque handle previously returned by `registerInterest` to release.
             *
             * @returns Indicates whether the operation succeeded.
             */
            releaseInterest: async (params: ReleaseEventInterestParams): Promise<EventLogReleaseInterestResult> =>
                connection.sendRequest("session.eventLog.releaseInterest", { sessionId, ...params }),
        },
        /** @experimental */
        usage: {
            /**
             * Gets accumulated usage metrics for the session.
             *
             * @returns Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.
             */
            getMetrics: async (): Promise<UsageGetMetricsResult> =>
                connection.sendRequest("session.usage.getMetrics", { sessionId }),
        },
        /** @experimental */
        remote: {
            /**
             * Enables remote session export or steering.
             *
             * @param params Optional remote session mode ("off", "export", or "on"); defaults to enabling both export and remote steering.
             *
             * @returns GitHub URL for the session and a flag indicating whether remote steering is enabled.
             */
            enable: async (params: RemoteEnableRequest): Promise<RemoteEnableResult> =>
                connection.sendRequest("session.remote.enable", { sessionId, ...params }),
            /**
             * Disables remote session export and steering.
             */
            disable: async (): Promise<void> =>
                connection.sendRequest("session.remote.disable", { sessionId }),
            /**
             * Persists a remote-steerability change emitted by the host as a session event.
             *
             * @param params New remote-steerability state to persist as a `session.remote_steerable_changed` event.
             *
             * @returns Persist a steerability change as a `session.remote_steerable_changed` event. Used by the host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a remote exporter that the runtime does not directly own.
             */
            notifySteerableChanged: async (params: RemoteNotifySteerableChangedRequest): Promise<RemoteNotifySteerableChangedResult> =>
                connection.sendRequest("session.remote.notifySteerableChanged", { sessionId, ...params }),
        },
        /** @experimental */
        schedule: {
            /**
             * Lists the session's currently active scheduled prompts.
             *
             * @returns Snapshot of the currently active recurring prompts for this session.
             */
            list: async (): Promise<ScheduleList> =>
                connection.sendRequest("session.schedule.list", { sessionId }),
            /**
             * Removes a scheduled prompt by id.
             *
             * @param params Identifier of the scheduled prompt to remove.
             *
             * @returns Remove a scheduled prompt by id. The result entry is omitted if the id was unknown.
             */
            stop: async (params: ScheduleStopRequest): Promise<ScheduleStopResult> =>
                connection.sendRequest("session.schedule.stop", { sessionId, ...params }),
        },
    };
}

/** Handler for `sessionFs` client session API methods. */
export interface SessionFsHandler {
    /**
     * Reads a file from the client-provided session filesystem.
     *
     * @param params Path of the file to read from the client-provided session filesystem.
     *
     * @returns File content as a UTF-8 string, or a filesystem error if the read failed.
     */
    readFile(params: SessionFsReadFileRequest): Promise<SessionFsReadFileResult>;
    /**
     * Writes a file in the client-provided session filesystem.
     *
     * @param params File path, content to write, and optional mode for the client-provided session filesystem.
     *
     * @returns Describes a filesystem error.
     */
    writeFile(params: SessionFsWriteFileRequest): Promise<SessionFsError | undefined>;
    /**
     * Appends content to a file in the client-provided session filesystem.
     *
     * @param params File path, content to append, and optional mode for the client-provided session filesystem.
     *
     * @returns Describes a filesystem error.
     */
    appendFile(params: SessionFsAppendFileRequest): Promise<SessionFsError | undefined>;
    /**
     * Checks whether a path exists in the client-provided session filesystem.
     *
     * @param params Path to test for existence in the client-provided session filesystem.
     *
     * @returns Indicates whether the requested path exists in the client-provided session filesystem.
     */
    exists(params: SessionFsExistsRequest): Promise<SessionFsExistsResult>;
    /**
     * Gets metadata for a path in the client-provided session filesystem.
     *
     * @param params Path whose metadata should be returned from the client-provided session filesystem.
     *
     * @returns Filesystem metadata for the requested path, or a filesystem error if the stat failed.
     */
    stat(params: SessionFsStatRequest): Promise<SessionFsStatResult>;
    /**
     * Creates a directory in the client-provided session filesystem.
     *
     * @param params Directory path to create in the client-provided session filesystem, with options for recursive creation and POSIX mode.
     *
     * @returns Describes a filesystem error.
     */
    mkdir(params: SessionFsMkdirRequest): Promise<SessionFsError | undefined>;
    /**
     * Lists entry names in a directory from the client-provided session filesystem.
     *
     * @param params Directory path whose entries should be listed from the client-provided session filesystem.
     *
     * @returns Names of entries in the requested directory, or a filesystem error if the read failed.
     */
    readdir(params: SessionFsReaddirRequest): Promise<SessionFsReaddirResult>;
    /**
     * Lists directory entries with type information from the client-provided session filesystem.
     *
     * @param params Directory path whose entries (with type information) should be listed from the client-provided session filesystem.
     *
     * @returns Entries in the requested directory paired with file/directory type information, or a filesystem error if the read failed.
     */
    readdirWithTypes(params: SessionFsReaddirWithTypesRequest): Promise<SessionFsReaddirWithTypesResult>;
    /**
     * Removes a file or directory from the client-provided session filesystem.
     *
     * @param params Path to remove from the client-provided session filesystem, with options for recursive removal and force.
     *
     * @returns Describes a filesystem error.
     */
    rm(params: SessionFsRmRequest): Promise<SessionFsError | undefined>;
    /**
     * Renames or moves a path in the client-provided session filesystem.
     *
     * @param params Source and destination paths for renaming or moving an entry in the client-provided session filesystem.
     *
     * @returns Describes a filesystem error.
     */
    rename(params: SessionFsRenameRequest): Promise<SessionFsError | undefined>;
    /**
     * Executes a SQLite query against the per-session database.
     *
     * @param params SQL query, query type, and optional bind parameters for executing a SQLite query against the per-session database.
     *
     * @returns Query results including rows, columns, and rows affected, or a filesystem error if execution failed.
     */
    sqliteQuery(params: SessionFsSqliteQueryRequest): Promise<SessionFsSqliteQueryResult>;
    /**
     * Checks whether the per-session SQLite database already exists, without creating it.
     *
     * @param params Identifies the target session.
     *
     * @returns Indicates whether the per-session SQLite database already exists.
     */
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
