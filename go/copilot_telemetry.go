/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package copilot

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/url"
	"os"
	"runtime/debug"
	"strconv"
	"strings"
	"sync"
	"time"

	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/codes"
	"go.opentelemetry.io/otel/metric"
	"go.opentelemetry.io/otel/trace"
)

// sdkVersion returns the module version from build info, or empty string.
func sdkVersion() string {
	bi, ok := debug.ReadBuildInfo()
	if !ok {
		return ""
	}
	return bi.Main.Version
}

// TelemetryConfig configures OpenTelemetry instrumentation for the Copilot SDK.
type TelemetryConfig struct {
	// EnableSensitiveData controls whether potentially sensitive data
	// (message content, tool arguments/results) is included in telemetry.
	// When nil, reads OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT
	// env var. Defaults to false.
	EnableSensitiveData *bool

	// SourceName is the name used for the Tracer and Meter.
	// Defaults to "github.copilot.sdk".
	SourceName string
}

// copilotTelemetry provides OpenTelemetry instrumentation for the Copilot SDK,
// implementing the Semantic Conventions for Generative AI systems.
type copilotTelemetry struct {
	tracer trace.Tracer
	meter  metric.Meter

	operationDurationHistogram  metric.Float64Histogram
	tokenUsageHistogram         metric.Int64Histogram
	timeToFirstChunkHistogram   metric.Float64Histogram
	timePerOutputChunkHistogram metric.Float64Histogram

	enableSensitiveData bool
}

func newCopilotTelemetry(config *TelemetryConfig) *copilotTelemetry {
	sourceName := otelDefaultSourceName
	if config != nil && config.SourceName != "" {
		sourceName = config.SourceName
	}

	enableSensitive := false
	if config != nil && config.EnableSensitiveData != nil {
		enableSensitive = *config.EnableSensitiveData
	} else {
		enableSensitive = strings.EqualFold(os.Getenv(otelCaptureMessageContentEnvVar), "true")
	}

	version := sdkVersion()
	var tracerOpts []trace.TracerOption
	var meterOpts []metric.MeterOption
	if version != "" {
		tracerOpts = append(tracerOpts, trace.WithInstrumentationVersion(version))
		meterOpts = append(meterOpts, metric.WithInstrumentationVersion(version))
	}

	tracer := otel.GetTracerProvider().Tracer(sourceName, tracerOpts...)
	m := otel.GetMeterProvider().Meter(sourceName, meterOpts...)

	opDur, _ := m.Float64Histogram(
		otelMetricOperationDuration,
		metric.WithUnit(otelSecondsUnit),
		metric.WithDescription(otelDescOperationDuration),
		metric.WithExplicitBucketBoundaries(otelDurationBucketBoundaries...),
	)

	tokUsage, _ := m.Int64Histogram(
		otelMetricTokenUsage,
		metric.WithUnit(otelTokensUnit),
		metric.WithDescription(otelDescTokenUsage),
		metric.WithExplicitBucketBoundaries(otelTokenBucketBoundaries...),
	)

	ttfc, _ := m.Float64Histogram(
		otelMetricTimeToFirstChunk,
		metric.WithUnit(otelSecondsUnit),
		metric.WithDescription(otelDescTimeToFirstChunk),
		metric.WithExplicitBucketBoundaries(otelDurationBucketBoundaries...),
	)

	tpoc, _ := m.Float64Histogram(
		otelMetricTimePerOutputChunk,
		metric.WithUnit(otelSecondsUnit),
		metric.WithDescription(otelDescTimePerOutputChunk),
		metric.WithExplicitBucketBoundaries(otelDurationBucketBoundaries...),
	)

	return &copilotTelemetry{
		tracer:                      tracer,
		meter:                       m,
		operationDurationHistogram:  opDur,
		tokenUsageHistogram:         tokUsage,
		timeToFirstChunkHistogram:   ttfc,
		timePerOutputChunkHistogram: tpoc,
		enableSensitiveData:         enableSensitive,
	}
}

// startInvokeAgentSpan starts an invoke_agent span for a session turn.
func (ct *copilotTelemetry) startInvokeAgentSpan(
	ctx context.Context,
	sessionID string,
	model string,
	providerName string,
	serverAddress string,
	serverPort int,
	agentName string,
	agentDescription string,
) (context.Context, trace.Span) {
	displayName := otelInvokeAgent
	if agentName != "" {
		displayName = otelInvokeAgent + " " + agentName
	}

	attrs := []attribute.KeyValue{
		attribute.String(otelGenAIOperationName, otelInvokeAgent),
		attribute.String(otelGenAIProviderName, providerName),
		attribute.String(otelGenAIAgentID, sessionID),
		attribute.String(otelGenAIConversationID, sessionID),
	}

	if model != "" {
		attrs = append(attrs, attribute.String(otelGenAIRequestModel, model))
	}

	if agentName != "" {
		attrs = append(attrs, attribute.String(otelGenAIAgentName, agentName))
	}
	if agentDescription != "" {
		attrs = append(attrs, attribute.String(otelGenAIAgentDescription, agentDescription))
	}

	if serverAddress != "" {
		attrs = append(attrs, attribute.String(otelServerAddress, serverAddress))
		if serverPort > 0 {
			attrs = append(attrs, attribute.Int(otelServerPort, serverPort))
		}
	}

	ctx, span := ct.tracer.Start(ctx, displayName,
		trace.WithSpanKind(trace.SpanKindClient),
		trace.WithAttributes(attrs...),
	)
	return ctx, span
}

// startChatSpan starts a chat child span for an individual LLM turn.
func (ct *copilotTelemetry) startChatSpan(
	ctx context.Context,
	model string,
	providerName string,
	serverAddress string,
	serverPort int,
	conversationID string,
) (context.Context, trace.Span) {
	displayName := otelChat
	if model != "" {
		displayName = otelChat + " " + model
	}

	attrs := []attribute.KeyValue{
		attribute.String(otelGenAIOperationName, otelChat),
		attribute.String(otelGenAIProviderName, providerName),
	}

	if model != "" {
		attrs = append(attrs, attribute.String(otelGenAIRequestModel, model))
	}
	if conversationID != "" {
		attrs = append(attrs, attribute.String(otelGenAIConversationID, conversationID))
	}
	if serverAddress != "" {
		attrs = append(attrs, attribute.String(otelServerAddress, serverAddress))
		if serverPort > 0 {
			attrs = append(attrs, attribute.Int(otelServerPort, serverPort))
		}
	}

	ctx, span := ct.tracer.Start(ctx, displayName,
		trace.WithSpanKind(trace.SpanKindClient),
		trace.WithAttributes(attrs...),
	)
	return ctx, span
}

// startExecuteToolSpan starts an execute_tool span for a tool call.
func (ct *copilotTelemetry) startExecuteToolSpan(
	ctx context.Context,
	toolName string,
	toolCallID string,
	description string,
	arguments any,
) (context.Context, trace.Span) {
	displayName := otelExecuteTool + " " + toolName

	attrs := []attribute.KeyValue{
		attribute.String(otelGenAIOperationName, otelExecuteTool),
		attribute.String(otelGenAIToolName, toolName),
		attribute.String(otelGenAIToolCallID, toolCallID),
		attribute.String(otelGenAIToolType, "function"),
	}

	if description != "" {
		attrs = append(attrs, attribute.String(otelGenAIToolDescription, description))
	}

	if ct.enableSensitiveData && arguments != nil {
		attrs = append(attrs, attribute.String(otelGenAIToolCallArguments, serializeTagValue(arguments)))
	}

	ctx, span := ct.tracer.Start(ctx, displayName,
		trace.WithSpanKind(trace.SpanKindInternal),
		trace.WithAttributes(attrs...),
	)
	return ctx, span
}

