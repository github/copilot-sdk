// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package rpc

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
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
	Capabilities ModelCapabilities `json:"capabilities"`
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
type ModelCapabilities struct {
	// Token limits for prompts, outputs, and context window
	Limits ModelCapabilitiesLimits `json:"limits"`
	// Feature flags indicating what the model supports
	Supports ModelCapabilitiesSupports `json:"supports"`
}

// Token limits for prompts, outputs, and context window
type ModelCapabilitiesLimits struct {
	// Maximum total context window size in tokens
	MaxContextWindowTokens float64 `json:"max_context_window_tokens"`
	// Maximum number of output/completion tokens
	MaxOutputTokens *float64 `json:"max_output_tokens,omitempty"`
	// Maximum number of prompt/input tokens
	MaxPromptTokens *float64 `json:"max_prompt_tokens,omitempty"`
	// Vision-specific limits
	Vision *ModelCapabilitiesLimitsVision `json:"vision,omitempty"`
}

// Vision-specific limits
type ModelCapabilitiesLimitsVision struct {
	// Maximum image size in bytes
	MaxPromptImageSize float64 `json:"max_prompt_image_size"`
	// Maximum number of images per prompt
	MaxPromptImages float64 `json:"max_prompt_images"`
	// MIME types the model accepts
	SupportedMediaTypes []string `json:"supported_media_types"`
}

