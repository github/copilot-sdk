// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package copilot

import (
	"encoding/json"
	"time"
)

// SessionEventData is the interface implemented by all per-event data types.
type SessionEventData interface {
	sessionEventData()
}

// RawSessionEventData holds unparsed JSON data for unrecognized event types.
type RawSessionEventData struct {
	Raw json.RawMessage
}

func (RawSessionEventData) sessionEventData() {}

// MarshalJSON returns the original raw JSON so round-tripping preserves the payload.
func (r RawSessionEventData) MarshalJSON() ([]byte, error) { return r.Raw, nil }

// SessionEvent represents a single session event with a typed data payload.
type SessionEvent struct {
	// Unique event identifier (UUID v4), generated when the event is emitted.
	ID string `json:"id"`
	// ISO 8601 timestamp when the event was created.
	Timestamp time.Time `json:"timestamp"`
	// ID of the preceding event in the session. Null for the first event.
	ParentID *string `json:"parentId"`
	// When true, the event is transient and not persisted.
	Ephemeral *bool `json:"ephemeral,omitempty"`
	// The event type discriminator.
	Type SessionEventType `json:"type"`
	// Typed event payload. Use a type switch to access per-event fields.
	Data SessionEventData `json:"-"`
}

// UnmarshalSessionEvent parses JSON bytes into a SessionEvent.
func UnmarshalSessionEvent(data []byte) (SessionEvent, error) {
	var r SessionEvent
	err := json.Unmarshal(data, &r)
	return r, err
}

// Marshal serializes the SessionEvent to JSON.
func (r *SessionEvent) Marshal() ([]byte, error) {
	return json.Marshal(r)
}

func (e *SessionEvent) UnmarshalJSON(data []byte) error {
	type rawEvent struct {
		ID        string           `json:"id"`
		Timestamp time.Time        `json:"timestamp"`
		ParentID  *string          `json:"parentId"`
		Ephemeral *bool            `json:"ephemeral,omitempty"`
		Type      SessionEventType `json:"type"`
		Data      json.RawMessage  `json:"data"`
	}
	var raw rawEvent
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}
	e.ID = raw.ID
	e.Timestamp = raw.Timestamp
	e.ParentID = raw.ParentID
	e.Ephemeral = raw.Ephemeral
	e.Type = raw.Type

	switch raw.Type {
	case SessionEventTypeSessionStart:
		var d SessionStartData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionResume:
		var d SessionResumeData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionRemoteSteerableChanged:
		var d SessionRemoteSteerableChangedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionError:
		var d SessionErrorData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionIdle:
		var d SessionIdleData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionTitleChanged:
		var d SessionTitleChangedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionInfo:
		var d SessionInfoData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionWarning:
		var d SessionWarningData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionModelChange:
		var d SessionModelChangeData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionModeChanged:
		var d SessionModeChangedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionPlanChanged:
		var d SessionPlanChangedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionWorkspaceFileChanged:
		var d SessionWorkspaceFileChangedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionHandoff:
		var d SessionHandoffData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionTruncation:
		var d SessionTruncationData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionSnapshotRewind:
		var d SessionSnapshotRewindData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionShutdown:
		var d SessionShutdownData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionContextChanged:
		var d SessionContextChangedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionUsageInfo:
		var d SessionUsageInfoData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionCompactionStart:
		var d SessionCompactionStartData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionCompactionComplete:
		var d SessionCompactionCompleteData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionTaskComplete:
		var d SessionTaskCompleteData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeUserMessage:
		var d UserMessageData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypePendingMessagesModified:
		var d PendingMessagesModifiedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeAssistantTurnStart:
		var d AssistantTurnStartData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeAssistantIntent:
		var d AssistantIntentData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeAssistantReasoning:
		var d AssistantReasoningData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeAssistantReasoningDelta:
		var d AssistantReasoningDeltaData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeAssistantStreamingDelta:
		var d AssistantStreamingDeltaData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeAssistantMessage:
		var d AssistantMessageData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeAssistantMessageDelta:
		var d AssistantMessageDeltaData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeAssistantTurnEnd:
		var d AssistantTurnEndData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeAssistantUsage:
		var d AssistantUsageData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeAbort:
		var d AbortData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeToolUserRequested:
		var d ToolUserRequestedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeToolExecutionStart:
		var d ToolExecutionStartData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeToolExecutionPartialResult:
		var d ToolExecutionPartialResultData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeToolExecutionProgress:
		var d ToolExecutionProgressData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeToolExecutionComplete:
		var d ToolExecutionCompleteData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSkillInvoked:
		var d SkillInvokedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSubagentStarted:
		var d SubagentStartedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSubagentCompleted:
		var d SubagentCompletedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSubagentFailed:
		var d SubagentFailedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSubagentSelected:
		var d SubagentSelectedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSubagentDeselected:
		var d SubagentDeselectedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeHookStart:
		var d HookStartData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeHookEnd:
		var d HookEndData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSystemMessage:
		var d SystemMessageData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSystemNotification:
		var d SystemNotificationData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypePermissionRequested:
		var d PermissionRequestedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypePermissionCompleted:
		var d PermissionCompletedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeUserInputRequested:
		var d UserInputRequestedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeUserInputCompleted:
		var d UserInputCompletedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeElicitationRequested:
		var d ElicitationRequestedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeElicitationCompleted:
		var d ElicitationCompletedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSamplingRequested:
		var d SamplingRequestedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSamplingCompleted:
		var d SamplingCompletedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeMcpOauthRequired:
		var d McpOauthRequiredData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeMcpOauthCompleted:
		var d McpOauthCompletedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeExternalToolRequested:
		var d ExternalToolRequestedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeExternalToolCompleted:
		var d ExternalToolCompletedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeCommandQueued:
		var d CommandQueuedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeCommandExecute:
		var d CommandExecuteData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeCommandCompleted:
		var d CommandCompletedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeCommandsChanged:
		var d CommandsChangedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeCapabilitiesChanged:
		var d CapabilitiesChangedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeExitPlanModeRequested:
		var d ExitPlanModeRequestedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeExitPlanModeCompleted:
		var d ExitPlanModeCompletedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionToolsUpdated:
		var d SessionToolsUpdatedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionBackgroundTasksChanged:
		var d SessionBackgroundTasksChangedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionSkillsLoaded:
		var d SessionSkillsLoadedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionCustomAgentsUpdated:
		var d SessionCustomAgentsUpdatedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionMcpServersLoaded:
		var d SessionMcpServersLoadedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionMcpServerStatusChanged:
		var d SessionMcpServerStatusChangedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	case SessionEventTypeSessionExtensionsLoaded:
		var d SessionExtensionsLoadedData
		if err := json.Unmarshal(raw.Data, &d); err != nil {
			return err
		}
		e.Data = &d
	default:
		e.Data = &RawSessionEventData{Raw: raw.Data}
	}
	return nil
}

func (e SessionEvent) MarshalJSON() ([]byte, error) {
	type rawEvent struct {
		ID        string           `json:"id"`
		Timestamp time.Time        `json:"timestamp"`
		ParentID  *string          `json:"parentId"`
		Ephemeral *bool            `json:"ephemeral,omitempty"`
		Type      SessionEventType `json:"type"`
		Data      any              `json:"data"`
	}
	return json.Marshal(rawEvent{
		ID:        e.ID,
		Timestamp: e.Timestamp,
		ParentID:  e.ParentID,
		Ephemeral: e.Ephemeral,
		Type:      e.Type,
		Data:      e.Data,
	})
}

// SessionEventType identifies the kind of session event.
type SessionEventType string

