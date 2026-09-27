package e2e

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// Mirrors dotnet/test/TelemetryExportTests.cs (snapshot category "telemetry").
func TestTelemetryE2E(t *testing.T) {
	testharness.SkipIfInProcess(t, "telemetry configuration is not honored in-process")
	t.Run("should export file telemetry for sdk interactions", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)

		telemetryPath := filepath.Join(ctx.WorkDir, fmt.Sprintf("telemetry-%s.jsonl", randomHex(t)))
		const marker = "copilot-sdk-telemetry-e2e"
		const sourceName = "go-sdk-telemetry-e2e"
		const toolName = "echo_telemetry_marker"
		prompt := fmt.Sprintf("Use the %s tool with value '%s', then respond with TELEMETRY_E2E_DONE.", toolName, marker)

		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.Telemetry = &copilot.TelemetryConfig{
				FilePath:       telemetryPath,
				ExporterType:   "file",
				SourceName:     sourceName,
				CaptureContent: copilot.Bool(true),
			}
		})
		t.Cleanup(func() { client.ForceStop() })

		type EchoParams struct {
			Value string `json:"value" jsonschema:"Marker value to echo"`
		}
		echoTool := copilot.DefineTool(toolName, "Echoes a marker string for telemetry validation.",
			func(params EchoParams, inv copilot.ToolInvocation) (string, error) {
				return params.Value, nil
			})

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Tools:               []copilot.Tool{echoTool},
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		sessionID := session.SessionID

		finalMessage := testharness.SubscribeToFinalAssistantMessage(session)
		defer finalMessage.Close()
		if _, err := session.Send(t.Context(), copilot.MessageOptions{Prompt: prompt}); err != nil {
			t.Fatalf("Send failed: %v", err)
		}
		final, err := finalMessage.Wait(t.Context())
		if err != nil {
			t.Fatalf("Failed to wait for final assistant message: %v", err)
		}
		assistant, ok := final.Data.(*copilot.AssistantMessageData)
		if !ok {
			t.Fatalf("Expected AssistantMessageData, got %T", final.Data)
		}
		if !strings.Contains(assistant.Content, "TELEMETRY_E2E_DONE") {
			t.Errorf("Expected response to contain 'TELEMETRY_E2E_DONE', got %q", assistant.Content)
		}

		session.Disconnect()
		if err := client.Stop(); err != nil {
			t.Logf("Stop returned: %v", err)
		}

		entries, err := readTelemetryEntries(t, telemetryPath)
		if err != nil {
			t.Fatalf("readTelemetryEntries failed: %v", err)
		}

		var spans []map[string]any
		for _, e := range entries {
			if telemetryType(e) == "span" {
				spans = append(spans, e)
			}
		}
		if len(spans) == 0 {
			t.Fatalf("Expected at least one span entry; got %d entries", len(entries))
		}

		for _, span := range spans {
			if got := instrumentationScopeName(span); got != sourceName {
				t.Errorf("Expected instrumentationScope.name=%q, got %q", sourceName, got)
			}
			if statusCode(span) == 2 {
				t.Errorf("Span has error status: %v", span)
			}
		}

		invokeAgent := findSpanWithOperation(spans, "invoke_agent")
		if invokeAgent == nil {
			t.Fatal("Expected an invoke_agent span")
		}
		if got := stringAttr(invokeAgent, "gen_ai.conversation.id"); got != sessionID {
			t.Errorf("Expected gen_ai.conversation.id=%q, got %q", sessionID, got)
		}
		if !isRootSpan(invokeAgent) {
			t.Errorf("invoke_agent should be a root span, got parentSpanId=%q", stringProp(invokeAgent, "parentSpanId"))
		}
		invokeAgentSpanID := stringProp(invokeAgent, "spanId")
		if invokeAgentSpanID == "" {
			t.Fatal("invoke_agent span has empty spanId")
		}
		invokeAgentTraceID := stringProp(invokeAgent, "traceId")
		if invokeAgentTraceID == "" {
			t.Fatal("invoke_agent span has empty traceId")
		}

		var chatSpans []map[string]any
		for _, span := range spans {
			if stringAttr(span, "gen_ai.operation.name") == "chat" {
				chatSpans = append(chatSpans, span)
			}
		}
		if len(chatSpans) == 0 {
			t.Fatal("Expected at least one chat span")
		}
		for _, chat := range chatSpans {
			if got := stringProp(chat, "parentSpanId"); got != invokeAgentSpanID {
				t.Errorf("Expected chat span parentSpanId=%q, got %q", invokeAgentSpanID, got)
			}
			if got := stringProp(chat, "traceId"); got != invokeAgentTraceID {
				t.Errorf("Expected chat span traceId=%q, got %q", invokeAgentTraceID, got)
			}
		}
		var sawPromptInput, sawDoneOutput bool
		for _, chat := range chatSpans {
			if strings.Contains(stringAttr(chat, "gen_ai.input.messages"), prompt) {
				sawPromptInput = true
			}
			if strings.Contains(stringAttr(chat, "gen_ai.output.messages"), "TELEMETRY_E2E_DONE") {
				sawDoneOutput = true
			}
		}
		if !sawPromptInput {
			t.Errorf("Expected at least one chat span input.messages containing the prompt")
		}
		if !sawDoneOutput {
			t.Errorf("Expected at least one chat span output.messages containing 'TELEMETRY_E2E_DONE'")
		}

		toolSpan := findSpanWithOperation(spans, "execute_tool")
		if toolSpan == nil {
			t.Fatal("Expected an execute_tool span")
		}
		if got := stringProp(toolSpan, "parentSpanId"); got != invokeAgentSpanID {
			t.Errorf("Expected execute_tool parentSpanId=%q, got %q", invokeAgentSpanID, got)
		}
		if got := stringProp(toolSpan, "traceId"); got != invokeAgentTraceID {
			t.Errorf("Expected execute_tool traceId=%q, got %q", invokeAgentTraceID, got)
		}
		if got := stringAttr(toolSpan, "gen_ai.tool.name"); got != toolName {
			t.Errorf("Expected gen_ai.tool.name=%q, got %q", toolName, got)
		}
		if got := stringAttr(toolSpan, "gen_ai.tool.call.id"); strings.TrimSpace(got) == "" {
			t.Errorf("Expected non-empty gen_ai.tool.call.id, got %q", got)
		}
		expectedArgs := fmt.Sprintf("{\"value\":\"%s\"}", marker)
		if got := stringAttr(toolSpan, "gen_ai.tool.call.arguments"); got != expectedArgs {
			t.Errorf("Expected gen_ai.tool.call.arguments=%q, got %q", expectedArgs, got)
		}
		if got := stringAttr(toolSpan, "gen_ai.tool.call.result"); got != marker {
			t.Errorf("Expected gen_ai.tool.call.result=%q, got %q", marker, got)
		}
	})

	t.Run("should export per request subagent chat spans", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)
		telemetryPath := filepath.Join(ctx.WorkDir, fmt.Sprintf("telemetry-%s.jsonl", randomHex(t)))
		const sourceName = "go-sdk-subagent-telemetry-e2e"
		const prompt = "Use the task tool in sync mode to ask a task agent to read subagent-otel.txt with the view tool. Then reply with SUBAGENT_OTEL_DONE."
		if err := os.WriteFile(filepath.Join(ctx.WorkDir, "subagent-otel.txt"), []byte("SUBAGENT_OTEL_FILE_CONTENT"), 0600); err != nil {
			t.Fatalf("WriteFile failed: %v", err)
		}
		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.Telemetry = &copilot.TelemetryConfig{
				FilePath: telemetryPath, ExporterType: "file", SourceName: sourceName,
				CaptureContent: copilot.Bool(true),
			}
		})
		t.Cleanup(func() { client.ForceStop() })
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		sessionID := session.SessionID
		finalMessage := testharness.SubscribeToFinalAssistantMessage(session)
		defer finalMessage.Close()
		if _, err := session.Send(t.Context(), copilot.MessageOptions{Prompt: prompt}); err != nil {
			t.Fatalf("Send failed: %v", err)
		}
		final, err := finalMessage.Wait(t.Context())
		if err != nil {
			t.Fatalf("Wait failed: %v", err)
		}
		assistant, ok := final.Data.(*copilot.AssistantMessageData)
		if !ok || !strings.Contains(assistant.Content, "SUBAGENT_OTEL_DONE") {
			t.Fatalf("Expected final response with SUBAGENT_OTEL_DONE, got %v", final.Data)
		}
		session.Disconnect()
		if err := client.Stop(); err != nil {
			t.Fatalf("Stop failed: %v", err)
		}

		entries, err := readTelemetryEntries(t, telemetryPath)
		if err != nil {
			t.Fatalf("readTelemetryEntries failed: %v", err)
		}
		var spans []map[string]any
		for _, entry := range entries {
			if telemetryType(entry) == "span" {
				spans = append(spans, entry)
			}
		}
		for _, span := range spans {
			if instrumentationScopeName(span) != sourceName || statusCode(span) == 2 {
				t.Errorf("Unexpected source or error span: %v", span)
			}
		}
		invocations := spansWithOperation(spans, "invoke_agent")
		if len(invocations) != 2 {
			t.Fatalf("Expected two invocation spans, got %d: %v", len(invocations), invocations)
		}
		var roots []map[string]any
		for _, span := range invocations {
			if isRootSpan(span) {
				roots = append(roots, span)
			}
		}
		root := requireSingleSpan(t, roots, "root invocation")
		if got := stringAttr(root, "gen_ai.conversation.id"); got != sessionID {
			t.Errorf("Expected conversation id %q, got %q", sessionID, got)
		}
		rootID, traceID := stringProp(root, "spanId"), stringProp(root, "traceId")
		if rootID == "" || traceID == "" {
			t.Fatalf("Root missing span/trace id: %v", root)
		}
		var tasks []map[string]any
		for _, span := range spansWithOperation(spans, "execute_tool") {
			if stringAttr(span, "gen_ai.tool.name") == "task" {
				tasks = append(tasks, span)
			}
		}
		task := requireSingleSpan(t, tasks, "task tool")
		if stringProp(task, "parentSpanId") != rootID {
			t.Errorf("Task parent is not root: %v", task)
		}
		var children []map[string]any
		for _, span := range invocations {
			if stringProp(span, "parentSpanId") == stringProp(task, "spanId") {
				children = append(children, span)
			}
		}
		child := requireSingleSpan(t, children, "child invocation")
		if stringProp(task, "traceId") != traceID || stringProp(child, "traceId") != traceID {
			t.Errorf("Task/child must share root trace: %v %v", task, child)
		}

		chats := spansWithOperation(spans, "chat")
		if len(chats) != 4 {
			t.Fatalf("Expected four chat spans, got %d: %v", len(chats), chats)
		}
		var parentChats, childChats []map[string]any
		for _, chat := range chats {
			switch stringProp(chat, "parentSpanId") {
			case rootID:
				parentChats = append(parentChats, chat)
			case stringProp(child, "spanId"):
				childChats = append(childChats, chat)
			}
		}
		if len(parentChats) != 2 || len(childChats) != 2 {
			t.Fatalf("Expected two chats per agent, got parent=%d child=%d: %v", len(parentChats), len(childChats), chats)
		}
		for _, chat := range parentChats {
			if stringProp(chat, "traceId") != traceID {
				t.Errorf("Parent chat must share root trace: %v", chat)
			}
		}
		var viewCount, finalCount int
		for _, chat := range childChats {
			if stringProp(chat, "traceId") != traceID || stringAttr(chat, "github.copilot.initiator") != "sub-agent" {
				t.Errorf("Child chat missing trace or initiator: %v", chat)
			}
			if strings.Contains(stringAttr(chat, "gen_ai.output.messages"), `"view"`) {
				viewCount++
				if strings.Contains(stringAttr(chat, "gen_ai.input.messages"), "SUBAGENT_OTEL_FILE_CONTENT") {
					t.Errorf("Requesting child chat contains the later tool result: %v", chat)
				}
			}
			if strings.Contains(stringAttr(chat, "gen_ai.output.messages"), "SUBAGENT_OTEL_CHILD_DONE") {
				finalCount++
				if !strings.Contains(stringAttr(chat, "gen_ai.input.messages"), "SUBAGENT_OTEL_FILE_CONTENT") {
					t.Errorf("Final child chat missing tool result: %v", chat)
				}
			}
		}
		if viewCount != 1 || finalCount != 1 {
			t.Errorf("Expected distinct view and final child requests, got view=%d final=%d", viewCount, finalCount)
		}
	})
}