// setExecuteToolResult sets the tool result on the span if sensitive data is enabled.
func (ct *copilotTelemetry) setExecuteToolResult(span trace.Span, result any) {
	if !ct.enableSensitiveData || span == nil || result == nil {
		return
	}
	span.SetAttributes(attribute.String(otelGenAIToolCallResult, serializeTagValue(result)))
}

// recordTokenUsageMetrics records token usage metrics at turn completion.
func (ct *copilotTelemetry) recordTokenUsageMetrics(
	ctx context.Context,
	inputTokens *int,
	outputTokens *int,
	requestModel string,
	responseModel string,
	providerName string,
	serverAddress string,
	serverPort int,
	err error,
	operationName string,
) {
	baseTags := createMetricAttributes(operationName, requestModel, responseModel, providerName, serverAddress, serverPort)
	if err != nil {
		baseTags = append(baseTags, attribute.String(otelErrorType, errorTypeName(err)))
	}

	if inputTokens != nil {
		tags := make([]attribute.KeyValue, len(baseTags)+1)
		copy(tags, baseTags)
		tags[len(baseTags)] = attribute.String(otelGenAITokenType, otelTokenTypeInput)
		ct.tokenUsageHistogram.Record(ctx, int64(*inputTokens), metric.WithAttributes(tags...))
	}
	if outputTokens != nil {
		tags := make([]attribute.KeyValue, len(baseTags)+1)
		copy(tags, baseTags)
		tags[len(baseTags)] = attribute.String(otelGenAITokenType, otelTokenTypeOutput)
		ct.tokenUsageHistogram.Record(ctx, int64(*outputTokens), metric.WithAttributes(tags...))
	}
}

// recordOperationDuration records operation duration metric.
func (ct *copilotTelemetry) recordOperationDuration(
	ctx context.Context,
	durationSeconds float64,
	requestModel string,
	responseModel string,
	providerName string,
	serverAddress string,
	serverPort int,
	err error,
	operationName string,
) {
	attrs := createMetricAttributes(operationName, requestModel, responseModel, providerName, serverAddress, serverPort)
	if err != nil {
		attrs = append(attrs, attribute.String(otelErrorType, errorTypeName(err)))
	}
	ct.operationDurationHistogram.Record(ctx, durationSeconds, metric.WithAttributes(attrs...))
}

// recordTimeToFirstChunk records time-to-first-chunk metric.
func (ct *copilotTelemetry) recordTimeToFirstChunk(
	ctx context.Context,
	durationSeconds float64,
	requestModel string,
	responseModel string,
	providerName string,
	serverAddress string,
	serverPort int,
) {
	attrs := createMetricAttributes(otelChat, requestModel, responseModel, providerName, serverAddress, serverPort)
	ct.timeToFirstChunkHistogram.Record(ctx, durationSeconds, metric.WithAttributes(attrs...))
}

// recordTimePerOutputChunk records time-per-output-chunk metric.
func (ct *copilotTelemetry) recordTimePerOutputChunk(
	ctx context.Context,
	durationSeconds float64,
	requestModel string,
	responseModel string,
	providerName string,
	serverAddress string,
	serverPort int,
) {
	attrs := createMetricAttributes(otelChat, requestModel, responseModel, providerName, serverAddress, serverPort)
	ct.timePerOutputChunkHistogram.Record(ctx, durationSeconds, metric.WithAttributes(attrs...))
}

// sessionTurnError represents an error from the session or subagent.
type sessionTurnError struct {
	Message string
}

func (e *sessionTurnError) Error() string {
	return e.Message
}

// sessionDestroyedError represents a session destroyed while a turn is in progress.
type sessionDestroyedError struct {
	Message string
}

func (e *sessionDestroyedError) Error() string {
	return e.Message
}

// toolPanicError represents a panic during tool execution.
type toolPanicError struct {
	Value any
}

func (e *toolPanicError) Error() string {
	return fmt.Sprintf("tool panic: %v", e.Value)
}

// errorTypeName returns the error type name for telemetry.
func errorTypeName(err error) string {
	switch err.(type) {
	case *sessionTurnError:
		return "SessionTurnError"
	case *sessionDestroyedError:
		return "SessionDestroyedError"
	case *toolPanicError:
		return "ToolPanicError"
	default:
		return fmt.Sprintf("%T", err)
	}
}

// recordSpanError records an error on a span.
func recordSpanError(span trace.Span, err error) {
	if span == nil || err == nil {
		return
	}
	span.SetAttributes(attribute.String(otelErrorType, errorTypeName(err)))
	span.SetStatus(codes.Error, err.Error())
}

// normalizeProviderName normalizes a provider type string to its OpenTelemetry semantic convention name.
func normalizeProviderName(providerType string) string {
	if providerType == "" {
		return otelDefaultProviderName
	}
	switch strings.ToLower(strings.TrimSpace(providerType)) {
	case "openai":
		return "openai"
	case "azure":
		return "azure.ai.openai"
	case "anthropic":
		return "anthropic"
	default:
		return otelDefaultProviderName
	}
}

// parseServerAddress extracts host and port from a URL.
func parseServerAddress(baseURL string) (address string, port int) {
	if baseURL == "" {
		return "", 0
	}
	u, err := url.Parse(baseURL)
	if err != nil || u.Host == "" {
		return "", 0
	}
	host := u.Hostname()
	if host == "" {
		return "", 0
	}
	portStr := u.Port()
	if portStr != "" {
		if p, err := strconv.Atoi(portStr); err == nil && p > 0 {
			return host, p
		}
	}
	switch strings.ToLower(u.Scheme) {
	case "https":
		return host, 443
	case "http":
		return host, 80
	}
	return host, 0
}

// createMetricAttributes builds the common metric attribute set.
func createMetricAttributes(
	operationName string,
	requestModel string,
	responseModel string,
	providerName string,
	serverAddress string,
	serverPort int,
) []attribute.KeyValue {
	attrs := []attribute.KeyValue{
		attribute.String(otelGenAIOperationName, operationName),
		attribute.String(otelGenAIProviderName, providerName),
	}
	if requestModel != "" {
		attrs = append(attrs, attribute.String(otelGenAIRequestModel, requestModel))
	}
	if responseModel != "" {
		attrs = append(attrs, attribute.String(otelGenAIResponseModel, responseModel))
	}
	if serverAddress != "" {
		attrs = append(attrs, attribute.String(otelServerAddress, serverAddress))
		if serverPort > 0 {
			attrs = append(attrs, attribute.Int(otelServerPort, serverPort))
		}
	}
	return attrs
}

func serializeTagValue(value any) string {
	switch v := value.(type) {
	case string:
		return v
	case json.RawMessage:
		return string(v)
	default:
		b, err := json.Marshal(v)
		if err != nil {
			return fmt.Sprintf("%v", v)
		}
		return string(b)
	}
}

// ============================================================================
// OTel message types for parts-based JSON
// ============================================================================

type otelPart struct {
	Type          string `json:"type"`
	Content       string `json:"content,omitempty"`
	ID            string `json:"id,omitempty"`
	Name          string `json:"name,omitempty"`
	Arguments     any    `json:"arguments,omitempty"`
	Response      any    `json:"response,omitempty"`
	MCPServerName string `json:"-"` // not serialized directly; used for server_tool_call nesting
}

type otelMsg struct {
	Role         string     `json:"-"`
	Parts        []otelPart `json:"-"`
	FinishReason string     `json:"-"`
}

// ============================================================================
// Subagent state
// ============================================================================

type subagentState struct {
	invokeAgentSpan     trace.Span
	spanCtx             context.Context
	startTime           time.Time
	chatSpan            trace.Span
	agentName           string
	responseModel       string
	responseID          string
	inputTokens         int
	outputTokens        int
	cacheReadTokens     int
	cacheCreationTokens int
	inputMessages       []otelMsg
	outputMessages      []otelMsg
}

