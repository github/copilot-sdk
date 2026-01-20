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
	// UseStdio enables stdio transport instead of TCP (default: true)
	UseStdio bool
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
	// Env is the environment variables for the CLI process (default: inherits from current process)
	Env []string
}

// Bool returns a pointer to the given bool value.
// Use for setting AutoStart or AutoRestart: AutoStart: Bool(false)
func Bool(v bool) *bool {
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

// SessionConfig configures a new session
type SessionConfig struct {
	// SessionID is an optional custom session ID
	SessionID string
	// Model to use for this session
	Model string
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
	// OnPermissionRequest is a handler for permission requests from the server
	OnPermissionRequest PermissionHandler
	// Streaming enables streaming of assistant message and reasoning chunks.
	// When true, assistant.message_delta and assistant.reasoning_delta events
	// with deltaContent are sent as the response is generated.
	Streaming bool
	// MCPServers configures MCP servers for the session
	MCPServers map[string]MCPServerConfig
	// CustomAgents configures custom agents for the session
	CustomAgents []CustomAgentConfig
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