func spansWithOperation(spans []map[string]any, operation string) []map[string]any {
	var matching []map[string]any
	for _, span := range spans {
		if stringAttr(span, "gen_ai.operation.name") == operation {
			matching = append(matching, span)
		}
	}
	return matching
}

func requireSingleSpan(t *testing.T, spans []map[string]any, description string) map[string]any {
	t.Helper()
	if len(spans) != 1 {
		t.Fatalf("Expected exactly one %s span, got %d: %v", description, len(spans), spans)
	}
	return spans[0]
}

func readTelemetryEntries(t *testing.T, path string) ([]map[string]any, error) {
	t.Helper()
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}

	var entries []map[string]any
	for _, line := range strings.Split(string(data), "\n") {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}

		var entry map[string]any
		if err := json.Unmarshal([]byte(line), &entry); err != nil {
			return nil, fmt.Errorf("parse telemetry entry in %q: %w", path, err)
		}
		entries = append(entries, entry)
	}
	return entries, nil
}

func telemetryType(e map[string]any) string { return stringProp(e, "type") }

func stringProp(e map[string]any, name string) string {
	v, ok := e[name]
	if !ok {
		return ""
	}
	switch x := v.(type) {
	case string:
		return x
	case float64, bool:
		raw, _ := json.Marshal(x)
		return string(raw)
	default:
		raw, _ := json.Marshal(x)
		return string(raw)
	}
}

