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
		hookLog   []string
		hookLogMu sync.Mutex
	)

	appendLog := func(entry string) {
		hookLogMu.Lock()
		hookLog = append(hookLog, entry)
		hookLogMu.Unlock()
	}

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
		Hooks: &copilot.SessionHooks{
			OnSessionStart: func(input copilot.SessionStartHookInput, inv copilot.HookInvocation) (*copilot.SessionStartHookOutput, error) {
				appendLog("onSessionStart")
				return nil, nil
			},
			OnSessionEnd: func(input copilot.SessionEndHookInput, inv copilot.HookInvocation) (*copilot.SessionEndHookOutput, error) {
				appendLog("onSessionEnd")
				return nil, nil
			},
			OnPreToolUse: func(input copilot.PreToolUseHookInput, inv copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
				appendLog(fmt.Sprintf("onPreToolUse:%s", input.ToolName))
				return &copilot.PreToolUseHookOutput{PermissionDecision: "allow"}, nil
			},
			OnPostToolUse: func(input copilot.PostToolUseHookInput, inv copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
				appendLog(fmt.Sprintf("onPostToolUse:%s", input.ToolName))
				return nil, nil
			},
			OnUserPromptSubmitted: func(input copilot.UserPromptSubmittedHookInput, inv copilot.HookInvocation) (*copilot.UserPromptSubmittedHookOutput, error) {
				appendLog("onUserPromptSubmitted")
				return &copilot.UserPromptSubmittedHookOutput{ModifiedPrompt: input.Prompt}, nil
			},
			OnErrorOccurred: func(input copilot.ErrorOccurredHookInput, inv copilot.HookInvocation) (*copilot.ErrorOccurredHookOutput, error) {
				appendLog(fmt.Sprintf("onErrorOccurred:%s", input.Error))
				return nil, nil
			},
		},
	})
	if err != nil {
		log.Fatal(err)
	}
	defer session.Destroy()

	response, err := session.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "List the files in the current directory using the glob tool with pattern '*.md'.",
	})
	if err != nil {
		log.Fatal(err)
	}

	if response != nil && response.Data.Content != nil {
		fmt.Println(*response.Data.Content)
	}

	fmt.Println("\n--- Hook execution log ---")
	hookLogMu.Lock()
	for _, entry := range hookLog {
		fmt.Printf("  %s\n", entry)
	}
	fmt.Printf("\nTotal hooks fired: %d\n", len(hookLog))
	hookLogMu.Unlock()
}
