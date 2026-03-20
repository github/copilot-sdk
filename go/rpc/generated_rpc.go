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
	// Billing cost multiplier relative to the base rate
	Multiplier float64 `json:"multiplier"`
}

// Model capabilities and limits
type Capabilities struct {
	// Token limits for prompts, outputs, and context window
	Limits Limits `json:"limits"`
	// Feature flags indicating what the model supports
	Supports Supports `json:"supports"`
}

// Token limits for prompts, outputs, and context window
type Limits struct {
	// Maximum total context window size in tokens
	MaxContextWindowTokens float64 `json:"max_context_window_tokens"`
	// Maximum number of output/completion tokens
	MaxOutputTokens *float64 `json:"max_output_tokens,omitempty"`
	// Maximum number of prompt/input tokens
	MaxPromptTokens *float64 `json:"max_prompt_tokens,omitempty"`
}

// Feature flags indicating what the model supports
type Supports struct {
	// Whether this model supports reasoning effort configuration
	ReasoningEffort *bool `json:"reasoningEffort,omitempty"`
	// Whether this model supports vision/image input
	Vision *bool `json:"vision,omitempty"`
}

// Policy state (if applicable)
type Policy struct {
	// Current policy state for this model
	State string `json:"state"`
	// Usage terms or conditions for this model
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
	// Currently active model identifier
	ModelID *string `json:"modelId,omitempty"`
}

type SessionModelSwitchToResult struct {
	// Currently active model identifier after the switch
	ModelID *string `json:"modelId,omitempty"`
}

type SessionModelSwitchToParams struct {
	// Model identifier to switch to
	ModelID string `json:"modelId"`
	// Reasoning effort level to use for the model
	ReasoningEffort *string `json:"reasoningEffort,omitempty"`
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
	// The content of the plan file, or null if it does not exist
	Content *string `json:"content"`
	// Whether the plan file exists in the workspace
	Exists bool `json:"exists"`
	// Absolute file path of the plan file, or null if workspace is not enabled
	Path *string `json:"path"`
}

type SessionPlanUpdateResult struct {
}

