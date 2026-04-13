/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: api.schema.json
 */

import type { MessageConnection } from "vscode-jsonrpc/node.js";

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

export interface PingRequest {
  /**
   * Optional message to echo back
   */
  message?: string;
}

export interface ModelList {
  /**
   * List of available models with full metadata
   */
  models: {
    /**
     * Model identifier (e.g., "claude-sonnet-4.5")
     */
    id: string;
    /**
     * Display name
     */
    name: string;
    capabilities: ModelCapabilities;
    /**
     * Policy state (if applicable)
     */
    policy?: {
      /**
       * Current policy state for this model
       */
      state: string;
      /**
       * Usage terms or conditions for this model
       */
      terms: string;
    };
    /**
     * Billing information
     */
    billing?: {
      /**
       * Billing cost multiplier relative to the base rate
       */
      multiplier: number;
    };
    /**
     * Supported reasoning effort levels (only present if model supports reasoning effort)
     */
    supportedReasoningEfforts?: string[];
    /**
     * Default reasoning effort level (only present if model supports reasoning effort)
     */
    defaultReasoningEffort?: string;
  }[];
}
/**
 * Model capabilities and limits
 */
export interface ModelCapabilities {
  /**
   * Feature flags indicating what the model supports
   */
  supports: {
    /**
     * Whether this model supports vision/image input
     */
    vision?: boolean;
    /**
     * Whether this model supports reasoning effort configuration
     */
    reasoningEffort?: boolean;
  };
  /**
   * Token limits for prompts, outputs, and context window
   */
  limits: {
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
    max_context_window_tokens: number;
    /**
     * Vision-specific limits
     */
    vision?: {
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
    };
  };
}

export interface ToolList {
  /**
   * List of available built-in tools with metadata
   */
  tools: {
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
  }[];
}

export interface ToolsListRequest {
  /**
   * Optional model ID — when provided, the returned tool list reflects model-specific overrides
   */
  model?: string;
}

export interface AccountGetQuotaResult {
  /**
   * Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)
   */
  quotaSnapshots: {
    [k: string]: {
      /**
       * Number of requests included in the entitlement
       */
      entitlementRequests: number;
      /**
       * Number of requests used so far this period
       */
      usedRequests: number;
      /**
       * Percentage of entitlement remaining
       */
      remainingPercentage: number;
      /**
       * Number of overage requests made this period
       */
      overage: number;
      /**
       * Whether pay-per-request usage is allowed when quota is exhausted
       */
      overageAllowedWithExhaustedQuota: boolean;
      /**
       * Date when the quota resets (ISO 8601)
       */
      resetDate?: string;
    };
  };
}

export interface McpConfigList {
  /**
   * All MCP servers from user config, keyed by name
   */
  servers: {
    /**
     * MCP server configuration (local/stdio or remote/http)
     */
    [k: string]:
      | {
          /**
           * Tools to include. Defaults to all tools if not specified.
           */
          tools?: string[];
          type?: "local" | "stdio";
          isDefaultServer?: boolean;
          filterMapping?:
            | {
                [k: string]: "none" | "markdown" | "hidden_characters";
              }
            | ("none" | "markdown" | "hidden_characters");
          /**
           * Timeout in milliseconds for tool calls to this server.
           */
          timeout?: number;
          command: string;
          args: string[];
          cwd?: string;
          env?: {
            [k: string]: string;
          };
        }
      | {
          /**
           * Tools to include. Defaults to all tools if not specified.
           */
          tools?: string[];
          type: "http" | "sse";
          isDefaultServer?: boolean;
          filterMapping?:
            | {
                [k: string]: "none" | "markdown" | "hidden_characters";
              }
            | ("none" | "markdown" | "hidden_characters");
          /**
           * Timeout in milliseconds for tool calls to this server.
           */
          timeout?: number;
          url: string;
          headers?: {
            [k: string]: string;
          };
          oauthClientId?: string;
          oauthPublicClient?: boolean;
        };
  };
}