// ============================================================================
// agentTurnTracker — two-level span hierarchy: invoke_agent → chat
// ============================================================================

type agentTurnTracker struct {
	telemetry *copilotTelemetry
	sessionID string
	mu        sync.Mutex

	// Context fields set once at construction
	requestModel           string
	agentName              string
	agentDescription       string
	providerName           string
	serverAddress          string
	serverPort             int
	systemInstructionsJSON string
	toolDefinitionsJSON    string
	isStreaming            bool

	// Per-invoke_agent mutable state
	agentSpan       trace.Span
	agentSpanCtx    context.Context
	agentStartTime  time.Time
	agentInputMsgs  []otelMsg
	agentOutputMsgs []otelMsg
	activeSubagents map[string]*subagentState

	// Pending tool call parent contexts
	pendingToolParents map[string]context.Context

	// MCP server tool tracking: toolCallID → mcpServerName
	serverToolCallIDs map[string]string

	// Agent-level accumulated usage across all chat turns
	agentResponseModel            string
	agentResponseID               string
	agentTotalInputTokens         int
	agentTotalOutputTokens        int
	agentTotalCacheReadTokens     int
	agentTotalCacheCreationTokens int
	agentTotalCost                float64
	agentTotalAIU                 float64

	// Per-chat-turn mutable state (reset on each assistant.turn_start)
	turnSpan                 trace.Span
	turnStartTime            time.Time
	firstOutputChunkRecorded bool
	lastOutputChunkTime      time.Time
	turnResponseModel        string
	turnResponseID           string
	turnInputTokens          int
	turnOutputTokens         int
	turnCacheReadTokens      int
	turnCacheCreationTokens  int
	turnInputMsgs            []otelMsg
	turnOutputMsgs           []otelMsg

	// Copilot-specific per-turn attributes
	turnCost           *float64
	turnServerDuration *float64
	turnInitiator      *string
	turnAIU            *float64
	turnID             *string
	turnInteractionID  *string

	// Stashed user prompt for the first chat turn
	pendingUserPrompt string
}

func newAgentTurnTracker(telemetry *copilotTelemetry, sessionID string, model string, provider *ProviderConfig, systemMessage *SystemMessageConfig, tools []Tool, streaming bool, agentName string, agentDescription string) *agentTurnTracker {
	providerName := otelDefaultProviderName
	var serverAddress string
	var serverPort int
	if provider != nil {
		providerName = normalizeProviderName(provider.Type)
		serverAddress, serverPort = parseServerAddress(provider.BaseURL)
	}
	return &agentTurnTracker{
		telemetry:              telemetry,
		sessionID:              sessionID,
		requestModel:           model,
		providerName:           providerName,
		serverAddress:          serverAddress,
		serverPort:             serverPort,
		agentName:              agentName,
		agentDescription:       agentDescription,
		systemInstructionsJSON: buildSystemInstructionsJSON(systemMessage),
		toolDefinitionsJSON:    buildToolDefinitionsJSON(tools),
		isStreaming:            streaming,
	}
}

// getProviderName returns the provider name.
func (t *agentTurnTracker) getProviderName() string {
	return t.providerName
}

// getServerAddress returns the server address.
func (t *agentTurnTracker) getServerAddress() string {
	return t.serverAddress
}

// getServerPort returns the server port.
func (t *agentTurnTracker) getServerPort() int {
	return t.serverPort
}

// getToolCallParentContext gets the parent context for a tool call span.
// For subagent tool calls this returns the subagent's invoke_agent context;
// for main agent tool calls this returns the root invoke_agent context.
func (t *agentTurnTracker) getToolCallParentContext(toolCallID string) context.Context {
	t.mu.Lock()
	defer t.mu.Unlock()
	if t.pendingToolParents != nil {
		if ctx, ok := t.pendingToolParents[toolCallID]; ok {
			delete(t.pendingToolParents, toolCallID)
			return ctx
		}
	}
	return t.agentSpanCtx
}

// completeOnDispose closes any active spans with an error status.
func (t *agentTurnTracker) completeOnDispose() {
	t.mu.Lock()
	defer t.mu.Unlock()
	if t.agentSpan != nil {
		disposeErr := &sessionDestroyedError{Message: "Session destroyed while agent turn was in progress"}
		t.completeChatTurnLocked(disposeErr)
		t.completeAgentTurnLocked(disposeErr)
	}
}

// beginSend is called at the start of Send() to start a span and record the user message.
func (t *agentTurnTracker) beginSend(ctx context.Context, prompt string) {
	t.mu.Lock()
	defer t.mu.Unlock()
	if t.agentSpan == nil {
		spanCtx, span := t.telemetry.startInvokeAgentSpan(
			ctx,
			t.sessionID,
			t.requestModel,
			t.providerName,
			t.serverAddress,
			t.serverPort,
			t.agentName,
			t.agentDescription,
		)
		t.agentSpan = span
		t.agentSpanCtx = spanCtx
		t.agentStartTime = time.Now()
		t.agentInputMsgs = nil
		t.agentOutputMsgs = nil
	}

	// Agent-level input = what the caller sent (all user prompts).
	if prompt != "" {
		t.agentInputMsgs = append(t.agentInputMsgs, otelMsg{
			Role:  "user",
			Parts: []otelPart{{Type: "text", Content: prompt}},
		})
	}

	// Stash user prompt for the first chat turn's input messages.
	t.pendingUserPrompt = prompt
}

