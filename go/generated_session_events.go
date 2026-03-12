// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

// Code generated from JSON Schema using quicktype. DO NOT EDIT.
// To parse and unparse this JSON data, add this code to your project and do:
//
//    sessionEvent, err := UnmarshalSessionEvent(bytes)
//    bytes, err = sessionEvent.Marshal()

package copilot

import "bytes"
import "errors"
import "time"

import "encoding/json"

func UnmarshalSessionEvent(data []byte) (SessionEvent, error) {
	var r SessionEvent
	err := json.Unmarshal(data, &r)
	return r, err
}

func (r *SessionEvent) Marshal() ([]byte, error) {
	return json.Marshal(r)
}

type SessionEvent struct {
	// Session initialization metadata including context and configuration
	//
	// Session resume metadata including current context and event count
	//
	// Error details for timeline display including message and optional diagnostic information
	//
	// Payload indicating the agent is idle; includes any background tasks still in flight
	//
	// Session title change payload containing the new display title
	//
	// Informational message for timeline display with categorization
	//
	// Warning message for timeline display with categorization
	//
	// Model change details including previous and new model identifiers
	//
	// Agent mode change details including previous and new modes
	//
	// Plan file operation details indicating what changed
	//
	// Workspace file change details including path and operation type
	//
	// Session handoff metadata including source, context, and repository information
	//
	// Conversation truncation statistics including token counts and removed content metrics
	//
	// Session rewind details including target event and count of removed events
	//
	// Session termination metrics including usage statistics, code changes, and shutdown
	// reason
	//
	// Updated working directory and git context after the change
	//
	// Current context window usage statistics including token and message counts
	//
	// Empty payload; the event signals that LLM-powered conversation compaction has begun
	//
	// Conversation compaction results including success status, metrics, and optional error
	// details
	//
	// Task completion notification with optional summary from the agent
	//
	// User message content with optional attachments, source information, and interaction
	// metadata
	//
	// Empty payload; the event signals that the pending message queue has changed
	//
	// Turn initialization metadata including identifier and interaction tracking
	//
	// Agent intent description for current activity or plan
	//
	// Assistant reasoning content for timeline display with complete thinking text
	//
	// Streaming reasoning delta for incremental extended thinking updates
	//
	// Streaming response progress with cumulative byte count
	//
	// Assistant response containing text content, optional tool requests, and interaction
	// metadata
	//
	// Streaming assistant message delta for incremental response updates
	//
	// Turn completion metadata including the turn identifier
	//
	// LLM API call usage metrics including tokens, costs, quotas, and billing information
	//
	// Turn abort information including the reason for termination
	//
	// User-initiated tool invocation request with tool name and arguments
	//
	// Tool execution startup details including MCP server information when applicable
	//
	// Streaming tool execution output for incremental result display
	//
	// Tool execution progress notification with status message
	//
	// Tool execution completion results including success status, detailed output, and error
	// information
	//
	// Skill invocation details including content, allowed tools, and plugin metadata
	//
	// Sub-agent startup details including parent tool call and agent information
	//
	// Sub-agent completion details for successful execution
	//
	// Sub-agent failure details including error message and agent information
	//
	// Custom agent selection details including name and available tools
	//
	// Empty payload; the event signals that the custom agent was deselected, returning to the
	// default agent
	//
	// Hook invocation start details including type and input data
	//
	// Hook invocation completion details including output, success status, and error
	// information
	//
	// System or developer message content with role and optional template metadata
	//
	// System-generated notification for runtime events like background task completion
	//
	// Permission request notification requiring client approval with request details
	//
	// Permission request completion notification signaling UI dismissal
	//
	// User input request notification with question and optional predefined choices
	//
	// User input request completion notification signaling UI dismissal
	//
	// Structured form elicitation request with JSON schema definition for form fields
	//
	// Elicitation request completion notification signaling UI dismissal
	//
	// External tool invocation request for client-side tool execution
	//
	// External tool completion notification signaling UI dismissal
	//
	// Queued slash command dispatch request for client execution
	//
	// Queued command completion notification signaling UI dismissal
	//
	// Plan approval request with plan content and available user actions
	//
	// Plan mode exit completion notification signaling UI dismissal
	Data Data `json:"data"`
	// When true, the event is transient and not persisted to the session event log on disk
	Ephemeral *bool `json:"ephemeral,omitempty"`
	// Unique event identifier (UUID v4), generated when the event is emitted
	ID string `json:"id"`
	// ID of the chronologically preceding event in the session, forming a linked chain. Null
	// for the first event.
	ParentID *string `json:"parentId"`
	// ISO 8601 timestamp when the event was created
	Timestamp time.Time        `json:"timestamp"`
	Type      SessionEventType `json:"type"`
}

