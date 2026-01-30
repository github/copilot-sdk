package copilot

// ConnectionState represents the client connection state
type ConnectionState string

const (
	StateDisconnected ConnectionState = "disconnected"
	StateConnecting   ConnectionState = "connecting"
	StateConnected    ConnectionState = "connected"
	StateError        ConnectionState = "error"
)

// ClientOptions configures the CopilotClient
type ClientOptions struct {
	// CLIPath is the path to the Copilot CLI executable (default: "copilot")
	CLIPath string
	// Cwd is the working directory for the CLI process (default: "" = inherit from current process)
	Cwd string
	// Port for TCP transport (default: 0 = random port)
	Port int
	// UseStdio controls whether to use stdio transport instead of TCP.
	// Default: nil (use default = true, i.e. stdio). Use Bool(false) to explicitly select TCP.
	UseStdio *bool
	// CLIUrl is the URL of an existing Copilot CLI server to connect to over TCP
	// Format: "host:port", "http://host:port", or just "port" (defaults to localhost)
	// Examples: "localhost:8080", "http://127.0.0.1:9000", "8080"
	// Mutually exclusive with CLIPath, UseStdio
	CLIUrl string
	// LogLevel for the CLI server
	LogLevel string
	// AutoStart automatically starts the CLI server on first use (default: true).
	// Use Bool(false) to disable.
	AutoStart *bool
	// AutoRestart automatically restarts the CLI server if it crashes (default: true).
	// Use Bool(false) to disable.
	AutoRestart *bool
	// Env is the environment variables for the CLI process (default: inherits from current process).
	// Each entry is of the form "key=value".
	// If Env is nil, the new process uses the current process's environment.
	// If Env contains duplicate environment keys, only the last value in the
	// slice for each duplicate key is used.
	Env []string
	// GithubToken is the GitHub token to use for authentication.
	// When provided, the token is passed to the CLI server via environment variable.
	// This takes priority over other authentication methods.
	GithubToken string
	// UseLoggedInUser controls whether to use the logged-in user for authentication.
	// When true, the CLI server will attempt to use stored OAuth tokens or gh CLI auth.
	// When false, only explicit tokens (GithubToken or environment variables) are used.
	// Default: true (but defaults to false when GithubToken is provided).
	// Use Bool(false) to explicitly disable.
	UseLoggedInUser *bool
}

// Bool returns a pointer to the given bool value.
// Use for setting AutoStart or AutoRestart: AutoStart: Bool(false)
func Bool(v bool) *bool {
	return &v
}

// Float64 returns a pointer to the given float64 value.
// Use for setting thresholds: BackgroundCompactionThreshold: Float64(0.80)
func Float64(v float64) *float64 {
	return &v
}

// SystemMessageAppendConfig is append mode: use CLI foundation with optional appended content.
type SystemMessageAppendConfig struct {
	// Mode is optional, defaults to "append"
	Mode string `json:"mode,omitempty"`
	// Content provides additional instructions appended after SDK-managed sections
	Content string `json:"content,omitempty"`
}

// SystemMessageReplaceConfig is replace mode: use caller-provided system message entirely.
// Removes all SDK guardrails including security restrictions.
type SystemMessageReplaceConfig struct {
	// Mode must be "replace"
	Mode string `json:"mode"`
	// Content is the complete system message (required)
	Content string `json:"content"`
}

// SystemMessageConfig represents system message configuration for session creation.
// Use SystemMessageAppendConfig for default behavior, SystemMessageReplaceConfig for full control.
// In Go, use one struct or the other based on your needs.
type SystemMessageConfig struct {
	Mode    string `json:"mode,omitempty"`
	Content string `json:"content,omitempty"`
}

// PermissionRequest represents a permission request from the server
type PermissionRequest struct {
	Kind       string                 `json:"kind"`
	ToolCallID string                 `json:"toolCallId,omitempty"`
	Extra      map[string]interface{} `json:"-"` // Additional fields vary by kind
}