// processEvent handles telemetry enrichment for dispatched events.
func (t *agentTurnTracker) processEvent(event SessionEvent) {
	t.mu.Lock()
	defer t.mu.Unlock()

	// Route subagent events by parentToolCallId.
	parentToolCallID := getParentToolCallID(event)
	if parentToolCallID != "" {
		if t.activeSubagents != nil {
			if sub, ok := t.activeSubagents[parentToolCallID]; ok {
				t.processSubagentEventLocked(sub, event)
			}
		}
		return
	}

	// Handle subagent lifecycle events.
	switch event.Type {
	case SubagentStarted:
		t.beginSubagentLocked(event)
		return
	case SubagentCompleted:
		if event.Data.ToolCallID != nil {
			t.completeSubagentLocked(*event.Data.ToolCallID, nil)
		}
		return
	case SubagentFailed:
		if event.Data.ToolCallID != nil {
			agentName := ""
			if event.Data.AgentName != nil {
				agentName = *event.Data.AgentName
			}
			errMsg := ""
			if event.Data.Error != nil {
				if event.Data.Error.String != nil {
					errMsg = *event.Data.Error.String
				} else if event.Data.Error.ErrorClass != nil {
					errMsg = event.Data.Error.ErrorClass.Message
				}
			}
			t.completeSubagentLocked(*event.Data.ToolCallID,
				&sessionTurnError{Message: fmt.Sprintf("Subagent '%s' failed: %s", agentName, errMsg)})
		}
		return
	}

	// Record chunk timing for main agent events during a turn.
	t.recordOutputChunkMetricLocked()

	// Per-turn event processing (writes to the chat child span).
	if t.turnSpan != nil {
		switch event.Type {
		case AssistantMessage:
			parts := []otelPart{}
			if event.Data.ReasoningText != nil && *event.Data.ReasoningText != "" {
				parts = append(parts, otelPart{Type: "reasoning", Content: *event.Data.ReasoningText})
			}
			if event.Data.Content != nil && *event.Data.Content != "" {
				parts = append(parts, otelPart{Type: "text", Content: *event.Data.Content})
			}
			if len(parts) > 0 {
				t.turnOutputMsgs = append(t.turnOutputMsgs, otelMsg{Role: "assistant", Parts: parts})
			}

		case AssistantUsage:
			if event.Data.Model != nil {
				t.turnResponseModel = *event.Data.Model
			}
			if event.Data.APICallID != nil && *event.Data.APICallID != "" {
				t.turnResponseID = *event.Data.APICallID
			} else if event.Data.ProviderCallID != nil && *event.Data.ProviderCallID != "" {
				t.turnResponseID = *event.Data.ProviderCallID
			}
			if event.Data.InputTokens != nil {
				t.turnInputTokens += int(*event.Data.InputTokens)
			}
			if event.Data.OutputTokens != nil {
				t.turnOutputTokens += int(*event.Data.OutputTokens)
			}
			if event.Data.CacheReadTokens != nil {
				t.turnCacheReadTokens += int(*event.Data.CacheReadTokens)
			}
			if event.Data.CacheWriteTokens != nil {
				t.turnCacheCreationTokens += int(*event.Data.CacheWriteTokens)
			}

			// Copilot-specific vendor attributes
			if event.Data.Cost != nil {
				v := ptrFloat64OrZero(t.turnCost) + *event.Data.Cost
				t.turnCost = &v
			}
			if event.Data.Duration != nil {
				v := ptrFloat64OrZero(t.turnServerDuration) + *event.Data.Duration
				t.turnServerDuration = &v
			}
			if event.Data.Initiator != nil {
				t.turnInitiator = event.Data.Initiator
			}
			if event.Data.CopilotUsage != nil {
				v := ptrFloat64OrZero(t.turnAIU) + event.Data.CopilotUsage.TotalNanoAiu
				t.turnAIU = &v
			}

		case SessionModelChange:
			if event.Data.NewModel != nil {
				t.turnResponseModel = *event.Data.NewModel
			}

		case ToolExecutionStart:
			isServerTool := event.Data.MCPServerName != nil && *event.Data.MCPServerName != ""
			if isServerTool && event.Data.ToolCallID != nil {
				if t.serverToolCallIDs == nil {
					t.serverToolCallIDs = make(map[string]string)
				}
				t.serverToolCallIDs[*event.Data.ToolCallID] = *event.Data.MCPServerName
			}

			partType := "tool_call"
			if isServerTool {
				partType = "server_tool_call"
			}
			p := otelPart{Type: partType}
			if event.Data.ToolCallID != nil {
				p.ID = *event.Data.ToolCallID
			}
			if event.Data.ToolName != nil {
				p.Name = *event.Data.ToolName
			}
			p.Arguments = event.Data.Arguments
			if isServerTool && event.Data.MCPServerName != nil {
				p.MCPServerName = *event.Data.MCPServerName
			}
			t.turnOutputMsgs = append(t.turnOutputMsgs, otelMsg{Role: "assistant", Parts: []otelPart{p}})

			// Store parent context for tool call span parenting.
			if t.agentSpan != nil && event.Data.ToolCallID != nil {
				if t.pendingToolParents == nil {
					t.pendingToolParents = make(map[string]context.Context)
				}
				t.pendingToolParents[*event.Data.ToolCallID] = t.agentSpanCtx
			}

		case ToolExecutionComplete:
			resultContent := ""
			if event.Data.Result != nil {
				resultContent = event.Data.Result.Content
			} else if event.Data.Error != nil {
				if event.Data.Error.String != nil {
					resultContent = *event.Data.Error.String
				} else if event.Data.Error.ErrorClass != nil {
					resultContent = event.Data.Error.ErrorClass.Message
				}
			}

			var serverName string
			isServerTool := false
			if event.Data.ToolCallID != nil && t.serverToolCallIDs != nil {
				if name, ok := t.serverToolCallIDs[*event.Data.ToolCallID]; ok {
					isServerTool = true
					serverName = name
					delete(t.serverToolCallIDs, *event.Data.ToolCallID)
				}
			}
			partType := "tool_call_response"
			if isServerTool {
				partType = "server_tool_call_response"
			}
			p := otelPart{Type: partType, Response: resultContent, MCPServerName: serverName}
			if event.Data.ToolCallID != nil {
				p.ID = *event.Data.ToolCallID
			}
			t.turnInputMsgs = append(t.turnInputMsgs, otelMsg{Role: "tool", Parts: []otelPart{p}})
		}
	}

	// Copilot-specific lifecycle events emitted as span events.
	target := t.turnSpan
	if target == nil {
		target = t.agentSpan
	}
	if target != nil {
		switch event.Type {
		case SessionTruncation:
			attrs := []attribute.KeyValue{}
			if event.Data.TokenLimit != nil {
				attrs = append(attrs, attribute.Int64(otelCopilotTokenLimit, int64(*event.Data.TokenLimit)))
			}
			if event.Data.PreTruncationTokensInMessages != nil {
				attrs = append(attrs, attribute.Int64(otelCopilotPreTokens, int64(*event.Data.PreTruncationTokensInMessages)))
			}
			if event.Data.PostTruncationTokensInMessages != nil {
				attrs = append(attrs, attribute.Int64(otelCopilotPostTokens, int64(*event.Data.PostTruncationTokensInMessages)))
			}
			if event.Data.PreTruncationMessagesLength != nil {
				attrs = append(attrs, attribute.Int64(otelCopilotPreMessages, int64(*event.Data.PreTruncationMessagesLength)))
			}
			if event.Data.PostTruncationMessagesLength != nil {
				attrs = append(attrs, attribute.Int64(otelCopilotPostMessages, int64(*event.Data.PostTruncationMessagesLength)))
			}
			if event.Data.TokensRemovedDuringTruncation != nil {
				attrs = append(attrs, attribute.Int64(otelCopilotTokensRemoved, int64(*event.Data.TokensRemovedDuringTruncation)))
			}
			if event.Data.MessagesRemovedDuringTruncation != nil {
				attrs = append(attrs, attribute.Int64(otelCopilotMessagesRemoved, int64(*event.Data.MessagesRemovedDuringTruncation)))
			}
			if event.Data.PerformedBy != nil {
				attrs = append(attrs, attribute.String(otelCopilotPerformedBy, *event.Data.PerformedBy))
			}
			target.AddEvent(otelCopilotSessionTruncation, trace.WithAttributes(attrs...))

		case SessionCompactionStart:
			target.AddEvent(otelCopilotSessionCompactionStart)

		case SessionCompactionComplete:
			attrs := []attribute.KeyValue{}
			if event.Data.Success != nil {
				attrs = append(attrs, attribute.Bool(otelCopilotSuccess, *event.Data.Success))
			}
			if t.telemetry.enableSensitiveData && event.Data.Error != nil {
				errStr := ""
				if event.Data.Error.String != nil {
					errStr = *event.Data.Error.String
				} else if event.Data.Error.ErrorClass != nil {
					errStr = event.Data.Error.ErrorClass.Message
				}
				if errStr != "" {
					attrs = append(attrs, attribute.String(otelCopilotMessage, errStr))
				}
			}
			if event.Data.PreCompactionTokens != nil {
				attrs = append(attrs, attribute.Int64(otelCopilotPreTokens, int64(*event.Data.PreCompactionTokens)))
			}
			if event.Data.PostCompactionTokens != nil {
				attrs = append(attrs, attribute.Int64(otelCopilotPostTokens, int64(*event.Data.PostCompactionTokens)))
			}
			if event.Data.TokensRemoved != nil {
				attrs = append(attrs, attribute.Int64(otelCopilotTokensRemoved, int64(*event.Data.TokensRemoved)))
			}
			if event.Data.MessagesRemoved != nil {
				attrs = append(attrs, attribute.Int64(otelCopilotMessagesRemoved, int64(*event.Data.MessagesRemoved)))
			}
			target.AddEvent(otelCopilotSessionCompactionComplete, trace.WithAttributes(attrs...))

		case SkillInvoked:
			attrs := []attribute.KeyValue{}
			if event.Data.Name != nil {
				attrs = append(attrs, attribute.String(otelCopilotSkillName, *event.Data.Name))
			}
			if event.Data.Path != nil {
				attrs = append(attrs, attribute.String(otelCopilotSkillPath, *event.Data.Path))
			}
			if event.Data.PluginName != nil {
				attrs = append(attrs, attribute.String(otelCopilotSkillPluginName, *event.Data.PluginName))
			}
			if event.Data.PluginVersion != nil {
				attrs = append(attrs, attribute.String(otelCopilotSkillPluginVersion, *event.Data.PluginVersion))
			}
			if t.telemetry.enableSensitiveData && event.Data.Content != nil {
				attrs = append(attrs, attribute.String(otelCopilotSkillContent, *event.Data.Content))
			}
			target.AddEvent(otelCopilotSkillInvoked, trace.WithAttributes(attrs...))
		}
	}

	// Lifecycle events that start/end turns or the overall agent operation.
	switch event.Type {
	case AssistantTurnStart:
		t.beginChatTurnLocked()
		if event.Data.TurnID != nil {
			t.turnID = event.Data.TurnID
		}
		if event.Data.InteractionID != nil {
			t.turnInteractionID = event.Data.InteractionID
		}

	case AssistantTurnEnd:
		t.completeChatTurnLocked(nil)

	case SessionIdle:
		t.completeChatTurnLocked(nil)
		t.completeAgentTurnLocked(nil)

	case SessionError:
		errMsg := "unknown error"
		if event.Data.Message != nil {
			errMsg = *event.Data.Message
		}
		ex := &sessionTurnError{Message: fmt.Sprintf("Session error: %s", errMsg)}
		t.completeChatTurnLocked(ex)
		t.completeAgentTurnLocked(ex)
	}
}