// Session initialization metadata including context and configuration
//
// # Session resume metadata including current context and event count
//
// # Error details for timeline display including message and optional diagnostic information
//
// Payload indicating the agent is idle; includes any background tasks still in flight
//
// # Session title change payload containing the new display title
//
// # Informational message for timeline display with categorization
//
// # Warning message for timeline display with categorization
//
// # Model change details including previous and new model identifiers
//
// # Agent mode change details including previous and new modes
//
// # Plan file operation details indicating what changed
//
// # Workspace file change details including path and operation type
//
// # Session handoff metadata including source, context, and repository information
//
// # Conversation truncation statistics including token counts and removed content metrics
//
// # Session rewind details including target event and count of removed events
//
// Session termination metrics including usage statistics, code changes, and shutdown
// reason
//
// # Updated working directory and git context after the change
//
// # Current context window usage statistics including token and message counts
//
// Empty payload; the event signals that LLM-powered conversation compaction has begun
//
// Conversation compaction results including success status, metrics, and optional error
// details
//
// # Task completion notification with optional summary from the agent
//
// User message content with optional attachments, source information, and interaction
// metadata
//
// Empty payload; the event signals that the pending message queue has changed
//
// # Turn initialization metadata including identifier and interaction tracking
//
// # Agent intent description for current activity or plan
//
// # Assistant reasoning content for timeline display with complete thinking text
//
// # Streaming reasoning delta for incremental extended thinking updates
//
// # Streaming response progress with cumulative byte count
//
// Assistant response containing text content, optional tool requests, and interaction
// metadata
//
// # Streaming assistant message delta for incremental response updates
//
// # Turn completion metadata including the turn identifier
//
// # LLM API call usage metrics including tokens, costs, quotas, and billing information
//
// # Turn abort information including the reason for termination
//
// # User-initiated tool invocation request with tool name and arguments
//
// # Tool execution startup details including MCP server information when applicable
//
// # Streaming tool execution output for incremental result display
//
// # Tool execution progress notification with status message
//
// Tool execution completion results including success status, detailed output, and error
// information
//
// # Skill invocation details including content, allowed tools, and plugin metadata
//
// # Sub-agent startup details including parent tool call and agent information
//
// # Sub-agent completion details for successful execution
//
// # Sub-agent failure details including error message and agent information
//
// # Custom agent selection details including name and available tools
//
// Empty payload; the event signals that the custom agent was deselected, returning to the
// default agent
//
// # Hook invocation start details including type and input data
//
// Hook invocation completion details including output, success status, and error
// information
//
// # System or developer message content with role and optional template metadata
//
// # System-generated notification for runtime events like background task completion
//
// # Permission request notification requiring client approval with request details
//
// # Permission request completion notification signaling UI dismissal
//
// # User input request notification with question and optional predefined choices
//
// # User input request completion notification signaling UI dismissal
//
// # Structured form elicitation request with JSON schema definition for form fields
//
// # Elicitation request completion notification signaling UI dismissal
//
// # External tool invocation request for client-side tool execution
//
// # External tool completion notification signaling UI dismissal
//
// # Queued slash command dispatch request for client execution
//
// # Queued command completion notification signaling UI dismissal
//
// # Plan approval request with plan content and available user actions
//
// Plan mode exit completion notification signaling UI dismissal
type Data struct {
	// Whether the session was already in use by another client at start time
	//
	// Whether the session was already in use by another client at resume time
	AlreadyInUse *bool `json:"alreadyInUse,omitempty"`
	// Working directory and git context at session start
	//
	// Updated working directory and git context at resume time
	//
	// Additional context information for the handoff
	Context *ContextUnion `json:"context"`
	// Version string of the Copilot application
	CopilotVersion *string `json:"copilotVersion,omitempty"`
	// Identifier of the software producing the events (e.g., "copilot-agent")
	Producer *string `json:"producer,omitempty"`
	// Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high",
	// "xhigh")
	//
	// Reasoning effort level after the model change, if applicable
	ReasoningEffort *string `json:"reasoningEffort,omitempty"`
	// Model selected at session creation time, if any
	//
	// Model currently selected at resume time
	SelectedModel *string `json:"selectedModel,omitempty"`
	// Unique identifier for the session
	//
	// Session ID that this external tool request belongs to
	SessionID *string `json:"sessionId,omitempty"`
	// ISO 8601 timestamp when the session was created
	StartTime *time.Time `json:"startTime,omitempty"`
	// Schema version number for the session event format
	Version *float64 `json:"version,omitempty"`
	// Total number of persisted events in the session at the time of resume
	EventCount *float64 `json:"eventCount,omitempty"`
	// ISO 8601 timestamp when the session was resumed
	ResumeTime *time.Time `json:"resumeTime,omitempty"`
	// Category of error (e.g., "authentication", "authorization", "quota", "rate_limit",
	// "query")
	ErrorType *string `json:"errorType,omitempty"`
	// Human-readable error message
	//
	// Human-readable informational message for display in the timeline
	//
	// Human-readable warning message for display in the timeline
	//
	// Message describing what information is needed from the user
	Message *string `json:"message,omitempty"`
	// GitHub request tracing ID (x-github-request-id header) for correlating with server-side
	// logs
	//
	// GitHub request tracing ID (x-github-request-id header) for server-side log correlation
	ProviderCallID *string `json:"providerCallId,omitempty"`
	// Error stack trace, when available
	Stack *string `json:"stack,omitempty"`
	// HTTP status code from the upstream request, if applicable
	StatusCode *int64 `json:"statusCode,omitempty"`
	// Background tasks still running when the agent became idle
	BackgroundTasks *BackgroundTasks `json:"backgroundTasks,omitempty"`
	// The new display title for the session
	Title *string `json:"title,omitempty"`
	// Category of informational message (e.g., "notification", "timing", "context_window",
	// "mcp", "snapshot", "configuration", "authentication", "model")
	InfoType *string `json:"infoType,omitempty"`
	// Category of warning (e.g., "subscription", "policy", "mcp")
	WarningType *string `json:"warningType,omitempty"`
	// Newly selected model identifier
	NewModel *string `json:"newModel,omitempty"`
	// Model that was previously selected, if any
	PreviousModel *string `json:"previousModel,omitempty"`
	// Reasoning effort level before the model change, if applicable
	PreviousReasoningEffort *string `json:"previousReasoningEffort,omitempty"`
	// Agent mode after the change (e.g., "interactive", "plan", "autopilot")
	NewMode *string `json:"newMode,omitempty"`
	// Agent mode before the change (e.g., "interactive", "plan", "autopilot")
	PreviousMode *string `json:"previousMode,omitempty"`
	// The type of operation performed on the plan file
	//
	// Whether the file was newly created or updated
	Operation *Operation `json:"operation,omitempty"`
	// Relative path within the session workspace files directory
	//
	// File path to the SKILL.md definition
	Path *string `json:"path,omitempty"`
	// ISO 8601 timestamp when the handoff occurred
	HandoffTime *time.Time `json:"handoffTime,omitempty"`
	// Session ID of the remote session being handed off
	RemoteSessionID *string `json:"remoteSessionId,omitempty"`
	// Repository context for the handed-off session
	//
	// Repository identifier derived from the git remote URL ("owner/name" for GitHub,
	// "org/project/repo" for Azure DevOps)
	Repository *RepositoryUnion `json:"repository"`
	// Origin type of the session being handed off
	SourceType *SourceType `json:"sourceType,omitempty"`
	// Summary of the work done in the source session
	//
	// Optional summary of the completed task, provided by the agent
	//
	// Summary of the plan that was created
	Summary *string `json:"summary,omitempty"`
	// Number of messages removed by truncation
	MessagesRemovedDuringTruncation *float64 `json:"messagesRemovedDuringTruncation,omitempty"`
	// Identifier of the component that performed truncation (e.g., "BasicTruncator")
	PerformedBy *string `json:"performedBy,omitempty"`
	// Number of conversation messages after truncation
	PostTruncationMessagesLength *float64 `json:"postTruncationMessagesLength,omitempty"`
	// Total tokens in conversation messages after truncation
	PostTruncationTokensInMessages *float64 `json:"postTruncationTokensInMessages,omitempty"`
	// Number of conversation messages before truncation
	PreTruncationMessagesLength *float64 `json:"preTruncationMessagesLength,omitempty"`
	// Total tokens in conversation messages before truncation
	PreTruncationTokensInMessages *float64 `json:"preTruncationTokensInMessages,omitempty"`
	// Maximum token count for the model's context window
	TokenLimit *float64 `json:"tokenLimit,omitempty"`
	// Number of tokens removed by truncation
	TokensRemovedDuringTruncation *float64 `json:"tokensRemovedDuringTruncation,omitempty"`
	// Number of events that were removed by the rewind
	EventsRemoved *float64 `json:"eventsRemoved,omitempty"`
	// Event ID that was rewound to; all events after this one were removed
	UpToEventID *string `json:"upToEventId,omitempty"`
	// Aggregate code change metrics for the session
	CodeChanges *CodeChanges `json:"codeChanges,omitempty"`
	// Model that was selected at the time of shutdown
	CurrentModel *string `json:"currentModel,omitempty"`
	// Error description when shutdownType is "error"
	ErrorReason *string `json:"errorReason,omitempty"`
	// Per-model usage breakdown, keyed by model identifier
	ModelMetrics map[string]ModelMetric `json:"modelMetrics,omitempty"`
	// Unix timestamp (milliseconds) when the session started
	SessionStartTime *float64 `json:"sessionStartTime,omitempty"`
	// Whether the session ended normally ("routine") or due to a crash/fatal error ("error")
	ShutdownType *ShutdownType `json:"shutdownType,omitempty"`
	// Cumulative time spent in API calls during the session, in milliseconds
	TotalAPIDurationMS *float64 `json:"totalApiDurationMs,omitempty"`
	// Total number of premium API requests used during the session
	TotalPremiumRequests *float64 `json:"totalPremiumRequests,omitempty"`
	// Base commit of current git branch at session start time
	BaseCommit *string `json:"baseCommit,omitempty"`
	// Current git branch name
	Branch *string `json:"branch,omitempty"`
	// Current working directory path
	Cwd *string `json:"cwd,omitempty"`
	// Root directory of the git repository, resolved via git rev-parse
	GitRoot *string `json:"gitRoot,omitempty"`
	// Head commit of current git branch at session start time
	HeadCommit *string `json:"headCommit,omitempty"`
	// Hosting platform type of the repository (github or ado)
	HostType *HostType `json:"hostType,omitempty"`
	// Current number of tokens in the context window
	CurrentTokens *float64 `json:"currentTokens,omitempty"`
	// Current number of messages in the conversation
	MessagesLength *float64 `json:"messagesLength,omitempty"`
	// Checkpoint snapshot number created for recovery
	CheckpointNumber *float64 `json:"checkpointNumber,omitempty"`
	// File path where the checkpoint was stored
	CheckpointPath *string `json:"checkpointPath,omitempty"`
	// Token usage breakdown for the compaction LLM call
	CompactionTokensUsed *CompactionTokensUsed `json:"compactionTokensUsed,omitempty"`
	// Error message if compaction failed
	//
	// Error details when the tool execution failed
	//
	// Error message describing why the sub-agent failed
	//
	// Error details when the hook failed
	Error *ErrorUnion `json:"error"`
	// Number of messages removed during compaction
	MessagesRemoved *float64 `json:"messagesRemoved,omitempty"`
	// Total tokens in conversation after compaction
	PostCompactionTokens *float64 `json:"postCompactionTokens,omitempty"`
	// Number of messages before compaction
	PreCompactionMessagesLength *float64 `json:"preCompactionMessagesLength,omitempty"`
	// Total tokens in conversation before compaction
	PreCompactionTokens *float64 `json:"preCompactionTokens,omitempty"`
	// GitHub request tracing ID (x-github-request-id header) for the compaction LLM call
	//
	// Unique identifier for this permission request; used to respond via
	// session.respondToPermission()
	//
	// Request ID of the resolved permission request; clients should dismiss any UI for this
	// request
	//
	// Unique identifier for this input request; used to respond via
	// session.respondToUserInput()
	//
	// Request ID of the resolved user input request; clients should dismiss any UI for this
	// request
	//
	// Unique identifier for this elicitation request; used to respond via
	// session.respondToElicitation()
	//
	// Request ID of the resolved elicitation request; clients should dismiss any UI for this
	// request
	//
	// Unique identifier for this request; used to respond via session.respondToExternalTool()
	//
	// Request ID of the resolved external tool request; clients should dismiss any UI for this
	// request
	//
	// Unique identifier for this request; used to respond via session.respondToQueuedCommand()
	//
	// Request ID of the resolved command request; clients should dismiss any UI for this
	// request
	//
	// Unique identifier for this request; used to respond via session.respondToExitPlanMode()
	//
	// Request ID of the resolved exit plan mode request; clients should dismiss any UI for this
	// request
	RequestID *string `json:"requestId,omitempty"`
	// Whether compaction completed successfully
	//
	// Whether the tool execution completed successfully
	//
	// Whether the hook completed successfully
	Success *bool `json:"success,omitempty"`
	// LLM-generated summary of the compacted conversation history
	SummaryContent *string `json:"summaryContent,omitempty"`
	// Number of tokens removed during compaction
	TokensRemoved *float64 `json:"tokensRemoved,omitempty"`
	// The agent mode that was active when this message was sent
	AgentMode *AgentMode `json:"agentMode,omitempty"`
	// Files, selections, or GitHub references attached to the message
	Attachments []Attachment `json:"attachments,omitempty"`
	// The user's message text as displayed in the timeline
	//
	// The complete extended thinking text from the model
	//
	// The assistant's text response content
	//
	// Full content of the skill file, injected into the conversation for the model
	//
	// The system or developer prompt text
	//
	// The notification text, typically wrapped in <system_notification> XML tags
	Content *string `json:"content,omitempty"`
	// CAPI interaction ID for correlating this user message with its turn
	//
	// CAPI interaction ID for correlating this turn with upstream telemetry
	//
	// CAPI interaction ID for correlating this message with upstream telemetry
	//
	// CAPI interaction ID for correlating this tool execution with upstream telemetry
	InteractionID *string `json:"interactionId,omitempty"`
	// Origin of this message, used for timeline filtering and telemetry (e.g., "user",
	// "autopilot", "skill", or "command")
	Source *Source `json:"source,omitempty"`
	// Transformed version of the message sent to the model, with XML wrapping, timestamps, and
	// other augmentations for prompt caching
	TransformedContent *string `json:"transformedContent,omitempty"`
	// Identifier for this turn within the agentic loop, typically a stringified turn number
	//
	// Identifier of the turn that has ended, matching the corresponding assistant.turn_start
	// event
	TurnID *string `json:"turnId,omitempty"`
	// Short description of what the agent is currently doing or planning to do
	Intent *string `json:"intent,omitempty"`
	// Unique identifier for this reasoning block
	//
	// Reasoning block ID this delta belongs to, matching the corresponding assistant.reasoning
	// event
	ReasoningID *string `json:"reasoningId,omitempty"`
	// Incremental text chunk to append to the reasoning content
	//
	// Incremental text chunk to append to the message content
	DeltaContent *string `json:"deltaContent,omitempty"`
	// Cumulative total bytes received from the streaming response so far
	TotalResponseSizeBytes *float64 `json:"totalResponseSizeBytes,omitempty"`
	// Encrypted reasoning content from OpenAI models. Session-bound and stripped on resume.
	EncryptedContent *string `json:"encryptedContent,omitempty"`
	// Unique identifier for this assistant message
	//
	// Message ID this delta belongs to, matching the corresponding assistant.message event
	MessageID *string `json:"messageId,omitempty"`
	// Actual output token count from the API response (completion_tokens), used for accurate
	// token accounting
	//
	// Number of output tokens produced
	OutputTokens *float64 `json:"outputTokens,omitempty"`
	// Tool call ID of the parent tool invocation when this event originates from a sub-agent
	//
	// Parent tool call ID when this usage originates from a sub-agent
	ParentToolCallID *string `json:"parentToolCallId,omitempty"`
	// Generation phase for phased-output models (e.g., thinking vs. response phases)
	Phase *string `json:"phase,omitempty"`
	// Opaque/encrypted extended thinking data from Anthropic models. Session-bound and stripped
	// on resume.
	ReasoningOpaque *string `json:"reasoningOpaque,omitempty"`
	// Readable reasoning text from the model's extended thinking
	ReasoningText *string `json:"reasoningText,omitempty"`
	// Tool invocations requested by the assistant in this message
	ToolRequests []ToolRequest `json:"toolRequests,omitempty"`
	// Completion ID from the model provider (e.g., chatcmpl-abc123)
	APICallID *string `json:"apiCallId,omitempty"`
	// Number of tokens read from prompt cache
	CacheReadTokens *float64 `json:"cacheReadTokens,omitempty"`
	// Number of tokens written to prompt cache
	CacheWriteTokens *float64 `json:"cacheWriteTokens,omitempty"`
	// Per-request cost and usage data from the CAPI copilot_usage response field
	CopilotUsage *CopilotUsage `json:"copilotUsage,omitempty"`
	// Model multiplier cost for billing purposes
	Cost *float64 `json:"cost,omitempty"`
	// Duration of the API call in milliseconds
	Duration *float64 `json:"duration,omitempty"`
	// What initiated this API call (e.g., "sub-agent"); absent for user-initiated calls
	Initiator *string `json:"initiator,omitempty"`
	// Number of input tokens consumed
	InputTokens *float64 `json:"inputTokens,omitempty"`
	// Model identifier used for this API call
	//
	// Model identifier that generated this tool call
	Model *string `json:"model,omitempty"`
	// Per-quota resource usage snapshots, keyed by quota identifier
	QuotaSnapshots map[string]QuotaSnapshot `json:"quotaSnapshots,omitempty"`
	// Reason the current turn was aborted (e.g., "user initiated")
	Reason *string `json:"reason,omitempty"`
	// Arguments for the tool invocation
	//
	// Arguments passed to the tool
	//
	// Arguments to pass to the external tool
	Arguments interface{} `json:"arguments"`
	// Unique identifier for this tool call
	//
	// Tool call ID this partial result belongs to
	//
	// Tool call ID this progress notification belongs to
	//
	// Unique identifier for the completed tool call
	//
	// Tool call ID of the parent tool invocation that spawned this sub-agent
	//
	// Tool call ID assigned to this external tool invocation
	ToolCallID *string `json:"toolCallId,omitempty"`
	// Name of the tool the user wants to invoke
	//
	// Name of the tool being executed
	//
	// Name of the external tool to invoke
	ToolName *string `json:"toolName,omitempty"`
	// Name of the MCP server hosting this tool, when the tool is an MCP tool
	MCPServerName *string `json:"mcpServerName,omitempty"`
	// Original tool name on the MCP server, when the tool is an MCP tool
	MCPToolName *string `json:"mcpToolName,omitempty"`
	// Incremental output chunk from the running tool
	PartialOutput *string `json:"partialOutput,omitempty"`
	// Human-readable progress status message (e.g., from an MCP server)
	ProgressMessage *string `json:"progressMessage,omitempty"`
	// Whether this tool call was explicitly requested by the user rather than the assistant
	IsUserRequested *bool `json:"isUserRequested,omitempty"`
	// Tool execution result on success
	//
	// The result of the permission request
	Result *Result `json:"result,omitempty"`
	// Tool-specific telemetry data (e.g., CodeQL check counts, grep match counts)
	ToolTelemetry map[string]interface{} `json:"toolTelemetry,omitempty"`
	// Tool names that should be auto-approved when this skill is active
	AllowedTools []string `json:"allowedTools,omitempty"`
	// Name of the invoked skill
	//
	// Optional name identifier for the message source
	Name *string `json:"name,omitempty"`
	// Name of the plugin this skill originated from, when applicable
	PluginName *string `json:"pluginName,omitempty"`
	// Version of the plugin this skill originated from, when applicable
	PluginVersion *string `json:"pluginVersion,omitempty"`
	// Description of what the sub-agent does
	AgentDescription *string `json:"agentDescription,omitempty"`
	// Human-readable display name of the sub-agent
	//
	// Human-readable display name of the selected custom agent
	AgentDisplayName *string `json:"agentDisplayName,omitempty"`
	// Internal name of the sub-agent
	//
	// Internal name of the selected custom agent
	AgentName *string `json:"agentName,omitempty"`
	// List of tool names available to this agent, or null for all tools
	Tools []string `json:"tools"`
	// Unique identifier for this hook invocation
	//
	// Identifier matching the corresponding hook.start event
	HookInvocationID *string `json:"hookInvocationId,omitempty"`
	// Type of hook being invoked (e.g., "preToolUse", "postToolUse", "sessionStart")
	//
	// Type of hook that was invoked (e.g., "preToolUse", "postToolUse", "sessionStart")
	HookType *string `json:"hookType,omitempty"`
	// Input data passed to the hook
	Input interface{} `json:"input"`
	// Output data produced by the hook
	Output interface{} `json:"output"`
	// Metadata about the prompt template and its construction
	Metadata *Metadata `json:"metadata,omitempty"`
	// Message role: "system" for system prompts, "developer" for developer-injected instructions
	Role *Role `json:"role,omitempty"`
	// Structured metadata identifying what triggered this notification
	Kind *KindClass `json:"kind,omitempty"`
	// Details of the permission being requested
	PermissionRequest *PermissionRequest `json:"permissionRequest,omitempty"`
	// Whether the user can provide a free-form text response in addition to predefined choices
	AllowFreeform *bool `json:"allowFreeform,omitempty"`
	// Predefined choices for the user to select from, if applicable
	Choices []string `json:"choices,omitempty"`
	// The question or prompt to present to the user
	Question *string `json:"question,omitempty"`
	// Elicitation mode; currently only "form" is supported. Defaults to "form" when absent.
	Mode *Mode `json:"mode,omitempty"`
	// JSON Schema describing the form fields to present to the user
	RequestedSchema *RequestedSchema `json:"requestedSchema,omitempty"`
	// W3C Trace Context traceparent header for the execute_tool span
	Traceparent *string `json:"traceparent,omitempty"`
	// W3C Trace Context tracestate header for the execute_tool span
	Tracestate *string `json:"tracestate,omitempty"`
	// The slash command text to be executed (e.g., /help, /clear)
	Command *string `json:"command,omitempty"`
	// Available actions the user can take (e.g., approve, edit, reject)
	Actions []string `json:"actions,omitempty"`
	// Full content of the plan file
	PlanContent *string `json:"planContent,omitempty"`
	// The recommended action for the user to take
	RecommendedAction *string `json:"recommendedAction,omitempty"`
}