// Feature flags indicating what the model supports
type ModelCapabilitiesSupports struct {
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
	Parameters map[string]any `json:"parameters,omitempty"`
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

type MCPConfigListResult struct {
	// All MCP servers from user config, keyed by name
	Servers map[string]ServerValue `json:"servers"`
}

// MCP server configuration (local/stdio or remote/http)
type ServerValue struct {
	Args            []string            `json:"args,omitempty"`
	Command         *string             `json:"command,omitempty"`
	Cwd             *string             `json:"cwd,omitempty"`
	Env             map[string]string   `json:"env,omitempty"`
	FilterMapping   *FilterMappingUnion `json:"filterMapping"`
	IsDefaultServer *bool               `json:"isDefaultServer,omitempty"`
	Timeout         *float64            `json:"timeout,omitempty"`
	// Tools to include. Defaults to all tools if not specified.
	Tools             []string          `json:"tools,omitempty"`
	Type              *ServerType       `json:"type,omitempty"`
	Headers           map[string]string `json:"headers,omitempty"`
	OauthClientID     *string           `json:"oauthClientId,omitempty"`
	OauthPublicClient *bool             `json:"oauthPublicClient,omitempty"`
	URL               *string           `json:"url,omitempty"`
}

type MCPConfigAddParams struct {
	// MCP server configuration (local/stdio or remote/http)
	Config MCPConfigAddParamsConfig `json:"config"`
	// Unique name for the MCP server
	Name string `json:"name"`
}

// MCP server configuration (local/stdio or remote/http)
type MCPConfigAddParamsConfig struct {
	Args            []string            `json:"args,omitempty"`
	Command         *string             `json:"command,omitempty"`
	Cwd             *string             `json:"cwd,omitempty"`
	Env             map[string]string   `json:"env,omitempty"`
	FilterMapping   *FilterMappingUnion `json:"filterMapping"`
	IsDefaultServer *bool               `json:"isDefaultServer,omitempty"`
	Timeout         *float64            `json:"timeout,omitempty"`
	// Tools to include. Defaults to all tools if not specified.
	Tools             []string          `json:"tools,omitempty"`
	Type              *ServerType       `json:"type,omitempty"`
	Headers           map[string]string `json:"headers,omitempty"`
	OauthClientID     *string           `json:"oauthClientId,omitempty"`
	OauthPublicClient *bool             `json:"oauthPublicClient,omitempty"`
	URL               *string           `json:"url,omitempty"`
}

type MCPConfigUpdateParams struct {
	// MCP server configuration (local/stdio or remote/http)
	Config MCPConfigUpdateParamsConfig `json:"config"`
	// Name of the MCP server to update
	Name string `json:"name"`
}

// MCP server configuration (local/stdio or remote/http)
type MCPConfigUpdateParamsConfig struct {
	Args            []string            `json:"args,omitempty"`
	Command         *string             `json:"command,omitempty"`
	Cwd             *string             `json:"cwd,omitempty"`
	Env             map[string]string   `json:"env,omitempty"`
	FilterMapping   *FilterMappingUnion `json:"filterMapping"`
	IsDefaultServer *bool               `json:"isDefaultServer,omitempty"`
	Timeout         *float64            `json:"timeout,omitempty"`
	// Tools to include. Defaults to all tools if not specified.
	Tools             []string          `json:"tools,omitempty"`
	Type              *ServerType       `json:"type,omitempty"`
	Headers           map[string]string `json:"headers,omitempty"`
	OauthClientID     *string           `json:"oauthClientId,omitempty"`
	OauthPublicClient *bool             `json:"oauthPublicClient,omitempty"`
	URL               *string           `json:"url,omitempty"`
}

type MCPConfigRemoveParams struct {
	// Name of the MCP server to remove
	Name string `json:"name"`
}

type MCPDiscoverResult struct {
	// MCP servers discovered from all sources
	Servers []DiscoveredMCPServer `json:"servers"`
}

type DiscoveredMCPServer struct {
	// Whether the server is enabled (not in the disabled list)
	Enabled bool `json:"enabled"`
	// Server name (config key)
	Name string `json:"name"`
	// Configuration source
	Source ServerSource `json:"source"`
	// Server type: local, stdio, http, or sse
	Type *string `json:"type,omitempty"`
}

type MCPDiscoverParams struct {
	// Working directory used as context for discovery (e.g., plugin resolution)
	WorkingDirectory *string `json:"workingDirectory,omitempty"`
}

type SessionFSSetProviderResult struct {
	// Whether the provider was set successfully
	Success bool `json:"success"`
}

type SessionFSSetProviderParams struct {
	// Path conventions used by this filesystem
	Conventions Conventions `json:"conventions"`
	// Initial working directory for sessions
	InitialCwd string `json:"initialCwd"`
	// Path within each session's SessionFs where the runtime stores files for that session
	SessionStatePath string `json:"sessionStatePath"`
}

// Experimental: SessionsForkResult is part of an experimental API and may change or be removed.
type SessionsForkResult struct {
	// The new forked session's ID
	SessionID string `json:"sessionId"`
}

// Experimental: SessionsForkParams is part of an experimental API and may change or be removed.
type SessionsForkParams struct {
	// Source session ID to fork from
	SessionID string `json:"sessionId"`
	// Optional event ID boundary. When provided, the fork includes only events before this ID
	// (exclusive). When omitted, all events are included.
	ToEventID *string `json:"toEventId,omitempty"`
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
	// Override individual model capabilities resolved by the runtime
	ModelCapabilities *ModelCapabilitiesOverride `json:"modelCapabilities,omitempty"`
	// Model identifier to switch to
	ModelID string `json:"modelId"`
	// Reasoning effort level to use for the model
	ReasoningEffort *string `json:"reasoningEffort,omitempty"`
}

// Override individual model capabilities resolved by the runtime
type ModelCapabilitiesOverride struct {
	// Token limits for prompts, outputs, and context window
	Limits *ModelCapabilitiesOverrideLimits `json:"limits,omitempty"`
	// Feature flags indicating what the model supports
	Supports *ModelCapabilitiesOverrideSupports `json:"supports,omitempty"`
}

// Token limits for prompts, outputs, and context window
type ModelCapabilitiesOverrideLimits struct {
	// Maximum total context window size in tokens
	MaxContextWindowTokens *float64                               `json:"max_context_window_tokens,omitempty"`
	MaxOutputTokens        *float64                               `json:"max_output_tokens,omitempty"`
	MaxPromptTokens        *float64                               `json:"max_prompt_tokens,omitempty"`
	Vision                 *ModelCapabilitiesOverrideLimitsVision `json:"vision,omitempty"`
}

type ModelCapabilitiesOverrideLimitsVision struct {
	// Maximum image size in bytes
	MaxPromptImageSize *float64 `json:"max_prompt_image_size,omitempty"`
	// Maximum number of images per prompt
	MaxPromptImages *float64 `json:"max_prompt_images,omitempty"`
	// MIME types the model accepts
	SupportedMediaTypes []string `json:"supported_media_types,omitempty"`
}

// Feature flags indicating what the model supports
type ModelCapabilitiesOverrideSupports struct {
	ReasoningEffort *bool `json:"reasoningEffort,omitempty"`
	Vision          *bool `json:"vision,omitempty"`
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
	Servers []ServerElement `json:"servers"`
}

type ServerElement struct {
	// Error message if the server failed to connect
	Error *string `json:"error,omitempty"`
	// Server name (config key)
	Name string `json:"name"`
	// Configuration source: user, workspace, plugin, or builtin
	Source *string `json:"source,omitempty"`
	// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
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
	Plugins []PluginElement `json:"plugins"`
}

type PluginElement struct {
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
	Source ExtensionSource `json:"source"`
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

type SessionToolsHandlePendingToolCallResult struct {
	// Whether the tool call result was handled successfully
	Success bool `json:"success"`
}

type SessionToolsHandlePendingToolCallParams struct {
	// Error message if the tool call failed
	Error *string `json:"error,omitempty"`
	// Request ID of the pending tool call
	RequestID string `json:"requestId"`
	// Tool call result (string or expanded result object)
	Result *ResultUnion `json:"result"`
}

type ResultResult struct {
	// Error message if the tool call failed
	Error *string `json:"error,omitempty"`
	// Type of the tool result
	ResultType *string `json:"resultType,omitempty"`
	// Text result to send back to the LLM
	TextResultForLlm string `json:"textResultForLlm"`
	// Telemetry data from tool execution
	ToolTelemetry map[string]any `json:"toolTelemetry,omitempty"`
}

type SessionCommandsHandlePendingCommandResult struct {
	// Whether the command was handled successfully
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

type SessionUIHandlePendingElicitationResult struct {
	// Whether the response was accepted. False if the request was already resolved by another
	// client.
	Success bool `json:"success"`
}

type SessionUIHandlePendingElicitationParams struct {
	// The unique request ID from the elicitation.requested event
	RequestID string `json:"requestId"`
	// The elicitation response (accept with form values, decline, or cancel)
	Result SessionUIHandlePendingElicitationParamsResult `json:"result"`
}

// The elicitation response (accept with form values, decline, or cancel)
type SessionUIHandlePendingElicitationParamsResult struct {
	// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
	Action Action `json:"action"`
	// The form values submitted by the user (present when action is 'accept')
	Content map[string]*Content `json:"content,omitempty"`
}

type SessionPermissionsHandlePendingPermissionRequestResult struct {
	// Whether the permission request was handled successfully
	Success bool `json:"success"`
}

type SessionPermissionsHandlePendingPermissionRequestParams struct {
	// Request ID of the pending permission request
	RequestID string                                                       `json:"requestId"`
	Result    SessionPermissionsHandlePendingPermissionRequestParamsResult `json:"result"`
}

type SessionPermissionsHandlePendingPermissionRequestParamsResult struct {
	// The permission request was approved
	//
	// Denied because approval rules explicitly blocked it
	//
	// Denied because no approval rule matched and user confirmation was unavailable
	//
	// Denied by the user during an interactive prompt
	//
	// Denied by the organization's content exclusion policy
	//
	// Denied by a permission request hook registered by an extension or plugin
	Kind Kind `json:"kind"`
	// Rules that denied the request
	Rules []any `json:"rules,omitempty"`
	// Optional feedback from the user explaining the denial
	Feedback *string `json:"feedback,omitempty"`
	// Human-readable explanation of why the path was excluded
	//
	// Optional message from the hook explaining the denial
	Message *string `json:"message,omitempty"`
	// File path that triggered the exclusion
	Path *string `json:"path,omitempty"`
	// Whether to interrupt the current agent turn
	Interrupt *bool `json:"interrupt,omitempty"`
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

// Experimental: SessionHistoryCompactResult is part of an experimental API and may change or be removed.
type SessionHistoryCompactResult struct {
	// Post-compaction context window usage breakdown
	ContextWindow *ContextWindow `json:"contextWindow,omitempty"`
	// Number of messages removed during compaction
	MessagesRemoved float64 `json:"messagesRemoved"`
	// Whether compaction completed successfully
	Success bool `json:"success"`
	// Number of tokens freed by compaction
	TokensRemoved float64 `json:"tokensRemoved"`
}

// Post-compaction context window usage breakdown
type ContextWindow struct {
	// Token count from non-system messages (user, assistant, tool)
	ConversationTokens *float64 `json:"conversationTokens,omitempty"`
	// Current total tokens in the context window (system + conversation + tool definitions)
	CurrentTokens float64 `json:"currentTokens"`
	// Current number of messages in the conversation
	MessagesLength float64 `json:"messagesLength"`
	// Token count from system message(s)
	SystemTokens *float64 `json:"systemTokens,omitempty"`
	// Maximum token count for the model's context window
	TokenLimit float64 `json:"tokenLimit"`
	// Token count from tool definitions
	ToolDefinitionsTokens *float64 `json:"toolDefinitionsTokens,omitempty"`
}

// Experimental: SessionHistoryTruncateResult is part of an experimental API and may change or be removed.
type SessionHistoryTruncateResult struct {
	// Number of events that were removed
	EventsRemoved float64 `json:"eventsRemoved"`
}

// Experimental: SessionHistoryTruncateParams is part of an experimental API and may change or be removed.
type SessionHistoryTruncateParams struct {
	// Event ID to truncate to. This event and all events after it are removed from the session.
	EventID string `json:"eventId"`
}

// Experimental: SessionUsageGetMetricsResult is part of an experimental API and may change or be removed.
type SessionUsageGetMetricsResult struct {
	// Aggregated code change metrics
	CodeChanges CodeChanges `json:"codeChanges"`
	// Currently active model identifier
	CurrentModel *string `json:"currentModel,omitempty"`
	// Input tokens from the most recent main-agent API call
	LastCallInputTokens int64 `json:"lastCallInputTokens"`
	// Output tokens from the most recent main-agent API call
	LastCallOutputTokens int64 `json:"lastCallOutputTokens"`
	// Per-model token and request metrics, keyed by model identifier
	ModelMetrics map[string]ModelMetric `json:"modelMetrics"`
	// Session start timestamp (epoch milliseconds)
	SessionStartTime int64 `json:"sessionStartTime"`
	// Total time spent in model API calls (milliseconds)
	TotalAPIDurationMS float64 `json:"totalApiDurationMs"`
	// Total user-initiated premium request cost across all models (may be fractional due to
	// multipliers)
	TotalPremiumRequestCost float64 `json:"totalPremiumRequestCost"`
	// Raw count of user-initiated API requests
	TotalUserRequests int64 `json:"totalUserRequests"`
}

// Aggregated code change metrics
type CodeChanges struct {
	// Number of distinct files modified
	FilesModifiedCount int64 `json:"filesModifiedCount"`
	// Total lines of code added
	LinesAdded int64 `json:"linesAdded"`
	// Total lines of code removed
	LinesRemoved int64 `json:"linesRemoved"`
}

type ModelMetric struct {
	// Request count and cost metrics for this model
	Requests Requests `json:"requests"`
	// Token usage metrics for this model
	Usage Usage `json:"usage"`
}

// Request count and cost metrics for this model
type Requests struct {
	// User-initiated premium request cost (with multiplier applied)
	Cost float64 `json:"cost"`
	// Number of API requests made with this model
	Count int64 `json:"count"`
}

// Token usage metrics for this model
type Usage struct {
	// Total tokens read from prompt cache
	CacheReadTokens int64 `json:"cacheReadTokens"`
	// Total tokens written to prompt cache
	CacheWriteTokens int64 `json:"cacheWriteTokens"`
	// Total input tokens consumed
	InputTokens int64 `json:"inputTokens"`
	// Total output tokens produced
	OutputTokens int64 `json:"outputTokens"`
	// Total output tokens used for reasoning
	ReasoningTokens *int64 `json:"reasoningTokens,omitempty"`
}

type SessionFSReadFileResult struct {
	// File content as UTF-8 string
	Content string `json:"content"`
}

type SessionFSReadFileParams struct {
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSWriteFileParams struct {
	// Content to write
	Content string `json:"content"`
	// Optional POSIX-style mode for newly created files
	Mode *float64 `json:"mode,omitempty"`
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSAppendFileParams struct {
	// Content to append
	Content string `json:"content"`
	// Optional POSIX-style mode for newly created files
	Mode *float64 `json:"mode,omitempty"`
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSExistsResult struct {
	// Whether the path exists
	Exists bool `json:"exists"`
}

type SessionFSExistsParams struct {
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSStatResult struct {
	// ISO 8601 timestamp of creation
	Birthtime string `json:"birthtime"`
	// Whether the path is a directory
	IsDirectory bool `json:"isDirectory"`
	// Whether the path is a file
	IsFile bool `json:"isFile"`
	// ISO 8601 timestamp of last modification
	Mtime string `json:"mtime"`
	// File size in bytes
	Size float64 `json:"size"`
}

type SessionFSStatParams struct {
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSMkdirParams struct {
	// Optional POSIX-style mode for newly created directories
	Mode *float64 `json:"mode,omitempty"`
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Create parent directories as needed
	Recursive *bool `json:"recursive,omitempty"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSReaddirResult struct {
	// Entry names in the directory
	Entries []string `json:"entries"`
}

type SessionFSReaddirParams struct {
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSReaddirWithTypesResult struct {
	// Directory entries with type information
	Entries []Entry `json:"entries"`
}

type Entry struct {
	// Entry name
	Name string `json:"name"`
	// Entry type
	Type EntryType `json:"type"`
}

type SessionFSReaddirWithTypesParams struct {
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSRmParams struct {
	// Ignore errors if the path does not exist
	Force *bool `json:"force,omitempty"`
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Remove directories and their contents recursively
	Recursive *bool `json:"recursive,omitempty"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSRenameParams struct {
	// Destination path using SessionFs conventions
	Dest string `json:"dest"`
	// Target session identifier
	SessionID string `json:"sessionId"`
	// Source path using SessionFs conventions
	Src string `json:"src"`
}

type FilterMappingEnum string

const (
	FilterMappingEnumHiddenCharacters FilterMappingEnum = "hidden_characters"
	FilterMappingEnumMarkdown         FilterMappingEnum = "markdown"
	FilterMappingEnumNone             FilterMappingEnum = "none"
)

type ServerType string

const (
	ServerTypeHTTP  ServerType = "http"
	ServerTypeLocal ServerType = "local"
	ServerTypeSse   ServerType = "sse"
	ServerTypeStdio ServerType = "stdio"
)

// Configuration source
type ServerSource string

const (
	ServerSourceBuiltin   ServerSource = "builtin"
	ServerSourcePlugin    ServerSource = "plugin"
	ServerSourceUser      ServerSource = "user"
	ServerSourceWorkspace ServerSource = "workspace"
)

// Path conventions used by this filesystem
type Conventions string

const (
	ConventionsPosix   Conventions = "posix"
	ConventionsWindows Conventions = "windows"
)

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

// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
type ServerStatus string

const (
	ServerStatusConnected     ServerStatus = "connected"
	ServerStatusNeedsAuth     ServerStatus = "needs-auth"
	ServerStatusNotConfigured ServerStatus = "not_configured"
	ServerStatusPending       ServerStatus = "pending"
	ServerStatusDisabled      ServerStatus = "disabled"
	ServerStatusFailed        ServerStatus = "failed"
)

// Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
type ExtensionSource string

const (
	ExtensionSourceUser    ExtensionSource = "user"
	ExtensionSourceProject ExtensionSource = "project"
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
	FormatURI      Format = "uri"
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
	KindDeniedByPermissionRequestHook                  Kind = "denied-by-permission-request-hook"
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

// Entry type
type EntryType string

const (
	EntryTypeDirectory EntryType = "directory"
	EntryTypeFile      EntryType = "file"
)

type FilterMappingUnion struct {
	Enum    *FilterMappingEnum
	EnumMap map[string]FilterMappingEnum
}

// Tool call result (string or expanded result object)
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

type serverApi struct {
	client *jsonrpc2.Client
}

type ServerModelsApi serverApi

func (a *ServerModelsApi) List(ctx context.Context) (*ModelsListResult, error) {
	raw, err := a.client.Request("models.list", nil)
	if err != nil {
		return nil, err
	}
	var result ModelsListResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ServerToolsApi serverApi

func (a *ServerToolsApi) List(ctx context.Context, params *ToolsListParams) (*ToolsListResult, error) {
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

type ServerAccountApi serverApi

func (a *ServerAccountApi) GetQuota(ctx context.Context) (*AccountGetQuotaResult, error) {
	raw, err := a.client.Request("account.getQuota", nil)
	if err != nil {
		return nil, err
	}
	var result AccountGetQuotaResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ServerMcpApi serverApi

func (a *ServerMcpApi) Discover(ctx context.Context, params *MCPDiscoverParams) (*MCPDiscoverResult, error) {
	raw, err := a.client.Request("mcp.discover", params)
	if err != nil {
		return nil, err
	}
	var result MCPDiscoverResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ServerSessionFsApi serverApi

func (a *ServerSessionFsApi) SetProvider(ctx context.Context, params *SessionFSSetProviderParams) (*SessionFSSetProviderResult, error) {
	raw, err := a.client.Request("sessionFs.setProvider", params)
	if err != nil {
		return nil, err
	}
	var result SessionFSSetProviderResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: ServerSessionsApi contains experimental APIs that may change or be removed.
type ServerSessionsApi serverApi

func (a *ServerSessionsApi) Fork(ctx context.Context, params *SessionsForkParams) (*SessionsForkResult, error) {
	raw, err := a.client.Request("sessions.fork", params)
	if err != nil {
		return nil, err
	}
	var result SessionsForkResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// ServerRpc provides typed server-scoped RPC methods.
type ServerRpc struct {
	common serverApi // Reuse a single struct instead of allocating one for each service on the heap.

	Models    *ServerModelsApi
	Tools     *ServerToolsApi
	Account   *ServerAccountApi
	Mcp       *ServerMcpApi
	SessionFs *ServerSessionFsApi
	Sessions  *ServerSessionsApi
}

func (a *ServerRpc) Ping(ctx context.Context, params *PingParams) (*PingResult, error) {
	raw, err := a.common.client.Request("ping", params)
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
	r := &ServerRpc{}
	r.common = serverApi{client: client}
	r.Models = (*ServerModelsApi)(&r.common)
	r.Tools = (*ServerToolsApi)(&r.common)
	r.Account = (*ServerAccountApi)(&r.common)
	r.Mcp = (*ServerMcpApi)(&r.common)
	r.SessionFs = (*ServerSessionFsApi)(&r.common)
	r.Sessions = (*ServerSessionsApi)(&r.common)
	return r
}

type sessionApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

type ModelApi sessionApi

func (a *ModelApi) GetCurrent(ctx context.Context) (*SessionModelGetCurrentResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *ModelApi) SwitchTo(ctx context.Context, params *SessionModelSwitchToParams) (*SessionModelSwitchToResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["modelId"] = params.ModelID
		if params.ReasoningEffort != nil {
			req["reasoningEffort"] = *params.ReasoningEffort
		}
		if params.ModelCapabilities != nil {
			req["modelCapabilities"] = *params.ModelCapabilities
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

type ModeApi sessionApi

func (a *ModeApi) Get(ctx context.Context) (*SessionModeGetResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *ModeApi) Set(ctx context.Context, params *SessionModeSetParams) (*SessionModeSetResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

type PlanApi sessionApi

func (a *PlanApi) Read(ctx context.Context) (*SessionPlanReadResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *PlanApi) Update(ctx context.Context, params *SessionPlanUpdateParams) (*SessionPlanUpdateResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *PlanApi) Delete(ctx context.Context) (*SessionPlanDeleteResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

type WorkspaceApi sessionApi

func (a *WorkspaceApi) ListFiles(ctx context.Context) (*SessionWorkspaceListFilesResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *WorkspaceApi) ReadFile(ctx context.Context, params *SessionWorkspaceReadFileParams) (*SessionWorkspaceReadFileResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *WorkspaceApi) CreateFile(ctx context.Context, params *SessionWorkspaceCreateFileParams) (*SessionWorkspaceCreateFileResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

// Experimental: FleetApi contains experimental APIs that may change or be removed.
type FleetApi sessionApi

func (a *FleetApi) Start(ctx context.Context, params *SessionFleetStartParams) (*SessionFleetStartResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

// Experimental: AgentApi contains experimental APIs that may change or be removed.
type AgentApi sessionApi

func (a *AgentApi) List(ctx context.Context) (*SessionAgentListResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *AgentApi) GetCurrent(ctx context.Context) (*SessionAgentGetCurrentResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *AgentApi) Select(ctx context.Context, params *SessionAgentSelectParams) (*SessionAgentSelectResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *AgentApi) Deselect(ctx context.Context) (*SessionAgentDeselectResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *AgentApi) Reload(ctx context.Context) (*SessionAgentReloadResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

// Experimental: SkillsApi contains experimental APIs that may change or be removed.
type SkillsApi sessionApi

func (a *SkillsApi) List(ctx context.Context) (*SessionSkillsListResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *SkillsApi) Enable(ctx context.Context, params *SessionSkillsEnableParams) (*SessionSkillsEnableResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *SkillsApi) Disable(ctx context.Context, params *SessionSkillsDisableParams) (*SessionSkillsDisableResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *SkillsApi) Reload(ctx context.Context) (*SessionSkillsReloadResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

// Experimental: McpApi contains experimental APIs that may change or be removed.
type McpApi sessionApi

func (a *McpApi) List(ctx context.Context) (*SessionMCPListResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *McpApi) Enable(ctx context.Context, params *SessionMCPEnableParams) (*SessionMCPEnableResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *McpApi) Disable(ctx context.Context, params *SessionMCPDisableParams) (*SessionMCPDisableResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *McpApi) Reload(ctx context.Context) (*SessionMCPReloadResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

// Experimental: PluginsApi contains experimental APIs that may change or be removed.
type PluginsApi sessionApi

func (a *PluginsApi) List(ctx context.Context) (*SessionPluginsListResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

// Experimental: ExtensionsApi contains experimental APIs that may change or be removed.
type ExtensionsApi sessionApi

func (a *ExtensionsApi) List(ctx context.Context) (*SessionExtensionsListResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *ExtensionsApi) Enable(ctx context.Context, params *SessionExtensionsEnableParams) (*SessionExtensionsEnableResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *ExtensionsApi) Disable(ctx context.Context, params *SessionExtensionsDisableParams) (*SessionExtensionsDisableResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *ExtensionsApi) Reload(ctx context.Context) (*SessionExtensionsReloadResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

type ToolsApi sessionApi

func (a *ToolsApi) HandlePendingToolCall(ctx context.Context, params *SessionToolsHandlePendingToolCallParams) (*SessionToolsHandlePendingToolCallResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

type CommandsApi sessionApi

func (a *CommandsApi) HandlePendingCommand(ctx context.Context, params *SessionCommandsHandlePendingCommandParams) (*SessionCommandsHandlePendingCommandResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

type UIApi sessionApi

func (a *UIApi) Elicitation(ctx context.Context, params *SessionUIElicitationParams) (*SessionUIElicitationResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

func (a *UIApi) HandlePendingElicitation(ctx context.Context, params *SessionUIHandlePendingElicitationParams) (*SessionUIHandlePendingElicitationResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["requestId"] = params.RequestID
		req["result"] = params.Result
	}
	raw, err := a.client.Request("session.ui.handlePendingElicitation", req)
	if err != nil {
		return nil, err
	}
	var result SessionUIHandlePendingElicitationResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type PermissionsApi sessionApi

func (a *PermissionsApi) HandlePendingPermissionRequest(ctx context.Context, params *SessionPermissionsHandlePendingPermissionRequestParams) (*SessionPermissionsHandlePendingPermissionRequestResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

type ShellApi sessionApi

func (a *ShellApi) Exec(ctx context.Context, params *SessionShellExecParams) (*SessionShellExecResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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
	return &result, nil
}

func (a *ShellApi) Kill(ctx context.Context, params *SessionShellKillParams) (*SessionShellKillResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
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

// Experimental: HistoryApi contains experimental APIs that may change or be removed.
type HistoryApi sessionApi

func (a *HistoryApi) Compact(ctx context.Context) (*SessionHistoryCompactResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.history.compact", req)
	if err != nil {
		return nil, err
	}
	var result SessionHistoryCompactResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *HistoryApi) Truncate(ctx context.Context, params *SessionHistoryTruncateParams) (*SessionHistoryTruncateResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["eventId"] = params.EventID
	}
	raw, err := a.client.Request("session.history.truncate", req)
	if err != nil {
		return nil, err
	}
	var result SessionHistoryTruncateResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: UsageApi contains experimental APIs that may change or be removed.
type UsageApi sessionApi

func (a *UsageApi) GetMetrics(ctx context.Context) (*SessionUsageGetMetricsResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.usage.getMetrics", req)
	if err != nil {
		return nil, err
	}
	var result SessionUsageGetMetricsResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// SessionRpc provides typed session-scoped RPC methods.
type SessionRpc struct {
	common sessionApi // Reuse a single struct instead of allocating one for each service on the heap.

	Model       *ModelApi
	Mode        *ModeApi
	Plan        *PlanApi
	Workspace   *WorkspaceApi
	Fleet       *FleetApi
	Agent       *AgentApi
	Skills      *SkillsApi
	Mcp         *McpApi
	Plugins     *PluginsApi
	Extensions  *ExtensionsApi
	Tools       *ToolsApi
	Commands    *CommandsApi
	UI          *UIApi
	Permissions *PermissionsApi
	Shell       *ShellApi
	History     *HistoryApi
	Usage       *UsageApi
}

func (a *SessionRpc) Log(ctx context.Context, params *SessionLogParams) (*SessionLogResult, error) {
	req := map[string]any{"sessionId": a.common.sessionID}
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
	raw, err := a.common.client.Request("session.log", req)
	if err != nil {
		return nil, err
	}
	var result SessionLogResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func NewSessionRpc(client *jsonrpc2.Client, sessionID string) *SessionRpc {
	r := &SessionRpc{}
	r.common = sessionApi{client: client, sessionID: sessionID}
	r.Model = (*ModelApi)(&r.common)
	r.Mode = (*ModeApi)(&r.common)
	r.Plan = (*PlanApi)(&r.common)
	r.Workspace = (*WorkspaceApi)(&r.common)
	r.Fleet = (*FleetApi)(&r.common)
	r.Agent = (*AgentApi)(&r.common)
	r.Skills = (*SkillsApi)(&r.common)
	r.Mcp = (*McpApi)(&r.common)
	r.Plugins = (*PluginsApi)(&r.common)
	r.Extensions = (*ExtensionsApi)(&r.common)
	r.Tools = (*ToolsApi)(&r.common)
	r.Commands = (*CommandsApi)(&r.common)
	r.UI = (*UIApi)(&r.common)
	r.Permissions = (*PermissionsApi)(&r.common)
	r.Shell = (*ShellApi)(&r.common)
	r.History = (*HistoryApi)(&r.common)
	r.Usage = (*UsageApi)(&r.common)
	return r
}

type SessionFsHandler interface {
	ReadFile(request *SessionFSReadFileParams) (*SessionFSReadFileResult, error)
	WriteFile(request *SessionFSWriteFileParams) error
	AppendFile(request *SessionFSAppendFileParams) error
	Exists(request *SessionFSExistsParams) (*SessionFSExistsResult, error)
	Stat(request *SessionFSStatParams) (*SessionFSStatResult, error)
	Mkdir(request *SessionFSMkdirParams) error
	Readdir(request *SessionFSReaddirParams) (*SessionFSReaddirResult, error)
	ReaddirWithTypes(request *SessionFSReaddirWithTypesParams) (*SessionFSReaddirWithTypesResult, error)
	Rm(request *SessionFSRmParams) error
	Rename(request *SessionFSRenameParams) error
}

// ClientSessionApiHandlers provides all client session API handler groups for a session.
type ClientSessionApiHandlers struct {
	SessionFs SessionFsHandler
}

func clientSessionHandlerError(err error) *jsonrpc2.Error {
	if err == nil {
		return nil
	}
	var rpcErr *jsonrpc2.Error
	if errors.As(err, &rpcErr) {
		return rpcErr
	}
	return &jsonrpc2.Error{Code: -32603, Message: err.Error()}
}

// RegisterClientSessionApiHandlers registers handlers for server-to-client session API calls.
func RegisterClientSessionApiHandlers(client *jsonrpc2.Client, getHandlers func(sessionID string) *ClientSessionApiHandlers) {
	client.SetRequestHandler("sessionFs.readFile", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSReadFileParams
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.ReadFile(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.writeFile", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSWriteFileParams
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		if err := handlers.SessionFs.WriteFile(&request); err != nil {
			return nil, clientSessionHandlerError(err)
		}
		return json.RawMessage("null"), nil
	})
	client.SetRequestHandler("sessionFs.appendFile", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSAppendFileParams
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		if err := handlers.SessionFs.AppendFile(&request); err != nil {
			return nil, clientSessionHandlerError(err)
		}
		return json.RawMessage("null"), nil
	})
	client.SetRequestHandler("sessionFs.exists", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSExistsParams
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.Exists(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.stat", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSStatParams
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.Stat(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.mkdir", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSMkdirParams
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		if err := handlers.SessionFs.Mkdir(&request); err != nil {
			return nil, clientSessionHandlerError(err)
		}
		return json.RawMessage("null"), nil
	})
	client.SetRequestHandler("sessionFs.readdir", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSReaddirParams
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.Readdir(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.readdirWithTypes", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSReaddirWithTypesParams
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.ReaddirWithTypes(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.rm", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSRmParams
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		if err := handlers.SessionFs.Rm(&request); err != nil {
			return nil, clientSessionHandlerError(err)
		}
		return json.RawMessage("null"), nil
	})
	client.SetRequestHandler("sessionFs.rename", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSRenameParams
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		if err := handlers.SessionFs.Rename(&request); err != nil {
			return nil, clientSessionHandlerError(err)
		}
		return json.RawMessage("null"), nil
	})
}