// completeTurnWithError completes the current turn with an error.
func (t *agentTurnTracker) completeTurnWithError(err error) {
	t.mu.Lock()
	defer t.mu.Unlock()
	t.completeChatTurnLocked(err)
	t.completeAgentTurnLocked(err)
}

// ============================================================================
// Chat turn lifecycle
// ============================================================================

// beginChatTurnLocked starts a new chat child span for an LLM turn. Caller must hold mu.
func (t *agentTurnTracker) beginChatTurnLocked() {
	// If there's already an active turn, complete it first.
	t.completeChatTurnLocked(nil)

	t.turnResponseModel = ""
	t.turnResponseID = ""
	t.turnInputTokens = 0
	t.turnOutputTokens = 0
	t.turnCacheReadTokens = 0
	t.turnCacheCreationTokens = 0
	t.firstOutputChunkRecorded = false
	t.lastOutputChunkTime = time.Time{}
	t.turnInputMsgs = nil
	t.turnOutputMsgs = nil
	t.turnCost = nil
	t.turnServerDuration = nil
	t.turnInitiator = nil
	t.turnAIU = nil
	t.turnID = nil
	t.turnInteractionID = nil

	// Add stashed user prompt as input message for the first turn.
	if t.pendingUserPrompt != "" {
		t.turnInputMsgs = append(t.turnInputMsgs, otelMsg{
			Role:  "user",
			Parts: []otelPart{{Type: "text", Content: t.pendingUserPrompt}},
		})
		t.pendingUserPrompt = ""
	}

	parentCtx := t.agentSpanCtx
	if parentCtx == nil {
		parentCtx = context.Background()
	}
	_, span := t.telemetry.startChatSpan(
		parentCtx,
		t.requestModel,
		t.providerName,
		t.serverAddress,
		t.serverPort,
		t.sessionID,
	)
	t.turnSpan = span
	t.turnStartTime = time.Now()
}

// completeChatTurnLocked completes the current chat child span. Caller must hold mu.
func (t *agentTurnTracker) completeChatTurnLocked(err error) {
	span := t.turnSpan
	if span == nil {
		return
	}

	startTime := t.turnStartTime
	inputMessages := t.turnInputMsgs
	outputMessages := t.turnOutputMsgs
	responseModel := t.turnResponseModel
	responseID := t.turnResponseID
	inputTokens := t.turnInputTokens
	outputTokens := t.turnOutputTokens
	cacheReadTokens := t.turnCacheReadTokens
	cacheCreationTokens := t.turnCacheCreationTokens
	turnCost := t.turnCost
	turnServerDuration := t.turnServerDuration
	turnInitiator := t.turnInitiator
	turnAIU := t.turnAIU
	turnID := t.turnID
	turnInteractionID := t.turnInteractionID

	// Reset per-turn state
	t.turnSpan = nil
	t.turnStartTime = time.Time{}
	t.turnInputMsgs = nil
	t.turnOutputMsgs = nil
	t.turnResponseModel = ""
	t.turnResponseID = ""
	t.turnInputTokens = 0
	t.turnOutputTokens = 0
	t.turnCacheReadTokens = 0
	t.turnCacheCreationTokens = 0
	t.turnCost = nil
	t.turnServerDuration = nil
	t.turnInitiator = nil
	t.turnAIU = nil
	t.turnID = nil
	t.turnInteractionID = nil

	if err != nil {
		recordSpanError(span, err)
	}

	finishReason := "stop"
	if err != nil {
		finishReason = "error"
	}
	span.SetAttributes(attribute.StringSlice(otelGenAIResponseFinishReasons, []string{finishReason}))

	// Agent-level output = only the final turn's output.
	if len(outputMessages) > 0 {
		t.agentOutputMsgs = make([]otelMsg, len(outputMessages))
		for i, m := range outputMessages {
			t.agentOutputMsgs[i] = otelMsg{Role: m.Role, Parts: m.Parts, FinishReason: finishReason}
		}
	}

	// Accumulate agent-level usage across turns.
	if responseModel != "" {
		t.agentResponseModel = responseModel
	}
	if responseID != "" {
		t.agentResponseID = responseID
	}
	t.agentTotalInputTokens += inputTokens
	t.agentTotalOutputTokens += outputTokens
	t.agentTotalCacheReadTokens += cacheReadTokens
	t.agentTotalCacheCreationTokens += cacheCreationTokens
	if turnCost != nil {
		t.agentTotalCost += *turnCost
	}
	if turnAIU != nil {
		t.agentTotalAIU += *turnAIU
	}

	// Set usage-related span attributes for this LLM turn
	if responseModel != "" {
		span.SetAttributes(attribute.String(otelGenAIResponseModel, responseModel))
	}
	if responseID != "" {
		span.SetAttributes(attribute.String(otelGenAIResponseID, responseID))
	}
	if inputTokens > 0 {
		span.SetAttributes(attribute.Int(otelGenAIUsageInputTokens, inputTokens))
	}
	if outputTokens > 0 {
		span.SetAttributes(attribute.Int(otelGenAIUsageOutputTokens, outputTokens))
	}
	if cacheReadTokens > 0 {
		span.SetAttributes(attribute.Int(otelGenAIUsageCacheReadInputTokens, cacheReadTokens))
	}
	if cacheCreationTokens > 0 {
		span.SetAttributes(attribute.Int(otelGenAIUsageCacheCreationInputTokens, cacheCreationTokens))
	}

	// Copilot vendor-prefixed attributes on chat spans
	if turnCost != nil {
		span.SetAttributes(attribute.Float64(otelCopilotCost, *turnCost))
	}
	if turnServerDuration != nil {
		span.SetAttributes(attribute.Float64(otelCopilotServerDuration, *turnServerDuration))
	}
	if turnInitiator != nil {
		span.SetAttributes(attribute.String(otelCopilotInitiator, *turnInitiator))
	}
	if turnAIU != nil {
		span.SetAttributes(attribute.Float64(otelCopilotAIU, *turnAIU))
	}
	if turnID != nil {
		span.SetAttributes(attribute.String(otelCopilotTurnID, *turnID))
	}
	if turnInteractionID != nil {
		span.SetAttributes(attribute.String(otelCopilotInteractionID, *turnInteractionID))
	}

	// Set input/output message content as span attributes (sensitive)
	if t.telemetry.enableSensitiveData {
		inputMessagesJSON := buildPartsMessagesJSON(inputMessages, "")
		if inputMessagesJSON != "" {
			span.SetAttributes(attribute.String(otelGenAIInputMessages, inputMessagesJSON))
		}
		outputMessagesJSON := buildPartsMessagesJSON(outputMessages, finishReason)
		if outputMessagesJSON != "" {
			span.SetAttributes(attribute.String(otelGenAIOutputMessages, outputMessagesJSON))
		}
	}

	// Token usage metrics (per-turn)
	var inTok, outTok *int
	if inputTokens > 0 {
		inTok = &inputTokens
	}
	if outputTokens > 0 {
		outTok = &outputTokens
	}
	ctx := context.Background()
	if span != nil {
		ctx = trace.ContextWithSpan(ctx, span)
	}
	t.telemetry.recordTokenUsageMetrics(
		ctx,
		inTok,
		outTok,
		t.requestModel,
		responseModel,
		t.providerName,
		t.serverAddress,
		t.serverPort,
		err,
		otelChat,
	)

	// Per-turn operation duration
	if !startTime.IsZero() {
		durationSeconds := time.Since(startTime).Seconds()
		t.telemetry.recordOperationDuration(
			ctx,
			durationSeconds,
			t.requestModel,
			responseModel,
			t.providerName,
			t.serverAddress,
			t.serverPort,
			err,
			otelChat,
		)
	}

	t.firstOutputChunkRecorded = false
	t.lastOutputChunkTime = time.Time{}
	span.End()
}