// A user message attachment — a file, directory, code selection, blob, or GitHub reference
//
// # File attachment
//
// # Directory attachment
//
// # Code selection attachment from an editor
//
// # GitHub issue, pull request, or discussion reference
//
// Blob attachment with inline base64-encoded data
type Attachment struct {
	// User-facing display name for the attachment
	//
	// User-facing display name for the selection
	DisplayName *string `json:"displayName,omitempty"`
	// Optional line range to scope the attachment to a specific section of the file
	LineRange *LineRange `json:"lineRange,omitempty"`
	// Absolute file path
	//
	// Absolute directory path
	Path *string `json:"path,omitempty"`
	// Attachment type discriminator
	Type AttachmentType `json:"type"`
	// Absolute path to the file containing the selection
	FilePath *string `json:"filePath,omitempty"`
	// Position range of the selection within the file
	Selection *SelectionClass `json:"selection,omitempty"`
	// The selected text content
	Text *string `json:"text,omitempty"`
	// Issue, pull request, or discussion number
	Number *float64 `json:"number,omitempty"`
	// Type of GitHub reference
	ReferenceType *ReferenceType `json:"referenceType,omitempty"`
	// Current state of the referenced item (e.g., open, closed, merged)
	State *string `json:"state,omitempty"`
	// Title of the referenced item
	Title *string `json:"title,omitempty"`
	// URL to the referenced item on GitHub
	URL *string `json:"url,omitempty"`
	// Base64-encoded content
	Data *string `json:"data,omitempty"`
	// MIME type of the inline data
	MIMEType *string `json:"mimeType,omitempty"`
}