const (
	SessionEventTypeSessionStart                  SessionEventType = "session.start"
	SessionEventTypeSessionResume                 SessionEventType = "session.resume"
	SessionEventTypeSessionRemoteSteerableChanged SessionEventType = "session.remote_steerable_changed"
	SessionEventTypeSessionError                  SessionEventType = "session.error"
	SessionEventTypeSessionIdle                   SessionEventType = "session.idle"
	SessionEventTypeSessionTitleChanged           SessionEventType = "session.title_changed"
	SessionEventTypeSessionInfo                   SessionEventType = "session.info"
	SessionEventTypeSessionWarning                SessionEventType = "session.warning"
	SessionEventTypeSessionModelChange            SessionEventType = "session.model_change"
	SessionEventTypeSessionModeChanged            SessionEventType = "session.mode_changed"
	SessionEventTypeSessionPlanChanged            SessionEventType = "session.plan_changed"
	SessionEventTypeSessionWorkspaceFileChanged   SessionEventType = "session.workspace_file_changed"
	SessionEventTypeSessionHandoff                SessionEventType = "session.handoff"
	SessionEventTypeSessionTruncation             SessionEventType = "session.truncation"
	SessionEventTypeSessionSnapshotRewind         SessionEventType = "session.snapshot_rewind"
	SessionEventTypeSessionShutdown               SessionEventType = "session.shutdown"
	SessionEventTypeSessionContextChanged         SessionEventType = "session.context_changed"
	SessionEventTypeSessionUsageInfo              SessionEventType = "session.usage_info"
	SessionEventTypeSessionCompactionStart        SessionEventType = "session.compaction_start"
	SessionEventTypeSessionCompactionComplete     SessionEventType = "session.compaction_complete"
	SessionEventTypeSessionTaskComplete           SessionEventType = "session.task_complete"
	SessionEventTypeUserMessage                   SessionEventType = "user.message"
	SessionEventTypePendingMessagesModified       SessionEventType = "pending_messages.modified"
	SessionEventTypeAssistantTurnStart            SessionEventType = "assistant.turn_start"
	SessionEventTypeAssistantIntent               SessionEventType = "assistant.intent"
	SessionEventTypeAssistantReasoning            SessionEventType = "assistant.reasoning"
	SessionEventTypeAssistantReasoningDelta       SessionEventType = "assistant.reasoning_delta"
	SessionEventTypeAssistantStreamingDelta       SessionEventType = "assistant.streaming_delta"
	SessionEventTypeAssistantMessage              SessionEventType = "assistant.message"
	SessionEventTypeAssistantMessageDelta         SessionEventType = "assistant.message_delta"
	SessionEventTypeAssistantTurnEnd              SessionEventType = "assistant.turn_end"
	SessionEventTypeAssistantUsage                SessionEventType = "assistant.usage"
	SessionEventTypeAbort                         SessionEventType = "abort"
	SessionEventTypeToolUserRequested             SessionEventType = "tool.user_requested"
	SessionEventTypeToolExecutionStart            SessionEventType = "tool.execution_start"
	SessionEventTypeToolExecutionPartialResult    SessionEventType = "tool.execution_partial_result"
	SessionEventTypeToolExecutionProgress         SessionEventType = "tool.execution_progress"
	SessionEventTypeToolExecutionComplete         SessionEventType = "tool.execution_complete"
	SessionEventTypeSkillInvoked                  SessionEventType = "skill.invoked"
	SessionEventTypeSubagentStarted               SessionEventType = "subagent.started"
	SessionEventTypeSubagentCompleted             SessionEventType = "subagent.completed"
	SessionEventTypeSubagentFailed                SessionEventType = "subagent.failed"
	SessionEventTypeSubagentSelected              SessionEventType = "subagent.selected"
	SessionEventTypeSubagentDeselected            SessionEventType = "subagent.deselected"
	SessionEventTypeHookStart                     SessionEventType = "hook.start"
	SessionEventTypeHookEnd                       SessionEventType = "hook.end"
	SessionEventTypeSystemMessage                 SessionEventType = "system.message"
	SessionEventTypeSystemNotification            SessionEventType = "system.notification"
	SessionEventTypePermissionRequested           SessionEventType = "permission.requested"
	SessionEventTypePermissionCompleted           SessionEventType = "permission.completed"
	SessionEventTypeUserInputRequested            SessionEventType = "user_input.requested"
	SessionEventTypeUserInputCompleted            SessionEventType = "user_input.completed"
	SessionEventTypeElicitationRequested          SessionEventType = "elicitation.requested"
	SessionEventTypeElicitationCompleted          SessionEventType = "elicitation.completed"
	SessionEventTypeSamplingRequested             SessionEventType = "sampling.requested"
	SessionEventTypeSamplingCompleted             SessionEventType = "sampling.completed"
	SessionEventTypeMcpOauthRequired              SessionEventType = "mcp.oauth_required"
	SessionEventTypeMcpOauthCompleted             SessionEventType = "mcp.oauth_completed"
	SessionEventTypeExternalToolRequested         SessionEventType = "external_tool.requested"
	SessionEventTypeExternalToolCompleted         SessionEventType = "external_tool.completed"
	SessionEventTypeCommandQueued                 SessionEventType = "command.queued"
	SessionEventTypeCommandExecute                SessionEventType = "command.execute"
	SessionEventTypeCommandCompleted              SessionEventType = "command.completed"
	SessionEventTypeCommandsChanged               SessionEventType = "commands.changed"
	SessionEventTypeCapabilitiesChanged           SessionEventType = "capabilities.changed"
	SessionEventTypeExitPlanModeRequested         SessionEventType = "exit_plan_mode.requested"
	SessionEventTypeExitPlanModeCompleted         SessionEventType = "exit_plan_mode.completed"
	SessionEventTypeSessionToolsUpdated           SessionEventType = "session.tools_updated"
	SessionEventTypeSessionBackgroundTasksChanged SessionEventType = "session.background_tasks_changed"
	SessionEventTypeSessionSkillsLoaded           SessionEventType = "session.skills_loaded"
	SessionEventTypeSessionCustomAgentsUpdated    SessionEventType = "session.custom_agents_updated"
	SessionEventTypeSessionMcpServersLoaded       SessionEventType = "session.mcp_servers_loaded"
	SessionEventTypeSessionMcpServerStatusChanged SessionEventType = "session.mcp_server_status_changed"
	SessionEventTypeSessionExtensionsLoaded       SessionEventType = "session.extensions_loaded"
)

// Agent intent description for current activity or plan
type AssistantIntentData struct {
	// Short description of what the agent is currently doing or planning to do
	Intent string `json:"intent"`
}

func (*AssistantIntentData) sessionEventData() {}

// Agent mode change details including previous and new modes
type SessionModeChangedData struct {
	// Agent mode after the change (e.g., "interactive", "plan", "autopilot")
	NewMode string `json:"newMode"`
	// Agent mode before the change (e.g., "interactive", "plan", "autopilot")
	PreviousMode string `json:"previousMode"`
}

func (*SessionModeChangedData) sessionEventData() {}

// Assistant reasoning content for timeline display with complete thinking text
type AssistantReasoningData struct {
	// The complete extended thinking text from the model
	Content string `json:"content"`
	// Unique identifier for this reasoning block
	ReasoningID string `json:"reasoningId"`
}

func (*AssistantReasoningData) sessionEventData() {}

// Assistant response containing text content, optional tool requests, and interaction metadata
type AssistantMessageData struct {
	// The assistant's text response content
	Content string `json:"content"`
	// Encrypted reasoning content from OpenAI models. Session-bound and stripped on resume.
	EncryptedContent *string `json:"encryptedContent,omitempty"`
	// CAPI interaction ID for correlating this message with upstream telemetry
	InteractionID *string `json:"interactionId,omitempty"`
	// Unique identifier for this assistant message
	MessageID string `json:"messageId"`
	// Actual output token count from the API response (completion_tokens), used for accurate token accounting
	OutputTokens *float64 `json:"outputTokens,omitempty"`
	// Tool call ID of the parent tool invocation when this event originates from a sub-agent
	// Deprecated: ParentToolCallID is deprecated.
	ParentToolCallID *string `json:"parentToolCallId,omitempty"`
	// Generation phase for phased-output models (e.g., thinking vs. response phases)
	Phase *string `json:"phase,omitempty"`
	// Opaque/encrypted extended thinking data from Anthropic models. Session-bound and stripped on resume.
	ReasoningOpaque *string `json:"reasoningOpaque,omitempty"`
	// Readable reasoning text from the model's extended thinking
	ReasoningText *string `json:"reasoningText,omitempty"`
	// GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs
	RequestID *string `json:"requestId,omitempty"`
	// Tool invocations requested by the assistant in this message
	ToolRequests []AssistantMessageToolRequest `json:"toolRequests,omitempty"`
}

func (*AssistantMessageData) sessionEventData() {}

// Context window breakdown at the start of LLM-powered conversation compaction
type SessionCompactionStartData struct {
	// Token count from non-system messages (user, assistant, tool) at compaction start
	ConversationTokens *float64 `json:"conversationTokens,omitempty"`
	// Token count from system message(s) at compaction start
	SystemTokens *float64 `json:"systemTokens,omitempty"`
	// Token count from tool definitions at compaction start
	ToolDefinitionsTokens *float64 `json:"toolDefinitionsTokens,omitempty"`
}

func (*SessionCompactionStartData) sessionEventData() {}

// Conversation compaction results including success status, metrics, and optional error details
type SessionCompactionCompleteData struct {
	// Checkpoint snapshot number created for recovery
	CheckpointNumber *float64 `json:"checkpointNumber,omitempty"`
	// File path where the checkpoint was stored
	CheckpointPath *string `json:"checkpointPath,omitempty"`
	// Token usage breakdown for the compaction LLM call
	CompactionTokensUsed *CompactionCompleteCompactionTokensUsed `json:"compactionTokensUsed,omitempty"`
	// Token count from non-system messages (user, assistant, tool) after compaction
	ConversationTokens *float64 `json:"conversationTokens,omitempty"`
	// Error message if compaction failed
	Error *string `json:"error,omitempty"`
	// Number of messages removed during compaction
	MessagesRemoved *float64 `json:"messagesRemoved,omitempty"`
	// Total tokens in conversation after compaction
	PostCompactionTokens *float64 `json:"postCompactionTokens,omitempty"`
	// Number of messages before compaction
	PreCompactionMessagesLength *float64 `json:"preCompactionMessagesLength,omitempty"`
	// Total tokens in conversation before compaction
	PreCompactionTokens *float64 `json:"preCompactionTokens,omitempty"`
	// GitHub request tracing ID (x-github-request-id header) for the compaction LLM call
	RequestID *string `json:"requestId,omitempty"`
	// Whether compaction completed successfully
	Success bool `json:"success"`
	// LLM-generated summary of the compacted conversation history
	SummaryContent *string `json:"summaryContent,omitempty"`
	// Token count from system message(s) after compaction
	SystemTokens *float64 `json:"systemTokens,omitempty"`
	// Number of tokens removed during compaction
	TokensRemoved *float64 `json:"tokensRemoved,omitempty"`
	// Token count from tool definitions after compaction
	ToolDefinitionsTokens *float64 `json:"toolDefinitionsTokens,omitempty"`
}

func (*SessionCompactionCompleteData) sessionEventData() {}

// Conversation truncation statistics including token counts and removed content metrics
type SessionTruncationData struct {
	// Number of messages removed by truncation
	MessagesRemovedDuringTruncation float64 `json:"messagesRemovedDuringTruncation"`
	// Identifier of the component that performed truncation (e.g., "BasicTruncator")
	PerformedBy string `json:"performedBy"`
	// Number of conversation messages after truncation
	PostTruncationMessagesLength float64 `json:"postTruncationMessagesLength"`
	// Total tokens in conversation messages after truncation
	PostTruncationTokensInMessages float64 `json:"postTruncationTokensInMessages"`
	// Number of conversation messages before truncation
	PreTruncationMessagesLength float64 `json:"preTruncationMessagesLength"`
	// Total tokens in conversation messages before truncation
	PreTruncationTokensInMessages float64 `json:"preTruncationTokensInMessages"`
	// Maximum token count for the model's context window
	TokenLimit float64 `json:"tokenLimit"`
	// Number of tokens removed by truncation
	TokensRemovedDuringTruncation float64 `json:"tokensRemovedDuringTruncation"`
}

func (*SessionTruncationData) sessionEventData() {}

// Current context window usage statistics including token and message counts
type SessionUsageInfoData struct {
	// Token count from non-system messages (user, assistant, tool)
	ConversationTokens *float64 `json:"conversationTokens,omitempty"`
	// Current number of tokens in the context window
	CurrentTokens float64 `json:"currentTokens"`
	// Whether this is the first usage_info event emitted in this session
	IsInitial *bool `json:"isInitial,omitempty"`
	// Current number of messages in the conversation
	MessagesLength float64 `json:"messagesLength"`
	// Token count from system message(s)
	SystemTokens *float64 `json:"systemTokens,omitempty"`
	// Maximum token count for the model's context window
	TokenLimit float64 `json:"tokenLimit"`
	// Token count from tool definitions
	ToolDefinitionsTokens *float64 `json:"toolDefinitionsTokens,omitempty"`
}

