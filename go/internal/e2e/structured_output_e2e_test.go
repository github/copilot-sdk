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
		config.Tools = []copilot.Tool{copilot.DefineTool("get_inventory", "Get the current widget inventory.",
			func(_ struct{}, _ copilot.ToolInvocation) (string, error) {
				calls.Add(1)
				return "The inventory contains 42 red widgets.", nil
			})}
		session, err := client.CreateSession(ctx, config)
		if err != nil {
			t.Fatal(err)
		}
		result, err := copilot.SendAndWait[outputInventory](ctx, session, copilot.MessageOptions{
			Prompt: "Call get_inventory, then report the widget count and color.",
		})
		if err != nil || result != (outputInventory{42, "red"}) || calls.Load() == 0 {
			t.Fatalf("unexpected result %+v, calls %d, error %v", result, calls.Load(), err)
		}
		ordinary, err := session.SendAndWait(ctx, copilot.MessageOptions{Prompt: "Now reply with exactly the plain text HELLO, not JSON."})
		if err != nil {
			t.Fatal(err)
		}
		if ordinary == nil || strings.TrimSpace(ordinary.Data.(*copilot.AssistantMessageData).Content) != "HELLO" {
			t.Fatalf("schema leaked into ordinary follow-up: %v", ordinary)
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
