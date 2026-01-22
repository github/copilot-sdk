package e2e

import (
	"errors"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/e2e/testharness"
)

func TestTools(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("invokes built-in tools", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Write a test file
		err := os.WriteFile(filepath.Join(ctx.WorkDir, "README.md"), []byte("# ELIZA, the only chatbot you'll ever need"), 0644)
		if err != nil {
			t.Fatalf("Failed to write test file: %v", err)
		}

		session, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(copilot.MessageOptions{Prompt: "What's the first line of README.md in this directory?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		answer, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		if answer.Data.Content == nil || !strings.Contains(*answer.Data.Content, "ELIZA") {
			t.Errorf("Expected answer to contain 'ELIZA', got %v", answer.Data.Content)
		}
	})

	t.Run("invokes custom tool", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type EncryptParams struct {
			Input string `json:"input" jsonschema:"String to encrypt"`
		}

		session, err := client.CreateSession(&copilot.SessionConfig{
			Tools: []copilot.Tool{
				copilot.DefineTool("encrypt_string", "Encrypts a string",
					func(params EncryptParams, inv copilot.ToolInvocation) (string, error) {
						return strings.ToUpper(params.Input), nil
					}),
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(copilot.MessageOptions{Prompt: "Use encrypt_string to encrypt this string: Hello"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		answer, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		if answer.Data.Content == nil || !strings.Contains(*answer.Data.Content, "HELLO") {
			t.Errorf("Expected answer to contain 'HELLO', got %v", answer.Data.Content)
		}
	})

	t.Run("handles tool calling errors", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type EmptyParams struct{}

		session, err := client.CreateSession(&copilot.SessionConfig{
			Tools: []copilot.Tool{
				copilot.DefineTool("get_user_location", "Gets the user's location",
					func(params EmptyParams, inv copilot.ToolInvocation) (any, error) {
						return nil, errors.New("Melbourne")
					}),
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(copilot.MessageOptions{
			Prompt: "What is my location? If you can't find out, just say 'unknown'.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		answer, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		// Check the underlying traffic
		traffic, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("Failed to get exchanges: %v", err)
		}

		lastConversation := traffic[len(traffic)-1]

		// Find tool calls
		var toolCalls []testharness.ToolCall
		for _, msg := range lastConversation.Request.Messages {
			if msg.Role == "assistant" && msg.ToolCalls != nil {
				toolCalls = append(toolCalls, msg.ToolCalls...)
			}
		}

		if len(toolCalls) != 1 {
			t.Fatalf("Expected 1 tool call, got %d", len(toolCalls))
		}
		toolCall := toolCalls[0]
		if toolCall.Type != "function" {
			t.Errorf("Expected tool call type 'function', got '%s'", toolCall.Type)
		}
		if toolCall.Function.Name != "get_user_location" {
			t.Errorf("Expected tool call name 'get_user_location', got '%s'", toolCall.Function.Name)
		}

		// Find tool results
		var toolResults []testharness.Message
		for _, msg := range lastConversation.Request.Messages {
			if msg.Role == "tool" {
				toolResults = append(toolResults, msg)
			}
		}

		if len(toolResults) != 1 {
			t.Fatalf("Expected 1 tool result, got %d", len(toolResults))
		}
		toolResult := toolResults[0]
		if toolResult.ToolCallID != toolCall.ID {
			t.Errorf("Expected tool result ID '%s', got '%s'", toolCall.ID, toolResult.ToolCallID)
		}

		// The error message "Melbourne" should NOT be exposed to the LLM
		if strings.Contains(toolResult.Content, "Melbourne") {
			t.Errorf("Tool result should not contain error details 'Melbourne', got '%s'", toolResult.Content)
		}

		// The assistant should not see the exception information
		if answer.Data.Content != nil && strings.Contains(*answer.Data.Content, "Melbourne") {
			t.Errorf("Assistant should not see error details 'Melbourne', got '%s'", *answer.Data.Content)
		}
		if answer.Data.Content == nil || !strings.Contains(strings.ToLower(*answer.Data.Content), "unknown") {
			t.Errorf("Expected answer to contain 'unknown', got %v", answer.Data.Content)
		}
	})

	t.Run("can receive and return complex types", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type DbQuery struct {
			Table         string `json:"table"`
			IDs           []int  `json:"ids"`
			SortAscending bool   `json:"sortAscending"`
		}

		type DbQueryParams struct {
			Query DbQuery `json:"query"`
		}

		type City struct {
			CountryID  int    `json:"countryId"`
			CityName   string `json:"cityName"`
			Population int    `json:"population"`
		}

		var receivedInvocation *copilot.ToolInvocation

		session, err := client.CreateSession(&copilot.SessionConfig{
			Tools: []copilot.Tool{
				copilot.DefineTool("db_query", "Performs a database query",
					func(params DbQueryParams, inv copilot.ToolInvocation) ([]City, error) {
						receivedInvocation = &inv

						if params.Query.Table != "cities" {
							t.Errorf("Expected table 'cities', got '%s'", params.Query.Table)
						}
						if len(params.Query.IDs) != 2 || params.Query.IDs[0] != 12 || params.Query.IDs[1] != 19 {
							t.Errorf("Expected IDs [12, 19], got %v", params.Query.IDs)
						}
						if !params.Query.SortAscending {
							t.Errorf("Expected sortAscending to be true")
						}

						return []City{
							{CountryID: 19, CityName: "Passos", Population: 135460},
							{CountryID: 12, CityName: "San Lorenzo", Population: 204356},
						}, nil
					}),
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(copilot.MessageOptions{
			Prompt: "Perform a DB query for the 'cities' table using IDs 12 and 19, sorting ascending. " +
				"Reply only with lines of the form: [cityname] [population]",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		answer, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		if answer == nil || answer.Data.Content == nil {
			t.Fatalf("Expected assistant message with content")
		}

		responseContent := *answer.Data.Content
		if responseContent == "" {
			t.Errorf("Expected non-empty response")
		}
		if !strings.Contains(responseContent, "Passos") {
			t.Errorf("Expected response to contain 'Passos', got '%s'", responseContent)
		}
		if !strings.Contains(responseContent, "San Lorenzo") {
			t.Errorf("Expected response to contain 'San Lorenzo', got '%s'", responseContent)
		}
		// Remove commas for number checking (e.g., "135,460" -> "135460")
		responseWithoutCommas := strings.ReplaceAll(responseContent, ",", "")
		if !strings.Contains(responseWithoutCommas, "135460") {
			t.Errorf("Expected response to contain '135460', got '%s'", responseContent)
		}
		if !strings.Contains(responseWithoutCommas, "204356") {
			t.Errorf("Expected response to contain '204356', got '%s'", responseContent)
		}

		// We can access the raw invocation if needed
		if receivedInvocation == nil {
			t.Fatalf("Expected to receive invocation")
		}
		if receivedInvocation.SessionID != session.SessionID {
			t.Errorf("Expected session ID '%s', got '%s'", session.SessionID, receivedInvocation.SessionID)
		}
	})
}