// PermissionRequestResult represents the result of a permission request
type PermissionRequestResult struct {
	Kind  string        `json:"kind"`
	Rules []interface{} `json:"rules,omitempty"`
}

// PermissionHandler executes a permission request
// The handler should return a PermissionRequestResult. Returning an error denies the permission.
type PermissionHandler func(request PermissionRequest, invocation PermissionInvocation) (PermissionRequestResult, error)

// PermissionInvocation provides context about a permission request
type PermissionInvocation struct {
	SessionID string
}

// UserInputRequest represents a request for user input from the agent
type UserInputRequest struct {
	Question      string   `json:"question"`
	Choices       []string `json:"choices,omitempty"`
	AllowFreeform *bool    `json:"allowFreeform,omitempty"`
}

// UserInputResponse represents the user's response to an input request
type UserInputResponse struct {
	Answer      string `json:"answer"`
	WasFreeform bool   `json:"wasFreeform"`
}

// UserInputHandler handles user input requests from the agent
// The handler should return a UserInputResponse. Returning an error fails the request.
type UserInputHandler func(request UserInputRequest, invocation UserInputInvocation) (UserInputResponse, error)

// UserInputInvocation provides context about a user input request
type UserInputInvocation struct {
	SessionID string
}

// PreToolUseHookInput is the input for a pre-tool-use hook
type PreToolUseHookInput struct {
	Timestamp int64       `json:"timestamp"`
	Cwd       string      `json:"cwd"`
	ToolName  string      `json:"toolName"`
	ToolArgs  interface{} `json:"toolArgs"`
}

// PreToolUseHookOutput is the output for a pre-tool-use hook
type PreToolUseHookOutput struct {
	PermissionDecision       string      `json:"permissionDecision,omitempty"` // "allow", "deny", "ask"
	PermissionDecisionReason string      `json:"permissionDecisionReason,omitempty"`
	ModifiedArgs             interface{} `json:"modifiedArgs,omitempty"`
	AdditionalContext        string      `json:"additionalContext,omitempty"`
	SuppressOutput           bool        `json:"suppressOutput,omitempty"`
}

// PreToolUseHandler handles pre-tool-use hook invocations
type PreToolUseHandler func(input PreToolUseHookInput, invocation HookInvocation) (*PreToolUseHookOutput, error)

// PostToolUseHookInput is the input for a post-tool-use hook
type PostToolUseHookInput struct {
	Timestamp  int64       `json:"timestamp"`
	Cwd        string      `json:"cwd"`
	ToolName   string      `json:"toolName"`
	ToolArgs   interface{} `json:"toolArgs"`
	ToolResult interface{} `json:"toolResult"`
}

// PostToolUseHookOutput is the output for a post-tool-use hook
type PostToolUseHookOutput struct {
	ModifiedResult    interface{} `json:"modifiedResult,omitempty"`
	AdditionalContext string      `json:"additionalContext,omitempty"`
	SuppressOutput    bool        `json:"suppressOutput,omitempty"`
}

// PostToolUseHandler handles post-tool-use hook invocations
type PostToolUseHandler func(input PostToolUseHookInput, invocation HookInvocation) (*PostToolUseHookOutput, error)

// UserPromptSubmittedHookInput is the input for a user-prompt-submitted hook
type UserPromptSubmittedHookInput struct {
	Timestamp int64  `json:"timestamp"`
	Cwd       string `json:"cwd"`
	Prompt    string `json:"prompt"`
}

// UserPromptSubmittedHookOutput is the output for a user-prompt-submitted hook
type UserPromptSubmittedHookOutput struct {
	ModifiedPrompt    string `json:"modifiedPrompt,omitempty"`
	AdditionalContext string `json:"additionalContext,omitempty"`
	SuppressOutput    bool   `json:"suppressOutput,omitempty"`
}

// UserPromptSubmittedHandler handles user-prompt-submitted hook invocations
type UserPromptSubmittedHandler func(input UserPromptSubmittedHookInput, invocation HookInvocation) (*UserPromptSubmittedHookOutput, error)