export interface McpConfigAddRequest {
  /**
   * Unique name for the MCP server
   */
  name: string;
  /**
   * MCP server configuration (local/stdio or remote/http)
   */
  config:
    | {
        /**
         * Tools to include. Defaults to all tools if not specified.
         */
        tools?: string[];
        type?: "local" | "stdio";
        isDefaultServer?: boolean;
        filterMapping?:
          | {
              [k: string]: "none" | "markdown" | "hidden_characters";
            }
          | ("none" | "markdown" | "hidden_characters");
        /**
         * Timeout in milliseconds for tool calls to this server.
         */
        timeout?: number;
        command: string;
        args: string[];
        cwd?: string;
        env?: {
          [k: string]: string;
        };
      }
    | {
        /**
         * Tools to include. Defaults to all tools if not specified.
         */
        tools?: string[];
        type: "http" | "sse";
        isDefaultServer?: boolean;
        filterMapping?:
          | {
              [k: string]: "none" | "markdown" | "hidden_characters";
            }
          | ("none" | "markdown" | "hidden_characters");
        /**
         * Timeout in milliseconds for tool calls to this server.
         */
        timeout?: number;
        url: string;
        headers?: {
          [k: string]: string;
        };
        oauthClientId?: string;
        oauthPublicClient?: boolean;
      };
}

export interface McpConfigUpdateRequest {
  /**
   * Name of the MCP server to update
   */
  name: string;
  /**
   * MCP server configuration (local/stdio or remote/http)
   */
  config:
    | {
        /**
         * Tools to include. Defaults to all tools if not specified.
         */
        tools?: string[];
        type?: "local" | "stdio";
        isDefaultServer?: boolean;
        filterMapping?:
          | {
              [k: string]: "none" | "markdown" | "hidden_characters";
            }
          | ("none" | "markdown" | "hidden_characters");
        /**
         * Timeout in milliseconds for tool calls to this server.
         */
        timeout?: number;
        command: string;
        args: string[];
        cwd?: string;
        env?: {
          [k: string]: string;
        };
      }
    | {
        /**
         * Tools to include. Defaults to all tools if not specified.
         */
        tools?: string[];
        type: "http" | "sse";
        isDefaultServer?: boolean;
        filterMapping?:
          | {
              [k: string]: "none" | "markdown" | "hidden_characters";
            }
          | ("none" | "markdown" | "hidden_characters");
        /**
         * Timeout in milliseconds for tool calls to this server.
         */
        timeout?: number;
        url: string;
        headers?: {
          [k: string]: string;
        };
        oauthClientId?: string;
        oauthPublicClient?: boolean;
      };
}

export interface McpConfigRemoveRequest {
  /**
   * Name of the MCP server to remove
   */
  name: string;
}

export interface McpDiscoverResult {
  /**
   * MCP servers discovered from all sources
   */
  servers: DiscoveredMcpServer[];
}
export interface DiscoveredMcpServer {
  /**
   * Server name (config key)
   */
  name: string;
  /**
   * Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio)
   */
  type?: "stdio" | "http" | "sse" | "memory";
  /**
   * Configuration source
   */
  source: "user" | "workspace" | "plugin" | "builtin";
  /**
   * Whether the server is enabled (not in the disabled list)
   */
  enabled: boolean;
}

export interface McpDiscoverRequest {
  /**
   * Working directory used as context for discovery (e.g., plugin resolution)
   */
  workingDirectory?: string;
}

export interface SessionFsSetProviderResult {
  /**
   * Whether the provider was set successfully
   */
  success: boolean;
}

export interface SessionFsSetProviderRequest {
  /**
   * Initial working directory for sessions
   */
  initialCwd: string;
  /**
   * Path within each session's SessionFs where the runtime stores files for that session
   */
  sessionStatePath: string;
  /**
   * Path conventions used by this filesystem
   */
  conventions: "windows" | "posix";
}

/** @experimental */
export interface SessionsForkResult {
  /**
   * The new forked session's ID
   */
  sessionId: string;
}

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
}

export interface CurrentModel {
  /**
   * Currently active model identifier
   */
  modelId?: string;
}

export interface SessionModelGetCurrentRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface ModelSwitchToResult {
  /**
   * Currently active model identifier after the switch
   */
  modelId?: string;
}