// ============================================================================
// Agent turn lifecycle
// ============================================================================

// completeAgentTurnLocked completes the invoke_agent span and records overall operation duration.
// Caller must hold mu.
func (t *agentTurnTracker) completeAgentTurnLocked(err error) {
	span := t.agentSpan
	if span == nil {
		return
	}

	startTime := t.agentStartTime
	agentInputMsgs := t.agentInputMsgs
	agentOutputMsgs := t.agentOutputMsgs

	// Reset agent state
	t.agentSpan = nil
	t.agentSpanCtx = nil
	t.agentStartTime = time.Time{}
	t.pendingUserPrompt = ""
	t.agentInputMsgs = nil
	t.agentOutputMsgs = nil

	// Complete any remaining subagents before closing the parent.
	if len(t.activeSubagents) > 0 {
		for key := range t.activeSubagents {
			t.completeSubagentLocked(key, err)
		}
	}
	t.activeSubagents = nil
	t.pendingToolParents = nil
	t.serverToolCallIDs = nil

	if err != nil {
		recordSpanError(span, err)
	}

	finishReason := "stop"
	if err != nil {
		finishReason = "error"
	}
	span.SetAttributes(attribute.StringSlice(otelGenAIResponseFinishReasons, []string{finishReason}))

	// Set accumulated usage across all chat turns on the invoke_agent span.
	agentResponseModel := t.agentResponseModel
	agentResponseID := t.agentResponseID
	agentTotalInputTokens := t.agentTotalInputTokens
	agentTotalOutputTokens := t.agentTotalOutputTokens
	agentTotalCacheReadTokens := t.agentTotalCacheReadTokens
	agentTotalCacheCreationTokens := t.agentTotalCacheCreationTokens
	agentTotalCost := t.agentTotalCost
	agentTotalAIU := t.agentTotalAIU

	t.agentResponseModel = ""
	t.agentResponseID = ""
	t.agentTotalInputTokens = 0
	t.agentTotalOutputTokens = 0
	t.agentTotalCacheReadTokens = 0
	t.agentTotalCacheCreationTokens = 0
	t.agentTotalCost = 0
	t.agentTotalAIU = 0

	if agentResponseModel != "" {
		span.SetAttributes(attribute.String(otelGenAIResponseModel, agentResponseModel))
	}
	if agentResponseID != "" {
		span.SetAttributes(attribute.String(otelGenAIResponseID, agentResponseID))
	}
	if agentTotalInputTokens > 0 {
		span.SetAttributes(attribute.Int(otelGenAIUsageInputTokens, agentTotalInputTokens))
	}
	if agentTotalOutputTokens > 0 {
		span.SetAttributes(attribute.Int(otelGenAIUsageOutputTokens, agentTotalOutputTokens))
	}
	if agentTotalCacheReadTokens > 0 {
		span.SetAttributes(attribute.Int(otelGenAIUsageCacheReadInputTokens, agentTotalCacheReadTokens))
	}
	if agentTotalCacheCreationTokens > 0 {
		span.SetAttributes(attribute.Int(otelGenAIUsageCacheCreationInputTokens, agentTotalCacheCreationTokens))
	}

	// Copilot vendor-prefixed attributes on invoke_agent span
	if agentTotalCost > 0 {
		span.SetAttributes(attribute.Float64(otelCopilotCost, agentTotalCost))
	}
	if agentTotalAIU > 0 {
		span.SetAttributes(attribute.Float64(otelCopilotAIU, agentTotalAIU))
	}

	// Agent-level input = caller's message; output = agent's final response.
	if t.telemetry.enableSensitiveData {
		inputJSON := buildPartsMessagesJSON(agentInputMsgs, "")
		if inputJSON != "" {
			span.SetAttributes(attribute.String(otelGenAIInputMessages, inputJSON))
		}
		outputJSON := buildPartsMessagesJSON(agentOutputMsgs, "")
		if outputJSON != "" {
			span.SetAttributes(attribute.String(otelGenAIOutputMessages, outputJSON))
		}
		if t.systemInstructionsJSON != "" {
			span.SetAttributes(attribute.String(otelGenAISystemInstructions, t.systemInstructionsJSON))
		}
	}
	if t.toolDefinitionsJSON != "" {
		span.SetAttributes(attribute.String(otelGenAIToolDefinitions, t.toolDefinitionsJSON))
	}

	agentCtx := context.Background()
	if span != nil {
		agentCtx = trace.ContextWithSpan(agentCtx, span)
	}
	if !startTime.IsZero() {
		durationSeconds := time.Since(startTime).Seconds()
		t.telemetry.recordOperationDuration(
			agentCtx,
			durationSeconds,
			t.requestModel,
			agentResponseModel,
			t.providerName,
			t.serverAddress,
			t.serverPort,
			err,
			otelInvokeAgent,
		)
	}

	span.End()
}

// ============================================================================
// Streaming chunk timing
// ============================================================================

// recordOutputChunkMetricLocked records streaming chunk timing metrics. Caller must hold mu.
func (t *agentTurnTracker) recordOutputChunkMetricLocked() {
	if !t.isStreaming || t.turnStartTime.IsZero() {
		return
	}

	now := time.Now()
	elapsed := now.Sub(t.turnStartTime)

	if !t.firstOutputChunkRecorded {
		t.firstOutputChunkRecorded = true
		t.lastOutputChunkTime = now
		t.telemetry.recordTimeToFirstChunk(
			context.Background(),
			elapsed.Seconds(),
			t.requestModel,
			"", // response model not yet known during streaming
			t.providerName,
			t.serverAddress,
			t.serverPort,
		)
		return
	}

	delta := now.Sub(t.lastOutputChunkTime)
	t.lastOutputChunkTime = now
	t.telemetry.recordTimePerOutputChunk(
		context.Background(),
		delta.Seconds(),
		t.requestModel,
		"", // response model not yet known during streaming
		t.providerName,
		t.serverAddress,
		t.serverPort,
	)
}

// ============================================================================
// Subagent support
// ============================================================================

