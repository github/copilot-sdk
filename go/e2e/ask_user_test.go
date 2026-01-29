package e2e

import (
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/e2e/testharness"
)

func TestAskUser(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should invoke user input handler when model uses ask_user tool", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var userInputRequests []copilot.UserInputRequest
		var mu sync.Mutex

		session, err := client.CreateSession(&copilot.SessionConfig{
			OnUserInputRequest: func(request copilot.UserInputRequest, invocation copilot.UserInputInvocation) (copilot.UserInputResponse, error) {
				mu.Lock()
				userInputRequests = append(userInputRequests, request)
				mu.Unlock()

				if invocation.SessionID == "" {
					t.Error("Expected non-empty session ID in invocation")
				}

				// Return the first choice if available, otherwise a freeform answer
				answer := "freeform answer"
				wasFreeform := true
				if len(request.Choices) > 0 {
					answer = request.Choices[0]
					wasFreeform = false
				}

				return copilot.UserInputResponse{
					Answer:      answer,
					WasFreeform: wasFreeform,
				}, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.SendAndWait(copilot.MessageOptions{
			Prompt: "Ask me to choose between 'Option A' and 'Option B' using the ask_user tool. Wait for my response before continuing.",
		}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()

		if len(userInputRequests) == 0 {
			t.Error("Expected at least one user input request")
		}

		hasQuestion := false
		for _, req := range userInputRequests {
			if req.Question != "" {
				hasQuestion = true
				break
			}
		}
		if !hasQuestion {
			t.Error("Expected at least one request with a question")
		}
	})

	t.Run("should receive choices in user input request", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var userInputRequests []copilot.UserInputRequest
		var mu sync.Mutex

		session, err := client.CreateSession(&copilot.SessionConfig{
			OnUserInputRequest: func(request copilot.UserInputRequest, invocation copilot.UserInputInvocation) (copilot.UserInputResponse, error) {
				mu.Lock()
				userInputRequests = append(userInputRequests, request)
				mu.Unlock()

				// Pick the first choice
				answer := "default"
				if len(request.Choices) > 0 {
					answer = request.Choices[0]
				}

				return copilot.UserInputResponse{
					Answer:      answer,
					WasFreeform: false,
				}, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.SendAndWait(copilot.MessageOptions{
			Prompt: "Use the ask_user tool to ask me to pick between exactly two options: 'Red' and 'Blue'. These should be provided as choices. Wait for my answer.",
		}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()

		if len(userInputRequests) == 0 {
			t.Error("Expected at least one user input request")
		}

		hasChoices := false
		for _, req := range userInputRequests {
			if len(req.Choices) > 0 {
				hasChoices = true
				break
			}
		}
		if !hasChoices {
			t.Error("Expected at least one request with choices")
		}
	})

	t.Run("should handle freeform user input response", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var userInputRequests []copilot.UserInputRequest
		var mu sync.Mutex
		freeformAnswer := "This is my custom freeform answer that was not in the choices"

		session, err := client.CreateSession(&copilot.SessionConfig{
			OnUserInputRequest: func(request copilot.UserInputRequest, invocation copilot.UserInputInvocation) (copilot.UserInputResponse, error) {
				mu.Lock()
				userInputRequests = append(userInputRequests, request)
				mu.Unlock()

				// Return a freeform answer (not from choices)
				return copilot.UserInputResponse{
					Answer:      freeformAnswer,
					WasFreeform: true,
				}, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		response, err := session.SendAndWait(copilot.MessageOptions{
			Prompt: "Ask me a question using ask_user and then include my answer in your response. The question should be 'What is your favorite color?'",
		}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()

		if len(userInputRequests) == 0 {
			t.Error("Expected at least one user input request")
		}

		// The model's response should be defined
		if response == nil {
			t.Error("Expected non-nil response")
		}
	})
}
