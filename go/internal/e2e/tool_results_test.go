package e2e

import (
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestToolResults(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should handle structured toolresultobject from custom tool", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type WeatherParams struct {
			City string `json:"city" jsonschema:"City name"`
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools: []copilot.Tool{
				copilot.DefineTool("get_weather", "Gets weather for a city",
					func(params WeatherParams, inv copilot.ToolInvocation) (copilot.ToolResult, error) {
						return copilot.ToolResult{
							TextResultForLLM: "The weather in " + params.City + " is sunny and 72°F",
							ResultType:       "success",
						}, nil
					}),
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(t.Context(), copilot.MessageOptions{Prompt: "What's the weather in Paris?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		answer, err := testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		content := ""
		if answer.Data.Content != nil {
			content = *answer.Data.Content.String
		}
		if !strings.Contains(strings.ToLower(content), "sunny") && !strings.Contains(content, "72") {
			t.Errorf("Expected answer to mention sunny or 72, got %q", content)
		}

		if err := session.Disconnect(); err != nil {
			t.Errorf("Failed to disconnect session: %v", err)
		}
	})

	t.Run("should handle tool result with failure resulttype", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools: []copilot.Tool{
				{
					Name:        "check_status",
					Description: "Checks the status of a service",
					Handler: func(inv copilot.ToolInvocation) (copilot.ToolResult, error) {
						return copilot.ToolResult{
							TextResultForLLM: "Service unavailable",
							ResultType:       "failure",
							Error:            "API timeout",
						}, nil
					},
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(t.Context(), copilot.MessageOptions{
			Prompt: "Check the status of the service using check_status. If it fails, say 'service is down'.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		answer, err := testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		content := ""
		if answer.Data.Content != nil {
			content = *answer.Data.Content.String
		}
		if !strings.Contains(strings.ToLower(content), "service is down") {
			t.Errorf("Expected 'service is down', got %q", content)
		}

		if err := session.Disconnect(); err != nil {
			t.Errorf("Failed to disconnect session: %v", err)
		}
	})

	t.Run("should preserve tooltelemetry and not stringify structured results for llm", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type AnalyzeParams struct {
			File string `json:"file" jsonschema:"File to analyze"`
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools: []copilot.Tool{
				copilot.DefineTool("analyze_code", "Analyzes code for issues",
					func(params AnalyzeParams, inv copilot.ToolInvocation) (copilot.ToolResult, error) {
						return copilot.ToolResult{
							TextResultForLLM: "Analysis of " + params.File + ": no issues found",
							ResultType:       "success",
							ToolTelemetry: map[string]any{
								"metrics":    map[string]any{"analysisTimeMs": 150},
								"properties": map[string]any{"analyzer": "eslint"},
							},
						}, nil
					}),
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(t.Context(), copilot.MessageOptions{Prompt: "Analyze the file main.ts for issues."})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		answer, err := testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		content := ""
		if answer.Data.Content != nil {
			content = *answer.Data.Content.String
		}
		if !strings.Contains(strings.ToLower(content), "no issues") {
			t.Errorf("Expected 'no issues', got %q", content)
		}

		// Verify the LLM received just textResultForLlm, not stringified JSON
		traffic, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("Failed to get exchanges: %v", err)
		}

		lastConversation := traffic[len(traffic)-1]
		var toolResults []testharness.ChatCompletionMessage
		for _, msg := range lastConversation.Request.Messages {
			if msg.Role == "tool" {
				toolResults = append(toolResults, msg)
			}
		}

		if len(toolResults) != 1 {
			t.Fatalf("Expected 1 tool result, got %d", len(toolResults))
		}
		if strings.Contains(toolResults[0].Content, "toolTelemetry") {
			t.Error("Tool result content should not contain 'toolTelemetry'")
		}
		if strings.Contains(toolResults[0].Content, "resultType") {
			t.Error("Tool result content should not contain 'resultType'")
		}

		if err := session.Disconnect(); err != nil {
			t.Errorf("Failed to disconnect session: %v", err)
		}
	})
}