// SessionStartHookInput is the input for a session-start hook
type SessionStartHookInput struct {
	Timestamp     int64  `json:"timestamp"`
	Cwd           string `json:"cwd"`
	Source        string `json:"source"` // "startup", "resume", "new"
	InitialPrompt string `json:"initialPrompt,omitempty"`
}

// SessionStartHookOutput is the output for a session-start hook
type SessionStartHookOutput struct {
	AdditionalContext string                 `json:"additionalContext,omitempty"`
	ModifiedConfig    map[string]interface{} `json:"modifiedConfig,omitempty"`
}

// SessionStartHandler handles session-start hook invocations
type SessionStartHandler func(input SessionStartHookInput, invocation HookInvocation) (*SessionStartHookOutput, error)

// SessionEndHookInput is the input for a session-end hook
type SessionEndHookInput struct {
	Timestamp    int64  `json:"timestamp"`
	Cwd          string `json:"cwd"`
	Reason       string `json:"reason"` // "complete", "error", "abort", "timeout", "user_exit"
	FinalMessage string `json:"finalMessage,omitempty"`
	Error        string `json:"error,omitempty"`
}

// SessionEndHookOutput is the output for a session-end hook
type SessionEndHookOutput struct {
	SuppressOutput bool     `json:"suppressOutput,omitempty"`
	CleanupActions []string `json:"cleanupActions,omitempty"`
	SessionSummary string   `json:"sessionSummary,omitempty"`
}

// SessionEndHandler handles session-end hook invocations
type SessionEndHandler func(input SessionEndHookInput, invocation HookInvocation) (*SessionEndHookOutput, error)

// ErrorOccurredHookInput is the input for an error-occurred hook
type ErrorOccurredHookInput struct {
	Timestamp    int64  `json:"timestamp"`
	Cwd          string `json:"cwd"`
	Error        string `json:"error"`
	ErrorContext string `json:"errorContext"` // "model_call", "tool_execution", "system", "user_input"
	Recoverable  bool   `json:"recoverable"`
}

// ErrorOccurredHookOutput is the output for an error-occurred hook
type ErrorOccurredHookOutput struct {
	SuppressOutput   bool   `json:"suppressOutput,omitempty"`
	ErrorHandling    string `json:"errorHandling,omitempty"` // "retry", "skip", "abort"
	RetryCount       int    `json:"retryCount,omitempty"`
	UserNotification string `json:"userNotification,omitempty"`
}

// ErrorOccurredHandler handles error-occurred hook invocations
type ErrorOccurredHandler func(input ErrorOccurredHookInput, invocation HookInvocation) (*ErrorOccurredHookOutput, error)

// HookInvocation provides context about a hook invocation
type HookInvocation struct {
	SessionID string
}

// SessionHooks configures hook handlers for a session
type SessionHooks struct {
	OnPreToolUse          PreToolUseHandler
	OnPostToolUse         PostToolUseHandler
	OnUserPromptSubmitted UserPromptSubmittedHandler
	OnSessionStart        SessionStartHandler
	OnSessionEnd          SessionEndHandler
	OnErrorOccurred       ErrorOccurredHandler
}

// MCPLocalServerConfig configures a local/stdio MCP server
type MCPLocalServerConfig struct {
	Tools   []string          `json:"tools"`
	Type    string            `json:"type,omitempty"` // "local" or "stdio"
	Timeout int               `json:"timeout,omitempty"`
	Command string            `json:"command"`
	Args    []string          `json:"args"`
	Env     map[string]string `json:"env,omitempty"`
	Cwd     string            `json:"cwd,omitempty"`
}

// MCPRemoteServerConfig configures a remote MCP server (HTTP or SSE)
type MCPRemoteServerConfig struct {
	Tools   []string          `json:"tools"`
	Type    string            `json:"type"` // "http" or "sse"
	Timeout int               `json:"timeout,omitempty"`
	URL     string            `json:"url"`
	Headers map[string]string `json:"headers,omitempty"`
}