func (*SessionUsageInfoData) sessionEventData() {}

// Custom agent selection details including name and available tools
type SubagentSelectedData struct {
	// Human-readable display name of the selected custom agent
	AgentDisplayName string `json:"agentDisplayName"`
	// Internal name of the selected custom agent
	AgentName string `json:"agentName"`
	// List of tool names available to this agent, or null for all tools
	Tools []string `json:"tools"`
}

func (*SubagentSelectedData) sessionEventData() {}

// Elicitation request completion with the user's response
type ElicitationCompletedData struct {
	// The user action: "accept" (submitted form), "decline" (explicitly refused), or "cancel" (dismissed)
	Action *ElicitationCompletedAction `json:"action,omitempty"`
	// The submitted form data when action is 'accept'; keys match the requested schema fields
	Content map[string]any `json:"content,omitempty"`
	// Request ID of the resolved elicitation request; clients should dismiss any UI for this request
	RequestID string `json:"requestId"`
}

func (*ElicitationCompletedData) sessionEventData() {}

// Elicitation request; may be form-based (structured input) or URL-based (browser redirect)
type ElicitationRequestedData struct {
	// The source that initiated the request (MCP server name, or absent for agent-initiated)
	ElicitationSource *string `json:"elicitationSource,omitempty"`
	// Message describing what information is needed from the user
	Message string `json:"message"`
	// Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent.
	Mode *ElicitationRequestedMode `json:"mode,omitempty"`
	// JSON Schema describing the form fields to present to the user (form mode only)
	RequestedSchema *ElicitationRequestedSchema `json:"requestedSchema,omitempty"`
	// Unique identifier for this elicitation request; used to respond via session.respondToElicitation()
	RequestID string `json:"requestId"`
	// Tool call ID from the LLM completion; used to correlate with CompletionChunk.toolCall.id for remote UIs
	ToolCallID *string `json:"toolCallId,omitempty"`
	// URL to open in the user's browser (url mode only)
	URL *string `json:"url,omitempty"`
}

func (*ElicitationRequestedData) sessionEventData() {}

// Empty payload; the event signals that the custom agent was deselected, returning to the default agent
type SubagentDeselectedData struct {
}

func (*SubagentDeselectedData) sessionEventData() {}

// Empty payload; the event signals that the pending message queue has changed
type PendingMessagesModifiedData struct {
}

func (*PendingMessagesModifiedData) sessionEventData() {}

// Error details for timeline display including message and optional diagnostic information
type SessionErrorData struct {
	// Category of error (e.g., "authentication", "authorization", "quota", "rate_limit", "context_limit", "query")
	ErrorType string `json:"errorType"`
	// Human-readable error message
	Message string `json:"message"`
	// GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs
	ProviderCallID *string `json:"providerCallId,omitempty"`
	// Error stack trace, when available
	Stack *string `json:"stack,omitempty"`
	// HTTP status code from the upstream request, if applicable
	StatusCode *int64 `json:"statusCode,omitempty"`
	// Optional URL associated with this error that the user can open in a browser
	URL *string `json:"url,omitempty"`
}

func (*SessionErrorData) sessionEventData() {}

// External tool completion notification signaling UI dismissal
type ExternalToolCompletedData struct {
	// Request ID of the resolved external tool request; clients should dismiss any UI for this request
	RequestID string `json:"requestId"`
}

func (*ExternalToolCompletedData) sessionEventData() {}

// External tool invocation request for client-side tool execution
type ExternalToolRequestedData struct {
	// Arguments to pass to the external tool
	Arguments any `json:"arguments,omitempty"`
	// Unique identifier for this request; used to respond via session.respondToExternalTool()
	RequestID string `json:"requestId"`
	// Session ID that this external tool request belongs to
	SessionID string `json:"sessionId"`
	// Tool call ID assigned to this external tool invocation
	ToolCallID string `json:"toolCallId"`
	// Name of the external tool to invoke
	ToolName string `json:"toolName"`
	// W3C Trace Context traceparent header for the execute_tool span
	Traceparent *string `json:"traceparent,omitempty"`
	// W3C Trace Context tracestate header for the execute_tool span
	Tracestate *string `json:"tracestate,omitempty"`
}

func (*ExternalToolRequestedData) sessionEventData() {}

// Hook invocation completion details including output, success status, and error information
type HookEndData struct {
	// Error details when the hook failed
	Error *HookEndError `json:"error,omitempty"`
	// Identifier matching the corresponding hook.start event
	HookInvocationID string `json:"hookInvocationId"`
	// Type of hook that was invoked (e.g., "preToolUse", "postToolUse", "sessionStart")
	HookType string `json:"hookType"`
	// Output data produced by the hook
	Output any `json:"output,omitempty"`
	// Whether the hook completed successfully
	Success bool `json:"success"`
}

func (*HookEndData) sessionEventData() {}

// Hook invocation start details including type and input data
type HookStartData struct {
	// Unique identifier for this hook invocation
	HookInvocationID string `json:"hookInvocationId"`
	// Type of hook being invoked (e.g., "preToolUse", "postToolUse", "sessionStart")
	HookType string `json:"hookType"`
	// Input data passed to the hook
	Input any `json:"input,omitempty"`
}

func (*HookStartData) sessionEventData() {}

// Informational message for timeline display with categorization
type SessionInfoData struct {
	// Category of informational message (e.g., "notification", "timing", "context_window", "mcp", "snapshot", "configuration", "authentication", "model")
	InfoType string `json:"infoType"`
	// Human-readable informational message for display in the timeline
	Message string `json:"message"`
	// Optional URL associated with this message that the user can open in a browser
	URL *string `json:"url,omitempty"`
}

func (*SessionInfoData) sessionEventData() {}

// LLM API call usage metrics including tokens, costs, quotas, and billing information
type AssistantUsageData struct {
	// Completion ID from the model provider (e.g., chatcmpl-abc123)
	APICallID *string `json:"apiCallId,omitempty"`
	// Number of tokens read from prompt cache
	CacheReadTokens *float64 `json:"cacheReadTokens,omitempty"`
	// Number of tokens written to prompt cache
	CacheWriteTokens *float64 `json:"cacheWriteTokens,omitempty"`
	// Per-request cost and usage data from the CAPI copilot_usage response field
	CopilotUsage *AssistantUsageCopilotUsage `json:"copilotUsage,omitempty"`
	// Model multiplier cost for billing purposes
	Cost *float64 `json:"cost,omitempty"`
	// Duration of the API call in milliseconds
	Duration *float64 `json:"duration,omitempty"`
	// What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls
	Initiator *string `json:"initiator,omitempty"`
	// Number of input tokens consumed
	InputTokens *float64 `json:"inputTokens,omitempty"`
	// Average inter-token latency in milliseconds. Only available for streaming requests
	InterTokenLatencyMs *float64 `json:"interTokenLatencyMs,omitempty"`
	// Model identifier used for this API call
	Model string `json:"model"`
	// Number of output tokens produced
	OutputTokens *float64 `json:"outputTokens,omitempty"`
	// Parent tool call ID when this usage originates from a sub-agent
	// Deprecated: ParentToolCallID is deprecated.
	ParentToolCallID *string `json:"parentToolCallId,omitempty"`
	// GitHub request tracing ID (x-github-request-id header) for server-side log correlation
	ProviderCallID *string `json:"providerCallId,omitempty"`
	// Per-quota resource usage snapshots, keyed by quota identifier
	QuotaSnapshots map[string]AssistantUsageQuotaSnapshot `json:"quotaSnapshots,omitempty"`
	// Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh")
	ReasoningEffort *string `json:"reasoningEffort,omitempty"`
	// Number of output tokens used for reasoning (e.g., chain-of-thought)
	ReasoningTokens *float64 `json:"reasoningTokens,omitempty"`
	// Time to first token in milliseconds. Only available for streaming requests
	TtftMs *float64 `json:"ttftMs,omitempty"`
}

func (*AssistantUsageData) sessionEventData() {}

// MCP OAuth request completion notification
type McpOauthCompletedData struct {
	// Request ID of the resolved OAuth request
	RequestID string `json:"requestId"`
}

func (*McpOauthCompletedData) sessionEventData() {}

// Model change details including previous and new model identifiers
type SessionModelChangeData struct {
	// Newly selected model identifier
	NewModel string `json:"newModel"`
	// Model that was previously selected, if any
	PreviousModel *string `json:"previousModel,omitempty"`
	// Reasoning effort level before the model change, if applicable
	PreviousReasoningEffort *string `json:"previousReasoningEffort,omitempty"`
	// Reasoning effort level after the model change, if applicable
	ReasoningEffort *string `json:"reasoningEffort,omitempty"`
}

func (*SessionModelChangeData) sessionEventData() {}

// Notifies Mission Control that the session's remote steering capability has changed
type SessionRemoteSteerableChangedData struct {
	// Whether this session now supports remote steering via Mission Control
	RemoteSteerable bool `json:"remoteSteerable"`
}

func (*SessionRemoteSteerableChangedData) sessionEventData() {}

// OAuth authentication request for an MCP server
type McpOauthRequiredData struct {
	// Unique identifier for this OAuth request; used to respond via session.respondToMcpOAuth()
	RequestID string `json:"requestId"`
	// Display name of the MCP server that requires OAuth
	ServerName string `json:"serverName"`
	// URL of the MCP server that requires OAuth
	ServerURL string `json:"serverUrl"`
	// Static OAuth client configuration, if the server specifies one
	StaticClientConfig *McpOauthRequiredStaticClientConfig `json:"staticClientConfig,omitempty"`
}

func (*McpOauthRequiredData) sessionEventData() {}

// Payload indicating the session is idle with no background agents in flight
type SessionIdleData struct {
	// True when the preceding agentic loop was cancelled via abort signal
	Aborted *bool `json:"aborted,omitempty"`
}

func (*SessionIdleData) sessionEventData() {}

// Permission request completion notification signaling UI dismissal
type PermissionCompletedData struct {
	// Request ID of the resolved permission request; clients should dismiss any UI for this request
	RequestID string `json:"requestId"`
	// The result of the permission request
	Result PermissionCompletedResult `json:"result"`
}

func (*PermissionCompletedData) sessionEventData() {}

// Permission request notification requiring client approval with request details
type PermissionRequestedData struct {
	// Details of the permission being requested
	PermissionRequest PermissionRequest `json:"permissionRequest"`
	// Unique identifier for this permission request; used to respond via session.respondToPermission()
	RequestID string `json:"requestId"`
	// When true, this permission was already resolved by a permissionRequest hook and requires no client action
	ResolvedByHook *bool `json:"resolvedByHook,omitempty"`
}