// beginSubagentLocked creates nested invoke_agent + chat spans for a subagent. Caller must hold mu.
func (t *agentTurnTracker) beginSubagentLocked(event SessionEvent) {
	parentCtx := t.agentSpanCtx
	if parentCtx == nil {
		parentCtx = context.Background()
	}

	agentName := ""
	if event.Data.AgentName != nil {
		agentName = *event.Data.AgentName
	}
	agentDesc := ""
	if event.Data.AgentDescription != nil {
		agentDesc = *event.Data.AgentDescription
	}

	invokeCtx, invokeSpan := t.telemetry.startInvokeAgentSpan(
		parentCtx,
		t.sessionID,
		t.requestModel,
		t.providerName,
		t.serverAddress,
		t.serverPort,
		agentName,
		agentDesc,
	)

	_, chatSpan := t.telemetry.startChatSpan(
		invokeCtx,
		t.requestModel,
		t.providerName,
		t.serverAddress,
		t.serverPort,
		t.sessionID,
	)

	state := &subagentState{
		invokeAgentSpan: invokeSpan,
		spanCtx:         invokeCtx,
		startTime:       time.Now(),
		chatSpan:        chatSpan,
		agentName:       agentName,
	}

	if t.activeSubagents == nil {
		t.activeSubagents = make(map[string]*subagentState)
	}
	if event.Data.ToolCallID != nil {
		t.activeSubagents[*event.Data.ToolCallID] = state
	}
}

// processSubagentEventLocked routes an event to its owning subagent's spans. Caller must hold mu.
func (t *agentTurnTracker) processSubagentEventLocked(sub *subagentState, event SessionEvent) {
	switch event.Type {
	case AssistantUsage:
		if event.Data.Model != nil {
			sub.responseModel = *event.Data.Model
			if sub.chatSpan != nil {
				sub.chatSpan.SetAttributes(attribute.String(otelGenAIResponseModel, *event.Data.Model))
			}
		}
		if event.Data.APICallID != nil && *event.Data.APICallID != "" {
			sub.responseID = *event.Data.APICallID
		} else if event.Data.ProviderCallID != nil && *event.Data.ProviderCallID != "" {
			sub.responseID = *event.Data.ProviderCallID
		}
		if event.Data.InputTokens != nil {
			sub.inputTokens += int(*event.Data.InputTokens)
		}
		if event.Data.OutputTokens != nil {
			sub.outputTokens += int(*event.Data.OutputTokens)
		}
		if event.Data.CacheReadTokens != nil {
			sub.cacheReadTokens += int(*event.Data.CacheReadTokens)
		}
		if event.Data.CacheWriteTokens != nil {
			sub.cacheCreationTokens += int(*event.Data.CacheWriteTokens)
		}

	case AssistantMessage:
		parts := []otelPart{}
		if event.Data.ReasoningText != nil && *event.Data.ReasoningText != "" {
			parts = append(parts, otelPart{Type: "reasoning", Content: *event.Data.ReasoningText})
		}
		if event.Data.Content != nil && *event.Data.Content != "" {
			parts = append(parts, otelPart{Type: "text", Content: *event.Data.Content})
		}
		if len(parts) > 0 {
			sub.outputMessages = append(sub.outputMessages, otelMsg{Role: "assistant", Parts: parts})
		}

	case ToolExecutionStart:
		isServerTool := event.Data.MCPServerName != nil && *event.Data.MCPServerName != ""
		if isServerTool && event.Data.ToolCallID != nil {
			if t.serverToolCallIDs == nil {
				t.serverToolCallIDs = make(map[string]string)
			}
			t.serverToolCallIDs[*event.Data.ToolCallID] = *event.Data.MCPServerName
		}

		partType := "tool_call"
		if isServerTool {
			partType = "server_tool_call"
		}
		p := otelPart{Type: partType}
		if event.Data.ToolCallID != nil {
			p.ID = *event.Data.ToolCallID
		}
		if event.Data.ToolName != nil {
			p.Name = *event.Data.ToolName
		}
		p.Arguments = event.Data.Arguments
		if isServerTool && event.Data.MCPServerName != nil {
			p.MCPServerName = *event.Data.MCPServerName
		}
		sub.outputMessages = append(sub.outputMessages, otelMsg{Role: "assistant", Parts: []otelPart{p}})

		// For subagent tool calls, parent is the subagent's invoke_agent.
		if event.Data.ToolCallID != nil {
			if t.pendingToolParents == nil {
				t.pendingToolParents = make(map[string]context.Context)
			}
			t.pendingToolParents[*event.Data.ToolCallID] = sub.spanCtx
		}

	case ToolExecutionComplete:
		resultContent := ""
		if event.Data.Result != nil {
			resultContent = event.Data.Result.Content
		} else if event.Data.Error != nil {
			if event.Data.Error.String != nil {
				resultContent = *event.Data.Error.String
			} else if event.Data.Error.ErrorClass != nil {
				resultContent = event.Data.Error.ErrorClass.Message
			}
		}

		var serverName string
		isServerTool := false
		if event.Data.ToolCallID != nil && t.serverToolCallIDs != nil {
			if name, ok := t.serverToolCallIDs[*event.Data.ToolCallID]; ok {
				isServerTool = true
				serverName = name
				delete(t.serverToolCallIDs, *event.Data.ToolCallID)
			}
		}
		partType := "tool_call_response"
		if isServerTool {
			partType = "server_tool_call_response"
		}
		p := otelPart{Type: partType, Response: resultContent, MCPServerName: serverName}
		if event.Data.ToolCallID != nil {
			p.ID = *event.Data.ToolCallID
		}
		sub.inputMessages = append(sub.inputMessages, otelMsg{Role: "tool", Parts: []otelPart{p}})
	}
}