func stringAttr(e map[string]any, name string) string {
	attrs, ok := e["attributes"].(map[string]any)
	if !ok {
		return ""
	}
	v, ok := attrs[name]
	if !ok {
		return ""
	}
	switch x := v.(type) {
	case string:
		return x
	default:
		raw, _ := json.Marshal(x)
		return string(raw)
	}
}

func instrumentationScopeName(e map[string]any) string {
	scope, ok := e["instrumentationScope"].(map[string]any)
	if !ok {
		return ""
	}
	if name, ok := scope["name"].(string); ok {
		return name
	}
	return ""
}

func statusCode(e map[string]any) int {
	status, ok := e["status"].(map[string]any)
	if !ok {
		return 0
	}
	switch v := status["code"].(type) {
	case float64:
		return int(v)
	case int:
		return v
	}
	return 0
}

func isRootSpan(e map[string]any) bool {
	parent := stringProp(e, "parentSpanId")
	return parent == "" || parent == "0000000000000000"
}

func findSpanWithOperation(spans []map[string]any, op string) map[string]any {
	for _, span := range spans {
		if stringAttr(span, "gen_ai.operation.name") == op {
			return span
		}
	}
	return nil
}

// ---------------------------------------------------------------------------
// Unit-style tests mirroring dotnet/test/TelemetryTests.cs.
// These exercise the TelemetryConfig / ClientOptions struct shape only.
// ---------------------------------------------------------------------------