// MCPServerConfig can be either MCPLocalServerConfig or MCPRemoteServerConfig
// Use a map[string]interface{} for flexibility, or create separate configs
type MCPServerConfig map[string]interface{}

// CustomAgentConfig configures a custom agent
type CustomAgentConfig struct {
	// Name is the unique name of the custom agent
	Name string `json:"name"`
	// DisplayName is the display name for UI purposes
	DisplayName string `json:"displayName,omitempty"`
	// Description of what the agent does
	Description string `json:"description,omitempty"`
	// Tools is the list of tool names the agent can use (nil for all tools)
	Tools []string `json:"tools,omitempty"`
	// Prompt is the prompt content for the agent
	Prompt string `json:"prompt"`
	// MCPServers are MCP servers specific to this agent
	MCPServers map[string]MCPServerConfig `json:"mcpServers,omitempty"`
	// Infer indicates whether the agent should be available for model inference
	Infer *bool `json:"infer,omitempty"`
}

// InfiniteSessionConfig configures infinite sessions with automatic context compaction
// and workspace persistence. When enabled, sessions automatically manage context window
// limits through background compaction and persist state to a workspace directory.
type InfiniteSessionConfig struct {
	// Enabled controls whether infinite sessions are enabled (default: true)
	Enabled *bool
	// BackgroundCompactionThreshold is the context utilization (0.0-1.0) at which
	// background compaction starts. Default: 0.80
	BackgroundCompactionThreshold *float64
	// BufferExhaustionThreshold is the context utilization (0.0-1.0) at which
	// the session blocks until compaction completes. Default: 0.95
	BufferExhaustionThreshold *float64
}

// SessionConfig configures a new session
type SessionConfig struct {
	// SessionID is an optional custom session ID
	SessionID string
	// Model to use for this session
	Model string
	// ReasoningEffort level for models that support it.
	// Valid values: "low", "medium", "high", "xhigh"
	// Only applies to models where capabilities.supports.reasoningEffort is true.
	ReasoningEffort string
	// ConfigDir overrides the default configuration directory location.
	// When specified, the session will use this directory for storing config and state.
	ConfigDir string
	// Tools exposes caller-implemented tools to the CLI
	Tools []Tool
	// SystemMessage configures system message customization
	SystemMessage *SystemMessageConfig
	// AvailableTools is a list of tool names to allow. When specified, only these tools will be available.
	// Takes precedence over ExcludedTools.
	AvailableTools []string
	// ExcludedTools is a list of tool names to disable. All other tools remain available.
	// Ignored if AvailableTools is specified.
	ExcludedTools []string
	// OnPermissionRequest is a handler for permission requests from the server
	OnPermissionRequest PermissionHandler
	// OnUserInputRequest is a handler for user input requests from the agent (enables ask_user tool)
	OnUserInputRequest UserInputHandler
	// Hooks configures hook handlers for session lifecycle events
	Hooks *SessionHooks
	// WorkingDirectory is the working directory for the session.
	// Tool operations will be relative to this directory.
	WorkingDirectory string
	// Streaming enables streaming of assistant message and reasoning chunks.
	// When true, assistant.message_delta and assistant.reasoning_delta events
	// with deltaContent are sent as the response is generated.
	Streaming bool
	// Provider configures a custom model provider (BYOK)
	Provider *ProviderConfig
	// MCPServers configures MCP servers for the session
	MCPServers map[string]MCPServerConfig
	// CustomAgents configures custom agents for the session
	CustomAgents []CustomAgentConfig
	// SkillDirectories is a list of directories to load skills from
	SkillDirectories []string
	// DisabledSkills is a list of skill names to disable
	DisabledSkills []string
	// InfiniteSessions configures infinite sessions for persistent workspaces and automatic compaction.
	// When enabled (default), sessions automatically manage context limits and persist state.
	InfiniteSessions *InfiniteSessionConfig
}

// Tool describes a caller-implemented tool that can be invoked by Copilot
type Tool struct {
	Name        string
	Description string // optional
	Parameters  map[string]interface{}
	Handler     ToolHandler
}