export interface ModelSwitchToRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Model identifier to switch to
   */
  modelId: string;
  /**
   * Reasoning effort level to use for the model
   */
  reasoningEffort?: string;
  modelCapabilities?: ModelCapabilitiesOverride;
}
/**
 * Override individual model capabilities resolved by the runtime
 */
export interface ModelCapabilitiesOverride {
  /**
   * Feature flags indicating what the model supports
   */
  supports?: {
    vision?: boolean;
    reasoningEffort?: boolean;
  };
  /**
   * Token limits for prompts, outputs, and context window
   */
  limits?: {
    max_prompt_tokens?: number;
    max_output_tokens?: number;
    /**
     * Maximum total context window size in tokens
     */
    max_context_window_tokens?: number;
    vision?: {
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
    };
  };
}

/**
 * The agent mode. Valid values: "interactive", "plan", "autopilot".
 */
export type SessionMode = "interactive" | "plan" | "autopilot";

export interface SessionModeGetRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface ModeSetRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  mode: SessionMode;
}

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

export interface SessionPlanReadRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface PlanUpdateRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * The new content for the plan file
   */
  content: string;
}

export interface SessionPlanDeleteRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface WorkspaceListFilesResult {
  /**
   * Relative file paths in the workspace files directory
   */
  files: string[];
}

export interface SessionWorkspaceListFilesRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface WorkspaceReadFileResult {
  /**
   * File content as a UTF-8 string
   */
  content: string;
}

export interface WorkspaceReadFileRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Relative path within the workspace files directory
   */
  path: string;
}

export interface WorkspaceCreateFileRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Relative path within the workspace files directory
   */
  path: string;
  /**
   * File content to write as a UTF-8 string
   */
  content: string;
}

/** @experimental */
export interface FleetStartResult {
  /**
   * Whether fleet mode was successfully activated
   */
  started: boolean;
}

/** @experimental */
export interface FleetStartRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Optional user prompt to combine with fleet instructions
   */
  prompt?: string;
}

/** @experimental */
export interface AgentList {
  /**
   * Available custom agents
   */
  agents: {
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
  }[];
}

/** @experimental */
export interface SessionAgentListRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface AgentGetCurrentResult {
  /**
   * Currently selected custom agent, or null if using the default agent
   */
  agent: {
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
  } | null;
}

/** @experimental */
export interface SessionAgentGetCurrentRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface AgentSelectResult {
  /**
   * The newly selected custom agent
   */
  agent: {
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
  };
}

/** @experimental */
export interface AgentSelectRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Name of the custom agent to select
   */
  name: string;
}

/** @experimental */
export interface SessionAgentDeselectRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface AgentReloadResult {
  /**
   * Reloaded custom agents
   */
  agents: {
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
  }[];
}

/** @experimental */
export interface SessionAgentReloadRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SkillList {
  /**
   * Available skills
   */
  skills: {
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
  }[];
}

/** @experimental */
export interface SessionSkillsListRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SkillsEnableRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Name of the skill to enable
   */
  name: string;
}

/** @experimental */
export interface SkillsDisableRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Name of the skill to disable
   */
  name: string;
}

/** @experimental */
export interface SessionSkillsReloadRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface McpServerList {
  /**
   * Configured MCP servers
   */
  servers: {
    /**
     * Server name (config key)
     */
    name: string;
    /**
     * Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
     */
    status: "connected" | "failed" | "needs-auth" | "pending" | "disabled" | "not_configured";
    /**
     * Configuration source: user, workspace, plugin, or builtin
     */
    source?: "user" | "workspace" | "plugin" | "builtin";
    /**
     * Error message if the server failed to connect
     */
    error?: string;
  }[];
}

/** @experimental */
export interface SessionMcpListRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface McpEnableRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Name of the MCP server to enable
   */
  serverName: string;
}

/** @experimental */
export interface McpDisableRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Name of the MCP server to disable
   */
  serverName: string;
}

/** @experimental */
export interface SessionMcpReloadRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface PluginList {
  /**
   * Installed plugins
   */
  plugins: {
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
  }[];
}