// Optional line range to scope the attachment to a specific section of the file
type LineRange struct {
	// End line number (1-based, inclusive)
	End float64 `json:"end"`
	// Start line number (1-based)
	Start float64 `json:"start"`
}

// Position range of the selection within the file
type SelectionClass struct {
	// End position of the selection
	End End `json:"end"`
	// Start position of the selection
	Start Start `json:"start"`
}

// End position of the selection
type End struct {
	// End character offset within the line (0-based)
	Character float64 `json:"character"`
	// End line number (0-based)
	Line float64 `json:"line"`
}

// Start position of the selection
type Start struct {
	// Start character offset within the line (0-based)
	Character float64 `json:"character"`
	// Start line number (0-based)
	Line float64 `json:"line"`
}

// Background tasks still running when the agent became idle
type BackgroundTasks struct {
	// Currently running background agents
	Agents []Agent `json:"agents"`
	// Currently running background shell commands
	Shells []Shell `json:"shells"`
}

// A background agent task
type Agent struct {
	// Unique identifier of the background agent
	AgentID string `json:"agentId"`
	// Type of the background agent
	AgentType string `json:"agentType"`
	// Human-readable description of the agent task
	Description *string `json:"description,omitempty"`
}

// A background shell command
type Shell struct {
	// Human-readable description of the shell command
	Description *string `json:"description,omitempty"`
	// Unique identifier of the background shell
	ShellID string `json:"shellId"`
}

