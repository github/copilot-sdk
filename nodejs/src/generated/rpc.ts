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

export interface PingParams {
  /**
   * Optional message to echo back
   */
  message?: string;
}

export interface ModelsListResult {
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
    /**
     * Model capabilities and limits
     */
    capabilities: {
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
      };
    };
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

export interface ToolsListResult {
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

export interface ToolsListParams {
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

export interface McpConfigListResult {
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

export interface McpConfigAddParams {
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
        timeout?: number;
        url: string;
        headers?: {
          [k: string]: string;
        };
        oauthClientId?: string;
        oauthPublicClient?: boolean;
      };
}

export interface McpConfigUpdateParams {
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
        timeout?: number;
        url: string;
        headers?: {
          [k: string]: string;
        };
        oauthClientId?: string;
        oauthPublicClient?: boolean;
      };
}

export interface McpConfigRemoveParams {
  /**
   * Name of the MCP server to remove
   */
  name: string;
}

export interface SessionFsSetProviderResult {
  /**
   * Whether the provider was set successfully
   */
  success: boolean;
}

export interface SessionFsSetProviderParams {
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

export interface SessionModelGetCurrentResult {
  /**
   * Currently active model identifier
   */
  modelId?: string;
}

export interface SessionModelGetCurrentParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface SessionModelSwitchToResult {
  /**
   * Currently active model identifier after the switch
   */
  modelId?: string;
}

export interface SessionModelSwitchToParams {
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
}

export interface SessionModeGetResult {
  /**
   * The current agent mode.
   */
  mode: "interactive" | "plan" | "autopilot";
}

export interface SessionModeGetParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface SessionModeSetResult {
  /**
   * The agent mode after switching.
   */
  mode: "interactive" | "plan" | "autopilot";
}

export interface SessionModeSetParams {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * The mode to switch to. Valid values: "interactive", "plan", "autopilot".
   */
  mode: "interactive" | "plan" | "autopilot";
}

export interface SessionPlanReadResult {
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

export interface SessionPlanReadParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface SessionPlanUpdateResult {}

export interface SessionPlanUpdateParams {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * The new content for the plan file
   */
  content: string;
}

export interface SessionPlanDeleteResult {}

export interface SessionPlanDeleteParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface SessionWorkspaceListFilesResult {
  /**
   * Relative file paths in the workspace files directory
   */
  files: string[];
}

export interface SessionWorkspaceListFilesParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface SessionWorkspaceReadFileResult {
  /**
   * File content as a UTF-8 string
   */
  content: string;
}

export interface SessionWorkspaceReadFileParams {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Relative path within the workspace files directory
   */
  path: string;
}

export interface SessionWorkspaceCreateFileResult {}

export interface SessionWorkspaceCreateFileParams {
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
export interface SessionFleetStartResult {
  /**
   * Whether fleet mode was successfully activated
   */
  started: boolean;
}

/** @experimental */
export interface SessionFleetStartParams {
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
export interface SessionAgentListResult {
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
export interface SessionAgentListParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SessionAgentGetCurrentResult {
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
export interface SessionAgentGetCurrentParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SessionAgentSelectResult {
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
export interface SessionAgentSelectParams {
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
export interface SessionAgentDeselectResult {}

/** @experimental */
export interface SessionAgentDeselectParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SessionAgentReloadResult {
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
export interface SessionAgentReloadParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SessionSkillsListResult {
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
export interface SessionSkillsListParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SessionSkillsEnableResult {}

/** @experimental */
export interface SessionSkillsEnableParams {
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
export interface SessionSkillsDisableResult {}

/** @experimental */
export interface SessionSkillsDisableParams {
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
export interface SessionSkillsReloadResult {}

/** @experimental */
export interface SessionSkillsReloadParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SessionMcpListResult {
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
    source?: string;
    /**
     * Error message if the server failed to connect
     */
    error?: string;
  }[];
}

/** @experimental */
export interface SessionMcpListParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SessionMcpEnableResult {}

/** @experimental */
export interface SessionMcpEnableParams {
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
export interface SessionMcpDisableResult {}

/** @experimental */
export interface SessionMcpDisableParams {
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
export interface SessionMcpReloadResult {}

/** @experimental */
export interface SessionMcpReloadParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SessionPluginsListResult {
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
export interface SessionPluginsListParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SessionExtensionsListResult {
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
export interface SessionExtensionsListParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SessionExtensionsEnableResult {}

/** @experimental */
export interface SessionExtensionsEnableParams {
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
export interface SessionExtensionsDisableResult {}

/** @experimental */
export interface SessionExtensionsDisableParams {
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
export interface SessionExtensionsReloadResult {}

/** @experimental */
export interface SessionExtensionsReloadParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** @experimental */
export interface SessionCompactionCompactResult {
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
}

/** @experimental */
export interface SessionCompactionCompactParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface SessionToolsHandlePendingToolCallResult {
  /**
   * Whether the tool call result was handled successfully
   */
  success: boolean;
}

export interface SessionToolsHandlePendingToolCallParams {
  /**
   * Target session identifier
   */
  sessionId: string;
  requestId: string;
  result?:
    | string
    | {
        textResultForLlm: string;
        resultType?: string;
        error?: string;
        toolTelemetry?: {
          [k: string]: unknown;
        };
      };
  error?: string;
}

export interface SessionCommandsHandlePendingCommandResult {
  success: boolean;
}

export interface SessionCommandsHandlePendingCommandParams {
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

export interface SessionUiElicitationResult {
  /**
   * The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
   */
  action: "accept" | "decline" | "cancel";
  /**
   * The form values submitted by the user (present when action is 'accept')
   */
  content?: {
    [k: string]: string | number | boolean | string[];
  };
}

export interface SessionUiElicitationParams {
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
            oneOf: {
              const: string;
              title: string;
            }[];
            default?: string;
          }
        | {
            type: "array";
            title?: string;
            description?: string;
            minItems?: number;
            maxItems?: number;
            items: {
              type: "string";
              enum: string[];
            };
            default?: string[];
          }
        | {
            type: "array";
            title?: string;
            description?: string;
            minItems?: number;
            maxItems?: number;
            items: {
              anyOf: {
                const: string;
                title: string;
              }[];
            };
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
    };
    /**
     * List of required field names
     */
    required?: string[];
  };
}

export interface SessionUiHandlePendingElicitationResult {
  /**
   * Whether the response was accepted. False if the request was already resolved by another client.
   */
  success: boolean;
}

export interface SessionUiHandlePendingElicitationParams {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * The unique request ID from the elicitation.requested event
   */
  requestId: string;
  /**
   * The elicitation response (accept with form values, decline, or cancel)
   */
  result: {
    /**
     * The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
     */
    action: "accept" | "decline" | "cancel";
    /**
     * The form values submitted by the user (present when action is 'accept')
     */
    content?: {
      [k: string]: string | number | boolean | string[];
    };
  };
}

export interface SessionPermissionsHandlePendingPermissionRequestResult {
  /**
   * Whether the permission request was handled successfully
   */
  success: boolean;
}

export interface SessionPermissionsHandlePendingPermissionRequestParams {
  /**
   * Target session identifier
   */
  sessionId: string;
  requestId: string;
  result:
    | {
        kind: "approved";
      }
    | {
        kind: "denied-by-rules";
        rules: unknown[];
      }
    | {
        kind: "denied-no-approval-rule-and-could-not-request-from-user";
      }
    | {
        kind: "denied-interactively-by-user";
        feedback?: string;
      }
    | {
        kind: "denied-by-content-exclusion-policy";
        path: string;
        message: string;
      };
}

export interface SessionLogResult {
  /**
   * The unique identifier of the emitted session event
   */
  eventId: string;
}

export interface SessionLogParams {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Human-readable message
   */
  message: string;
  /**
   * Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
   */
  level?: "info" | "warning" | "error";
  /**
   * When true, the message is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Optional URL the user can open in their browser for more details
   */
  url?: string;
}

export interface SessionShellExecResult {
  /**
   * Unique identifier for tracking streamed output
   */
  processId: string;
}

export interface SessionShellExecParams {
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

export interface SessionShellKillResult {
  /**
   * Whether the signal was sent successfully
   */
  killed: boolean;
}

export interface SessionShellKillParams {
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

export interface SessionFsReadFileResult {
  /**
   * File content as UTF-8 string
   */
  content: string;
}

export interface SessionFsReadFileParams {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}

export interface SessionFsWriteFileParams {
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

export interface SessionFsAppendFileParams {
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

export interface SessionFsExistsParams {
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

export interface SessionFsStatParams {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}

export interface SessionFsMkdirParams {
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

export interface SessionFsReaddirParams {
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

export interface SessionFsReaddirWithTypesParams {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}

export interface SessionFsRmParams {
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

export interface SessionFsRenameParams {
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
        ping: async (params: PingParams): Promise<PingResult> =>
            connection.sendRequest("ping", params),
        models: {
            list: async (): Promise<ModelsListResult> =>
                connection.sendRequest("models.list", {}),
        },
        tools: {
            list: async (params: ToolsListParams): Promise<ToolsListResult> =>
                connection.sendRequest("tools.list", params),
        },
        account: {
            getQuota: async (): Promise<AccountGetQuotaResult> =>
                connection.sendRequest("account.getQuota", {}),
        },
        mcp: {
            config: {
                list: async (): Promise<McpConfigListResult> =>
                    connection.sendRequest("mcp.config.list", {}),
                add: async (params: McpConfigAddParams): Promise<void> =>
                    connection.sendRequest("mcp.config.add", params),
                update: async (params: McpConfigUpdateParams): Promise<void> =>
                    connection.sendRequest("mcp.config.update", params),
                remove: async (params: McpConfigRemoveParams): Promise<void> =>
                    connection.sendRequest("mcp.config.remove", params),
            },
        },
        sessionFs: {
            setProvider: async (params: SessionFsSetProviderParams): Promise<SessionFsSetProviderResult> =>
                connection.sendRequest("sessionFs.setProvider", params),
        },
    };
}

/** Create typed session-scoped RPC methods. */
export function createSessionRpc(connection: MessageConnection, sessionId: string) {
    return {
        model: {
            getCurrent: async (): Promise<SessionModelGetCurrentResult> =>
                connection.sendRequest("session.model.getCurrent", { sessionId }),
            switchTo: async (params: Omit<SessionModelSwitchToParams, "sessionId">): Promise<SessionModelSwitchToResult> =>
                connection.sendRequest("session.model.switchTo", { sessionId, ...params }),
        },
        mode: {
            get: async (): Promise<SessionModeGetResult> =>
                connection.sendRequest("session.mode.get", { sessionId }),
            set: async (params: Omit<SessionModeSetParams, "sessionId">): Promise<SessionModeSetResult> =>
                connection.sendRequest("session.mode.set", { sessionId, ...params }),
        },
        plan: {
            read: async (): Promise<SessionPlanReadResult> =>
                connection.sendRequest("session.plan.read", { sessionId }),
            update: async (params: Omit<SessionPlanUpdateParams, "sessionId">): Promise<SessionPlanUpdateResult> =>
                connection.sendRequest("session.plan.update", { sessionId, ...params }),
            delete: async (): Promise<SessionPlanDeleteResult> =>
                connection.sendRequest("session.plan.delete", { sessionId }),
        },
        workspace: {
            listFiles: async (): Promise<SessionWorkspaceListFilesResult> =>
                connection.sendRequest("session.workspace.listFiles", { sessionId }),
            readFile: async (params: Omit<SessionWorkspaceReadFileParams, "sessionId">): Promise<SessionWorkspaceReadFileResult> =>
                connection.sendRequest("session.workspace.readFile", { sessionId, ...params }),
            createFile: async (params: Omit<SessionWorkspaceCreateFileParams, "sessionId">): Promise<SessionWorkspaceCreateFileResult> =>
                connection.sendRequest("session.workspace.createFile", { sessionId, ...params }),
        },
        /** @experimental */
        fleet: {
            start: async (params: Omit<SessionFleetStartParams, "sessionId">): Promise<SessionFleetStartResult> =>
                connection.sendRequest("session.fleet.start", { sessionId, ...params }),
        },
        /** @experimental */
        agent: {
            list: async (): Promise<SessionAgentListResult> =>
                connection.sendRequest("session.agent.list", { sessionId }),
            getCurrent: async (): Promise<SessionAgentGetCurrentResult> =>
                connection.sendRequest("session.agent.getCurrent", { sessionId }),
            select: async (params: Omit<SessionAgentSelectParams, "sessionId">): Promise<SessionAgentSelectResult> =>
                connection.sendRequest("session.agent.select", { sessionId, ...params }),
            deselect: async (): Promise<SessionAgentDeselectResult> =>
                connection.sendRequest("session.agent.deselect", { sessionId }),
            reload: async (): Promise<SessionAgentReloadResult> =>
                connection.sendRequest("session.agent.reload", { sessionId }),
        },
        /** @experimental */
        skills: {
            list: async (): Promise<SessionSkillsListResult> =>
                connection.sendRequest("session.skills.list", { sessionId }),
            enable: async (params: Omit<SessionSkillsEnableParams, "sessionId">): Promise<SessionSkillsEnableResult> =>
                connection.sendRequest("session.skills.enable", { sessionId, ...params }),
            disable: async (params: Omit<SessionSkillsDisableParams, "sessionId">): Promise<SessionSkillsDisableResult> =>
                connection.sendRequest("session.skills.disable", { sessionId, ...params }),
            reload: async (): Promise<SessionSkillsReloadResult> =>
                connection.sendRequest("session.skills.reload", { sessionId }),
        },
        /** @experimental */
        mcp: {
            list: async (): Promise<SessionMcpListResult> =>
                connection.sendRequest("session.mcp.list", { sessionId }),
            enable: async (params: Omit<SessionMcpEnableParams, "sessionId">): Promise<SessionMcpEnableResult> =>
                connection.sendRequest("session.mcp.enable", { sessionId, ...params }),
            disable: async (params: Omit<SessionMcpDisableParams, "sessionId">): Promise<SessionMcpDisableResult> =>
                connection.sendRequest("session.mcp.disable", { sessionId, ...params }),
            reload: async (): Promise<SessionMcpReloadResult> =>
                connection.sendRequest("session.mcp.reload", { sessionId }),
        },
        /** @experimental */
        plugins: {
            list: async (): Promise<SessionPluginsListResult> =>
                connection.sendRequest("session.plugins.list", { sessionId }),
        },
        /** @experimental */
        extensions: {
            list: async (): Promise<SessionExtensionsListResult> =>
                connection.sendRequest("session.extensions.list", { sessionId }),
            enable: async (params: Omit<SessionExtensionsEnableParams, "sessionId">): Promise<SessionExtensionsEnableResult> =>
                connection.sendRequest("session.extensions.enable", { sessionId, ...params }),
            disable: async (params: Omit<SessionExtensionsDisableParams, "sessionId">): Promise<SessionExtensionsDisableResult> =>
                connection.sendRequest("session.extensions.disable", { sessionId, ...params }),
            reload: async (): Promise<SessionExtensionsReloadResult> =>
                connection.sendRequest("session.extensions.reload", { sessionId }),
        },
        /** @experimental */
        compaction: {
            compact: async (): Promise<SessionCompactionCompactResult> =>
                connection.sendRequest("session.compaction.compact", { sessionId }),
        },
        tools: {
            handlePendingToolCall: async (params: Omit<SessionToolsHandlePendingToolCallParams, "sessionId">): Promise<SessionToolsHandlePendingToolCallResult> =>
                connection.sendRequest("session.tools.handlePendingToolCall", { sessionId, ...params }),
        },
        commands: {
            handlePendingCommand: async (params: Omit<SessionCommandsHandlePendingCommandParams, "sessionId">): Promise<SessionCommandsHandlePendingCommandResult> =>
                connection.sendRequest("session.commands.handlePendingCommand", { sessionId, ...params }),
        },
        ui: {
            elicitation: async (params: Omit<SessionUiElicitationParams, "sessionId">): Promise<SessionUiElicitationResult> =>
                connection.sendRequest("session.ui.elicitation", { sessionId, ...params }),
            handlePendingElicitation: async (params: Omit<SessionUiHandlePendingElicitationParams, "sessionId">): Promise<SessionUiHandlePendingElicitationResult> =>
                connection.sendRequest("session.ui.handlePendingElicitation", { sessionId, ...params }),
        },
        permissions: {
            handlePendingPermissionRequest: async (params: Omit<SessionPermissionsHandlePendingPermissionRequestParams, "sessionId">): Promise<SessionPermissionsHandlePendingPermissionRequestResult> =>
                connection.sendRequest("session.permissions.handlePendingPermissionRequest", { sessionId, ...params }),
        },
        log: async (params: Omit<SessionLogParams, "sessionId">): Promise<SessionLogResult> =>
            connection.sendRequest("session.log", { sessionId, ...params }),
        shell: {
            exec: async (params: Omit<SessionShellExecParams, "sessionId">): Promise<SessionShellExecResult> =>
                connection.sendRequest("session.shell.exec", { sessionId, ...params }),
            kill: async (params: Omit<SessionShellKillParams, "sessionId">): Promise<SessionShellKillResult> =>
                connection.sendRequest("session.shell.kill", { sessionId, ...params }),
        },
    };
}

/** Handler for `sessionFs` client session API methods. */
export interface SessionFsHandler {
    readFile(params: SessionFsReadFileParams): Promise<SessionFsReadFileResult>;
    writeFile(params: SessionFsWriteFileParams): Promise<void>;
    appendFile(params: SessionFsAppendFileParams): Promise<void>;
    exists(params: SessionFsExistsParams): Promise<SessionFsExistsResult>;
    stat(params: SessionFsStatParams): Promise<SessionFsStatResult>;
    mkdir(params: SessionFsMkdirParams): Promise<void>;
    readdir(params: SessionFsReaddirParams): Promise<SessionFsReaddirResult>;
    readdirWithTypes(params: SessionFsReaddirWithTypesParams): Promise<SessionFsReaddirWithTypesResult>;
    rm(params: SessionFsRmParams): Promise<void>;
    rename(params: SessionFsRenameParams): Promise<void>;
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
    connection.onRequest("sessionFs.readFile", async (params: SessionFsReadFileParams) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.readFile(params);
    });
    connection.onRequest("sessionFs.writeFile", async (params: SessionFsWriteFileParams) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.writeFile(params);
    });
    connection.onRequest("sessionFs.appendFile", async (params: SessionFsAppendFileParams) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.appendFile(params);
    });
    connection.onRequest("sessionFs.exists", async (params: SessionFsExistsParams) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.exists(params);
    });
    connection.onRequest("sessionFs.stat", async (params: SessionFsStatParams) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.stat(params);
    });
    connection.onRequest("sessionFs.mkdir", async (params: SessionFsMkdirParams) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.mkdir(params);
    });
    connection.onRequest("sessionFs.readdir", async (params: SessionFsReaddirParams) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.readdir(params);
    });
    connection.onRequest("sessionFs.readdirWithTypes", async (params: SessionFsReaddirWithTypesParams) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.readdirWithTypes(params);
    });
    connection.onRequest("sessionFs.rm", async (params: SessionFsRmParams) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.rm(params);
    });
    connection.onRequest("sessionFs.rename", async (params: SessionFsRenameParams) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.rename(params);
    });
}