type SessionPlanUpdateParams struct {
	// The new content for the plan file
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

// Experimental: SessionFleetStartResult is part of an experimental API and may change or be removed.
type SessionFleetStartResult struct {
	// Whether fleet mode was successfully activated
	Started bool `json:"started"`
}

// Experimental: SessionFleetStartParams is part of an experimental API and may change or be removed.
type SessionFleetStartParams struct {
	// Optional user prompt to combine with fleet instructions
	Prompt *string `json:"prompt,omitempty"`
}

// Experimental: SessionAgentListResult is part of an experimental API and may change or be removed.
type SessionAgentListResult struct {
	// Available custom agents
	Agents []SessionAgentListResultAgent `json:"agents"`
}

type SessionAgentListResultAgent struct {
	// Description of the agent's purpose
	Description string `json:"description"`
	// Human-readable display name
	DisplayName string `json:"displayName"`
	// Unique identifier of the custom agent
	Name string `json:"name"`
}

// Experimental: SessionAgentGetCurrentResult is part of an experimental API and may change or be removed.
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

// Experimental: SessionAgentSelectResult is part of an experimental API and may change or be removed.
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

// Experimental: SessionAgentSelectParams is part of an experimental API and may change or be removed.
type SessionAgentSelectParams struct {
	// Name of the custom agent to select
	Name string `json:"name"`
}

// Experimental: SessionAgentDeselectResult is part of an experimental API and may change or be removed.
type SessionAgentDeselectResult struct {
}

// Experimental: SessionAgentReloadResult is part of an experimental API and may change or be removed.
type SessionAgentReloadResult struct {
	// Reloaded custom agents
	Agents []SessionAgentReloadResultAgent `json:"agents"`
}

type SessionAgentReloadResultAgent struct {
	// Description of the agent's purpose
	Description string `json:"description"`
	// Human-readable display name
	DisplayName string `json:"displayName"`
	// Unique identifier of the custom agent
	Name string `json:"name"`
}

// Experimental: SessionSkillsListResult is part of an experimental API and may change or be removed.
type SessionSkillsListResult struct {
	// Available skills
	Skills []Skill `json:"skills"`
}

type Skill struct {
	// Description of what the skill does
	Description string `json:"description"`
	// Whether the skill is currently enabled
	Enabled bool `json:"enabled"`
	// Unique identifier for the skill
	Name string `json:"name"`
	// Absolute path to the skill file
	Path *string `json:"path,omitempty"`
	// Source location type (e.g., project, personal, plugin)
	Source string `json:"source"`
	// Whether the skill can be invoked by the user as a slash command
	UserInvocable bool `json:"userInvocable"`
}

// Experimental: SessionSkillsEnableResult is part of an experimental API and may change or be removed.
type SessionSkillsEnableResult struct {
}

// Experimental: SessionSkillsEnableParams is part of an experimental API and may change or be removed.
type SessionSkillsEnableParams struct {
	// Name of the skill to enable
	Name string `json:"name"`
}

// Experimental: SessionSkillsDisableResult is part of an experimental API and may change or be removed.
type SessionSkillsDisableResult struct {
}

// Experimental: SessionSkillsDisableParams is part of an experimental API and may change or be removed.
type SessionSkillsDisableParams struct {
	// Name of the skill to disable
	Name string `json:"name"`
}

// Experimental: SessionSkillsReloadResult is part of an experimental API and may change or be removed.
type SessionSkillsReloadResult struct {
}

type SessionMCPListResult struct {
	// Configured MCP servers
	Servers []Server `json:"servers"`
}

type Server struct {
	// Error message if the server failed to connect
	Error *string `json:"error,omitempty"`
	// Server name (config key)
	Name string `json:"name"`
	// Configuration source: user, workspace, plugin, or builtin
	Source *string `json:"source,omitempty"`
	// Connection status: connected, failed, pending, disabled, or not_configured
	Status ServerStatus `json:"status"`
}

type SessionMCPEnableResult struct {
}

type SessionMCPEnableParams struct {
	// Name of the MCP server to enable
	ServerName string `json:"serverName"`
}

type SessionMCPDisableResult struct {
}

type SessionMCPDisableParams struct {
	// Name of the MCP server to disable
	ServerName string `json:"serverName"`
}

type SessionMCPReloadResult struct {
}

// Experimental: SessionPluginsListResult is part of an experimental API and may change or be removed.
type SessionPluginsListResult struct {
	// Installed plugins
	Plugins []Plugin `json:"plugins"`
}

type Plugin struct {
	// Whether the plugin is currently enabled
	Enabled bool `json:"enabled"`
	// Marketplace the plugin came from
	Marketplace string `json:"marketplace"`
	// Plugin name
	Name string `json:"name"`
	// Installed version
	Version *string `json:"version,omitempty"`
}

// Experimental: SessionExtensionsListResult is part of an experimental API and may change or be removed.
type SessionExtensionsListResult struct {
	// Discovered extensions and their current status
	Extensions []Extension `json:"extensions"`
}

type Extension struct {
	// Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper')
	ID string `json:"id"`
	// Extension name (directory name)
	Name string `json:"name"`
	// Process ID if the extension is running
	PID *int64 `json:"pid,omitempty"`
	// Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
	Source Source `json:"source"`
	// Current status: running, disabled, failed, or starting
	Status ExtensionStatus `json:"status"`
}

// Experimental: SessionExtensionsEnableResult is part of an experimental API and may change or be removed.
type SessionExtensionsEnableResult struct {
}

// Experimental: SessionExtensionsEnableParams is part of an experimental API and may change or be removed.
type SessionExtensionsEnableParams struct {
	// Source-qualified extension ID to enable
	ID string `json:"id"`
}

// Experimental: SessionExtensionsDisableResult is part of an experimental API and may change or be removed.
type SessionExtensionsDisableResult struct {
}

// Experimental: SessionExtensionsDisableParams is part of an experimental API and may change or be removed.
type SessionExtensionsDisableParams struct {
	// Source-qualified extension ID to disable
	ID string `json:"id"`
}

// Experimental: SessionExtensionsReloadResult is part of an experimental API and may change or be removed.
type SessionExtensionsReloadResult struct {
}

// Experimental: SessionCompactionCompactResult is part of an experimental API and may change or be removed.
type SessionCompactionCompactResult struct {
	// Number of messages removed during compaction
	MessagesRemoved float64 `json:"messagesRemoved"`
	// Whether compaction completed successfully
	Success bool `json:"success"`
	// Number of tokens freed by compaction
	TokensRemoved float64 `json:"tokensRemoved"`
}

type SessionToolsHandlePendingToolCallResult struct {
	// Whether the tool call result was handled successfully
	Success bool `json:"success"`
}

type SessionToolsHandlePendingToolCallParams struct {
	Error     *string      `json:"error,omitempty"`
	RequestID string       `json:"requestId"`
	Result    *ResultUnion `json:"result"`
}

type ResultResult struct {
	Error            *string                `json:"error,omitempty"`
	ResultType       *string                `json:"resultType,omitempty"`
	TextResultForLlm string                 `json:"textResultForLlm"`
	ToolTelemetry    map[string]interface{} `json:"toolTelemetry,omitempty"`
}

type SessionCommandsHandlePendingCommandResult struct {
	Success bool `json:"success"`
}

type SessionCommandsHandlePendingCommandParams struct {
	// Error message if the command handler failed
	Error *string `json:"error,omitempty"`
	// Request ID from the command invocation event
	RequestID string `json:"requestId"`
}

type SessionUIElicitationResult struct {
	// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
	Action Action `json:"action"`
	// The form values submitted by the user (present when action is 'accept')
	Content map[string]*Content `json:"content,omitempty"`
}

type SessionUIElicitationParams struct {
	// Message describing what information is needed from the user
	Message string `json:"message"`
	// JSON Schema describing the form fields to present to the user
	RequestedSchema RequestedSchema `json:"requestedSchema"`
}

// JSON Schema describing the form fields to present to the user
type RequestedSchema struct {
	// Form field definitions, keyed by field name
	Properties map[string]Property `json:"properties"`
	// List of required field names
	Required []string `json:"required,omitempty"`
	// Schema type indicator (always 'object')
	Type RequestedSchemaType `json:"type"`
}

type Property struct {
	Default     *Content     `json:"default"`
	Description *string      `json:"description,omitempty"`
	Enum        []string     `json:"enum,omitempty"`
	EnumNames   []string     `json:"enumNames,omitempty"`
	Title       *string      `json:"title,omitempty"`
	Type        PropertyType `json:"type"`
	OneOf       []OneOf      `json:"oneOf,omitempty"`
	Items       *Items       `json:"items,omitempty"`
	MaxItems    *float64     `json:"maxItems,omitempty"`
	MinItems    *float64     `json:"minItems,omitempty"`
	Format      *Format      `json:"format,omitempty"`
	MaxLength   *float64     `json:"maxLength,omitempty"`
	MinLength   *float64     `json:"minLength,omitempty"`
	Maximum     *float64     `json:"maximum,omitempty"`
	Minimum     *float64     `json:"minimum,omitempty"`
}

type Items struct {
	Enum  []string   `json:"enum,omitempty"`
	Type  *ItemsType `json:"type,omitempty"`
	AnyOf []AnyOf    `json:"anyOf,omitempty"`
}

type AnyOf struct {
	Const string `json:"const"`
	Title string `json:"title"`
}

type OneOf struct {
	Const string `json:"const"`
	Title string `json:"title"`
}

type SessionPermissionsHandlePendingPermissionRequestResult struct {
	// Whether the permission request was handled successfully
	Success bool `json:"success"`
}

type SessionPermissionsHandlePendingPermissionRequestParams struct {
	RequestID string                                                       `json:"requestId"`
	Result    SessionPermissionsHandlePendingPermissionRequestParamsResult `json:"result"`
}

type SessionPermissionsHandlePendingPermissionRequestParamsResult struct {
	Kind     Kind          `json:"kind"`
	Rules    []interface{} `json:"rules,omitempty"`
	Feedback *string       `json:"feedback,omitempty"`
	Message  *string       `json:"message,omitempty"`
	Path     *string       `json:"path,omitempty"`
}

type SessionLogResult struct {
	// The unique identifier of the emitted session event
	EventID string `json:"eventId"`
}

type SessionLogParams struct {
	// When true, the message is transient and not persisted to the session event log on disk
	Ephemeral *bool `json:"ephemeral,omitempty"`
	// Log severity level. Determines how the message is displayed in the timeline. Defaults to
	// "info".
	Level *Level `json:"level,omitempty"`
	// Human-readable message
	Message string `json:"message"`
	// Optional URL the user can open in their browser for more details
	URL *string `json:"url,omitempty"`
}

type SessionShellExecResult struct {
	// Unique identifier for tracking streamed output
	ProcessID string `json:"processId"`
}

type SessionShellExecParams struct {
	// Shell command to execute
	Command string `json:"command"`
	// Working directory (defaults to session working directory)
	Cwd *string `json:"cwd,omitempty"`
	// Timeout in milliseconds (default: 30000)
	Timeout *float64 `json:"timeout,omitempty"`
}

type SessionShellKillResult struct {
	// Whether the signal was sent successfully
	Killed bool `json:"killed"`
}

type SessionShellKillParams struct {
	// Process identifier returned by shell.exec
	ProcessID string `json:"processId"`
	// Signal to send (default: SIGTERM)
	Signal *Signal `json:"signal,omitempty"`
}

// The current agent mode.
//
// The agent mode after switching.
//
// The mode to switch to. Valid values: "interactive", "plan", "autopilot".
type Mode string

const (
	ModeAutopilot   Mode = "autopilot"
	ModeInteractive Mode = "interactive"
	ModePlan        Mode = "plan"
)

// Connection status: connected, failed, pending, disabled, or not_configured
type ServerStatus string

const (
	ServerStatusConnected     ServerStatus = "connected"
	ServerStatusNotConfigured ServerStatus = "not_configured"
	ServerStatusPending       ServerStatus = "pending"
	ServerStatusDisabled      ServerStatus = "disabled"
	ServerStatusFailed        ServerStatus = "failed"
)

// Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
type Source string

const (
	SourceProject Source = "project"
	SourceUser    Source = "user"
)

// Current status: running, disabled, failed, or starting
type ExtensionStatus string

const (
	ExtensionStatusDisabled ExtensionStatus = "disabled"
	ExtensionStatusFailed   ExtensionStatus = "failed"
	ExtensionStatusRunning  ExtensionStatus = "running"
	ExtensionStatusStarting ExtensionStatus = "starting"
)

// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
type Action string

const (
	ActionAccept  Action = "accept"
	ActionCancel  Action = "cancel"
	ActionDecline Action = "decline"
)

type Format string

const (
	FormatDate     Format = "date"
	FormatDateTime Format = "date-time"
	FormatEmail    Format = "email"
	FormatUri      Format = "uri"
)

type ItemsType string

const (
	ItemsTypeString ItemsType = "string"
)

type PropertyType string

const (
	PropertyTypeArray   PropertyType = "array"
	PropertyTypeBoolean PropertyType = "boolean"
	PropertyTypeString  PropertyType = "string"
	PropertyTypeInteger PropertyType = "integer"
	PropertyTypeNumber  PropertyType = "number"
)

type RequestedSchemaType string

const (
	RequestedSchemaTypeObject RequestedSchemaType = "object"
)

type Kind string

const (
	KindApproved                                       Kind = "approved"
	KindDeniedByContentExclusionPolicy                 Kind = "denied-by-content-exclusion-policy"
	KindDeniedByRules                                  Kind = "denied-by-rules"
	KindDeniedInteractivelyByUser                      Kind = "denied-interactively-by-user"
	KindDeniedNoApprovalRuleAndCouldNotRequestFromUser Kind = "denied-no-approval-rule-and-could-not-request-from-user"
)

// Log severity level. Determines how the message is displayed in the timeline. Defaults to
// "info".
type Level string

const (
	LevelError   Level = "error"
	LevelInfo    Level = "info"
	LevelWarning Level = "warning"
)

// Signal to send (default: SIGTERM)
type Signal string

const (
	SignalSIGINT  Signal = "SIGINT"
	SignalSIGKILL Signal = "SIGKILL"
	SignalSIGTERM Signal = "SIGTERM"
)

type ResultUnion struct {
	ResultResult *ResultResult
	String       *string
}

type Content struct {
	Bool        *bool
	Double      *float64
	String      *string
	StringArray []string
}

type ServerModelsRpcApi struct {
	client *jsonrpc2.Client
}

func (a *ServerModelsRpcApi) List(ctx context.Context) (*ModelsListResult, error) {
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

type ServerToolsRpcApi struct {
	client *jsonrpc2.Client
}

func (a *ServerToolsRpcApi) List(ctx context.Context, params *ToolsListParams) (*ToolsListResult, error) {
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

type ServerAccountRpcApi struct {
	client *jsonrpc2.Client
}

func (a *ServerAccountRpcApi) GetQuota(ctx context.Context) (*AccountGetQuotaResult, error) {
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
	Models  *ServerModelsRpcApi
	Tools   *ServerToolsRpcApi
	Account *ServerAccountRpcApi
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
		Models:  &ServerModelsRpcApi{client: client},
		Tools:   &ServerToolsRpcApi{client: client},
		Account: &ServerAccountRpcApi{client: client},
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
		if params.ReasoningEffort != nil {
			req["reasoningEffort"] = *params.ReasoningEffort
		}
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

// Experimental: FleetRpcApi contains experimental APIs that may change or be removed.
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

// Experimental: AgentRpcApi contains experimental APIs that may change or be removed.
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

func (a *AgentRpcApi) Reload(ctx context.Context) (*SessionAgentReloadResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.agent.reload", req)
	if err != nil {
		return nil, err
	}
	var result SessionAgentReloadResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: SkillsRpcApi contains experimental APIs that may change or be removed.
type SkillsRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *SkillsRpcApi) List(ctx context.Context) (*SessionSkillsListResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.skills.list", req)
	if err != nil {
		return nil, err
	}
	var result SessionSkillsListResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *SkillsRpcApi) Enable(ctx context.Context, params *SessionSkillsEnableParams) (*SessionSkillsEnableResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["name"] = params.Name
	}
	raw, err := a.client.Request("session.skills.enable", req)
	if err != nil {
		return nil, err
	}
	var result SessionSkillsEnableResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *SkillsRpcApi) Disable(ctx context.Context, params *SessionSkillsDisableParams) (*SessionSkillsDisableResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["name"] = params.Name
	}
	raw, err := a.client.Request("session.skills.disable", req)
	if err != nil {
		return nil, err
	}
	var result SessionSkillsDisableResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *SkillsRpcApi) Reload(ctx context.Context) (*SessionSkillsReloadResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.skills.reload", req)
	if err != nil {
		return nil, err
	}
	var result SessionSkillsReloadResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: McpRpcApi contains experimental APIs that may change or be removed.
type McpRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *McpRpcApi) List(ctx context.Context) (*SessionMCPListResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.mcp.list", req)
	if err != nil {
		return nil, err
	}
	var result SessionMCPListResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *McpRpcApi) Enable(ctx context.Context, params *SessionMCPEnableParams) (*SessionMCPEnableResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["serverName"] = params.ServerName
	}
	raw, err := a.client.Request("session.mcp.enable", req)
	if err != nil {
		return nil, err
	}
	var result SessionMCPEnableResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *McpRpcApi) Disable(ctx context.Context, params *SessionMCPDisableParams) (*SessionMCPDisableResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["serverName"] = params.ServerName
	}
	raw, err := a.client.Request("session.mcp.disable", req)
	if err != nil {
		return nil, err
	}
	var result SessionMCPDisableResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *McpRpcApi) Reload(ctx context.Context) (*SessionMCPReloadResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.mcp.reload", req)
	if err != nil {
		return nil, err
	}
	var result SessionMCPReloadResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: PluginsRpcApi contains experimental APIs that may change or be removed.
type PluginsRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *PluginsRpcApi) List(ctx context.Context) (*SessionPluginsListResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.plugins.list", req)
	if err != nil {
		return nil, err
	}
	var result SessionPluginsListResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: ExtensionsRpcApi contains experimental APIs that may change or be removed.
type ExtensionsRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *ExtensionsRpcApi) List(ctx context.Context) (*SessionExtensionsListResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.extensions.list", req)
	if err != nil {
		return nil, err
	}
	var result SessionExtensionsListResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ExtensionsRpcApi) Enable(ctx context.Context, params *SessionExtensionsEnableParams) (*SessionExtensionsEnableResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["id"] = params.ID
	}
	raw, err := a.client.Request("session.extensions.enable", req)
	if err != nil {
		return nil, err
	}
	var result SessionExtensionsEnableResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ExtensionsRpcApi) Disable(ctx context.Context, params *SessionExtensionsDisableParams) (*SessionExtensionsDisableResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["id"] = params.ID
	}
	raw, err := a.client.Request("session.extensions.disable", req)
	if err != nil {
		return nil, err
	}
	var result SessionExtensionsDisableResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ExtensionsRpcApi) Reload(ctx context.Context) (*SessionExtensionsReloadResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.extensions.reload", req)
	if err != nil {
		return nil, err
	}
	var result SessionExtensionsReloadResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: CompactionRpcApi contains experimental APIs that may change or be removed.
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

type ToolsRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *ToolsRpcApi) HandlePendingToolCall(ctx context.Context, params *SessionToolsHandlePendingToolCallParams) (*SessionToolsHandlePendingToolCallResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["requestId"] = params.RequestID
		if params.Result != nil {
			req["result"] = *params.Result
		}
		if params.Error != nil {
			req["error"] = *params.Error
		}
	}
	raw, err := a.client.Request("session.tools.handlePendingToolCall", req)
	if err != nil {
		return nil, err
	}
	var result SessionToolsHandlePendingToolCallResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type CommandsRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *CommandsRpcApi) HandlePendingCommand(ctx context.Context, params *SessionCommandsHandlePendingCommandParams) (*SessionCommandsHandlePendingCommandResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["requestId"] = params.RequestID
		if params.Error != nil {
			req["error"] = *params.Error
		}
	}
	raw, err := a.client.Request("session.commands.handlePendingCommand", req)
	if err != nil {
		return nil, err
	}
	var result SessionCommandsHandlePendingCommandResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type UiRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *UiRpcApi) Elicitation(ctx context.Context, params *SessionUIElicitationParams) (*SessionUIElicitationResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["message"] = params.Message
		req["requestedSchema"] = params.RequestedSchema
	}
	raw, err := a.client.Request("session.ui.elicitation", req)
	if err != nil {
		return nil, err
	}
	var result SessionUIElicitationResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type PermissionsRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

