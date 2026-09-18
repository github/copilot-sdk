package e2e

import (
	"context"
	"encoding/json"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

type outputInventory struct {
	Count int    `json:"count"`
	Color string `json:"color"`
}

type outputAnswer struct {
	Answer int `json:"answer"`
}

func structuredSessionConfig(proxy string) *copilot.SessionConfig {
	return &copilot.SessionConfig{
		Model: "gpt-4.1", AvailableTools: []string{},
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		Provider: &copilot.ProviderConfig{
			Type: "openai", WireAPI: "completions", BaseURL: proxy,
			ModelID: "gpt-4.1", WireModel: "gpt-4.1", APIKey: "fake-token-for-e2e-tests",
			Headers: map[string]string{
				"Copilot-Integration-Id": "copilot-developer-cli",
				"Copilot-Harness-Id":     "copilot-sdk",
				"X-GitHub-Api-Version":   "2026-08-01",
			},
		},
	}
}

func TestStructuredOutputE2E(t *testing.T) {
	harness := testharness.NewTestContext(t)
	client := harness.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("infers_typed_result_after_custom_tool", func(t *testing.T) {
		harness.ConfigureForTest(t)
		ctx, cancel := context.WithTimeout(t.Context(), 45*time.Second)
		defer cancel()
		var calls atomic.Int32
		config := structuredSessionConfig(harness.ProxyURL)
		streaming := true
		config.Streaming = &streaming
		config.Tools = []copilot.Tool{copilot.DefineTool("get_inventory", "Get the current widget inventory.",
			func(_ struct{}, _ copilot.ToolInvocation) (string, error) {
				calls.Add(1)
				return "The inventory contains 42 red widgets.", nil
			})}
		session, err := client.CreateSession(ctx, config)
		if err != nil {
			t.Fatal(err)
		}
		var deltas atomic.Int32
		unsubscribe := session.On(func(event copilot.SessionEvent) {
			if _, ok := event.Data.(*copilot.AssistantMessageDeltaData); ok {
				deltas.Add(1)
			}
		})
		defer unsubscribe()
		result, err := copilot.SendAndWait[outputInventory](ctx, session, copilot.MessageOptions{
			Prompt: "Call get_inventory, then report the widget count and color.",
		})
		if err != nil || result != (outputInventory{42, "red"}) || calls.Load() == 0 {
			t.Fatalf("unexpected result %+v, calls %d, error %v", result, calls.Load(), err)
		}
		if deltas.Load() == 0 {
			t.Fatal("typed wait did not stream text updates")
		}
		ordinary, err := session.SendAndWait(ctx, copilot.MessageOptions{Prompt: "Now reply with exactly the plain text HELLO, not JSON."})
		if err != nil {
			t.Fatal(err)
		}
		if ordinary == nil || strings.TrimSpace(ordinary.Data.(*copilot.AssistantMessageData).Content) != "HELLO" {
			t.Fatalf("schema leaked into ordinary follow-up: %v", ordinary)
		}
		exchanges, err := harness.GetExchanges()
		if err != nil || len(exchanges) < 3 {
			t.Fatalf("expected tool, final, and ordinary requests: %d, error %v", len(exchanges), err)
		}
		for _, exchange := range exchanges[:len(exchanges)-1] {
			format := exchange.Request.ResponseFormat
			contract, ok := format["json_schema"].(map[string]any)
			if format["type"] != "json_schema" || !ok || contract["strict"] != true {
				t.Fatalf("missing provider-native schema: %+v", format)
			}
			schema := contract["schema"].(map[string]any)
			properties := schema["properties"].(map[string]any)
			if schema["additionalProperties"] != false ||
				properties["count"].(map[string]any)["type"] != "integer" ||
				properties["color"].(map[string]any)["type"] != "string" {
				t.Fatalf("incorrect inferred inventory schema: %+v", schema)
			}
		}
		if exchanges[len(exchanges)-1].Request.ResponseFormat != nil {
			t.Fatal("response_format leaked into ordinary follow-up")
		}
	})

	t.Run("typed_wait_returns_stop_hook_correction", func(t *testing.T) {
		harness.ConfigureForTest(t)
		ctx, cancel := context.WithTimeout(t.Context(), 45*time.Second)
		defer cancel()
		var stops atomic.Int32
		config := structuredSessionConfig(harness.ProxyURL)
		config.Hooks = &copilot.SessionHooks{OnAgentStop: func(_ copilot.AgentStopHookInput, _ copilot.HookInvocation) (*copilot.AgentStopHookOutput, error) {
			if stops.Add(1) == 1 {
				return &copilot.AgentStopHookOutput{Decision: "block", Reason: "Correct the answer to 99, not 42. Do not use tools."}, nil
			}
			return nil, nil
		}}
		session, err := client.CreateSession(ctx, config)
		if err != nil {
			t.Fatal(err)
		}
		var mu sync.Mutex
		var replies []*copilot.AssistantMessageData
		unsubscribe := session.On(func(event copilot.SessionEvent) {
			if reply, ok := event.Data.(*copilot.AssistantMessageData); ok && (event.AgentID == nil || *event.AgentID == "") {
				mu.Lock()
				replies = append(replies, reply)
				mu.Unlock()
			}
		})
		defer unsubscribe()
		result, err := copilot.SendAndWait[outputAnswer](ctx, session, copilot.MessageOptions{Prompt: "What is 19 + 23? Do not use tools."})
		if err != nil || result.Answer != 99 || stops.Load() != 2 {
			t.Fatalf("result %+v, stops %d, error %v", result, stops.Load(), err)
		}
		mu.Lock()
		defer mu.Unlock()
		if len(replies) != 2 || replies[0].OriginatingMessageID == nil || *replies[0].OriginatingMessageID == "" ||
			replies[1].OriginatingMessageID == nil || *replies[0].OriginatingMessageID != *replies[1].OriginatingMessageID {
			t.Fatalf("expected two replies with the same nonempty origin: %+v", replies)
		}
		for i, expected := range []int{42, 99} {
			var answer outputAnswer
			if err := json.Unmarshal([]byte(replies[i].Content), &answer); err != nil || answer.Answer != expected {
				t.Fatalf("reply %d: %+v, error %v", i, answer, err)
			}
		}
	})

	t.Run("send_selects_correlated_response_after_idle", func(t *testing.T) {
		harness.ConfigureForTest(t)
		ctx, cancel := context.WithTimeout(t.Context(), 45*time.Second)
		defer cancel()
		entered, release := make(chan struct{}), make(chan struct{})
		var releaseOnce sync.Once
		defer releaseOnce.Do(func() { close(release) })
		config := structuredSessionConfig(harness.ProxyURL)
		config.Tools = []copilot.Tool{copilot.DefineTool("read_inventory", "Read the current widget count and color.",
			func(_ struct{}, _ copilot.ToolInvocation) (string, error) {
				return "The inventory contains 42 red widgets.", nil
			})}
		config.Hooks = &copilot.SessionHooks{OnAgentStop: func(_ copilot.AgentStopHookInput, _ copilot.HookInvocation) (*copilot.AgentStopHookOutput, error) {
			close(entered)
			select {
			case <-release:
				return nil, nil
			case <-ctx.Done():
				return nil, ctx.Err()
			}
		}}
		session, err := client.CreateSession(ctx, config)
		if err != nil {
			t.Fatal(err)
		}
		idle := make(chan struct{}, 1)
		failures := make(chan string, 1)
		var mu sync.Mutex
		var replies []*copilot.AssistantMessageData
		unsubscribe := session.On(func(event copilot.SessionEvent) {
			if event.AgentID != nil && *event.AgentID != "" {
				return
			}
			switch data := event.Data.(type) {
			case *copilot.AssistantMessageData:
				mu.Lock()
				replies = append(replies, data)
				mu.Unlock()
			case *copilot.SessionIdleData:
				select {
				case idle <- struct{}{}:
				default:
				}
			case *copilot.SessionErrorData:
				select {
				case failures <- data.Message:
				default:
				}
			}
		})
		defer unsubscribe()
		schema := map[string]any{
			"type": "object", "properties": map[string]any{"count": map[string]any{"type": "integer"}, "color": map[string]any{"type": "string"}},
			"required": []string{"count", "color"}, "additionalProperties": false,
		}
		origin, err := session.Send(ctx, copilot.MessageOptions{
			Prompt: "Call read_inventory once, then report the current widget count and color.", ResponseSchema: schema,
		})
		if err != nil {
			t.Fatal(err)
		}
		select {
		case <-entered:
		case err := <-failures:
			t.Fatal(err)
		case <-ctx.Done():
			t.Fatal(ctx.Err())
		}
		select {
		case <-idle:
			t.Fatal("idle before stop hook finished")
		default:
		}
		releaseOnce.Do(func() { close(release) })
		select {
		case <-idle:
		case err := <-failures:
			t.Fatal(err)
		case <-ctx.Done():
			t.Fatal(ctx.Err())
		}
		mu.Lock()
		defer mu.Unlock()
		if len(replies) < 2 {
			t.Fatalf("expected tool and final messages, got %d", len(replies))
		}
		final := replies[len(replies)-1]
		var inventory outputInventory
		if final.OriginatingMessageID == nil || *final.OriginatingMessageID != origin || len(final.ToolRequests) != 0 {
			t.Fatalf("invalid final message: %+v", final)
		}
		if err := json.Unmarshal([]byte(final.Content), &inventory); err != nil || inventory != (outputInventory{42, "red"}) {
			t.Fatalf("result %+v, error %v", inventory, err)
		}
		if len(replies[0].ToolRequests) == 0 {
			t.Fatal("missing intermediate tool request")
		}
	})

	t.Run("rejects_invalid_formats_before_admission", func(t *testing.T) {
		harness.ConfigureForTest(t)
		ctx, cancel := context.WithTimeout(t.Context(), 45*time.Second)
		defer cancel()
		session, err := client.CreateSession(ctx, structuredSessionConfig(harness.ProxyURL))
		if err != nil {
			t.Fatal(err)
		}
		var admitted atomic.Bool
		unsubscribe := session.On(func(event copilot.SessionEvent) {
			switch event.Data.(type) {
			case *copilot.UserMessageData, *copilot.SessionErrorData:
				admitted.Store(true)
			}
		})
		defer unsubscribe()
		if _, err := copilot.SendAndWait[outputAnswer](ctx, session, copilot.MessageOptions{Prompt: "Must not be admitted", Mode: "immediate"}); err == nil {
			t.Fatal("typed immediate steering should be rejected")
		}
		schema := map[string]any{"type": "object", "description": strings.Repeat("x", 32*1024*1024)}
		if _, err := session.SendAndWait(ctx, copilot.MessageOptions{Prompt: "Must not be admitted", ResponseSchema: schema}); err == nil || !strings.Contains(err.Error(), "32 MiB") {
			t.Fatalf("expected schema size rejection: %v", err)
		}
		if _, err := session.RPC.SendMessages(ctx, &rpc.SendMessagesRequest{
			Messages:       []rpc.SendMessageItem{},
			ResponseFormat: &rpc.ResponseFormat{Type: rpc.ResponseFormatTypeJSONSchema, JSONSchema: rpc.JSONSchemaResponseFormat{Name: "response", Schema: schema}},
		}); err == nil || !strings.Contains(err.Error(), "32 MiB") {
			t.Fatalf("expected batch schema size rejection: %v", err)
		}
		pending, err := session.RPC.Queue.PendingItems(ctx)
		if err != nil || len(pending.Items) != 0 || admitted.Load() {
			t.Fatalf("rejected message was admitted: queue %+v, event %v, error %v", pending, admitted.Load(), err)
		}
		exchanges, err := harness.GetExchanges()
		if err != nil || len(exchanges) != 0 {
			t.Fatalf("unexpected provider calls: %d, error %v", len(exchanges), err)
		}
	})

	t.Run("sends_explicit_schema_for_message_and_batch", func(t *testing.T) {
		harness.ConfigureForTest(t)
		ctx, cancel := context.WithTimeout(t.Context(), 45*time.Second)
		defer cancel()
		session, err := client.CreateSession(ctx, structuredSessionConfig(harness.ProxyURL))
		if err != nil {
			t.Fatal(err)
		}
		var schema map[string]any
		if err := json.Unmarshal([]byte(`{"type":"object","properties":{"count":{"type":"integer"},"color":{"type":"string"}},"required":["count","color"],"additionalProperties":false}`), &schema); err != nil {
			t.Fatal(err)
		}
		completed := make(chan struct{}, 1)
		var mu sync.Mutex
		var replies []*copilot.AssistantMessageData
		unsubscribe := session.On(func(event copilot.SessionEvent) {
			if event.AgentID != nil && *event.AgentID != "" {
				return
			}
			switch data := event.Data.(type) {
			case *copilot.AssistantMessageData:
				mu.Lock()
				replies = append(replies, data)
				mu.Unlock()
			case *copilot.SessionIdleData:
				select {
				case completed <- struct{}{}:
				default:
				}
			}
		})
		defer unsubscribe()
		strict := true
		accepted, err := session.RPC.SendMessages(ctx, &rpc.SendMessagesRequest{
			Messages: []rpc.SendMessageItem{
				{Prompt: "There are 42 red widgets in stock."},
				{Prompt: "Report the widget count and color."},
			},
			ResponseFormat: &rpc.ResponseFormat{Type: rpc.ResponseFormatTypeJSONSchema,
				JSONSchema: rpc.JSONSchemaResponseFormat{Name: "inventory", Schema: schema, Strict: &strict}},
		})
		if err != nil {
			t.Fatal(err)
		}
		select {
		case <-completed:
		case <-ctx.Done():
			t.Fatal(ctx.Err())
		}
		mu.Lock()
		var content string
		for _, reply := range replies {
			if reply.OriginatingMessageID != nil && *reply.OriginatingMessageID == accepted.MessageIDs[len(accepted.MessageIDs)-1] {
				content = reply.Content
			}
		}
		mu.Unlock()
		var inventory outputInventory
		if err := json.Unmarshal([]byte(content), &inventory); err != nil {
			t.Fatal(err)
		}
		if inventory != (outputInventory{42, "red"}) {
			t.Fatalf("unexpected batch result: %+v", inventory)
		}
		raw, err := session.SendAndWait(ctx, copilot.MessageOptions{
			Prompt: "The inventory now has 21 blue widgets. Report the new count and color.", ResponseSchema: schema,
		})
		if err != nil {
			t.Fatal(err)
		}
		if err := json.Unmarshal([]byte(raw.Data.(*copilot.AssistantMessageData).Content), &inventory); err != nil {
			t.Fatal(err)
		}
		if inventory != (outputInventory{21, "blue"}) {
			t.Fatalf("unexpected raw result: %+v", inventory)
		}
	})

	t.Run("typed_wait_returns_stop_hook_correction_after_terminal_tool", func(t *testing.T) {
		harness.ConfigureForTest(t)
		ctx, cancel := context.WithTimeout(t.Context(), 45*time.Second)
		defer cancel()
		var calls, stops atomic.Int32
		tool := copilot.DefineTool("lookup_number", "Return the number needed for the calculation.",
			func(_ struct{}, _ copilot.ToolInvocation) (int, error) { calls.Add(1); return 58, nil })
		tool.IsTerminal, tool.SkipPermission = true, true
		config := structuredSessionConfig(harness.ProxyURL)
		config.Tools = []copilot.Tool{tool}
		config.Hooks = &copilot.SessionHooks{OnAgentStop: func(_ copilot.AgentStopHookInput, _ copilot.HookInvocation) (*copilot.AgentStopHookOutput, error) {
			if stops.Add(1) == 1 {
				return &copilot.AgentStopHookOutput{Decision: "block", Reason: "Correct the answer to 99, not 63. Do not use tools."}, nil
			}
			return nil, nil
		}}
		session, err := client.CreateSession(ctx, config)
		if err != nil {
			t.Fatal(err)
		}
		result, err := copilot.SendAndWait[outputAnswer](ctx, session, copilot.MessageOptions{
			Prompt: "Call lookup_number exactly once, then add 5 to the returned number. Do not guess its result.",
		})
		if err != nil || result.Answer != 99 || calls.Load() != 1 || stops.Load() != 2 {
			t.Fatalf("result %+v, calls %d, stops %d, error %v", result, calls.Load(), stops.Load(), err)
		}
	})

	t.Run("typed_wait_returns_late_steering_response", func(t *testing.T) {
		harness.ConfigureForTest(t)
		ctx, cancel := context.WithTimeout(t.Context(), 45*time.Second)
		defer cancel()
		var session *copilot.Session
		var stops atomic.Int32
		config := structuredSessionConfig(harness.ProxyURL)
		config.Hooks = &copilot.SessionHooks{OnAgentStop: func(_ copilot.AgentStopHookInput, _ copilot.HookInvocation) (*copilot.AgentStopHookOutput, error) {
			if stops.Add(1) == 1 {
				_, err := session.Send(ctx, copilot.MessageOptions{Prompt: "Change the answer to 99. Do not use tools.", Mode: "immediate"})
				return nil, err
			}
			return nil, nil
		}}
		var err error
		session, err = client.CreateSession(ctx, config)
		if err != nil {
			t.Fatal(err)
		}
		result, err := copilot.SendAndWait[outputAnswer](ctx, session, copilot.MessageOptions{Prompt: "What is 19 + 23? Do not use tools."})
		if err != nil || result.Answer != 99 || stops.Load() != 2 {
			t.Fatalf("result %+v, stops %d, error %v", result, stops.Load(), err)
		}
	})

	t.Run("typed_result_after_terminal_tool_and_steering", func(t *testing.T) {
		harness.ConfigureForTest(t)
		ctx, cancel := context.WithTimeout(t.Context(), 45*time.Second)
		defer cancel()
		var session *copilot.Session
		var calls atomic.Int32
		tool := copilot.DefineTool("lookup_number", "Return the number needed for the calculation.",
			func(_ struct{}, _ copilot.ToolInvocation) (int, error) {
				calls.Add(1)
				_, err := session.Send(ctx, copilot.MessageOptions{
					Prompt: "Continue with the original calculation. Do not call any more tools.", Mode: "immediate",
				})
				return 58, err
			})
		tool.IsTerminal, tool.SkipPermission = true, true
		config := structuredSessionConfig(harness.ProxyURL)
		config.Tools = []copilot.Tool{tool}
		var err error
		session, err = client.CreateSession(ctx, config)
		if err != nil {
			t.Fatal(err)
		}
		type toolAnswer struct {
			Answer   int    `json:"answer"`
			Contract string `json:"contract"`
		}
		result, err := copilot.SendAndWait[toolAnswer](ctx, session, copilot.MessageOptions{
			Prompt: "Call lookup_number exactly once, then add 5 to the returned number. Do not guess its result.",
		})
		if err != nil || result != (toolAnswer{63, "typed_tool"}) || calls.Load() != 1 {
			t.Fatalf("result %+v, calls %d, error %v", result, calls.Load(), err)
		}
		exchanges, err := harness.GetExchanges()
		if err != nil || len(exchanges) < 2 {
			t.Fatalf("expected tool and final requests: %d, error %v", len(exchanges), err)
		}
		for _, exchange := range exchanges[1:] {
			if string(exchange.Request.ToolChoice) != `"none"` {
				t.Fatalf("terminal tool did not disable tools: %s", exchange.Request.ToolChoice)
			}
		}
		for _, exchange := range exchanges {
			if exchange.Request.ResponseFormat["type"] != "json_schema" {
				t.Fatal("steering lost the active output schema")
			}
		}
	})

	t.Run("concurrent_typed_sends_return_their_own_results", func(t *testing.T) {
		harness.ConfigureForTest(t)
		ctx, cancel := context.WithTimeout(t.Context(), 45*time.Second)
		defer cancel()
		entered, release := make(chan struct{}), make(chan struct{})
		var releaseOnce sync.Once
		defer releaseOnce.Do(func() { close(release) })
		config := structuredSessionConfig(harness.ProxyURL)
		config.Tools = []copilot.Tool{copilot.DefineTool("first_number", "Get the number for the first question.",
			func(_ struct{}, _ copilot.ToolInvocation) (int, error) {
				close(entered)
				select {
				case <-release:
					return 42, nil
				case <-ctx.Done():
					return 0, ctx.Err()
				}
			})}
		session, err := client.CreateSession(ctx, config)
		if err != nil {
			t.Fatal(err)
		}
		type firstAnswer struct {
			First int `json:"first"`
		}
		type secondAnswer struct {
			Second int `json:"second"`
		}
		type outcome struct {
			value int
			err   error
		}
		first, second := make(chan outcome, 1), make(chan outcome, 1)
		go func() {
			result, err := copilot.SendAndWait[firstAnswer](ctx, session, copilot.MessageOptions{Prompt: "Call first_number exactly once and report its returned number."})
			first <- outcome{result.First, err}
		}()
		select {
		case <-entered:
		case result := <-first:
			t.Fatalf("tool was not entered: %+v", result)
		case <-ctx.Done():
			t.Fatal(ctx.Err())
		}
		go func() {
			result, err := copilot.SendAndWait[secondAnswer](ctx, session, copilot.MessageOptions{Prompt: "What is 30 + 7? Do not use tools."})
			second <- outcome{result.Second, err}
		}()
		for {
			pending, err := session.RPC.Queue.PendingItems(ctx)
			if err != nil {
				t.Fatal(err)
			}
			if len(pending.Items) > 0 {
				break
			}
			select {
			case <-time.After(10 * time.Millisecond):
			case <-ctx.Done():
				t.Fatal(ctx.Err())
			}
		}
		releaseOnce.Do(func() { close(release) })
		for _, expected := range []struct {
			result <-chan outcome
			value  int
		}{{first, 42}, {second, 37}} {
			select {
			case result := <-expected.result:
				if result.err != nil || result.value != expected.value {
					t.Fatalf("unexpected result: %+v", result)
				}
			case <-ctx.Done():
				t.Fatal(ctx.Err())
			}
		}
	})
}
