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

type SessionModeGetResult struct {
	// The current agent mode.
	Mode Mode `json:"mode"`
}

type SessionModeSetResult struct {
	// The agent mode after switching.
	Mode Mode `json:"mode"`
}

type SessionModeSetParams struct {
	// The mode to switch to. Valid values: "interactive", "plan", "autopilot".
	Mode Mode `json:"mode"`
}

type SessionPlanReadResult struct {
	// The content of plan.md, or null if it does not exist
	Content *string `json:"content"`
	// Whether plan.md exists in the workspace
	Exists bool `json:"exists"`
}

type SessionPlanUpdateResult struct {
}

type SessionPlanUpdateParams struct {
	// The new content for plan.md
	Content string `json:"content"`
}

type SessionPlanDeleteResult struct {
}

type SessionWorkspaceListFilesResult struct {
	// Relative file paths in the workspace files directory
	Files []string `json:"files"`
}

type SessionWorkspaceReadFileResult struct {
	// File content as a UTF-8 string
	Content string `json:"content"`
}

type SessionWorkspaceReadFileParams struct {
	// Relative path within the workspace files directory
	Path string `json:"path"`
}

type SessionWorkspaceCreateFileResult struct {
}

type SessionWorkspaceCreateFileParams struct {
	// File content to write as a UTF-8 string
	Content string `json:"content"`
	// Relative path within the workspace files directory
	Path string `json:"path"`
}

type SessionFleetStartResult struct {
	// Whether fleet mode was successfully activated
	Started bool `json:"started"`
}

type SessionFleetStartParams struct {
	// Optional user prompt to combine with fleet instructions
	Prompt *string `json:"prompt,omitempty"`
}

type SessionAgentListResult struct {
	// Available custom agents
	Agents []AgentElement `json:"agents"`
}

type AgentElement struct {
	// Description of the agent's purpose
	Description string `json:"description"`
	// Human-readable display name
	DisplayName string `json:"displayName"`
	// Unique identifier of the custom agent
	Name string `json:"name"`
}

type SessionAgentGetCurrentResult struct {
	// Currently selected custom agent, or null if using the default agent
	Agent *SessionAgentGetCurrentResultAgent `json:"agent"`
}

type SessionAgentGetCurrentResultAgent struct {
	// Description of the agent's purpose
	Description string `json:"description"`
	// Human-readable display name
	DisplayName string `json:"displayName"`
	// Unique identifier of the custom agent
	Name string `json:"name"`
}

type SessionAgentSelectResult struct {
	// The newly selected custom agent
	Agent SessionAgentSelectResultAgent `json:"agent"`
}

// The newly selected custom agent
type SessionAgentSelectResultAgent struct {
	// Description of the agent's purpose
	Description string `json:"description"`
	// Human-readable display name
	DisplayName string `json:"displayName"`
	// Unique identifier of the custom agent
	Name string `json:"name"`
}

type SessionAgentSelectParams struct {
	// Name of the custom agent to select
	Name string `json:"name"`
}

type SessionAgentDeselectResult struct {
}

type SessionCompactionCompactResult struct {
	// Number of messages removed during compaction
	MessagesRemoved float64 `json:"messagesRemoved"`
	// Whether compaction completed successfully
	Success bool `json:"success"`
	// Number of tokens freed by compaction
	TokensRemoved float64 `json:"tokensRemoved"`
}

// The current agent mode.
//
// The agent mode after switching.
//
// The mode to switch to. Valid values: "interactive", "plan", "autopilot".
type Mode string

const (
	Autopilot   Mode = "autopilot"
	Interactive Mode = "interactive"
	Plan        Mode = "plan"
)

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

type ModeRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *ModeRpcApi) Get(ctx context.Context) (*SessionModeGetResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.mode.get", req)
	if err != nil {
		return nil, err
	}
	var result SessionModeGetResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ModeRpcApi) Set(ctx context.Context, params *SessionModeSetParams) (*SessionModeSetResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["mode"] = params.Mode
	}
	raw, err := a.client.Request("session.mode.set", req)
	if err != nil {
		return nil, err
	}
	var result SessionModeSetResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type PlanRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *PlanRpcApi) Read(ctx context.Context) (*SessionPlanReadResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.plan.read", req)
	if err != nil {
		return nil, err
	}
	var result SessionPlanReadResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *PlanRpcApi) Update(ctx context.Context, params *SessionPlanUpdateParams) (*SessionPlanUpdateResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["content"] = params.Content
	}
	raw, err := a.client.Request("session.plan.update", req)
	if err != nil {
		return nil, err
	}
	var result SessionPlanUpdateResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *PlanRpcApi) Delete(ctx context.Context) (*SessionPlanDeleteResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.plan.delete", req)
	if err != nil {
		return nil, err
	}
	var result SessionPlanDeleteResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type WorkspaceRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *WorkspaceRpcApi) ListFiles(ctx context.Context) (*SessionWorkspaceListFilesResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.workspace.listFiles", req)
	if err != nil {
		return nil, err
	}
	var result SessionWorkspaceListFilesResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *WorkspaceRpcApi) ReadFile(ctx context.Context, params *SessionWorkspaceReadFileParams) (*SessionWorkspaceReadFileResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["path"] = params.Path
	}
	raw, err := a.client.Request("session.workspace.readFile", req)
	if err != nil {
		return nil, err
	}
	var result SessionWorkspaceReadFileResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *WorkspaceRpcApi) CreateFile(ctx context.Context, params *SessionWorkspaceCreateFileParams) (*SessionWorkspaceCreateFileResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["path"] = params.Path
		req["content"] = params.Content
	}
	raw, err := a.client.Request("session.workspace.createFile", req)
	if err != nil {
		return nil, err
	}
	var result SessionWorkspaceCreateFileResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type FleetRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *FleetRpcApi) Start(ctx context.Context, params *SessionFleetStartParams) (*SessionFleetStartResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		if params.Prompt != nil {
			req["prompt"] = *params.Prompt
		}
	}
	raw, err := a.client.Request("session.fleet.start", req)
	if err != nil {
		return nil, err
	}
	var result SessionFleetStartResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type AgentRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *AgentRpcApi) List(ctx context.Context) (*SessionAgentListResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.agent.list", req)
	if err != nil {
		return nil, err
	}
	var result SessionAgentListResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *AgentRpcApi) GetCurrent(ctx context.Context) (*SessionAgentGetCurrentResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.agent.getCurrent", req)
	if err != nil {
		return nil, err
	}
	var result SessionAgentGetCurrentResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *AgentRpcApi) Select(ctx context.Context, params *SessionAgentSelectParams) (*SessionAgentSelectResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["name"] = params.Name
	}
	raw, err := a.client.Request("session.agent.select", req)
	if err != nil {
		return nil, err
	}
	var result SessionAgentSelectResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *AgentRpcApi) Deselect(ctx context.Context) (*SessionAgentDeselectResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.agent.deselect", req)
	if err != nil {
		return nil, err
	}
	var result SessionAgentDeselectResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type CompactionRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *CompactionRpcApi) Compact(ctx context.Context) (*SessionCompactionCompactResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.compaction.compact", req)
	if err != nil {
		return nil, err
	}
	var result SessionCompactionCompactResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// SessionRpc provides typed session-scoped RPC methods.
type SessionRpc struct {
	client     *jsonrpc2.Client
	sessionID  string
	Model      *ModelRpcApi
	Mode       *ModeRpcApi
	Plan       *PlanRpcApi
	Workspace  *WorkspaceRpcApi
	Fleet      *FleetRpcApi
	Agent      *AgentRpcApi
	Compaction *CompactionRpcApi
}

func NewSessionRpc(client *jsonrpc2.Client, sessionID string) *SessionRpc {
	return &SessionRpc{client: client, sessionID: sessionID,
		Model:      &ModelRpcApi{client: client, sessionID: sessionID},
		Mode:       &ModeRpcApi{client: client, sessionID: sessionID},
		Plan:       &PlanRpcApi{client: client, sessionID: sessionID},
		Workspace:  &WorkspaceRpcApi{client: client, sessionID: sessionID},
		Fleet:      &FleetRpcApi{client: client, sessionID: sessionID},
		Agent:      &AgentRpcApi{client: client, sessionID: sessionID},
		Compaction: &CompactionRpcApi{client: client, sessionID: sessionID},
	}
}