// Aggregate code change metrics for the session
type CodeChanges struct {
	// List of file paths that were modified during the session
	FilesModified []string `json:"filesModified"`
	// Total number of lines added during the session
	LinesAdded float64 `json:"linesAdded"`
	// Total number of lines removed during the session
	LinesRemoved float64 `json:"linesRemoved"`
}

// Token usage breakdown for the compaction LLM call
type CompactionTokensUsed struct {
	// Cached input tokens reused in the compaction LLM call
	CachedInput float64 `json:"cachedInput"`
	// Input tokens consumed by the compaction LLM call
	Input float64 `json:"input"`
	// Output tokens produced by the compaction LLM call
	Output float64 `json:"output"`
}

// Working directory and git context at session start
//
// Updated working directory and git context at resume time
type ContextClass struct {
	// Base commit of current git branch at session start time
	BaseCommit *string `json:"baseCommit,omitempty"`
	// Current git branch name
	Branch *string `json:"branch,omitempty"`
	// Current working directory path
	Cwd string `json:"cwd"`
	// Root directory of the git repository, resolved via git rev-parse
	GitRoot *string `json:"gitRoot,omitempty"`
	// Head commit of current git branch at session start time
	HeadCommit *string `json:"headCommit,omitempty"`
	// Hosting platform type of the repository (github or ado)
	HostType *HostType `json:"hostType,omitempty"`
	// Repository identifier derived from the git remote URL ("owner/name" for GitHub,
	// "org/project/repo" for Azure DevOps)
	Repository *string `json:"repository,omitempty"`
}

// Per-request cost and usage data from the CAPI copilot_usage response field
type CopilotUsage struct {
	// Itemized token usage breakdown
	TokenDetails []TokenDetail `json:"tokenDetails"`
	// Total cost in nano-AIU (AI Units) for this request
	TotalNanoAiu float64 `json:"totalNanoAiu"`
}

// Token usage detail for a single billing category
type TokenDetail struct {
	// Number of tokens in this billing batch
	BatchSize float64 `json:"batchSize"`
	// Cost per batch of tokens
	CostPerBatch float64 `json:"costPerBatch"`
	// Total token count for this entry
	TokenCount float64 `json:"tokenCount"`
	// Token category (e.g., "input", "output")
	TokenType string `json:"tokenType"`
}

// Error details when the tool execution failed
//
// Error details when the hook failed
type ErrorClass struct {
	// Machine-readable error code
	Code *string `json:"code,omitempty"`
	// Human-readable error message
	Message string `json:"message"`
	// Error stack trace, when available
	Stack *string `json:"stack,omitempty"`
}

// Structured metadata identifying what triggered this notification
type KindClass struct {
	// Unique identifier of the background agent
	AgentID *string `json:"agentId,omitempty"`
	// Type of the agent (e.g., explore, task, general-purpose)
	AgentType *string `json:"agentType,omitempty"`
	// Human-readable description of the agent task
	//
	// Human-readable description of the command
	Description *string `json:"description,omitempty"`
	// The full prompt given to the background agent
	Prompt *string `json:"prompt,omitempty"`
	// Whether the agent completed successfully or failed
	Status *Status  `json:"status,omitempty"`
	Type   KindType `json:"type"`
	// Exit code of the shell command, if available
	ExitCode *float64 `json:"exitCode,omitempty"`
	// Unique identifier of the shell session
	//
	// Unique identifier of the detached shell session
	ShellID *string `json:"shellId,omitempty"`
}

// Metadata about the prompt template and its construction
type Metadata struct {
	// Version identifier of the prompt template used
	PromptVersion *string `json:"promptVersion,omitempty"`
	// Template variables used when constructing the prompt
	Variables map[string]interface{} `json:"variables,omitempty"`
}

type ModelMetric struct {
	// Request count and cost metrics
	Requests Requests `json:"requests"`
	// Token usage breakdown
	Usage Usage `json:"usage"`
}

// Request count and cost metrics
type Requests struct {
	// Cumulative cost multiplier for requests to this model
	Cost float64 `json:"cost"`
	// Total number of API requests made to this model
	Count float64 `json:"count"`
}

// Token usage breakdown
type Usage struct {
	// Total tokens read from prompt cache across all requests
	CacheReadTokens float64 `json:"cacheReadTokens"`
	// Total tokens written to prompt cache across all requests
	CacheWriteTokens float64 `json:"cacheWriteTokens"`
	// Total input tokens consumed across all requests to this model
	InputTokens float64 `json:"inputTokens"`
	// Total output tokens produced across all requests to this model
	OutputTokens float64 `json:"outputTokens"`
}