/** @experimental */
export interface SessionPluginsListRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface ExtensionList {
  /**
   * Discovered extensions and their current status
   */
  extensions: {
    /**
     * Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper')
     */
    id: string;
    /**
     * Extension name (directory name)
     */
    name: string;
    /**
     * Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
     */
    source: "project" | "user";
    /**
     * Current status: running, disabled, failed, or starting
     */
    status: "running" | "disabled" | "failed" | "starting";
    /**
     * Process ID if the extension is running
     */
    pid?: number;
  }[];
}

/** @experimental */
export interface SessionExtensionsListRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface ExtensionsEnableRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Source-qualified extension ID to enable
   */
  id: string;
}

/** @experimental */
export interface ExtensionsDisableRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Source-qualified extension ID to disable
   */
  id: string;
}

/** @experimental */
export interface SessionExtensionsReloadRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface HandleToolCallResult {
  /**
   * Whether the tool call result was handled successfully
   */
  success: boolean;
}

export interface ToolsHandlePendingToolCallRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Request ID of the pending tool call
   */
  requestId: string;
  /**
   * Tool call result (string or expanded result object)
   */
  result?: string | ToolCallResult;
  /**
   * Error message if the tool call failed
   */
  error?: string;
}
export interface ToolCallResult {
  /**
   * Text result to send back to the LLM
   */
  textResultForLlm: string;
  /**
   * Type of the tool result
   */
  resultType?: string;
  /**
   * Error message if the tool call failed
   */
  error?: string;
  /**
   * Telemetry data from tool execution
   */
  toolTelemetry?: {
    [k: string]: unknown;
  };
}

export interface CommandsHandlePendingCommandResult {
  /**
   * Whether the command was handled successfully
   */
  success: boolean;
}

export interface CommandsHandlePendingCommandRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
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
 * The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
 */
export type UIElicitationResponseAction = "accept" | "decline" | "cancel";
export type UIElicitationFieldValue = string | number | boolean | string[];
/**
 * The elicitation response (accept with form values, decline, or cancel)
 */
export interface UIElicitationResponse {
  action: UIElicitationResponseAction;
  content?: UIElicitationResponseContent;
}
/**
 * The form values submitted by the user (present when action is 'accept')
 */
export interface UIElicitationResponseContent {
  [k: string]: UIElicitationFieldValue;
}

export interface UIElicitationRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Message describing what information is needed from the user
   */
  message: string;
  /**
   * JSON Schema describing the form fields to present to the user
   */
  requestedSchema: {
    /**
     * Schema type indicator (always 'object')
     */
    type: "object";
    /**
     * Form field definitions, keyed by field name
     */
    properties: {
      [k: string]:
        | UIElicitationStringEnumField
        | UIElicitationStringOneOfField
        | UIElicitationArrayEnumField
        | UIElicitationArrayAnyOfField
        | {
            type: "boolean";
            description?: string;
            default?: boolean;
          }
        | {
            type: "string";
            description?: string;
            minLength?: number;
            maxLength?: number;
            format?: "email" | "uri" | "date" | "date-time";
            default?: string;
          }
        | {
            type: "number" | "integer";
            description?: string;
            minimum?: number;
            maximum?: number;
            default?: number;
          };
    };
    /**
     * List of required field names
     */
    required?: string[];
  };
}
export interface UIElicitationStringEnumField {
  type: "string";
  description?: string;
  enum: string[];
  enumNames?: string[];
  default?: string;
}
export interface UIElicitationStringOneOfField {
  type: "string";
  description?: string;
  oneOf: {
    const: string;
  }[];
  default?: string;
}
export interface UIElicitationArrayEnumField {
  type: "array";
  description?: string;
  minItems?: number;
  maxItems?: number;
  items: {
    type: "string";
    enum: string[];
  };
  default?: string[];
}
export interface UIElicitationArrayAnyOfField {
  type: "array";
  description?: string;
  minItems?: number;
  maxItems?: number;
  items: {
    anyOf: {
      const: string;
    }[];
  };
  default?: string[];
}

