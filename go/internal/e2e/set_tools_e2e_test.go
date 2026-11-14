package e2e

import (
	"fmt"
	"slices"
	"strings"
	"sync"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

const (
	fruitPrompt             = "Use lookup_fruit to find the fruit for code 42."
	fruitAndVegetablePrompt = "Use lookup_fruit to find the fruit for code 42 again, and use lookup_vegetable to find the vegetable for code 7."
	vegetablePrompt         = "Use lookup_vegetable to find the vegetable for code 7."
)

func TestSetToolsE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(client.ForceStop)

	t.Run("replaces_tools_on_a_created_session", func(t *testing.T) {
		ctx.ConfigureSnapshot(t, "set_tools/replaces_tools_on_a_created_session")
		var originalLookups, replacementLookups, vegetableLookups toolCallRecorder
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools: []copilot.Tool{
				lookupFruitTool("apple", &originalLookups),
				retiredLookupTool(),
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		first, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: fruitPrompt})
		if err != nil {
			t.Fatalf("first SendAndWait failed: %v", err)
		}
		assertAssistantContains(t, first, "apple")

		if err := session.SetTools(t.Context(), []copilot.Tool{
			lookupFruitTool("dragonfruit", &replacementLookups),
			lookupVegetableTool(&vegetableLookups),
		}); err != nil {
			t.Fatalf("SetTools failed: %v", err)
		}

		second, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: fruitAndVegetablePrompt})
		if err != nil {
			t.Fatalf("second SendAndWait failed: %v", err)
		}
		assertAssistantContains(t, second, "dragonfruit")
		assertAssistantContains(t, second, "carrot")
		assertCalls(t, "original fruit lookups", originalLookups.calls(), []int{42})
		assertCalls(t, "replacement fruit lookups", replacementLookups.calls(), []int{42})
		assertCalls(t, "vegetable lookups", vegetableLookups.calls(), []int{7})

		// Model requests after the replacement offer exactly the new tool set.
		exchanges, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("GetExchanges failed: %v", err)
		}
		replacedFrom := indexOfPrompt(exchanges, fruitAndVegetablePrompt)
		if replacedFrom <= 0 {
			t.Fatalf("replacement prompt first sent in exchange %d, want an earlier exchange before it", replacedFrom)
		}
		assertOffered(t, exchanges[:replacedFrom], []string{"lookup_fruit", "retired_lookup"}, []string{"lookup_vegetable"})
		assertOffered(t, exchanges[replacedFrom:], []string{"lookup_fruit", "lookup_vegetable"}, []string{"retired_lookup"})
	})

	t.Run("replaces_tools_on_a_resumed_session", func(t *testing.T) {
		ctx.ConfigureSnapshot(t, "set_tools/replaces_tools_on_a_resumed_session")
		var createdLookups toolCallRecorder
		created, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools:               []copilot.Tool{lookupFruitTool("apple", &createdLookups)},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		sessionID := created.SessionID
		first, err := created.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: fruitPrompt})
		if err != nil {
			t.Fatalf("first SendAndWait failed: %v", err)
		}
		assertAssistantContains(t, first, "apple")
		assertCalls(t, "created fruit lookups", createdLookups.calls(), []int{42})
		if err := created.Disconnect(); err != nil {
			t.Fatalf("Disconnect failed: %v", err)
		}

		var fruitLookups, vegetableLookups toolCallRecorder
		resumed, err := client.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools:               []copilot.Tool{lookupFruitTool("apple", &fruitLookups)},
		})
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		t.Cleanup(func() { _ = resumed.Disconnect() })
		if err := resumed.SetTools(t.Context(), []copilot.Tool{lookupVegetableTool(&vegetableLookups)}); err != nil {
			t.Fatalf("SetTools failed: %v", err)
		}

		answer, err := resumed.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: vegetablePrompt})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		assertAssistantContains(t, answer, "carrot")
		assertCalls(t, "vegetable lookups", vegetableLookups.calls(), []int{7})
		assertCalls(t, "fruit lookups", fruitLookups.calls(), nil)

		exchanges, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("GetExchanges failed: %v", err)
		}
		replacedFrom := indexOfPrompt(exchanges, vegetablePrompt)
		if replacedFrom <= 0 {
			t.Fatalf("replacement prompt first sent in exchange %d, want an earlier exchange before it", replacedFrom)
		}
		assertOffered(t, exchanges[replacedFrom:], []string{"lookup_vegetable"}, []string{"lookup_fruit"})
	})

	t.Run("keeps_the_previous_tools_when_a_replacement_is_rejected", func(t *testing.T) {
		ctx.ConfigureSnapshot(t, "set_tools/keeps_the_previous_tools_when_a_replacement_is_rejected")
		var originalLookups, replacementLookups toolCallRecorder
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools:               []copilot.Tool{lookupFruitTool("apple", &originalLookups)},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		err = session.SetTools(t.Context(), []copilot.Tool{
			lookupFruitTool("dragonfruit", &replacementLookups),
			invalidTool(),
		})
		if err == nil {
			t.Fatal("SetTools succeeded; want runtime rejection")
		}

		answer, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: fruitPrompt})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		assertAssistantContains(t, answer, "apple")
		assertCalls(t, "original fruit lookups", originalLookups.calls(), []int{42})
		assertCalls(t, "replacement fruit lookups", replacementLookups.calls(), nil)
	})
}