// Details of the permission being requested
//
// # Shell command permission request
//
// # File write permission request
//
// # File or directory read permission request
//
// # MCP tool invocation permission request
//
// # URL access permission request
//
// # Memory storage permission request
//
// # Custom tool invocation permission request
//
// Hook confirmation permission request
type PermissionRequest struct {
	// Whether the UI can offer session-wide approval for this command pattern
	CanOfferSessionApproval *bool `json:"canOfferSessionApproval,omitempty"`
	// Parsed command identifiers found in the command text
	Commands []CommandElement `json:"commands,omitempty"`
	// The complete shell command text to be executed
	FullCommandText *string `json:"fullCommandText,omitempty"`
	// Whether the command includes a file write redirection (e.g., > or >>)
	HasWriteFileRedirection *bool `json:"hasWriteFileRedirection,omitempty"`
	// Human-readable description of what the command intends to do
	//
	// Human-readable description of the intended file change
	//
	// Human-readable description of why the file is being read
	//
	// Human-readable description of why the URL is being accessed
	Intention *string `json:"intention,omitempty"`
	// Permission kind discriminator
	Kind PermissionRequestKind `json:"kind"`
	// File paths that may be read or written by the command
	PossiblePaths []string `json:"possiblePaths,omitempty"`
	// URLs that may be accessed by the command
	PossibleUrls []PossibleURL `json:"possibleUrls,omitempty"`
	// Tool call ID that triggered this permission request
	ToolCallID *string `json:"toolCallId,omitempty"`
	// Optional warning message about risks of running this command
	Warning *string `json:"warning,omitempty"`
	// Unified diff showing the proposed changes
	Diff *string `json:"diff,omitempty"`
	// Path of the file being written to
	FileName *string `json:"fileName,omitempty"`
	// Complete new file contents for newly created files
	NewFileContents *string `json:"newFileContents,omitempty"`
	// Path of the file or directory being read
	Path *string `json:"path,omitempty"`
	// Arguments to pass to the MCP tool
	//
	// Arguments to pass to the custom tool
	Args interface{} `json:"args"`
	// Whether this MCP tool is read-only (no side effects)
	ReadOnly *bool `json:"readOnly,omitempty"`
	// Name of the MCP server providing the tool
	ServerName *string `json:"serverName,omitempty"`
	// Internal name of the MCP tool
	//
	// Name of the custom tool
	//
	// Name of the tool the hook is gating
	ToolName *string `json:"toolName,omitempty"`
	// Human-readable title of the MCP tool
	ToolTitle *string `json:"toolTitle,omitempty"`
	// URL to be fetched
	URL *string `json:"url,omitempty"`
	// Source references for the stored fact
	Citations *string `json:"citations,omitempty"`
	// The fact or convention being stored
	Fact *string `json:"fact,omitempty"`
	// Topic or subject of the memory being stored
	Subject *string `json:"subject,omitempty"`
	// Description of what the custom tool does
	ToolDescription *string `json:"toolDescription,omitempty"`
	// Optional message from the hook explaining why confirmation is needed
	HookMessage *string `json:"hookMessage,omitempty"`
	// Arguments of the tool call being gated
	ToolArgs interface{} `json:"toolArgs"`
}

type CommandElement struct {
	// Command identifier (e.g., executable name)
	Identifier string `json:"identifier"`
	// Whether this command is read-only (no side effects)
	ReadOnly bool `json:"readOnly"`
}

type PossibleURL struct {
	// URL that may be accessed by the command
	URL string `json:"url"`
}

type QuotaSnapshot struct {
	// Total requests allowed by the entitlement
	EntitlementRequests float64 `json:"entitlementRequests"`
	// Whether the user has an unlimited usage entitlement
	IsUnlimitedEntitlement bool `json:"isUnlimitedEntitlement"`
	// Number of requests over the entitlement limit
	Overage float64 `json:"overage"`
	// Whether overage is allowed when quota is exhausted
	OverageAllowedWithExhaustedQuota bool `json:"overageAllowedWithExhaustedQuota"`
	// Percentage of quota remaining (0.0 to 1.0)
	RemainingPercentage float64 `json:"remainingPercentage"`
	// Date when the quota resets
	ResetDate *time.Time `json:"resetDate,omitempty"`
	// Whether usage is still permitted after quota exhaustion
	UsageAllowedWithExhaustedQuota bool `json:"usageAllowedWithExhaustedQuota"`
	// Number of requests already consumed
	UsedRequests float64 `json:"usedRequests"`
}

// Repository context for the handed-off session
type RepositoryClass struct {
	// Git branch name, if applicable
	Branch *string `json:"branch,omitempty"`
	// Repository name
	Name string `json:"name"`
	// Repository owner (user or organization)
	Owner string `json:"owner"`
}

// JSON Schema describing the form fields to present to the user
type RequestedSchema struct {
	// Form field definitions, keyed by field name
	Properties map[string]interface{} `json:"properties"`
	// List of required field names
	Required []string `json:"required,omitempty"`
	// Schema type indicator (always 'object')
	Type RequestedSchemaType `json:"type"`
}

// Tool execution result on success
//
// The result of the permission request
type Result struct {
	// Concise tool result text sent to the LLM for chat completion, potentially truncated for
	// token efficiency
	Content *string `json:"content,omitempty"`
	// Structured content blocks (text, images, audio, resources) returned by the tool in their
	// native format
	Contents []Content `json:"contents,omitempty"`
	// Full detailed tool result for UI/timeline display, preserving complete content such as
	// diffs. Falls back to content when absent.
	DetailedContent *string `json:"detailedContent,omitempty"`
	// The outcome of the permission request
	Kind *ResultKind `json:"kind,omitempty"`
}

// A content block within a tool result, which may be text, terminal output, image, audio,
// or a resource
//
// # Plain text content block
//
// Terminal/shell output content block with optional exit code and working directory
//
// # Image content block with base64-encoded data
//
// # Audio content block with base64-encoded data
//
// # Resource link content block referencing an external resource
//
// Embedded resource content block with inline text or binary data
type Content struct {
	// The text content
	//
	// Terminal/shell output text
	Text *string `json:"text,omitempty"`
	// Content block type discriminator
	Type ContentType `json:"type"`
	// Working directory where the command was executed
	Cwd *string `json:"cwd,omitempty"`
	// Process exit code, if the command has completed
	ExitCode *float64 `json:"exitCode,omitempty"`
	// Base64-encoded image data
	//
	// Base64-encoded audio data
	Data *string `json:"data,omitempty"`
	// MIME type of the image (e.g., image/png, image/jpeg)
	//
	// MIME type of the audio (e.g., audio/wav, audio/mpeg)
	//
	// MIME type of the resource content
	MIMEType *string `json:"mimeType,omitempty"`
	// Human-readable description of the resource
	Description *string `json:"description,omitempty"`
	// Icons associated with this resource
	Icons []Icon `json:"icons,omitempty"`
	// Resource name identifier
	Name *string `json:"name,omitempty"`
	// Size of the resource in bytes
	Size *float64 `json:"size,omitempty"`
	// Human-readable display title for the resource
	Title *string `json:"title,omitempty"`
	// URI identifying the resource
	URI *string `json:"uri,omitempty"`
	// The embedded resource contents, either text or base64-encoded binary
	Resource *ResourceClass `json:"resource,omitempty"`
}

// Icon image for a resource
type Icon struct {
	// MIME type of the icon image
	MIMEType *string `json:"mimeType,omitempty"`
	// Available icon sizes (e.g., ['16x16', '32x32'])
	Sizes []string `json:"sizes,omitempty"`
	// URL or path to the icon image
	Src string `json:"src"`
	// Theme variant this icon is intended for
	Theme *Theme `json:"theme,omitempty"`
}