func (a *PermissionsRpcApi) HandlePendingPermissionRequest(ctx context.Context, params *SessionPermissionsHandlePendingPermissionRequestParams) (*SessionPermissionsHandlePendingPermissionRequestResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["requestId"] = params.RequestID
		req["result"] = params.Result
	}
	raw, err := a.client.Request("session.permissions.handlePendingPermissionRequest", req)
	if err != nil {
		return nil, err
	}
	var result SessionPermissionsHandlePendingPermissionRequestResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ShellRpcApi struct {
	client    *jsonrpc2.Client
	sessionID string
	onExec    func(string)
}

func (a *ShellRpcApi) Exec(ctx context.Context, params *SessionShellExecParams) (*SessionShellExecResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["command"] = params.Command
		if params.Cwd != nil {
			req["cwd"] = *params.Cwd
		}
		if params.Timeout != nil {
			req["timeout"] = *params.Timeout
		}
	}
	raw, err := a.client.Request("session.shell.exec", req)
	if err != nil {
		return nil, err
	}
	var result SessionShellExecResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	if a.onExec != nil {
		a.onExec(result.ProcessID)
	}
	return &result, nil
}

func (a *ShellRpcApi) Kill(ctx context.Context, params *SessionShellKillParams) (*SessionShellKillResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["processId"] = params.ProcessID
		if params.Signal != nil {
			req["signal"] = *params.Signal
		}
	}
	raw, err := a.client.Request("session.shell.kill", req)
	if err != nil {
		return nil, err
	}
	var result SessionShellKillResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// SessionRpc provides typed session-scoped RPC methods.