export interface UIElicitationResult {
  /**
   * Whether the response was accepted. False if the request was already resolved by another client.
   */
  success: boolean;
}

export interface UIHandlePendingElicitationRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * The unique request ID from the elicitation.requested event
   */
  requestId: string;
  result: UIElicitationResponse;
}

export interface PermissionRequestResult {
  /**
   * Whether the permission request was handled successfully
   */
  success: boolean;
}

export type PermissionDecision =
  | {
      /**
       * The permission request was approved
       */
      kind: "approved";
    }
| {
      /**
       * Denied because approval rules explicitly blocked it
       */
      kind: "denied-by-rules";
      /**
       * Rules that denied the request
       */
      rules: unknown[];
    }
  | {
      /**
       * Denied because no approval rule matched and user confirmation was unavailable
       */
      kind: "denied-no-approval-rule-and-could-not-request-from-user";
    }
  | {
      /**
       * Denied by the user during an interactive prompt
       */
      kind: "denied-interactively-by-user";
      /**
       * Optional feedback from the user explaining the denial
       */
      feedback?: string;
    }
  | {
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
  | {
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
    };

export interface PermissionDecisionRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Request ID of the pending permission request
   */
  requestId: string;
  result: PermissionDecision;
}

export interface LogResult {
  /**
   * The unique identifier of the emitted session event
   */
  eventId: string;
}

/**
 * Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
 */
export type SessionLogLevel = "info" | "warning" | "error";
export interface LogRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
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

export interface ShellExecResult {
  /**
   * Unique identifier for tracking streamed output
   */
  processId: string;
}

export interface ShellExecRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
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

export interface ShellKillResult {
  /**
   * Whether the signal was sent successfully
   */
  killed: boolean;
}

export interface ShellKillRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Process identifier returned by shell.exec
   */
  processId: string;
  /**
   * Signal to send (default: SIGTERM)
   */
  signal?: "SIGTERM" | "SIGKILL" | "SIGINT";
}

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
   * Post-compaction context window usage breakdown
   */
  contextWindow?: {
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
  };
}

/** @experimental */
export interface SessionHistoryCompactRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface HistoryTruncateResult {
  /**
   * Number of events that were removed
   */
  eventsRemoved: number;
}

/** @experimental */
export interface HistoryTruncateRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Event ID to truncate to. This event and all events after it are removed from the session.
   */
  eventId: string;
}

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
   * Total time spent in model API calls (milliseconds)
   */
  totalApiDurationMs: number;
  /**
   * Session start timestamp (epoch milliseconds)
   */
  sessionStartTime: number;
  /**
   * Aggregated code change metrics
   */
  codeChanges: {
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
  };
  /**
   * Per-model token and request metrics, keyed by model identifier
   */
  modelMetrics: {
    [k: string]: {
      /**
       * Request count and cost metrics for this model
       */
      requests: {
        /**
         * Number of API requests made with this model
         */
        count: number;
        /**
         * User-initiated premium request cost (with multiplier applied)
         */
        cost: number;
      };
      /**
       * Token usage metrics for this model
       */
      usage: {
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
      };
    };
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

/** @experimental */
export interface SessionUsageGetMetricsRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface SessionFsReadFileResult {
  /**
   * File content as UTF-8 string
   */
  content: string;
}

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

export interface SessionFsExistsResult {
  /**
   * Whether the path exists
   */
  exists: boolean;
}

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
}

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

export interface SessionFsReaddirResult {
  /**
   * Entry names in the directory
   */
  entries: string[];
}

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

export interface SessionFsReaddirWithTypesResult {
  /**
   * Directory entries with type information
   */
  entries: {
    /**
     * Entry name
     */
    name: string;
    /**
     * Entry type
     */
    type: "file" | "directory";
  }[];
}

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