func (*PermissionRequestedData) sessionEventData() {}

// Plan approval request with plan content and available user actions
type ExitPlanModeRequestedData struct {
	// Available actions the user can take (e.g., approve, edit, reject)
	Actions []string `json:"actions"`
	// Full content of the plan file
	PlanContent string `json:"planContent"`
	// The recommended action for the user to take
	RecommendedAction string `json:"recommendedAction"`
	// Unique identifier for this request; used to respond via session.respondToExitPlanMode()
	RequestID string `json:"requestId"`
	// Summary of the plan that was created
	Summary string `json:"summary"`
}

func (*ExitPlanModeRequestedData) sessionEventData() {}

// Plan file operation details indicating what changed
type SessionPlanChangedData struct {
	// The type of operation performed on the plan file
	Operation PlanChangedOperation `json:"operation"`
}

func (*SessionPlanChangedData) sessionEventData() {}

// Plan mode exit completion with the user's approval decision and optional feedback
type ExitPlanModeCompletedData struct {
	// Whether the plan was approved by the user
	Approved *bool `json:"approved,omitempty"`
	// Whether edits should be auto-approved without confirmation
	AutoApproveEdits *bool `json:"autoApproveEdits,omitempty"`
	// Free-form feedback from the user if they requested changes to the plan
	Feedback *string `json:"feedback,omitempty"`
	// Request ID of the resolved exit plan mode request; clients should dismiss any UI for this request
	RequestID string `json:"requestId"`
	// Which action the user selected (e.g. 'autopilot', 'interactive', 'exit_only')
	SelectedAction *string `json:"selectedAction,omitempty"`
}

func (*ExitPlanModeCompletedData) sessionEventData() {}

// Queued command completion notification signaling UI dismissal
type CommandCompletedData struct {
	// Request ID of the resolved command request; clients should dismiss any UI for this request
	RequestID string `json:"requestId"`
}

func (*CommandCompletedData) sessionEventData() {}

// Queued slash command dispatch request for client execution
type CommandQueuedData struct {
	// The slash command text to be executed (e.g., /help, /clear)
	Command string `json:"command"`
	// Unique identifier for this request; used to respond via session.respondToQueuedCommand()
	RequestID string `json:"requestId"`
}

func (*CommandQueuedData) sessionEventData() {}

// Registered command dispatch request routed to the owning client
type CommandExecuteData struct {
	// Raw argument string after the command name
	Args string `json:"args"`
	// The full command text (e.g., /deploy production)
	Command string `json:"command"`
	// Command name without leading /
	CommandName string `json:"commandName"`
	// Unique identifier; used to respond via session.commands.handlePendingCommand()
	RequestID string `json:"requestId"`
}

func (*CommandExecuteData) sessionEventData() {}

// SDK command registration change notification
type CommandsChangedData struct {
	// Current list of registered SDK commands
	Commands []CommandsChangedCommand `json:"commands"`
}

func (*CommandsChangedData) sessionEventData() {}

// Sampling request completion notification signaling UI dismissal
type SamplingCompletedData struct {
	// Request ID of the resolved sampling request; clients should dismiss any UI for this request
	RequestID string `json:"requestId"`
}

func (*SamplingCompletedData) sessionEventData() {}

// Sampling request from an MCP server; contains the server name and a requestId for correlation
type SamplingRequestedData struct {
	// The JSON-RPC request ID from the MCP protocol
	McpRequestID any `json:"mcpRequestId"`
	// Unique identifier for this sampling request; used to respond via session.respondToSampling()
	RequestID string `json:"requestId"`
	// Name of the MCP server that initiated the sampling request
	ServerName string `json:"serverName"`
}

func (*SamplingRequestedData) sessionEventData() {}

// Session capability change notification
type CapabilitiesChangedData struct {
	// UI capability changes
	UI *CapabilitiesChangedUI `json:"ui,omitempty"`
}

func (*CapabilitiesChangedData) sessionEventData() {}

// Session handoff metadata including source, context, and repository information
type SessionHandoffData struct {
	// Additional context information for the handoff
	Context *string `json:"context,omitempty"`
	// ISO 8601 timestamp when the handoff occurred
	HandoffTime time.Time `json:"handoffTime"`
	// GitHub host URL for the source session (e.g., https://github.com or https://tenant.ghe.com)
	Host *string `json:"host,omitempty"`
	// Session ID of the remote session being handed off
	RemoteSessionID *string `json:"remoteSessionId,omitempty"`
	// Repository context for the handed-off session
	Repository *HandoffRepository `json:"repository,omitempty"`
	// Origin type of the session being handed off
	SourceType HandoffSourceType `json:"sourceType"`
	// Summary of the work done in the source session
	Summary *string `json:"summary,omitempty"`
}

func (*SessionHandoffData) sessionEventData() {}

// Session initialization metadata including context and configuration
type SessionStartData struct {
	// Whether the session was already in use by another client at start time
	AlreadyInUse *bool `json:"alreadyInUse,omitempty"`
	// Working directory and git context at session start
	Context *WorkingDirectoryContext `json:"context,omitempty"`
	// Version string of the Copilot application
	CopilotVersion string `json:"copilotVersion"`
	// Identifier of the software producing the events (e.g., "copilot-agent")
	Producer string `json:"producer"`
	// Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh")
	ReasoningEffort *string `json:"reasoningEffort,omitempty"`
	// Whether this session supports remote steering via Mission Control
	RemoteSteerable *bool `json:"remoteSteerable,omitempty"`
	// Model selected at session creation time, if any
	SelectedModel *string `json:"selectedModel,omitempty"`
	// Unique identifier for the session
	SessionID string `json:"sessionId"`
	// ISO 8601 timestamp when the session was created
	StartTime time.Time `json:"startTime"`
	// Schema version number for the session event format
	Version float64 `json:"version"`
}

func (*SessionStartData) sessionEventData() {}

// Session resume metadata including current context and event count
type SessionResumeData struct {
	// Whether the session was already in use by another client at resume time
	AlreadyInUse *bool `json:"alreadyInUse,omitempty"`
	// Updated working directory and git context at resume time
	Context *WorkingDirectoryContext `json:"context,omitempty"`
	// Total number of persisted events in the session at the time of resume
	EventCount float64 `json:"eventCount"`
	// Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh")
	ReasoningEffort *string `json:"reasoningEffort,omitempty"`
	// Whether this session supports remote steering via Mission Control
	RemoteSteerable *bool `json:"remoteSteerable,omitempty"`
	// ISO 8601 timestamp when the session was resumed
	ResumeTime time.Time `json:"resumeTime"`
	// Model currently selected at resume time
	SelectedModel *string `json:"selectedModel,omitempty"`
}

func (*SessionResumeData) sessionEventData() {}

// Session rewind details including target event and count of removed events
type SessionSnapshotRewindData struct {
	// Number of events that were removed by the rewind
	EventsRemoved float64 `json:"eventsRemoved"`
	// Event ID that was rewound to; this event and all after it were removed
	UpToEventID string `json:"upToEventId"`
}

func (*SessionSnapshotRewindData) sessionEventData() {}

// Session termination metrics including usage statistics, code changes, and shutdown reason
type SessionShutdownData struct {
	// Aggregate code change metrics for the session
	CodeChanges ShutdownCodeChanges `json:"codeChanges"`
	// Non-system message token count at shutdown
	ConversationTokens *float64 `json:"conversationTokens,omitempty"`
	// Model that was selected at the time of shutdown
	CurrentModel *string `json:"currentModel,omitempty"`
	// Total tokens in context window at shutdown
	CurrentTokens *float64 `json:"currentTokens,omitempty"`
	// Error description when shutdownType is "error"
	ErrorReason *string `json:"errorReason,omitempty"`
	// Per-model usage breakdown, keyed by model identifier
	ModelMetrics map[string]ShutdownModelMetric `json:"modelMetrics"`
	// Unix timestamp (milliseconds) when the session started
	SessionStartTime float64 `json:"sessionStartTime"`
	// Whether the session ended normally ("routine") or due to a crash/fatal error ("error")
	ShutdownType ShutdownType `json:"shutdownType"`
	// System message token count at shutdown
	SystemTokens *float64 `json:"systemTokens,omitempty"`
	// Tool definitions token count at shutdown
	ToolDefinitionsTokens *float64 `json:"toolDefinitionsTokens,omitempty"`
	// Cumulative time spent in API calls during the session, in milliseconds
	TotalAPIDurationMs float64 `json:"totalApiDurationMs"`
	// Total number of premium API requests used during the session
	TotalPremiumRequests float64 `json:"totalPremiumRequests"`
}

func (*SessionShutdownData) sessionEventData() {}

// Session title change payload containing the new display title
type SessionTitleChangedData struct {
	// The new display title for the session
	Title string `json:"title"`
}

func (*SessionTitleChangedData) sessionEventData() {}

// SessionBackgroundTasksChangedData holds the payload for session.background_tasks_changed events.
type SessionBackgroundTasksChangedData struct {
}

func (*SessionBackgroundTasksChangedData) sessionEventData() {}

// SessionCustomAgentsUpdatedData holds the payload for session.custom_agents_updated events.
type SessionCustomAgentsUpdatedData struct {
	// Array of loaded custom agent metadata
	Agents []CustomAgentsUpdatedAgent `json:"agents"`
	// Fatal errors from agent loading
	Errors []string `json:"errors"`
	// Non-fatal warnings from agent loading
	Warnings []string `json:"warnings"`
}

func (*SessionCustomAgentsUpdatedData) sessionEventData() {}

// SessionExtensionsLoadedData holds the payload for session.extensions_loaded events.
type SessionExtensionsLoadedData struct {
	// Array of discovered extensions and their status
	Extensions []ExtensionsLoadedExtension `json:"extensions"`
}

func (*SessionExtensionsLoadedData) sessionEventData() {}

// SessionMcpServerStatusChangedData holds the payload for session.mcp_server_status_changed events.
type SessionMcpServerStatusChangedData struct {
	// Name of the MCP server whose status changed
	ServerName string `json:"serverName"`
	// New connection status: connected, failed, needs-auth, pending, disabled, or not_configured
	Status McpServerStatusChangedStatus `json:"status"`
}

func (*SessionMcpServerStatusChangedData) sessionEventData() {}