// ToolInvocation describes a tool call initiated by Copilot
type ToolInvocation struct {
	SessionID  string
	ToolCallID string
	ToolName   string
	Arguments  interface{}
}

// ToolHandler executes a tool invocation.
// The handler should return a ToolResult. Returning an error marks the tool execution as a failure.
type ToolHandler func(invocation ToolInvocation) (ToolResult, error)

// ToolResult represents the result of a tool invocation.
type ToolResult struct {
	TextResultForLLM    string                 `json:"textResultForLlm"`
	BinaryResultsForLLM []ToolBinaryResult     `json:"binaryResultsForLlm,omitempty"`
	ResultType          string                 `json:"resultType"`
	Error               string                 `json:"error,omitempty"`
	SessionLog          string                 `json:"sessionLog,omitempty"`
	ToolTelemetry       map[string]interface{} `json:"toolTelemetry,omitempty"`
}

// ResumeSessionConfig configures options when resuming a session
type ResumeSessionConfig struct {
	// Tools exposes caller-implemented tools to the CLI
	Tools []Tool
	// Provider configures a custom model provider
	Provider *ProviderConfig
	// ReasoningEffort level for models that support it.
	// Valid values: "low", "medium", "high", "xhigh"
	ReasoningEffort string
	// OnPermissionRequest is a handler for permission requests from the server
	OnPermissionRequest PermissionHandler
	// OnUserInputRequest is a handler for user input requests from the agent (enables ask_user tool)
	OnUserInputRequest UserInputHandler
	// Hooks configures hook handlers for session lifecycle events
	Hooks *SessionHooks
	// WorkingDirectory is the working directory for the session.
	// Tool operations will be relative to this directory.
	WorkingDirectory string
	// Streaming enables streaming of assistant message and reasoning chunks.
	// When true, assistant.message_delta and assistant.reasoning_delta events
	// with deltaContent are sent as the response is generated.
	Streaming bool
	// MCPServers configures MCP servers for the session
	MCPServers map[string]MCPServerConfig
	// CustomAgents configures custom agents for the session
	CustomAgents []CustomAgentConfig
	// SkillDirectories is a list of directories to load skills from
	SkillDirectories []string
	// DisabledSkills is a list of skill names to disable
	DisabledSkills []string
	// DisableResume, when true, skips emitting the session.resume event.
	// Useful for reconnecting to a session without triggering resume-related side effects.
	DisableResume bool
}

// ProviderConfig configures a custom model provider
type ProviderConfig struct {
	// Type is the provider type: "openai", "azure", or "anthropic". Defaults to "openai".
	Type string `json:"type,omitempty"`
	// WireApi is the API format (openai/azure only): "completions" or "responses". Defaults to "completions".
	WireApi string `json:"wireApi,omitempty"`
	// BaseURL is the API endpoint URL
	BaseURL string `json:"baseUrl"`
	// APIKey is the API key. Optional for local providers like Ollama.
	APIKey string `json:"apiKey,omitempty"`
	// BearerToken for authentication. Sets the Authorization header directly.
	// Use this for services requiring bearer token auth instead of API key.
	// Takes precedence over APIKey when both are set.
	BearerToken string `json:"bearerToken,omitempty"`
	// Azure contains Azure-specific options
	Azure *AzureProviderOptions `json:"azure,omitempty"`
}

// AzureProviderOptions contains Azure-specific provider configuration
type AzureProviderOptions struct {
	// APIVersion is the Azure API version. Defaults to "2024-10-21".
	APIVersion string `json:"apiVersion,omitempty"`
}

// ToolBinaryResult represents binary payloads returned by tools.
type ToolBinaryResult struct {
	Data        string `json:"data"`
	MimeType    string `json:"mimeType"`
	Type        string `json:"type"`
	Description string `json:"description,omitempty"`
}

// MessageOptions configures a message to send
type MessageOptions struct {
	// Prompt is the message to send
	Prompt string
	// Attachments are file or directory attachments
	Attachments []Attachment
	// Mode is the message delivery mode (default: "enqueue")
	Mode string
}