/** Create typed server-scoped RPC methods (no session required). */
export function createServerRpc(connection: MessageConnection) {
    return {
        ping: async (params: PingRequest): Promise<PingResult> =>
            connection.sendRequest("ping", params),
        models: {
            list: async (): Promise<ModelList> =>
                connection.sendRequest("models.list", {}),
        },
        tools: {
            list: async (params: ToolsListRequest): Promise<ToolList> =>
                connection.sendRequest("tools.list", params),
        },
        account: {
            getQuota: async (): Promise<AccountGetQuotaResult> =>
                connection.sendRequest("account.getQuota", {}),
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
            },
            discover: async (params: McpDiscoverRequest): Promise<McpDiscoverResult> =>
                connection.sendRequest("mcp.discover", params),
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

/** Create typed session-scoped RPC methods. */
export function createSessionRpc(connection: MessageConnection, sessionId: string) {
    return {
        model: {
            getCurrent: async (): Promise<CurrentModel> =>
                connection.sendRequest("session.model.getCurrent", { sessionId }),
            switchTo: async (params: Omit<ModelSwitchToRequest, "sessionId">): Promise<ModelSwitchToResult> =>
                connection.sendRequest("session.model.switchTo", { sessionId, ...params }),
        },
        mode: {
            get: async (): Promise<SessionMode> =>
                connection.sendRequest("session.mode.get", { sessionId }),
            set: async (params: Omit<ModeSetRequest, "sessionId">): Promise<void> =>
                connection.sendRequest("session.mode.set", { sessionId, ...params }),
        },
        plan: {
            read: async (): Promise<PlanReadResult> =>
                connection.sendRequest("session.plan.read", { sessionId }),
            update: async (params: Omit<PlanUpdateRequest, "sessionId">): Promise<void> =>
                connection.sendRequest("session.plan.update", { sessionId, ...params }),
            delete: async (): Promise<void> =>
                connection.sendRequest("session.plan.delete", { sessionId }),
        },
        workspace: {
            listFiles: async (): Promise<WorkspaceListFilesResult> =>
                connection.sendRequest("session.workspace.listFiles", { sessionId }),
            readFile: async (params: Omit<WorkspaceReadFileRequest, "sessionId">): Promise<WorkspaceReadFileResult> =>
                connection.sendRequest("session.workspace.readFile", { sessionId, ...params }),
            createFile: async (params: Omit<WorkspaceCreateFileRequest, "sessionId">): Promise<void> =>
                connection.sendRequest("session.workspace.createFile", { sessionId, ...params }),
        },
        /** @experimental */
        fleet: {
            start: async (params: Omit<FleetStartRequest, "sessionId">): Promise<FleetStartResult> =>
                connection.sendRequest("session.fleet.start", { sessionId, ...params }),
        },
        /** @experimental */
        agent: {
            list: async (): Promise<AgentList> =>
                connection.sendRequest("session.agent.list", { sessionId }),
            getCurrent: async (): Promise<AgentGetCurrentResult> =>
                connection.sendRequest("session.agent.getCurrent", { sessionId }),
            select: async (params: Omit<AgentSelectRequest, "sessionId">): Promise<AgentSelectResult> =>
                connection.sendRequest("session.agent.select", { sessionId, ...params }),
            deselect: async (): Promise<void> =>
                connection.sendRequest("session.agent.deselect", { sessionId }),
            reload: async (): Promise<AgentReloadResult> =>
                connection.sendRequest("session.agent.reload", { sessionId }),
        },
        /** @experimental */
        skills: {
            list: async (): Promise<SkillList> =>
                connection.sendRequest("session.skills.list", { sessionId }),
            enable: async (params: Omit<SkillsEnableRequest, "sessionId">): Promise<void> =>
                connection.sendRequest("session.skills.enable", { sessionId, ...params }),
            disable: async (params: Omit<SkillsDisableRequest, "sessionId">): Promise<void> =>
                connection.sendRequest("session.skills.disable", { sessionId, ...params }),
            reload: async (): Promise<void> =>
                connection.sendRequest("session.skills.reload", { sessionId }),
        },
        /** @experimental */
        mcp: {
            list: async (): Promise<McpServerList> =>
                connection.sendRequest("session.mcp.list", { sessionId }),
            enable: async (params: Omit<McpEnableRequest, "sessionId">): Promise<void> =>
                connection.sendRequest("session.mcp.enable", { sessionId, ...params }),
            disable: async (params: Omit<McpDisableRequest, "sessionId">): Promise<void> =>
                connection.sendRequest("session.mcp.disable", { sessionId, ...params }),
            reload: async (): Promise<void> =>
                connection.sendRequest("session.mcp.reload", { sessionId }),
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
            enable: async (params: Omit<ExtensionsEnableRequest, "sessionId">): Promise<void> =>
                connection.sendRequest("session.extensions.enable", { sessionId, ...params }),
            disable: async (params: Omit<ExtensionsDisableRequest, "sessionId">): Promise<void> =>
                connection.sendRequest("session.extensions.disable", { sessionId, ...params }),
            reload: async (): Promise<void> =>
                connection.sendRequest("session.extensions.reload", { sessionId }),
        },
        tools: {
            handlePendingToolCall: async (params: Omit<ToolsHandlePendingToolCallRequest, "sessionId">): Promise<HandleToolCallResult> =>
                connection.sendRequest("session.tools.handlePendingToolCall", { sessionId, ...params }),
        },
        commands: {
            handlePendingCommand: async (params: Omit<CommandsHandlePendingCommandRequest, "sessionId">): Promise<CommandsHandlePendingCommandResult> =>
                connection.sendRequest("session.commands.handlePendingCommand", { sessionId, ...params }),
        },
        ui: {
            elicitation: async (params: Omit<UIElicitationRequest, "sessionId">): Promise<UIElicitationResponse> =>
                connection.sendRequest("session.ui.elicitation", { sessionId, ...params }),
            handlePendingElicitation: async (params: Omit<UIHandlePendingElicitationRequest, "sessionId">): Promise<UIElicitationResult> =>
                connection.sendRequest("session.ui.handlePendingElicitation", { sessionId, ...params }),
        },
        permissions: {
            handlePendingPermissionRequest: async (params: Omit<PermissionDecisionRequest, "sessionId">): Promise<PermissionRequestResult> =>
                connection.sendRequest("session.permissions.handlePendingPermissionRequest", { sessionId, ...params }),
        },
        log: async (params: Omit<LogRequest, "sessionId">): Promise<LogResult> =>
            connection.sendRequest("session.log", { sessionId, ...params }),
        shell: {
            exec: async (params: Omit<ShellExecRequest, "sessionId">): Promise<ShellExecResult> =>
                connection.sendRequest("session.shell.exec", { sessionId, ...params }),
            kill: async (params: Omit<ShellKillRequest, "sessionId">): Promise<ShellKillResult> =>
                connection.sendRequest("session.shell.kill", { sessionId, ...params }),
        },
        /** @experimental */
        history: {
            compact: async (): Promise<HistoryCompactResult> =>
                connection.sendRequest("session.history.compact", { sessionId }),
            truncate: async (params: Omit<HistoryTruncateRequest, "sessionId">): Promise<HistoryTruncateResult> =>
                connection.sendRequest("session.history.truncate", { sessionId, ...params }),
        },
        /** @experimental */
        usage: {
            getMetrics: async (): Promise<UsageGetMetricsResult> =>
                connection.sendRequest("session.usage.getMetrics", { sessionId }),
        },
    };
}

/** Handler for `sessionFs` client session API methods. */
export interface SessionFsHandler {
    readFile(params: SessionFsReadFileRequest): Promise<SessionFsReadFileResult>;
    writeFile(params: SessionFsWriteFileRequest): Promise<void>;
    appendFile(params: SessionFsAppendFileRequest): Promise<void>;
    exists(params: SessionFsExistsRequest): Promise<SessionFsExistsResult>;
    stat(params: SessionFsStatRequest): Promise<SessionFsStatResult>;
    mkdir(params: SessionFsMkdirRequest): Promise<void>;
    readdir(params: SessionFsReaddirRequest): Promise<SessionFsReaddirResult>;
    readdirWithTypes(params: SessionFsReaddirWithTypesRequest): Promise<SessionFsReaddirWithTypesResult>;
    rm(params: SessionFsRmRequest): Promise<void>;
    rename(params: SessionFsRenameRequest): Promise<void>;
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
}