// The embedded resource contents, either text or base64-encoded binary
type ResourceClass struct {
	// MIME type of the text content
	//
	// MIME type of the blob content
	MIMEType *string `json:"mimeType,omitempty"`
	// Text content of the resource
	Text *string `json:"text,omitempty"`
	// URI identifying the resource
	URI string `json:"uri"`
	// Base64-encoded binary content of the resource
	Blob *string `json:"blob,omitempty"`
}

// A tool invocation request from the assistant
type ToolRequest struct {
	// Arguments to pass to the tool, format depends on the tool
	Arguments interface{} `json:"arguments"`
	// Resolved intention summary describing what this specific call does
	IntentionSummary *string `json:"intentionSummary"`
	// Name of the tool being invoked
	Name string `json:"name"`
	// Unique identifier for this tool call
	ToolCallID string `json:"toolCallId"`
	// Human-readable display title for the tool
	ToolTitle *string `json:"toolTitle,omitempty"`
	// Tool call type: "function" for standard tool calls, "custom" for grammar-based tool
	// calls. Defaults to "function" when absent.
	Type *ToolRequestType `json:"type,omitempty"`
}

// The agent mode that was active when this message was sent
type AgentMode string

const (
	AgentModeAutopilot AgentMode = "autopilot"
	AgentModeShell     AgentMode = "shell"
	Interactive        AgentMode = "interactive"
	Plan               AgentMode = "plan"
)

// Type of GitHub reference
type ReferenceType string

const (
	Discussion ReferenceType = "discussion"
	Issue      ReferenceType = "issue"
	PR         ReferenceType = "pr"
)

type AttachmentType string

const (
	Blob            AttachmentType = "blob"
	Directory       AttachmentType = "directory"
	File            AttachmentType = "file"
	GithubReference AttachmentType = "github_reference"
	Selection       AttachmentType = "selection"
)

// Hosting platform type of the repository (github or ado)
type HostType string

const (
	ADO    HostType = "ado"
	Github HostType = "github"
)

// Whether the agent completed successfully or failed
type Status string

const (
	Completed Status = "completed"
	Failed    Status = "failed"
)

type KindType string

const (
	AgentCompleted         KindType = "agent_completed"
	ShellCompleted         KindType = "shell_completed"
	ShellDetachedCompleted KindType = "shell_detached_completed"
)

type Mode string

const (
	Form Mode = "form"
)

// The type of operation performed on the plan file
//
// Whether the file was newly created or updated
type Operation string

const (
	Create Operation = "create"
	Delete Operation = "delete"
	Update Operation = "update"
)

type PermissionRequestKind string

const (
	CustomTool PermissionRequestKind = "custom-tool"
	Hook       PermissionRequestKind = "hook"
	KindShell  PermissionRequestKind = "shell"
	MCP        PermissionRequestKind = "mcp"
	Memory     PermissionRequestKind = "memory"
	Read       PermissionRequestKind = "read"
	URL        PermissionRequestKind = "url"
	Write      PermissionRequestKind = "write"
)

type RequestedSchemaType string

const (
	Object RequestedSchemaType = "object"
)

// Theme variant this icon is intended for
type Theme string

const (
	Dark  Theme = "dark"
	Light Theme = "light"
)

type ContentType string

const (
	Audio        ContentType = "audio"
	Image        ContentType = "image"
	Resource     ContentType = "resource"
	ResourceLink ContentType = "resource_link"
	Terminal     ContentType = "terminal"
	Text         ContentType = "text"
)

// The outcome of the permission request
type ResultKind string

const (
	Approved                                       ResultKind = "approved"
	DeniedByContentExclusionPolicy                 ResultKind = "denied-by-content-exclusion-policy"
	DeniedByRules                                  ResultKind = "denied-by-rules"
	DeniedInteractivelyByUser                      ResultKind = "denied-interactively-by-user"
	DeniedNoApprovalRuleAndCouldNotRequestFromUser ResultKind = "denied-no-approval-rule-and-could-not-request-from-user"
)

// Message role: "system" for system prompts, "developer" for developer-injected instructions
type Role string

const (
	Developer  Role = "developer"
	RoleSystem Role = "system"
)

// Whether the session ended normally ("routine") or due to a crash/fatal error ("error")
type ShutdownType string

const (
	Error   ShutdownType = "error"
	Routine ShutdownType = "routine"
)

// Origin of this message, used for timeline filtering and telemetry (e.g., "user",
// "autopilot", "skill", or "command")
type Source string

const (
	Command                       Source = "command"
	ImmediatePrompt               Source = "immediate-prompt"
	JITInstruction                Source = "jit-instruction"
	Other                         Source = "other"
	Skill                         Source = "skill"
	SnippyBlocking                Source = "snippy-blocking"
	SourceAutopilot               Source = "autopilot"
	SourceSystem                  Source = "system"
	ThinkingExhaustedContinuation Source = "thinking-exhausted-continuation"
	User                          Source = "user"
)

// Origin type of the session being handed off
type SourceType string

const (
	Local  SourceType = "local"
	Remote SourceType = "remote"
)

// Tool call type: "function" for standard tool calls, "custom" for grammar-based tool
// calls. Defaults to "function" when absent.
type ToolRequestType string

const (
	Custom   ToolRequestType = "custom"
	Function ToolRequestType = "function"
)

type SessionEventType string

