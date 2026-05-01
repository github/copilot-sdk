package e2e

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// Mirrors dotnet/test/TelemetryExportTests.cs (snapshot category "telemetry").
func TestTelemetryE2E(t *testing.T) {
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

		if _, err := session.Send(t.Context(), copilot.MessageOptions{Prompt: prompt}); err != nil {
			t.Fatalf("Send failed: %v", err)
		}
		final, err := testharness.GetFinalAssistantMessage(t.Context(), session)
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

		entries, err := readTelemetryEntries(t, telemetryPath, 30*time.Second, func(es []map[string]any) bool {
			for _, e := range es {
				if telemetryType(e) == "span" && stringAttr(e, "gen_ai.operation.name") == "invoke_agent" {
					return true
				}
			}
			return false
		})
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

		traceIDs := map[string]struct{}{}
		for _, span := range spans {
			id := stringProp(span, "traceId")
			if id != "" {
				traceIDs[id] = struct{}{}
			}
		}
		if len(traceIDs) != 1 {
			t.Errorf("Expected exactly 1 trace id across spans, got %d (%v)", len(traceIDs), traceIDs)
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
}

func readTelemetryEntries(t *testing.T, path string, timeout time.Duration, isComplete func([]map[string]any) bool) ([]map[string]any, error) {
	t.Helper()
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		if info, err := os.Stat(path); err == nil && info.Size() > 0 {
			data, err := os.ReadFile(path)
			if err == nil {
				var entries []map[string]any
				for _, line := range strings.Split(string(data), "\n") {
					line = strings.TrimSpace(line)
					if line == "" {
						continue
					}
					var entry map[string]any
					if err := json.Unmarshal([]byte(line), &entry); err != nil {
						continue
					}
					entries = append(entries, entry)
				}
				if len(entries) > 0 && isComplete(entries) {
					return entries, nil
				}
			}
		}
		time.Sleep(100 * time.Millisecond)
	}
	return nil, fmt.Errorf("timed out waiting for telemetry records in %q", path)
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
			FilePath:       "/tmp/traces.json",
			ExporterType:   "otlp-http",
			SourceName:     "my-app",
			CaptureContent: copilot.Bool(true),
		}
		if cfg.OTLPEndpoint != "http://localhost:4318" {
			t.Errorf("OTLPEndpoint mismatch: %q", cfg.OTLPEndpoint)
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
