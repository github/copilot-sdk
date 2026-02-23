package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"sync"

	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	var (
		permissionLog   []string
		permissionLogMu sync.Mutex
	)

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
			permissionLogMu.Lock()
			toolName, _ := req.Extra["toolName"].(string)
			permissionLog = append(permissionLog, fmt.Sprintf("approved:%s", toolName))
			permissionLogMu.Unlock()
			return copilot.PermissionRequestResult{Kind: "approved"}, nil
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
		Prompt: "List the files in the current directory using glob with pattern '*.md'.",
	})
	if err != nil {
		log.Fatal(err)
	}

	if response != nil && response.Data.Content != nil {
		fmt.Println(*response.Data.Content)
	}

	fmt.Println("\n--- Permission request log ---")
	for _, entry := range permissionLog {
		fmt.Printf("  %s\n", entry)
	}
	fmt.Printf("\nTotal permission requests: %d\n", len(permissionLog))
}