// SessionMcpServersLoadedData holds the payload for session.mcp_servers_loaded events.
type SessionMcpServersLoadedData struct {
	// Array of MCP server status summaries
	Servers []McpServersLoadedServer `json:"servers"`
}

func (*SessionMcpServersLoadedData) sessionEventData() {}

// SessionSkillsLoadedData holds the payload for session.skills_loaded events.
type SessionSkillsLoadedData struct {
	// Array of resolved skill metadata
	Skills []SkillsLoadedSkill `json:"skills"`
}

func (*SessionSkillsLoadedData) sessionEventData() {}

// SessionToolsUpdatedData holds the payload for session.tools_updated events.
type SessionToolsUpdatedData struct {
	Model string `json:"model"`
}

func (*SessionToolsUpdatedData) sessionEventData() {}

// Skill invocation details including content, allowed tools, and plugin metadata
type SkillInvokedData struct {
	// Tool names that should be auto-approved when this skill is active
	AllowedTools []string `json:"allowedTools,omitempty"`
	// Full content of the skill file, injected into the conversation for the model
	Content string `json:"content"`
	// Description of the skill from its SKILL.md frontmatter
	Description *string `json:"description,omitempty"`
	// Name of the invoked skill
	Name string `json:"name"`
	// File path to the SKILL.md definition
	Path string `json:"path"`
	// Name of the plugin this skill originated from, when applicable
	PluginName *string `json:"pluginName,omitempty"`
	// Version of the plugin this skill originated from, when applicable
	PluginVersion *string `json:"pluginVersion,omitempty"`
}

func (*SkillInvokedData) sessionEventData() {}

// Streaming assistant message delta for incremental response updates
type AssistantMessageDeltaData struct {
	// Incremental text chunk to append to the message content
	DeltaContent string `json:"deltaContent"`
	// Message ID this delta belongs to, matching the corresponding assistant.message event
	MessageID string `json:"messageId"`
	// Tool call ID of the parent tool invocation when this event originates from a sub-agent
	// Deprecated: ParentToolCallID is deprecated.
	ParentToolCallID *string `json:"parentToolCallId,omitempty"`
}

func (*AssistantMessageDeltaData) sessionEventData() {}

// Streaming reasoning delta for incremental extended thinking updates
type AssistantReasoningDeltaData struct {
	// Incremental text chunk to append to the reasoning content
	DeltaContent string `json:"deltaContent"`
	// Reasoning block ID this delta belongs to, matching the corresponding assistant.reasoning event
	ReasoningID string `json:"reasoningId"`
}

func (*AssistantReasoningDeltaData) sessionEventData() {}

// Streaming response progress with cumulative byte count
type AssistantStreamingDeltaData struct {
	// Cumulative total bytes received from the streaming response so far
	TotalResponseSizeBytes float64 `json:"totalResponseSizeBytes"`
}

func (*AssistantStreamingDeltaData) sessionEventData() {}

// Streaming tool execution output for incremental result display
type ToolExecutionPartialResultData struct {
	// Incremental output chunk from the running tool
	PartialOutput string `json:"partialOutput"`
	// Tool call ID this partial result belongs to
	ToolCallID string `json:"toolCallId"`
}

func (*ToolExecutionPartialResultData) sessionEventData() {}

// Sub-agent completion details for successful execution
type SubagentCompletedData struct {
	// Human-readable display name of the sub-agent
	AgentDisplayName string `json:"agentDisplayName"`
	// Internal name of the sub-agent
	AgentName string `json:"agentName"`
	// Wall-clock duration of the sub-agent execution in milliseconds
	DurationMs *float64 `json:"durationMs,omitempty"`
	// Model used by the sub-agent
	Model *string `json:"model,omitempty"`
	// Tool call ID of the parent tool invocation that spawned this sub-agent
	ToolCallID string `json:"toolCallId"`
	// Total tokens (input + output) consumed by the sub-agent
	TotalTokens *float64 `json:"totalTokens,omitempty"`
	// Total number of tool calls made by the sub-agent
	TotalToolCalls *float64 `json:"totalToolCalls,omitempty"`
}

func (*SubagentCompletedData) sessionEventData() {}

// Sub-agent failure details including error message and agent information
type SubagentFailedData struct {
	// Human-readable display name of the sub-agent
	AgentDisplayName string `json:"agentDisplayName"`
	// Internal name of the sub-agent
	AgentName string `json:"agentName"`
	// Wall-clock duration of the sub-agent execution in milliseconds
	DurationMs *float64 `json:"durationMs,omitempty"`
	// Error message describing why the sub-agent failed
	Error string `json:"error"`
	// Model used by the sub-agent (if any model calls succeeded before failure)
	Model *string `json:"model,omitempty"`
	// Tool call ID of the parent tool invocation that spawned this sub-agent
	ToolCallID string `json:"toolCallId"`
	// Total tokens (input + output) consumed before the sub-agent failed
	TotalTokens *float64 `json:"totalTokens,omitempty"`
	// Total number of tool calls made before the sub-agent failed
	TotalToolCalls *float64 `json:"totalToolCalls,omitempty"`
}

func (*SubagentFailedData) sessionEventData() {}

// Sub-agent startup details including parent tool call and agent information
type SubagentStartedData struct {
	// Description of what the sub-agent does
	AgentDescription string `json:"agentDescription"`
	// Human-readable display name of the sub-agent
	AgentDisplayName string `json:"agentDisplayName"`
	// Internal name of the sub-agent
	AgentName string `json:"agentName"`
	// Tool call ID of the parent tool invocation that spawned this sub-agent
	ToolCallID string `json:"toolCallId"`
}

func (*SubagentStartedData) sessionEventData() {}

// System-generated notification for runtime events like background task completion
type SystemNotificationData struct {
	// The notification text, typically wrapped in <system_notification> XML tags
	Content string `json:"content"`
	// Structured metadata identifying what triggered this notification
	Kind SystemNotification `json:"kind"`
}

func (*SystemNotificationData) sessionEventData() {}

// System/developer instruction content with role and optional template metadata
type SystemMessageData struct {
	// The system or developer prompt text sent as model input
	Content string `json:"content"`
	// Metadata about the prompt template and its construction
	Metadata *SystemMessageMetadata `json:"metadata,omitempty"`
	// Optional name identifier for the message source
	Name *string `json:"name,omitempty"`
	// Message role: "system" for system prompts, "developer" for developer-injected instructions
	Role SystemMessageRole `json:"role"`
}

func (*SystemMessageData) sessionEventData() {}

// Task completion notification with summary from the agent
type SessionTaskCompleteData struct {
	// Whether the tool call succeeded. False when validation failed (e.g., invalid arguments)
	Success *bool `json:"success,omitempty"`
	// Summary of the completed task, provided by the agent
	Summary *string `json:"summary,omitempty"`
}

func (*SessionTaskCompleteData) sessionEventData() {}

// Tool execution completion results including success status, detailed output, and error information
type ToolExecutionCompleteData struct {
	// Error details when the tool execution failed
	Error *ToolExecutionCompleteError `json:"error,omitempty"`
	// CAPI interaction ID for correlating this tool execution with upstream telemetry
	InteractionID *string `json:"interactionId,omitempty"`
	// Whether this tool call was explicitly requested by the user rather than the assistant
	IsUserRequested *bool `json:"isUserRequested,omitempty"`
	// Model identifier that generated this tool call
	Model *string `json:"model,omitempty"`
	// Tool call ID of the parent tool invocation when this event originates from a sub-agent
	// Deprecated: ParentToolCallID is deprecated.
	ParentToolCallID *string `json:"parentToolCallId,omitempty"`
	// Tool execution result on success
	Result *ToolExecutionCompleteResult `json:"result,omitempty"`
	// Whether the tool execution completed successfully
	Success bool `json:"success"`
	// Unique identifier for the completed tool call
	ToolCallID string `json:"toolCallId"`
	// Tool-specific telemetry data (e.g., CodeQL check counts, grep match counts)
	ToolTelemetry map[string]any `json:"toolTelemetry,omitempty"`
}

func (*ToolExecutionCompleteData) sessionEventData() {}

// Tool execution progress notification with status message
type ToolExecutionProgressData struct {
	// Human-readable progress status message (e.g., from an MCP server)
	ProgressMessage string `json:"progressMessage"`
	// Tool call ID this progress notification belongs to
	ToolCallID string `json:"toolCallId"`
}

func (*ToolExecutionProgressData) sessionEventData() {}

// Tool execution startup details including MCP server information when applicable
type ToolExecutionStartData struct {
	// Arguments passed to the tool
	Arguments any `json:"arguments,omitempty"`
	// Name of the MCP server hosting this tool, when the tool is an MCP tool
	McpServerName *string `json:"mcpServerName,omitempty"`
	// Original tool name on the MCP server, when the tool is an MCP tool
	McpToolName *string `json:"mcpToolName,omitempty"`
	// Tool call ID of the parent tool invocation when this event originates from a sub-agent
	// Deprecated: ParentToolCallID is deprecated.
	ParentToolCallID *string `json:"parentToolCallId,omitempty"`
	// Unique identifier for this tool call
	ToolCallID string `json:"toolCallId"`
	// Name of the tool being executed
	ToolName string `json:"toolName"`
}

func (*ToolExecutionStartData) sessionEventData() {}

// Turn abort information including the reason for termination
type AbortData struct {
	// Reason the current turn was aborted (e.g., "user initiated")
	Reason string `json:"reason"`
}

func (*AbortData) sessionEventData() {}

// Turn completion metadata including the turn identifier
type AssistantTurnEndData struct {
	// Identifier of the turn that has ended, matching the corresponding assistant.turn_start event
	TurnID string `json:"turnId"`
}

func (*AssistantTurnEndData) sessionEventData() {}

// Turn initialization metadata including identifier and interaction tracking
type AssistantTurnStartData struct {
	// CAPI interaction ID for correlating this turn with upstream telemetry
	InteractionID *string `json:"interactionId,omitempty"`
	// Identifier for this turn within the agentic loop, typically a stringified turn number
	TurnID string `json:"turnId"`
}

func (*AssistantTurnStartData) sessionEventData() {}

// User input request completion with the user's response
type UserInputCompletedData struct {
	// The user's answer to the input request
	Answer *string `json:"answer,omitempty"`
	// Request ID of the resolved user input request; clients should dismiss any UI for this request
	RequestID string `json:"requestId"`
	// Whether the answer was typed as free-form text rather than selected from choices
	WasFreeform *bool `json:"wasFreeform,omitempty"`
}

