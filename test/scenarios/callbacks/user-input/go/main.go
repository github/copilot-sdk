package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"sync"

	copilot "github.com/github/copilot-sdk/go"
)

var (
	inputLog   []string
	inputLogMu sync.Mutex
)

func main() {
	client := copilot.NewClient(&copilot.ClientOptions{
		GitHubToken: os.Getenv("GITHUB_TOKEN"),
	})

	ctx := context.Background()
	if err := client.Start(ctx); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model: "claude-haiku-4.5",
		OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
			return copilot.PermissionRequestResult{Kind: "approved"}, nil
		},
		OnUserInputRequest: func(req copilot.UserInputRequest, inv copilot.UserInputInvocation) (copilot.UserInputResponse, error) {
			inputLogMu.Lock()
			inputLog = append(inputLog, fmt.Sprintf("question: %s", req.Question))
			inputLogMu.Unlock()
			return copilot.UserInputResponse{Answer: "Paris", WasFreeform: true}, nil
		},
		Hooks: &copilot.SessionHooks{
			OnPreToolUse: func(input copilot.PreToolUseHookInput, inv copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
				return &copilot.PreToolUseHookOutput{PermissionDecision: "allow"}, nil
			},
		},
	})
	if err != nil {
		log.Fatal(err)
	}
	defer session.Destroy()

	response, err := session.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "I want to learn about a city. Use the ask_user tool to ask me " +
			"which city I'm interested in. Then tell me about that city.",
	})
	if err != nil {
		log.Fatal(err)
	}

	if response != nil && response.Data.Content != nil {
		fmt.Println(*response.Data.Content)
	}

	fmt.Println("\n--- User input log ---")
	for _, entry := range inputLog {
		fmt.Printf("  %s\n", entry)
	}
	fmt.Printf("\nTotal user input requests: %d\n", len(inputLog))
}