type SessionRpc struct {
	client      *jsonrpc2.Client
	sessionID   string
	Model       *ModelRpcApi
	Mode        *ModeRpcApi
	Plan        *PlanRpcApi
	Workspace   *WorkspaceRpcApi
	Fleet       *FleetRpcApi
	Agent       *AgentRpcApi
	Skills      *SkillsRpcApi
	Mcp         *McpRpcApi
	Plugins     *PluginsRpcApi
	Extensions  *ExtensionsRpcApi
	Compaction  *CompactionRpcApi
	Tools       *ToolsRpcApi
	Commands    *CommandsRpcApi
	Ui          *UiRpcApi
	Permissions *PermissionsRpcApi
	Shell       *ShellRpcApi
}

func (a *SessionRpc) Log(ctx context.Context, params *SessionLogParams) (*SessionLogResult, error) {
	req := map[string]interface{}{"sessionId": a.sessionID}
	if params != nil {
		req["message"] = params.Message
		if params.Level != nil {
			req["level"] = *params.Level
		}
		if params.Ephemeral != nil {
			req["ephemeral"] = *params.Ephemeral
		}
		if params.URL != nil {
			req["url"] = *params.URL
		}
	}
	raw, err := a.client.Request("session.log", req)
	if err != nil {
		return nil, err
	}
	var result SessionLogResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func NewSessionRpc(client *jsonrpc2.Client, sessionID string, onShellExec ...func(string)) *SessionRpc {
	var shellExecHandler func(string)
	if len(onShellExec) > 0 {
		shellExecHandler = onShellExec[0]
	}
	return &SessionRpc{client: client, sessionID: sessionID,
		Model:       &ModelRpcApi{client: client, sessionID: sessionID},
		Mode:        &ModeRpcApi{client: client, sessionID: sessionID},
		Plan:        &PlanRpcApi{client: client, sessionID: sessionID},
		Workspace:   &WorkspaceRpcApi{client: client, sessionID: sessionID},
		Fleet:       &FleetRpcApi{client: client, sessionID: sessionID},
		Agent:       &AgentRpcApi{client: client, sessionID: sessionID},
		Skills:      &SkillsRpcApi{client: client, sessionID: sessionID},
		Mcp:         &McpRpcApi{client: client, sessionID: sessionID},
		Plugins:     &PluginsRpcApi{client: client, sessionID: sessionID},
		Extensions:  &ExtensionsRpcApi{client: client, sessionID: sessionID},
		Compaction:  &CompactionRpcApi{client: client, sessionID: sessionID},
		Tools:       &ToolsRpcApi{client: client, sessionID: sessionID},
		Commands:    &CommandsRpcApi{client: client, sessionID: sessionID},
		Ui:          &UiRpcApi{client: client, sessionID: sessionID},
		Permissions: &PermissionsRpcApi{client: client, sessionID: sessionID},
		Shell:       &ShellRpcApi{client: client, sessionID: sessionID, onExec: shellExecHandler},
	}
}