// TestTelemetryConfigUnit covers the dataclass-equivalent unit tests.
//
// CopilotClientOptions_Clone_CopiesTelemetry from the C# baseline has no Go
// equivalent (ClientOptions has no Clone() method).
//
// TelemetryHelpers_Restores_W3C_Trace_Context lives in the copilot package
// (helpers are unexported), so it is tested in go/telemetry_test.go and is
// intentionally not duplicated here.
func TestTelemetryConfigUnit(t *testing.T) {
	t.Run("default values are zero", func(t *testing.T) {
		// Mirrors: TelemetryConfig_DefaultValues_AreNull
		var cfg copilot.TelemetryConfig
		if cfg.OTLPEndpoint != "" {
			t.Errorf("Expected empty OTLPEndpoint, got %q", cfg.OTLPEndpoint)
		}
		if cfg.OTLPProtocol != "" {
			t.Errorf("Expected empty OTLPProtocol, got %q", cfg.OTLPProtocol)
		}
		if cfg.FilePath != "" {
			t.Errorf("Expected empty FilePath, got %q", cfg.FilePath)
		}
		if cfg.ExporterType != "" {
			t.Errorf("Expected empty ExporterType, got %q", cfg.ExporterType)
		}
		if cfg.SourceName != "" {
			t.Errorf("Expected empty SourceName, got %q", cfg.SourceName)
		}
		if cfg.CaptureContent != nil {
			t.Errorf("Expected nil CaptureContent, got %v", cfg.CaptureContent)
		}
	})

	t.Run("can set all properties", func(t *testing.T) {
		// Mirrors: TelemetryConfig_CanSetAllProperties
		cfg := copilot.TelemetryConfig{
			OTLPEndpoint:   "http://localhost:4318",
			OTLPProtocol:   "http/protobuf",
			FilePath:       "/tmp/traces.json",
			ExporterType:   "otlp-http",
			SourceName:     "my-app",
			CaptureContent: copilot.Bool(true),
		}
		if cfg.OTLPEndpoint != "http://localhost:4318" {
			t.Errorf("OTLPEndpoint mismatch: %q", cfg.OTLPEndpoint)
		}
		if cfg.OTLPProtocol != "http/protobuf" {
			t.Errorf("OTLPProtocol mismatch: %q", cfg.OTLPProtocol)
		}
		if cfg.FilePath != "/tmp/traces.json" {
			t.Errorf("FilePath mismatch: %q", cfg.FilePath)
		}
		if cfg.ExporterType != "otlp-http" {
			t.Errorf("ExporterType mismatch: %q", cfg.ExporterType)
		}
		if cfg.SourceName != "my-app" {
			t.Errorf("SourceName mismatch: %q", cfg.SourceName)
		}
		if cfg.CaptureContent == nil || *cfg.CaptureContent != true {
			t.Errorf("CaptureContent mismatch: %v", cfg.CaptureContent)
		}
	})

	t.Run("client options telemetry defaults to nil", func(t *testing.T) {
		// Mirrors: CopilotClientOptions_Telemetry_DefaultsToNull
		opts := copilot.ClientOptions{}
		if opts.Telemetry != nil {
			t.Errorf("Expected ClientOptions.Telemetry to be nil by default, got %v", opts.Telemetry)
		}
	})
}
