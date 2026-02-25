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
      supports: {
        vision: boolean;
        /**
         * Whether this model supports reasoning effort configuration
         */
        reasoningEffort: boolean;
      };
      limits: {
        max_prompt_tokens?: number;
        max_output_tokens?: number;
        max_context_window_tokens: number;
      };
    };
    /**
     * Policy state (if applicable)
     */
    policy?: {
      state: string;
      terms: string;
    };
    /**
     * Billing information
     */
    billing?: {
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

export interface SessionModelGetCurrentResult {
  modelId?: string;
}

export interface SessionModelGetCurrentParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

export interface SessionModelSwitchToResult {
  modelId?: string;
}

export interface SessionModelSwitchToParams {
  /**
   * Target session identifier
   */
  sessionId: string;
  modelId: string;
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
   * Whether plan.md exists in the workspace
   */
  exists: boolean;
  /**
   * The content of plan.md, or null if it does not exist
   */
  content: string | null;
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
   * The new content for plan.md
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

export interface SessionFleetStartResult {
  /**
   * Whether fleet mode was successfully activated
   */
  started: boolean;
}

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

export interface SessionAgentListParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

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

export interface SessionAgentGetCurrentParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

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

export interface SessionAgentDeselectResult {}

export interface SessionAgentDeselectParams {
  /**
   * Target session identifier
   */
  sessionId: string;
}

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

export interface SessionCompactionCompactParams {
  /**
   * Target session identifier
   */
  sessionId: string;
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
        fleet: {
            start: async (params: Omit<SessionFleetStartParams, "sessionId">): Promise<SessionFleetStartResult> =>
                connection.sendRequest("session.fleet.start", { sessionId, ...params }),
        },
        agent: {
            list: async (): Promise<SessionAgentListResult> =>
                connection.sendRequest("session.agent.list", { sessionId }),
            getCurrent: async (): Promise<SessionAgentGetCurrentResult> =>
                connection.sendRequest("session.agent.getCurrent", { sessionId }),
            select: async (params: Omit<SessionAgentSelectParams, "sessionId">): Promise<SessionAgentSelectResult> =>
                connection.sendRequest("session.agent.select", { sessionId, ...params }),
            deselect: async (): Promise<SessionAgentDeselectResult> =>
                connection.sendRequest("session.agent.deselect", { sessionId }),
        },
        compaction: {
            compact: async (): Promise<SessionCompactionCompactResult> =>
                connection.sendRequest("session.compaction.compact", { sessionId }),
        },
    };
}