// SessionEventHandler is a callback for session events
type SessionEventHandler func(event SessionEvent)

// PingResponse is the response from a ping request
type PingResponse struct {
	Message         string `json:"message"`
	Timestamp       int64  `json:"timestamp"`
	ProtocolVersion *int   `json:"protocolVersion,omitempty"`
}

// SessionCreateResponse is the response from session.create
type SessionCreateResponse struct {
	SessionID string `json:"sessionId"`
}

// SessionSendResponse is the response from session.send
type SessionSendResponse struct {
	MessageID string `json:"messageId"`
}

// SessionGetMessagesResponse is the response from session.getMessages
type SessionGetMessagesResponse struct {
	Events []SessionEvent `json:"events"`
}

// GetStatusResponse is the response from status.get
type GetStatusResponse struct {
	Version         string `json:"version"`
	ProtocolVersion int    `json:"protocolVersion"`
}

// GetAuthStatusResponse is the response from auth.getStatus
type GetAuthStatusResponse struct {
	IsAuthenticated bool    `json:"isAuthenticated"`
	AuthType        *string `json:"authType,omitempty"`
	Host            *string `json:"host,omitempty"`
	Login           *string `json:"login,omitempty"`
	StatusMessage   *string `json:"statusMessage,omitempty"`
}

// ModelVisionLimits contains vision-specific limits
type ModelVisionLimits struct {
	SupportedMediaTypes []string `json:"supported_media_types"`
	MaxPromptImages     int      `json:"max_prompt_images"`
	MaxPromptImageSize  int      `json:"max_prompt_image_size"`
}

// ModelLimits contains model limits
type ModelLimits struct {
	MaxPromptTokens        *int               `json:"max_prompt_tokens,omitempty"`
	MaxContextWindowTokens int                `json:"max_context_window_tokens"`
	Vision                 *ModelVisionLimits `json:"vision,omitempty"`
}

// ModelSupports contains model support flags
type ModelSupports struct {
	Vision          bool `json:"vision"`
	ReasoningEffort bool `json:"reasoningEffort"`
}

// ModelCapabilities contains model capabilities and limits
type ModelCapabilities struct {
	Supports ModelSupports `json:"supports"`
	Limits   ModelLimits   `json:"limits"`
}

// ModelPolicy contains model policy state
type ModelPolicy struct {
	State string `json:"state"`
	Terms string `json:"terms"`
}

// ModelBilling contains model billing information
type ModelBilling struct {
	Multiplier float64 `json:"multiplier"`
}

// ModelInfo contains information about an available model
type ModelInfo struct {
	ID                        string            `json:"id"`
	Name                      string            `json:"name"`
	Capabilities              ModelCapabilities `json:"capabilities"`
	Policy                    *ModelPolicy      `json:"policy,omitempty"`
	Billing                   *ModelBilling     `json:"billing,omitempty"`
	SupportedReasoningEfforts []string          `json:"supportedReasoningEfforts,omitempty"`
	DefaultReasoningEffort    string            `json:"defaultReasoningEffort,omitempty"`
}

// GetModelsResponse is the response from models.list
type GetModelsResponse struct {
	Models []ModelInfo `json:"models"`
}

// SessionMetadata contains metadata about a session
type SessionMetadata struct {
	SessionID    string  `json:"sessionId"`
	StartTime    string  `json:"startTime"`
	ModifiedTime string  `json:"modifiedTime"`
	Summary      *string `json:"summary,omitempty"`
	IsRemote     bool    `json:"isRemote"`
}

// ListSessionsResponse is the response from session.list
type ListSessionsResponse struct {
	Sessions []SessionMetadata `json:"sessions"`
}

// DeleteSessionRequest is the request for session.delete
type DeleteSessionRequest struct {
	SessionID string `json:"sessionId"`
}

// DeleteSessionResponse is the response from session.delete
type DeleteSessionResponse struct {
	Success bool    `json:"success"`
	Error   *string `json:"error,omitempty"`
}