func (*UserInputCompletedData) sessionEventData() {}

// User input request notification with question and optional predefined choices
type UserInputRequestedData struct {
	// Whether the user can provide a free-form text response in addition to predefined choices
	AllowFreeform *bool `json:"allowFreeform,omitempty"`
	// Predefined choices for the user to select from, if applicable
	Choices []string `json:"choices,omitempty"`
	// The question or prompt to present to the user
	Question string `json:"question"`
	// Unique identifier for this input request; used to respond via session.respondToUserInput()
	RequestID string `json:"requestId"`
	// The LLM-assigned tool call ID that triggered this request; used by remote UIs to correlate responses
	ToolCallID *string `json:"toolCallId,omitempty"`
}

func (*UserInputRequestedData) sessionEventData() {}

// User-initiated tool invocation request with tool name and arguments
type ToolUserRequestedData struct {
	// Arguments for the tool invocation
	Arguments any `json:"arguments,omitempty"`
	// Unique identifier for this tool call
	ToolCallID string `json:"toolCallId"`
	// Name of the tool the user wants to invoke
	ToolName string `json:"toolName"`
}

func (*ToolUserRequestedData) sessionEventData() {}

// UserMessageData holds the payload for user.message events.
type UserMessageData struct {
	// The agent mode that was active when this message was sent
	AgentMode *UserMessageAgentMode `json:"agentMode,omitempty"`
	// Files, selections, or GitHub references attached to the message
	Attachments []UserMessageAttachment `json:"attachments,omitempty"`
	// The user's message text as displayed in the timeline
	Content string `json:"content"`
	// CAPI interaction ID for correlating this user message with its turn
	InteractionID *string `json:"interactionId,omitempty"`
	// Path-backed native document attachments that stayed on the tagged_files path flow because native upload would exceed the request size limit
	NativeDocumentPathFallbackPaths []string `json:"nativeDocumentPathFallbackPaths,omitempty"`
	// Origin of this message, used for timeline filtering (e.g., "skill-pdf" for skill-injected messages that should be hidden from the user)
	Source *string `json:"source,omitempty"`
	// Normalized document MIME types that were sent natively instead of through tagged_files XML
	SupportedNativeDocumentMIMETypes []string `json:"supportedNativeDocumentMimeTypes,omitempty"`
	// Transformed version of the message sent to the model, with XML wrapping, timestamps, and other augmentations for prompt caching
	TransformedContent *string `json:"transformedContent,omitempty"`
}

func (*UserMessageData) sessionEventData() {}

// Warning message for timeline display with categorization
type SessionWarningData struct {
	// Human-readable warning message for display in the timeline
	Message string `json:"message"`
	// Optional URL associated with this warning that the user can open in a browser
	URL *string `json:"url,omitempty"`
	// Category of warning (e.g., "subscription", "policy", "mcp")
	WarningType string `json:"warningType"`
}

func (*SessionWarningData) sessionEventData() {}

// Working directory and git context at session start
type SessionContextChangedData struct {
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
	HostType *WorkingDirectoryContextHostType `json:"hostType,omitempty"`
	// Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
	Repository *string `json:"repository,omitempty"`
	// Raw host string from the git remote URL (e.g. "github.com", "mycompany.ghe.com", "dev.azure.com")
	RepositoryHost *string `json:"repositoryHost,omitempty"`
}

func (*SessionContextChangedData) sessionEventData() {}

// Workspace file change details including path and operation type
type SessionWorkspaceFileChangedData struct {
	// Whether the file was newly created or updated
	Operation WorkspaceFileChangedOperation `json:"operation"`
	// Relative path within the session workspace files directory
	Path string `json:"path"`
}

func (*SessionWorkspaceFileChangedData) sessionEventData() {}

// A content block within a tool result, which may be text, terminal output, image, audio, or a resource
type ToolExecutionCompleteContent struct {
	// Type discriminator
	Type ToolExecutionCompleteContentType `json:"type"`
	// Working directory where the command was executed
	Cwd *string `json:"cwd,omitempty"`
	// Base64-encoded image data
	Data *string `json:"data,omitempty"`
	// Human-readable description of the resource
	Description *string `json:"description,omitempty"`
	// Process exit code, if the command has completed
	ExitCode *float64 `json:"exitCode,omitempty"`
	// Icons associated with this resource
	Icons []ToolExecutionCompleteContentResourceLinkIcon `json:"icons,omitempty"`
	// MIME type of the image (e.g., image/png, image/jpeg)
	MIMEType *string `json:"mimeType,omitempty"`
	// Resource name identifier
	Name *string `json:"name,omitempty"`
	// The embedded resource contents, either text or base64-encoded binary
	Resource any `json:"resource,omitempty"`
	// Size of the resource in bytes
	Size *float64 `json:"size,omitempty"`
	// The text content
	Text *string `json:"text,omitempty"`
	// Human-readable display title for the resource
	Title *string `json:"title,omitempty"`
	// URI identifying the resource
	URI *string `json:"uri,omitempty"`
}

// A tool invocation request from the assistant
type AssistantMessageToolRequest struct {
	// Arguments to pass to the tool, format depends on the tool
	Arguments any `json:"arguments,omitempty"`
	// Resolved intention summary describing what this specific call does
	IntentionSummary *string `json:"intentionSummary,omitempty"`
	// Name of the MCP server hosting this tool, when the tool is an MCP tool
	McpServerName *string `json:"mcpServerName,omitempty"`
	// Name of the tool being invoked
	Name string `json:"name"`
	// Unique identifier for this tool call
	ToolCallID string `json:"toolCallId"`
	// Human-readable display title for the tool
	ToolTitle *string `json:"toolTitle,omitempty"`
	// Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.
	Type *AssistantMessageToolRequestType `json:"type,omitempty"`
}

// A user message attachment — a file, directory, code selection, blob, or GitHub reference
type UserMessageAttachment struct {
	// Type discriminator
	Type UserMessageAttachmentType `json:"type"`
	// Base64-encoded content
	Data *string `json:"data,omitempty"`
	// User-facing display name for the attachment
	DisplayName *string `json:"displayName,omitempty"`
	// Absolute path to the file containing the selection
	FilePath *string `json:"filePath,omitempty"`
	// Optional line range to scope the attachment to a specific section of the file
	LineRange *UserMessageAttachmentFileLineRange `json:"lineRange,omitempty"`
	// MIME type of the inline data
	MIMEType *string `json:"mimeType,omitempty"`
	// Issue, pull request, or discussion number
	Number *float64 `json:"number,omitempty"`
	// Absolute file path
	Path *string `json:"path,omitempty"`
	// Type of GitHub reference
	ReferenceType *UserMessageAttachmentGithubReferenceType `json:"referenceType,omitempty"`
	// Position range of the selection within the file
	Selection *UserMessageAttachmentSelectionDetails `json:"selection,omitempty"`
	// Current state of the referenced item (e.g., open, closed, merged)
	State *string `json:"state,omitempty"`
	// The selected text content
	Text *string `json:"text,omitempty"`
	// Title of the referenced item
	Title *string `json:"title,omitempty"`
	// URL to the referenced item on GitHub
	URL *string `json:"url,omitempty"`
}

// Aggregate code change metrics for the session
type ShutdownCodeChanges struct {
	// List of file paths that were modified during the session
	FilesModified []string `json:"filesModified"`
	// Total number of lines added during the session
	LinesAdded float64 `json:"linesAdded"`
	// Total number of lines removed during the session
	LinesRemoved float64 `json:"linesRemoved"`
}

// Details of the permission being requested
type PermissionRequest struct {
	// Kind discriminator
	Kind PermissionRequestKind `json:"kind"`
	// Whether this is a store or vote memory operation
	Action *PermissionRequestMemoryAction `json:"action,omitempty"`
	// Arguments to pass to the MCP tool
	Args any `json:"args,omitempty"`
	// Whether the UI can offer session-wide approval for this command pattern
	CanOfferSessionApproval *bool `json:"canOfferSessionApproval,omitempty"`
	// Source references for the stored fact (store only)
	Citations *string `json:"citations,omitempty"`
	// Parsed command identifiers found in the command text
	Commands []PermissionRequestShellCommand `json:"commands,omitempty"`
	// Unified diff showing the proposed changes
	Diff *string `json:"diff,omitempty"`
	// Vote direction (vote only)
	Direction *PermissionRequestMemoryDirection `json:"direction,omitempty"`
	// The fact being stored or voted on
	Fact *string `json:"fact,omitempty"`
	// Path of the file being written to
	FileName *string `json:"fileName,omitempty"`
	// The complete shell command text to be executed
	FullCommandText *string `json:"fullCommandText,omitempty"`
	// Whether the command includes a file write redirection (e.g., > or >>)
	HasWriteFileRedirection *bool `json:"hasWriteFileRedirection,omitempty"`
	// Optional message from the hook explaining why confirmation is needed
	HookMessage *string `json:"hookMessage,omitempty"`
	// Human-readable description of what the command intends to do
	Intention *string `json:"intention,omitempty"`
	// Complete new file contents for newly created files
	NewFileContents *string `json:"newFileContents,omitempty"`
	// Path of the file or directory being read
	Path *string `json:"path,omitempty"`
	// File paths that may be read or written by the command
	PossiblePaths []string `json:"possiblePaths,omitempty"`
	// URLs that may be accessed by the command
	PossibleUrls []PermissionRequestShellPossibleURL `json:"possibleUrls,omitempty"`
	// Whether this MCP tool is read-only (no side effects)
	ReadOnly *bool `json:"readOnly,omitempty"`
	// Reason for the vote (vote only)
	Reason *string `json:"reason,omitempty"`
	// Name of the MCP server providing the tool
	ServerName *string `json:"serverName,omitempty"`
	// Topic or subject of the memory (store only)
	Subject *string `json:"subject,omitempty"`
	// Arguments of the tool call being gated
	ToolArgs any `json:"toolArgs,omitempty"`
	// Tool call ID that triggered this permission request
	ToolCallID *string `json:"toolCallId,omitempty"`
	// Description of what the custom tool does
	ToolDescription *string `json:"toolDescription,omitempty"`
	// Internal name of the MCP tool
	ToolName *string `json:"toolName,omitempty"`
	// Human-readable title of the MCP tool
	ToolTitle *string `json:"toolTitle,omitempty"`
	// URL to be fetched
	URL *string `json:"url,omitempty"`
	// Optional warning message about risks of running this command
	Warning *string `json:"warning,omitempty"`
}