type toolCallRecorder struct {
	mu    sync.Mutex
	codes []int
}

func (r *toolCallRecorder) record(code int) {
	if r == nil {
		return
	}
	r.mu.Lock()
	defer r.mu.Unlock()
	r.codes = append(r.codes, code)
}

func (r *toolCallRecorder) calls() []int {
	r.mu.Lock()
	defer r.mu.Unlock()
	return append([]int(nil), r.codes...)
}

func lookupFruitTool(fruit string, calls *toolCallRecorder) copilot.Tool {
	return numericCodeTool("lookup_fruit", "Looks up the fruit for a numeric code", "Fruit code", func(code int) string {
		calls.record(code)
		return fruit
	})
}

func lookupVegetableTool(calls *toolCallRecorder) copilot.Tool {
	return numericCodeTool("lookup_vegetable", "Looks up the vegetable for a numeric code", "Vegetable code", func(code int) string {
		calls.record(code)
		return "carrot"
	})
}

func retiredLookupTool() copilot.Tool {
	return copilot.Tool{
		Name:        "retired_lookup",
		Description: "Looks up a retired value",
		Handler: func(_ copilot.ToolInvocation) (copilot.ToolResult, error) {
			return copilot.ToolResult{TextResultForLLM: "retired"}, nil
		},
	}
}

func invalidTool() copilot.Tool {
	return copilot.Tool{
		Name:        "invalid.tool",
		Description: "Has a name the runtime rejects",
		Handler: func(_ copilot.ToolInvocation) (copilot.ToolResult, error) {
			return copilot.ToolResult{TextResultForLLM: "never"}, nil
		},
	}
}

func numericCodeTool(name, description, codeDescription string, handler func(int) string) copilot.Tool {
	return copilot.Tool{
		Name:        name,
		Description: description,
		Parameters: map[string]any{
			"type": "object",
			"properties": map[string]any{
				"code": map[string]any{
					"type":        "integer",
					"description": codeDescription,
				},
			},
			"required": []string{"code"},
		},
		Handler: func(inv copilot.ToolInvocation) (copilot.ToolResult, error) {
			code, err := codeArgument(inv.Arguments)
			if err != nil {
				return copilot.ToolResult{}, err
			}
			return copilot.ToolResult{TextResultForLLM: handler(code)}, nil
		},
	}
}

func codeArgument(arguments any) (int, error) {
	args, ok := arguments.(map[string]any)
	if !ok {
		return 0, fmt.Errorf("arguments = %#v, want object", arguments)
	}
	switch code := args["code"].(type) {
	case float64:
		return int(code), nil
	case int:
		return code, nil
	default:
		return 0, fmt.Errorf("code argument = %#v, want number", args["code"])
	}
}

func assertAssistantContains(t *testing.T, event *copilot.SessionEvent, want string) {
	t.Helper()
	if event == nil {
		t.Fatalf("assistant response is nil, want content containing %q", want)
	}
	data, ok := event.Data.(*copilot.AssistantMessageData)
	if !ok {
		t.Fatalf("event data = %T, want AssistantMessageData", event.Data)
	}
	if !strings.Contains(data.Content, want) {
		t.Fatalf("assistant response = %q, want content containing %q", data.Content, want)
	}
}

func assertCalls(t *testing.T, label string, got, want []int) {
	t.Helper()
	if len(got) != len(want) {
		t.Fatalf("%s = %v, want %v", label, got, want)
	}
	for i := range got {
		if got[i] != want[i] {
			t.Fatalf("%s = %v, want %v", label, got, want)
		}
	}
}

// indexOfPrompt returns the index of the first model request that carries
// prompt as a user message, or -1.
func indexOfPrompt(exchanges []testharness.ParsedHttpExchange, prompt string) int {
	for i, exchange := range exchanges {
		for _, message := range exchange.Request.Messages {
			if message.Role == "user" && (strings.Contains(message.Content, prompt) || strings.Contains(string(message.RawContent), prompt)) {
				return i
			}
		}
	}
	return -1
}

// assertOffered checks that every exchange offered the tools in offered and
// none of the tools in notOffered.
func assertOffered(t *testing.T, exchanges []testharness.ParsedHttpExchange, offered, notOffered []string) {
	t.Helper()
	for _, exchange := range exchanges {
		tools := make([]string, 0, len(exchange.Request.Tools))
		for _, tool := range exchange.Request.Tools {
			tools = append(tools, tool.Function.Name)
		}
		for _, name := range offered {
			if !slices.Contains(tools, name) {
				t.Fatalf("model request offered %v, want it to include %q", tools, name)
			}
		}
		for _, name := range notOffered {
			if slices.Contains(tools, name) {
				t.Fatalf("model request offered %v, want it to omit %q", tools, name)
			}
		}
	}
}