// completeSubagentLocked completes a subagent's chat and invoke_agent spans. Caller must hold mu.
func (t *agentTurnTracker) completeSubagentLocked(toolCallID string, err error) {
	if t.activeSubagents == nil {
		return
	}
	sub, ok := t.activeSubagents[toolCallID]
	if !ok {
		return
	}
	delete(t.activeSubagents, toolCallID)

	finishReason := "stop"
	if err != nil {
		finishReason = "error"
	}

	// -- Complete the chat child span --
	if sub.chatSpan != nil {
		if err != nil {
			recordSpanError(sub.chatSpan, err)
		}
		sub.chatSpan.SetAttributes(attribute.StringSlice(otelGenAIResponseFinishReasons, []string{finishReason}))

		if sub.responseModel != "" {
			sub.chatSpan.SetAttributes(attribute.String(otelGenAIResponseModel, sub.responseModel))
		}
		if sub.responseID != "" {
			sub.chatSpan.SetAttributes(attribute.String(otelGenAIResponseID, sub.responseID))
		}
		if sub.inputTokens > 0 {
			sub.chatSpan.SetAttributes(attribute.Int(otelGenAIUsageInputTokens, sub.inputTokens))
		}
		if sub.outputTokens > 0 {
			sub.chatSpan.SetAttributes(attribute.Int(otelGenAIUsageOutputTokens, sub.outputTokens))
		}
		if sub.cacheReadTokens > 0 {
			sub.chatSpan.SetAttributes(attribute.Int(otelGenAIUsageCacheReadInputTokens, sub.cacheReadTokens))
		}
		if sub.cacheCreationTokens > 0 {
			sub.chatSpan.SetAttributes(attribute.Int(otelGenAIUsageCacheCreationInputTokens, sub.cacheCreationTokens))
		}

		if t.telemetry.enableSensitiveData {
			if len(sub.inputMessages) > 0 {
				inputJSON := buildPartsMessagesJSON(sub.inputMessages, "")
				if inputJSON != "" {
					sub.chatSpan.SetAttributes(attribute.String(otelGenAIInputMessages, inputJSON))
				}
			}
			if len(sub.outputMessages) > 0 {
				outputJSON := buildPartsMessagesJSON(sub.outputMessages, finishReason)
				if outputJSON != "" {
					sub.chatSpan.SetAttributes(attribute.String(otelGenAIOutputMessages, outputJSON))
				}
			}
		}

		var inTok, outTok *int
		if sub.inputTokens > 0 {
			inTok = &sub.inputTokens
		}
		if sub.outputTokens > 0 {
			outTok = &sub.outputTokens
		}
		reqModel := sub.responseModel
		if reqModel == "" {
			reqModel = t.requestModel
		}
		subChatCtx := context.Background()
		if sub.chatSpan != nil {
			subChatCtx = trace.ContextWithSpan(subChatCtx, sub.chatSpan)
		}
		t.telemetry.recordTokenUsageMetrics(
			subChatCtx,
			inTok, outTok,
			reqModel, sub.responseModel,
			t.providerName, t.serverAddress, t.serverPort,
			err, otelChat,
		)

		sub.chatSpan.End()
	}

	// -- Complete the invoke_agent span --
	if err != nil {
		recordSpanError(sub.invokeAgentSpan, err)
	}
	sub.invokeAgentSpan.SetAttributes(attribute.StringSlice(otelGenAIResponseFinishReasons, []string{finishReason}))

	if sub.responseModel != "" {
		sub.invokeAgentSpan.SetAttributes(attribute.String(otelGenAIResponseModel, sub.responseModel))
	}
	if sub.responseID != "" {
		sub.invokeAgentSpan.SetAttributes(attribute.String(otelGenAIResponseID, sub.responseID))
	}
	if sub.inputTokens > 0 {
		sub.invokeAgentSpan.SetAttributes(attribute.Int(otelGenAIUsageInputTokens, sub.inputTokens))
	}
	if sub.outputTokens > 0 {
		sub.invokeAgentSpan.SetAttributes(attribute.Int(otelGenAIUsageOutputTokens, sub.outputTokens))
	}
	if sub.cacheReadTokens > 0 {
		sub.invokeAgentSpan.SetAttributes(attribute.Int(otelGenAIUsageCacheReadInputTokens, sub.cacheReadTokens))
	}
	if sub.cacheCreationTokens > 0 {
		sub.invokeAgentSpan.SetAttributes(attribute.Int(otelGenAIUsageCacheCreationInputTokens, sub.cacheCreationTokens))
	}

	if t.telemetry.enableSensitiveData && len(sub.outputMessages) > 0 {
		msgsWithFinishReason := make([]otelMsg, len(sub.outputMessages))
		for i, m := range sub.outputMessages {
			msgsWithFinishReason[i] = otelMsg{Role: m.Role, Parts: m.Parts, FinishReason: finishReason}
		}
		outputJSON := buildPartsMessagesJSON(msgsWithFinishReason, "")
		if outputJSON != "" {
			sub.invokeAgentSpan.SetAttributes(attribute.String(otelGenAIOutputMessages, outputJSON))
		}
	}

	reqModel := sub.responseModel
	if reqModel == "" {
		reqModel = t.requestModel
	}
	subAgentCtx := context.Background()
	if sub.invokeAgentSpan != nil {
		subAgentCtx = trace.ContextWithSpan(subAgentCtx, sub.invokeAgentSpan)
	}
	durationSeconds := time.Since(sub.startTime).Seconds()
	t.telemetry.recordOperationDuration(
		subAgentCtx,
		durationSeconds,
		reqModel, sub.responseModel,
		t.providerName, t.serverAddress, t.serverPort,
		err, otelInvokeAgent,
	)

	sub.invokeAgentSpan.End()
}

// ============================================================================
// Helper functions
// ============================================================================

// getParentToolCallID extracts parentToolCallId from events that can route to subagents.
func getParentToolCallID(event SessionEvent) string {
	switch event.Type {
	case AssistantUsage, AssistantMessage, AssistantMessageDelta,
		ToolExecutionStart, ToolExecutionComplete:
		if event.Data.ParentToolCallID != nil {
			return *event.Data.ParentToolCallID
		}
	}
	return ""
}

func ptrFloat64OrZero(p *float64) float64 {
	if p == nil {
		return 0
	}
	return *p
}

// buildPartsMessagesJSON builds the JSON array of messages for span attributes (parts-based format).
func buildPartsMessagesJSON(messages []otelMsg, defaultFinishReason string) string {
	if len(messages) == 0 {
		return ""
	}

	var result []any
	for _, message := range messages {
		if len(message.Parts) == 0 {
			continue
		}

		var parts []any
		for _, part := range message.Parts {
			obj := map[string]any{"type": part.Type}

			switch part.Type {
			case "server_tool_call":
				if part.ID != "" {
					obj["id"] = part.ID
				}
				if part.Name != "" {
					obj["name"] = part.Name
				}
				stc := map[string]any{"type": "mcp"}
				if part.MCPServerName != "" {
					stc["server_name"] = part.MCPServerName
				}
				if part.Arguments != nil {
					stc["arguments"] = part.Arguments
				}
				obj["server_tool_call"] = stc

			case "server_tool_call_response":
				if part.ID != "" {
					obj["id"] = part.ID
				}
				stcr := map[string]any{"type": "mcp"}
				if part.MCPServerName != "" {
					stcr["server_name"] = part.MCPServerName
				}
				if part.Response != nil {
					stcr["response"] = part.Response
				}
				obj["server_tool_call_response"] = stcr

			default:
				if part.Content != "" {
					obj["content"] = part.Content
				}
				if part.ID != "" {
					obj["id"] = part.ID
				}
				if part.Name != "" {
					obj["name"] = part.Name
				}
				if part.Arguments != nil {
					obj["arguments"] = part.Arguments
				}
				if part.Response != nil {
					obj["response"] = part.Response
				}
			}

			parts = append(parts, obj)
		}

		msg := map[string]any{"role": message.Role, "parts": parts}
		effectiveFinishReason := message.FinishReason
		if effectiveFinishReason == "" {
			effectiveFinishReason = defaultFinishReason
		}
		if effectiveFinishReason != "" {
			msg["finish_reason"] = effectiveFinishReason
		}
		result = append(result, msg)
	}

	if len(result) == 0 {
		return ""
	}
	b, _ := jsonMarshalNoHTMLEscape(result)
	return string(b)
}

// buildSystemInstructionsJSON builds JSON for system instructions.
func buildSystemInstructionsJSON(systemMessage *SystemMessageConfig) string {
	if systemMessage == nil || systemMessage.Content == "" {
		return ""
	}
	type item struct {
		Type    string `json:"type"`
		Content string `json:"content"`
	}
	b, _ := jsonMarshalNoHTMLEscape([]item{{Type: "text", Content: systemMessage.Content}})
	return string(b)
}

// buildToolDefinitionsJSON builds JSON for tool definitions.
func buildToolDefinitionsJSON(tools []Tool) string {
	if len(tools) == 0 {
		return ""
	}
	type toolDef struct {
		Type        string         `json:"type"`
		Name        string         `json:"name"`
		Description string         `json:"description,omitempty"`
		Parameters  map[string]any `json:"parameters,omitempty"`
	}
	var defs []toolDef
	for _, t := range tools {
		defs = append(defs, toolDef{
			Type:        "function",
			Name:        t.Name,
			Description: t.Description,
			Parameters:  t.Parameters,
		})
	}
	b, _ := jsonMarshalNoHTMLEscape(defs)
	return string(b)
}

// jsonMarshalNoHTMLEscape marshals v to JSON without HTML-escaping (<, >, &),
// matching the behavior of JSON.stringify (Node), json.dumps (Python), and
// JavaScriptEncoder.UnsafeRelaxedJsonEscaping (.NET).
func jsonMarshalNoHTMLEscape(v any) ([]byte, error) {
	var buf bytes.Buffer
	enc := json.NewEncoder(&buf)
	enc.SetEscapeHTML(false)
	if err := enc.Encode(v); err != nil {
		return nil, err
	}
	b := buf.Bytes()
	// Encode appends a trailing newline; trim it.
	if len(b) > 0 && b[len(b)-1] == '\n' {
		b = b[:len(b)-1]
	}
	return b, nil
}