// End position of the selection
type UserMessageAttachmentSelectionDetailsEnd struct {
	// End character offset within the line (0-based)
	Character float64 `json:"character"`
	// End line number (0-based)
	Line float64 `json:"line"`
}

// Error details when the hook failed
type HookEndError struct {
	// Human-readable error message
	Message string `json:"message"`
	// Error stack trace, when available
	Stack *string `json:"stack,omitempty"`
}

// Error details when the tool execution failed
type ToolExecutionCompleteError struct {
	// Machine-readable error code
	Code *string `json:"code,omitempty"`
	// Human-readable error message
	Message string `json:"message"`
}

// Icon image for a resource
type ToolExecutionCompleteContentResourceLinkIcon struct {
	// MIME type of the icon image
	MIMEType *string `json:"mimeType,omitempty"`
	// Available icon sizes (e.g., ['16x16', '32x32'])
	Sizes []string `json:"sizes,omitempty"`
	// URL or path to the icon image
	Src string `json:"src"`
	// Theme variant this icon is intended for
	Theme *ToolExecutionCompleteContentResourceLinkIconTheme `json:"theme,omitempty"`
}

// JSON Schema describing the form fields to present to the user (form mode only)
type ElicitationRequestedSchema struct {
	// Form field definitions, keyed by field name
	Properties map[string]any `json:"properties"`
	// List of required field names
	Required []string `json:"required,omitempty"`
	// Schema type indicator (always 'object')
	Type string `json:"type"`
}

// Metadata about the prompt template and its construction
type SystemMessageMetadata struct {
	// Version identifier of the prompt template used
	PromptVersion *string `json:"promptVersion,omitempty"`
	// Template variables used when constructing the prompt
	Variables map[string]any `json:"variables,omitempty"`
}

// Optional line range to scope the attachment to a specific section of the file
type UserMessageAttachmentFileLineRange struct {
	// End line number (1-based, inclusive)
	End float64 `json:"end"`
	// Start line number (1-based)
	Start float64 `json:"start"`
}

// Per-request cost and usage data from the CAPI copilot_usage response field
type AssistantUsageCopilotUsage struct {
	// Itemized token usage breakdown
	TokenDetails []AssistantUsageCopilotUsageTokenDetail `json:"tokenDetails"`
	// Total cost in nano-AIU (AI Units) for this request
	TotalNanoAiu float64 `json:"totalNanoAiu"`
}

// Position range of the selection within the file
type UserMessageAttachmentSelectionDetails struct {
	// End position of the selection
	End UserMessageAttachmentSelectionDetailsEnd `json:"end"`
	// Start position of the selection
	Start UserMessageAttachmentSelectionDetailsStart `json:"start"`
}

// Repository context for the handed-off session
type HandoffRepository struct {
	// Git branch name, if applicable
	Branch *string `json:"branch,omitempty"`
	// Repository name
	Name string `json:"name"`
	// Repository owner (user or organization)
	Owner string `json:"owner"`
}

// Request count and cost metrics
type ShutdownModelMetricRequests struct {
	// Cumulative cost multiplier for requests to this model
	Cost float64 `json:"cost"`
	// Total number of API requests made to this model
	Count float64 `json:"count"`
}

// Start position of the selection
type UserMessageAttachmentSelectionDetailsStart struct {
	// Start character offset within the line (0-based)
	Character float64 `json:"character"`
	// Start line number (0-based)
	Line float64 `json:"line"`
}

// Static OAuth client configuration, if the server specifies one
type McpOauthRequiredStaticClientConfig struct {
	// OAuth client ID for the server
	ClientID string `json:"clientId"`
	// Whether this is a public OAuth client
	PublicClient *bool `json:"publicClient,omitempty"`
}

// Structured metadata identifying what triggered this notification
type SystemNotification struct {
	// Type discriminator
	Type SystemNotificationType `json:"type"`
	// Unique identifier of the background agent
	AgentID *string `json:"agentId,omitempty"`
	// Type of the agent (e.g., explore, task, general-purpose)
	AgentType *string `json:"agentType,omitempty"`
	// Human-readable description of the agent task
	Description *string `json:"description,omitempty"`
	// Unique identifier of the inbox entry
	EntryID *string `json:"entryId,omitempty"`
	// Exit code of the shell command, if available
	ExitCode *float64 `json:"exitCode,omitempty"`
	// The full prompt given to the background agent
	Prompt *string `json:"prompt,omitempty"`
	// Human-readable name of the sender
	SenderName *string `json:"senderName,omitempty"`
	// Category of the sender (e.g., ambient-agent, plugin, hook)
	SenderType *string `json:"senderType,omitempty"`
	// Unique identifier of the shell session
	ShellID *string `json:"shellId,omitempty"`
	// Whether the agent completed successfully or failed
	Status *SystemNotificationAgentCompletedStatus `json:"status,omitempty"`
	// Short summary shown before the agent decides whether to read the inbox
	Summary *string `json:"summary,omitempty"`
}

// The result of the permission request
type PermissionCompletedResult struct {
	// The outcome of the permission request
	Kind PermissionCompletedKind `json:"kind"`
}

// Token usage breakdown
type ShutdownModelMetricUsage struct {
	// Total tokens read from prompt cache across all requests
	CacheReadTokens float64 `json:"cacheReadTokens"`
	// Total tokens written to prompt cache across all requests
	CacheWriteTokens float64 `json:"cacheWriteTokens"`
	// Total input tokens consumed across all requests to this model
	InputTokens float64 `json:"inputTokens"`
	// Total output tokens produced across all requests to this model
	OutputTokens float64 `json:"outputTokens"`
	// Total reasoning tokens produced across all requests to this model
	ReasoningTokens *float64 `json:"reasoningTokens,omitempty"`
}

// Token usage breakdown for the compaction LLM call
type CompactionCompleteCompactionTokensUsed struct {
	// Cached input tokens reused in the compaction LLM call
	CachedInput float64 `json:"cachedInput"`
	// Input tokens consumed by the compaction LLM call
	Input float64 `json:"input"`
	// Output tokens produced by the compaction LLM call
	Output float64 `json:"output"`
}

// Token usage detail for a single billing category
type AssistantUsageCopilotUsageTokenDetail struct {
	// Number of tokens in this billing batch
	BatchSize float64 `json:"batchSize"`
	// Cost per batch of tokens
	CostPerBatch float64 `json:"costPerBatch"`
	// Total token count for this entry
	TokenCount float64 `json:"tokenCount"`
	// Token category (e.g., "input", "output")
	TokenType string `json:"tokenType"`
}

// Tool execution result on success
type ToolExecutionCompleteResult struct {
	// Concise tool result text sent to the LLM for chat completion, potentially truncated for token efficiency
	Content string `json:"content"`
	// Structured content blocks (text, images, audio, resources) returned by the tool in their native format
	Contents []ToolExecutionCompleteContent `json:"contents,omitempty"`
	// Full detailed tool result for UI/timeline display, preserving complete content such as diffs. Falls back to content when absent.
	DetailedContent *string `json:"detailedContent,omitempty"`
}

// UI capability changes
type CapabilitiesChangedUI struct {
	// Whether elicitation is now supported
	Elicitation *bool `json:"elicitation,omitempty"`
}

// Working directory and git context at session start
type WorkingDirectoryContext struct {
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
	HostType *WorkingDirectoryContextHostType `json:"hostType,omitempty"`
	// Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
	Repository *string `json:"repository,omitempty"`
	// Raw host string from the git remote URL (e.g. "github.com", "mycompany.ghe.com", "dev.azure.com")
	RepositoryHost *string `json:"repositoryHost,omitempty"`
}

