// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package rpc

import (
	"context"
	"encoding/json"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

type PingResult struct {
	// Echoed message (or default greeting)
	Message string `json:"message"`
	// Server protocol version number
	ProtocolVersion float64 `json:"protocolVersion"`
	// Server timestamp in milliseconds
	Timestamp float64 `json:"timestamp"`
}

type PingParams struct {
	// Optional message to echo back
	Message *string `json:"message,omitempty"`
}

type ModelsListResult struct {
	// List of available models with full metadata
	Models []Model `json:"models"`
}

type Model struct {
	// Billing information
	Billing *Billing `json:"billing,omitempty"`
	// Model capabilities and limits
	Capabilities Capabilities `json:"capabilities"`
	// Default reasoning effort level (only present if model supports reasoning effort)
	DefaultReasoningEffort *string `json:"defaultReasoningEffort,omitempty"`
	// Model identifier (e.g., "claude-sonnet-4.5")
	ID string `json:"id"`
	// Display name
	Name string `json:"name"`
	// Policy state (if applicable)
	Policy *Policy `json:"policy,omitempty"`
	// Supported reasoning effort levels (only present if model supports reasoning effort)
	SupportedReasoningEfforts []string `json:"supportedReasoningEfforts,omitempty"`
}

// Billing information
type Billing struct {
	Multiplier float64 `json:"multiplier"`
}

// Model capabilities and limits
type Capabilities struct {
	Limits   Limits   `json:"limits"`
	Supports Supports `json:"supports"`
}

type Limits struct {
	MaxContextWindowTokens float64  `json:"max_context_window_tokens"`
	MaxOutputTokens        *float64 `json:"max_output_tokens,omitempty"`
	MaxPromptTokens        *float64 `json:"max_prompt_tokens,omitempty"`
}

type Supports struct {
	// Whether this model supports reasoning effort configuration
	ReasoningEffort bool `json:"reasoningEffort"`
	Vision          bool `json:"vision"`
}

// Policy state (if applicable)
type Policy struct {
	State string `json:"state"`
	Terms string `json:"terms"`
}

type ToolsListResult struct {
	// List of available built-in tools with metadata
	Tools []Tool `json:"tools"`
}

type Tool struct {
	// Description of what the tool does
	Description string `json:"description"`
	// Optional instructions for how to use this tool effectively
	Instructions *string `json:"instructions,omitempty"`
	// Tool identifier (e.g., "bash", "grep", "str_replace_editor")
	Name string `json:"name"`
	// Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP
	// tools)
	NamespacedName *string `json:"namespacedName,omitempty"`
	// JSON Schema for the tool's input parameters
	Parameters map[string]interface{} `json:"parameters,omitempty"`
}

type ToolsListParams struct {
	// Optional model ID — when provided, the returned tool list reflects model-specific
	// overrides
	Model *string `json:"model,omitempty"`
}

type AccountGetQuotaResult struct {
	// Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)
	QuotaSnapshots map[string]QuotaSnapshot `json:"quotaSnapshots"`
}

type QuotaSnapshot struct {
	// Number of requests included in the entitlement
	EntitlementRequests float64 `json:"entitlementRequests"`
	// Number of overage requests made this period
	Overage float64 `json:"overage"`
	// Whether pay-per-request usage is allowed when quota is exhausted
	OverageAllowedWithExhaustedQuota bool `json:"overageAllowedWithExhaustedQuota"`
	// Percentage of entitlement remaining
	RemainingPercentage float64 `json:"remainingPercentage"`
	// Date when the quota resets (ISO 8601)
	ResetDate *string `json:"resetDate,omitempty"`
	// Number of requests used so far this period
	UsedRequests float64 `json:"usedRequests"`
}

type SessionModelGetCurrentResult struct {
	ModelID *string `json:"modelId,omitempty"`
}

type SessionModelSwitchToResult struct {
	ModelID *string `json:"modelId,omitempty"`
}

type SessionModelSwitchToParams struct {
	ModelID string `json:"modelId"`
}

type ModelsRpcApi struct{ client *jsonrpc2.Client }

func (a *ModelsRpcApi) List(ctx context.Context) (*ModelsListResult, error) {
	raw, err := a.client.Request("models.list", map[string]interface{}{})
	if err != nil {
		return nil, err
	}
	var result ModelsListResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ToolsRpcApi struct{ client *jsonrpc2.Client }

func (a *ToolsRpcApi) List(ctx context.Context, params *ToolsListParams) (*ToolsListResult, error) {
	raw, err := a.client.Request("tools.list", params)
	if err != nil {
		return nil, err
	}
	var result ToolsListResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type AccountRpcApi struct{ client *jsonrpc2.Client }

func (a *AccountRpcApi) GetQuota(ctx context.Context) (*AccountGetQuotaResult, error) {
	raw, err := a.client.Request("account.getQuota", map[string]interface{}{})
	if err != nil {
		return nil, err
	}
	var result AccountGetQuotaResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// ServerRpc provides typed server-scoped RPC methods.
type ServerRpc struct {
	client  *jsonrpc2.Client
	Models  *ModelsRpcApi
	Tools   *ToolsRpcApi
	Account *AccountRpcApi
}

func (a *ServerRpc) Ping(ctx context.Context, params *PingParams) (*PingResult, error) {
	raw, err := a.client.Request("ping", params)
	if err != nil {
		return nil, err
	}
	var result PingResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func NewServerRpc(client *jsonrpc2.Client) *ServerRpc {
	return &ServerRpc{client: client,
		Models:  &ModelsRpcApi{client: client},
		Tools:   &ToolsRpcApi{client: client},
		Account: &AccountRpcApi{client: client},
	}
}

type ModelRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *ModelRpcApi) GetCurrent(ctx context.Context) (*SessionModelGetCurrentResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.model.getCurrent", req)
	if err != nil {
		return nil, err
	}
	var result SessionModelGetCurrentResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ModelRpcApi) SwitchTo(ctx context.Context, params *SessionModelSwitchToParams) (*SessionModelSwitchToResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["modelId"] = params.ModelID
	}
	raw, err := a.client.Request("session.model.switchTo", req)
	if err != nil {
		return nil, err
	}
	var result SessionModelSwitchToResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// SessionRpc provides typed session-scoped RPC methods.
type SessionRpc struct {
	client    *jsonrpc2.Client
	sessionID string
	Model     *ModelRpcApi
}

func NewSessionRpc(client *jsonrpc2.Client, sessionID string) *SessionRpc {
	return &SessionRpc{client: client, sessionID: sessionID,
		Model: &ModelRpcApi{client: client, sessionID: sessionID},
	}
}
