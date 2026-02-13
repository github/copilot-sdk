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
    };
}