type AssistantUsageQuotaSnapshot struct {
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

type CommandsChangedCommand struct {
	Description *string `json:"description,omitempty"`
	Name        string  `json:"name"`
}

type CustomAgentsUpdatedAgent struct {
	// Description of what the agent does
	Description string `json:"description"`
	// Human-readable display name
	DisplayName string `json:"displayName"`
	// Unique identifier for the agent
	ID string `json:"id"`
	// Model override for this agent, if set
	Model *string `json:"model,omitempty"`
	// Internal name of the agent
	Name string `json:"name"`
	// Source location: user, project, inherited, remote, or plugin
	Source string `json:"source"`
	// List of tool names available to this agent
	Tools []string `json:"tools"`
	// Whether the agent can be selected by the user
	UserInvocable bool `json:"userInvocable"`
}

type ExtensionsLoadedExtension struct {
	// Source-qualified extension ID (e.g., 'project:my-ext', 'user:auth-helper')
	ID string `json:"id"`
	// Extension name (directory name)
	Name string `json:"name"`
	// Discovery source
	Source ExtensionsLoadedExtensionSource `json:"source"`
	// Current status: running, disabled, failed, or starting
	Status ExtensionsLoadedExtensionStatus `json:"status"`
}

type McpServersLoadedServer struct {
	// Error message if the server failed to connect
	Error *string `json:"error,omitempty"`
	// Server name (config key)
	Name string `json:"name"`
	// Configuration source: user, workspace, plugin, or builtin
	Source *string `json:"source,omitempty"`
	// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
	Status McpServersLoadedServerStatus `json:"status"`
}

type PermissionRequestShellCommand struct {
	// Command identifier (e.g., executable name)
	Identifier string `json:"identifier"`
	// Whether this command is read-only (no side effects)
	ReadOnly bool `json:"readOnly"`
}

type PermissionRequestShellPossibleURL struct {
	// URL that may be accessed by the command
	URL string `json:"url"`
}

type ShutdownModelMetric struct {
	// Request count and cost metrics
	Requests ShutdownModelMetricRequests `json:"requests"`
	// Token usage breakdown
	Usage ShutdownModelMetricUsage `json:"usage"`
}

type SkillsLoadedSkill struct {
	// Description of what the skill does
	Description string `json:"description"`
	// Whether the skill is currently enabled
	Enabled bool `json:"enabled"`
	// Unique identifier for the skill
	Name string `json:"name"`
	// Absolute path to the skill file, if available
	Path *string `json:"path,omitempty"`
	// Source location type of the skill (e.g., project, personal, plugin)
	Source string `json:"source"`
	// Whether the skill can be invoked by the user as a slash command
	UserInvocable bool `json:"userInvocable"`
}

// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
type McpServersLoadedServerStatus string

const (
	McpServersLoadedServerStatusConnected     McpServersLoadedServerStatus = "connected"
	McpServersLoadedServerStatusFailed        McpServersLoadedServerStatus = "failed"
	McpServersLoadedServerStatusNeedsAuth     McpServersLoadedServerStatus = "needs-auth"
	McpServersLoadedServerStatusPending       McpServersLoadedServerStatus = "pending"
	McpServersLoadedServerStatusDisabled      McpServersLoadedServerStatus = "disabled"
	McpServersLoadedServerStatusNotConfigured McpServersLoadedServerStatus = "not_configured"
)

// Current status: running, disabled, failed, or starting
type ExtensionsLoadedExtensionStatus string

const (
	ExtensionsLoadedExtensionStatusRunning  ExtensionsLoadedExtensionStatus = "running"
	ExtensionsLoadedExtensionStatusDisabled ExtensionsLoadedExtensionStatus = "disabled"
	ExtensionsLoadedExtensionStatusFailed   ExtensionsLoadedExtensionStatus = "failed"
	ExtensionsLoadedExtensionStatusStarting ExtensionsLoadedExtensionStatus = "starting"
)

// Discovery source
type ExtensionsLoadedExtensionSource string

const (
	ExtensionsLoadedExtensionSourceProject ExtensionsLoadedExtensionSource = "project"
	ExtensionsLoadedExtensionSourceUser    ExtensionsLoadedExtensionSource = "user"
)

// Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent.
type ElicitationRequestedMode string

const (
	ElicitationRequestedModeForm ElicitationRequestedMode = "form"
	ElicitationRequestedModeURL  ElicitationRequestedMode = "url"
)

// Hosting platform type of the repository (github or ado)
type WorkingDirectoryContextHostType string

const (
	WorkingDirectoryContextHostTypeGithub WorkingDirectoryContextHostType = "github"
	WorkingDirectoryContextHostTypeAdo    WorkingDirectoryContextHostType = "ado"
)

// Kind discriminator for PermissionRequest.
type PermissionRequestKind string

const (
	PermissionRequestKindShell      PermissionRequestKind = "shell"
	PermissionRequestKindWrite      PermissionRequestKind = "write"
	PermissionRequestKindRead       PermissionRequestKind = "read"
	PermissionRequestKindMcp        PermissionRequestKind = "mcp"
	PermissionRequestKindURL        PermissionRequestKind = "url"
	PermissionRequestKindMemory     PermissionRequestKind = "memory"
	PermissionRequestKindCustomTool PermissionRequestKind = "custom-tool"
	PermissionRequestKindHook       PermissionRequestKind = "hook"
)

// Message role: "system" for system prompts, "developer" for developer-injected instructions
type SystemMessageRole string

const (
	SystemMessageRoleSystem    SystemMessageRole = "system"
	SystemMessageRoleDeveloper SystemMessageRole = "developer"
)

// New connection status: connected, failed, needs-auth, pending, disabled, or not_configured
type McpServerStatusChangedStatus string

const (
	McpServerStatusChangedStatusConnected     McpServerStatusChangedStatus = "connected"
	McpServerStatusChangedStatusFailed        McpServerStatusChangedStatus = "failed"
	McpServerStatusChangedStatusNeedsAuth     McpServerStatusChangedStatus = "needs-auth"
	McpServerStatusChangedStatusPending       McpServerStatusChangedStatus = "pending"
	McpServerStatusChangedStatusDisabled      McpServerStatusChangedStatus = "disabled"
	McpServerStatusChangedStatusNotConfigured McpServerStatusChangedStatus = "not_configured"
)

// Origin type of the session being handed off
type HandoffSourceType string

const (
	HandoffSourceTypeRemote HandoffSourceType = "remote"
	HandoffSourceTypeLocal  HandoffSourceType = "local"
)

// The agent mode that was active when this message was sent
type UserMessageAgentMode string

const (
	UserMessageAgentModeInteractive UserMessageAgentMode = "interactive"
	UserMessageAgentModePlan        UserMessageAgentMode = "plan"
	UserMessageAgentModeAutopilot   UserMessageAgentMode = "autopilot"
	UserMessageAgentModeShell       UserMessageAgentMode = "shell"
)

// The outcome of the permission request
type PermissionCompletedKind string

const (
	PermissionCompletedKindApproved                                       PermissionCompletedKind = "approved"
	PermissionCompletedKindDeniedByRules                                  PermissionCompletedKind = "denied-by-rules"
	PermissionCompletedKindDeniedNoApprovalRuleAndCouldNotRequestFromUser PermissionCompletedKind = "denied-no-approval-rule-and-could-not-request-from-user"
	PermissionCompletedKindDeniedInteractivelyByUser                      PermissionCompletedKind = "denied-interactively-by-user"
	PermissionCompletedKindDeniedByContentExclusionPolicy                 PermissionCompletedKind = "denied-by-content-exclusion-policy"
	PermissionCompletedKindDeniedByPermissionRequestHook                  PermissionCompletedKind = "denied-by-permission-request-hook"
)

// The type of operation performed on the plan file
type PlanChangedOperation string

const (
	PlanChangedOperationCreate PlanChangedOperation = "create"
	PlanChangedOperationUpdate PlanChangedOperation = "update"
	PlanChangedOperationDelete PlanChangedOperation = "delete"
)

// The user action: "accept" (submitted form), "decline" (explicitly refused), or "cancel" (dismissed)
type ElicitationCompletedAction string

const (
	ElicitationCompletedActionAccept  ElicitationCompletedAction = "accept"
	ElicitationCompletedActionDecline ElicitationCompletedAction = "decline"
	ElicitationCompletedActionCancel  ElicitationCompletedAction = "cancel"
)

// Theme variant this icon is intended for
type ToolExecutionCompleteContentResourceLinkIconTheme string

const (
	ToolExecutionCompleteContentResourceLinkIconThemeLight ToolExecutionCompleteContentResourceLinkIconTheme = "light"
	ToolExecutionCompleteContentResourceLinkIconThemeDark  ToolExecutionCompleteContentResourceLinkIconTheme = "dark"
)

// Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.
type AssistantMessageToolRequestType string

const (
	AssistantMessageToolRequestTypeFunction AssistantMessageToolRequestType = "function"
	AssistantMessageToolRequestTypeCustom   AssistantMessageToolRequestType = "custom"
)

// Type discriminator for SystemNotification.
type SystemNotificationType string

const (
	SystemNotificationTypeAgentCompleted         SystemNotificationType = "agent_completed"
	SystemNotificationTypeAgentIdle              SystemNotificationType = "agent_idle"
	SystemNotificationTypeNewInboxMessage        SystemNotificationType = "new_inbox_message"
	SystemNotificationTypeShellCompleted         SystemNotificationType = "shell_completed"
	SystemNotificationTypeShellDetachedCompleted SystemNotificationType = "shell_detached_completed"
)

// Type discriminator for ToolExecutionCompleteContent.
type ToolExecutionCompleteContentType string

const (
	ToolExecutionCompleteContentTypeText         ToolExecutionCompleteContentType = "text"
	ToolExecutionCompleteContentTypeTerminal     ToolExecutionCompleteContentType = "terminal"
	ToolExecutionCompleteContentTypeImage        ToolExecutionCompleteContentType = "image"
	ToolExecutionCompleteContentTypeAudio        ToolExecutionCompleteContentType = "audio"
	ToolExecutionCompleteContentTypeResourceLink ToolExecutionCompleteContentType = "resource_link"
	ToolExecutionCompleteContentTypeResource     ToolExecutionCompleteContentType = "resource"
)

// Type discriminator for UserMessageAttachment.
type UserMessageAttachmentType string

const (
	UserMessageAttachmentTypeFile            UserMessageAttachmentType = "file"
	UserMessageAttachmentTypeDirectory       UserMessageAttachmentType = "directory"
	UserMessageAttachmentTypeSelection       UserMessageAttachmentType = "selection"
	UserMessageAttachmentTypeGithubReference UserMessageAttachmentType = "github_reference"
	UserMessageAttachmentTypeBlob            UserMessageAttachmentType = "blob"
)

// Type of GitHub reference
type UserMessageAttachmentGithubReferenceType string

const (
	UserMessageAttachmentGithubReferenceTypeIssue      UserMessageAttachmentGithubReferenceType = "issue"
	UserMessageAttachmentGithubReferenceTypePr         UserMessageAttachmentGithubReferenceType = "pr"
	UserMessageAttachmentGithubReferenceTypeDiscussion UserMessageAttachmentGithubReferenceType = "discussion"
)

// Vote direction (vote only)
type PermissionRequestMemoryDirection string

const (
	PermissionRequestMemoryDirectionUpvote   PermissionRequestMemoryDirection = "upvote"
	PermissionRequestMemoryDirectionDownvote PermissionRequestMemoryDirection = "downvote"
)

// Whether the agent completed successfully or failed
type SystemNotificationAgentCompletedStatus string

const (
	SystemNotificationAgentCompletedStatusCompleted SystemNotificationAgentCompletedStatus = "completed"
	SystemNotificationAgentCompletedStatusFailed    SystemNotificationAgentCompletedStatus = "failed"
)

// Whether the file was newly created or updated
type WorkspaceFileChangedOperation string

const (
	WorkspaceFileChangedOperationCreate WorkspaceFileChangedOperation = "create"
	WorkspaceFileChangedOperationUpdate WorkspaceFileChangedOperation = "update"
)

// Whether the session ended normally ("routine") or due to a crash/fatal error ("error")
type ShutdownType string

const (
	ShutdownTypeRoutine ShutdownType = "routine"
	ShutdownTypeError   ShutdownType = "error"
)

// Whether this is a store or vote memory operation
type PermissionRequestMemoryAction string

const (
	PermissionRequestMemoryActionStore PermissionRequestMemoryAction = "store"
	PermissionRequestMemoryActionVote  PermissionRequestMemoryAction = "vote"
)

// Type aliases for convenience.
type (
	PermissionRequestCommand = PermissionRequestShellCommand
	PossibleURL              = PermissionRequestShellPossibleURL
	Attachment               = UserMessageAttachment
	AttachmentType           = UserMessageAttachmentType
)

// Constant aliases for convenience.
const (
	AttachmentTypeFile            = UserMessageAttachmentTypeFile
	AttachmentTypeDirectory       = UserMessageAttachmentTypeDirectory
	AttachmentTypeSelection       = UserMessageAttachmentTypeSelection
	AttachmentTypeGithubReference = UserMessageAttachmentTypeGithubReference
	AttachmentTypeBlob            = UserMessageAttachmentTypeBlob
)