const (
	Abort                         SessionEventType = "abort"
	AssistantIntent               SessionEventType = "assistant.intent"
	AssistantMessage              SessionEventType = "assistant.message"
	AssistantMessageDelta         SessionEventType = "assistant.message_delta"
	AssistantReasoning            SessionEventType = "assistant.reasoning"
	AssistantReasoningDelta       SessionEventType = "assistant.reasoning_delta"
	AssistantStreamingDelta       SessionEventType = "assistant.streaming_delta"
	AssistantTurnEnd              SessionEventType = "assistant.turn_end"
	AssistantTurnStart            SessionEventType = "assistant.turn_start"
	AssistantUsage                SessionEventType = "assistant.usage"
	CommandCompleted              SessionEventType = "command.completed"
	CommandQueued                 SessionEventType = "command.queued"
	ElicitationCompleted          SessionEventType = "elicitation.completed"
	ElicitationRequested          SessionEventType = "elicitation.requested"
	ExitPlanModeCompleted         SessionEventType = "exit_plan_mode.completed"
	ExitPlanModeRequested         SessionEventType = "exit_plan_mode.requested"
	ExternalToolCompleted         SessionEventType = "external_tool.completed"
	ExternalToolRequested         SessionEventType = "external_tool.requested"
	HookEnd                       SessionEventType = "hook.end"
	HookStart                     SessionEventType = "hook.start"
	PendingMessagesModified       SessionEventType = "pending_messages.modified"
	PermissionCompleted           SessionEventType = "permission.completed"
	PermissionRequested           SessionEventType = "permission.requested"
	SessionBackgroundTasksChanged SessionEventType = "session.background_tasks_changed"
	SessionCompactionComplete     SessionEventType = "session.compaction_complete"
	SessionCompactionStart        SessionEventType = "session.compaction_start"
	SessionContextChanged         SessionEventType = "session.context_changed"
	SessionError                  SessionEventType = "session.error"
	SessionHandoff                SessionEventType = "session.handoff"
	SessionIdle                   SessionEventType = "session.idle"
	SessionInfo                   SessionEventType = "session.info"
	SessionModeChanged            SessionEventType = "session.mode_changed"
	SessionModelChange            SessionEventType = "session.model_change"
	SessionPlanChanged            SessionEventType = "session.plan_changed"
	SessionResume                 SessionEventType = "session.resume"
	SessionShutdown               SessionEventType = "session.shutdown"
	SessionSnapshotRewind         SessionEventType = "session.snapshot_rewind"
	SessionStart                  SessionEventType = "session.start"
	SessionTaskComplete           SessionEventType = "session.task_complete"
	SessionTitleChanged           SessionEventType = "session.title_changed"
	SessionToolsUpdated           SessionEventType = "session.tools_updated"
	SessionTruncation             SessionEventType = "session.truncation"
	SessionUsageInfo              SessionEventType = "session.usage_info"
	SessionWarning                SessionEventType = "session.warning"
	SessionWorkspaceFileChanged   SessionEventType = "session.workspace_file_changed"
	SkillInvoked                  SessionEventType = "skill.invoked"
	SubagentCompleted             SessionEventType = "subagent.completed"
	SubagentDeselected            SessionEventType = "subagent.deselected"
	SubagentFailed                SessionEventType = "subagent.failed"
	SubagentSelected              SessionEventType = "subagent.selected"
	SubagentStarted               SessionEventType = "subagent.started"
	SystemMessage                 SessionEventType = "system.message"
	SystemNotification            SessionEventType = "system.notification"
	ToolExecutionComplete         SessionEventType = "tool.execution_complete"
	ToolExecutionPartialResult    SessionEventType = "tool.execution_partial_result"
	ToolExecutionProgress         SessionEventType = "tool.execution_progress"
	ToolExecutionStart            SessionEventType = "tool.execution_start"
	ToolUserRequested             SessionEventType = "tool.user_requested"
	UserInputCompleted            SessionEventType = "user_input.completed"
	UserInputRequested            SessionEventType = "user_input.requested"
	UserMessage                   SessionEventType = "user.message"
)

type ContextUnion struct {
	ContextClass *ContextClass
	String       *string
}

func (x *ContextUnion) UnmarshalJSON(data []byte) error {
	x.ContextClass = nil
	var c ContextClass
	object, err := unmarshalUnion(data, nil, nil, nil, &x.String, false, nil, true, &c, false, nil, false, nil, false)
	if err != nil {
		return err
	}
	if object {
		x.ContextClass = &c
	}
	return nil
}

func (x *ContextUnion) MarshalJSON() ([]byte, error) {
	return marshalUnion(nil, nil, nil, x.String, false, nil, x.ContextClass != nil, x.ContextClass, false, nil, false, nil, false)
}

type ErrorUnion struct {
	ErrorClass *ErrorClass
	String     *string
}

func (x *ErrorUnion) UnmarshalJSON(data []byte) error {
	x.ErrorClass = nil
	var c ErrorClass
	object, err := unmarshalUnion(data, nil, nil, nil, &x.String, false, nil, true, &c, false, nil, false, nil, false)
	if err != nil {
		return err
	}
	if object {
		x.ErrorClass = &c
	}
	return nil
}

func (x *ErrorUnion) MarshalJSON() ([]byte, error) {
	return marshalUnion(nil, nil, nil, x.String, false, nil, x.ErrorClass != nil, x.ErrorClass, false, nil, false, nil, false)
}

type RepositoryUnion struct {
	RepositoryClass *RepositoryClass
	String          *string
}

func (x *RepositoryUnion) UnmarshalJSON(data []byte) error {
	x.RepositoryClass = nil
	var c RepositoryClass
	object, err := unmarshalUnion(data, nil, nil, nil, &x.String, false, nil, true, &c, false, nil, false, nil, false)
	if err != nil {
		return err
	}
	if object {
		x.RepositoryClass = &c
	}
	return nil
}

func (x *RepositoryUnion) MarshalJSON() ([]byte, error) {
	return marshalUnion(nil, nil, nil, x.String, false, nil, x.RepositoryClass != nil, x.RepositoryClass, false, nil, false, nil, false)
}

func unmarshalUnion(data []byte, pi **int64, pf **float64, pb **bool, ps **string, haveArray bool, pa interface{}, haveObject bool, pc interface{}, haveMap bool, pm interface{}, haveEnum bool, pe interface{}, nullable bool) (bool, error) {
	if pi != nil {
		*pi = nil
	}
	if pf != nil {
		*pf = nil
	}
	if pb != nil {
		*pb = nil
	}
	if ps != nil {
		*ps = nil
	}

	dec := json.NewDecoder(bytes.NewReader(data))
	dec.UseNumber()
	tok, err := dec.Token()
	if err != nil {
		return false, err
	}

	switch v := tok.(type) {
	case json.Number:
		if pi != nil {
			i, err := v.Int64()
			if err == nil {
				*pi = &i
				return false, nil
			}
		}
		if pf != nil {
			f, err := v.Float64()
			if err == nil {
				*pf = &f
				return false, nil
			}
			return false, errors.New("Unparsable number")
		}
		return false, errors.New("Union does not contain number")
	case float64:
		return false, errors.New("Decoder should not return float64")
	case bool:
		if pb != nil {
			*pb = &v
			return false, nil
		}
		return false, errors.New("Union does not contain bool")
	case string:
		if haveEnum {
			return false, json.Unmarshal(data, pe)
		}
		if ps != nil {
			*ps = &v
			return false, nil
		}
		return false, errors.New("Union does not contain string")
	case nil:
		if nullable {
			return false, nil
		}
		return false, errors.New("Union does not contain null")
	case json.Delim:
		if v == '{' {
			if haveObject {
				return true, json.Unmarshal(data, pc)
			}
			if haveMap {
				return false, json.Unmarshal(data, pm)
			}
			return false, errors.New("Union does not contain object")
		}
		if v == '[' {
			if haveArray {
				return false, json.Unmarshal(data, pa)
			}
			return false, errors.New("Union does not contain array")
		}
		return false, errors.New("Cannot handle delimiter")
	}
	return false, errors.New("Cannot unmarshal union")
}

func marshalUnion(pi *int64, pf *float64, pb *bool, ps *string, haveArray bool, pa interface{}, haveObject bool, pc interface{}, haveMap bool, pm interface{}, haveEnum bool, pe interface{}, nullable bool) ([]byte, error) {
	if pi != nil {
		return json.Marshal(*pi)
	}
	if pf != nil {
		return json.Marshal(*pf)
	}
	if pb != nil {
		return json.Marshal(*pb)
	}
	if ps != nil {
		return json.Marshal(*ps)
	}
	if haveArray {
		return json.Marshal(pa)
	}
	if haveObject {
		return json.Marshal(pc)
	}
	if haveMap {
		return json.Marshal(pm)
	}
	if haveEnum {
		return json.Marshal(pe)
	}
	if nullable {
		return json.Marshal(nil)
	}
	return nil, errors.New("Union must not be null")
}
