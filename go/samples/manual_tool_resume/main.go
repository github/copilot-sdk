package main

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/rpc"
)

const timeout = 2 * time.Minute

func manualTool() copilot.Tool {
	return copilot.Tool{
		Name:        "manual_resume_status",
		Description: "Looks up a status value. The SDK consumer supplies the result manually.",
		Parameters: map[string]any{
			"type": "object",
			"properties": map[string]any{
				"id": map[string]any{
					"type":        "string",
					"description": "Identifier to look up",
				},
			},
			"required": []string{"id"},
		},
		// No Handler: the SDK exposes the declaration and leaves execution pending.
	}
}

func newClient() *copilot.Client {
	cliPath := filepath.Join("..", "..", "nodejs", "node_modules", "@github", "copilot", "index.js")
	return copilot.NewClient(&copilot.ClientOptions{CLIPath: cliPath})
}

func watchPermission(session *copilot.Session) (<-chan *copilot.PermissionRequestedData, func()) {
	ch := make(chan *copilot.PermissionRequestedData, 1)
	unsubscribe := session.On(func(event copilot.SessionEvent) {
		if data, ok := event.Data.(*copilot.PermissionRequestedData); ok {
			ch <- data
		}
	})
	return ch, unsubscribe
}

func receivePermission(ctx context.Context, ch <-chan *copilot.PermissionRequestedData) (*copilot.PermissionRequestedData, error) {
	select {
	case data := <-ch:
		return data, nil
	case <-ctx.Done():
		return nil, ctx.Err()
	}
}

func watchTool(session *copilot.Session) (<-chan *copilot.ExternalToolRequestedData, func()) {
	ch := make(chan *copilot.ExternalToolRequestedData, 1)
	unsubscribe := session.On(func(event copilot.SessionEvent) {
		if data, ok := event.Data.(*copilot.ExternalToolRequestedData); ok && data.ToolName == "manual_resume_status" {
			ch <- data
		}
	})
	return ch, unsubscribe
}

func receiveTool(ctx context.Context, ch <-chan *copilot.ExternalToolRequestedData) (*copilot.ExternalToolRequestedData, error) {
	select {
	case data := <-ch:
		return data, nil
	case <-ctx.Done():
		return nil, ctx.Err()
	}
}

func watchAssistant(session *copilot.Session) (<-chan string, func()) {
	ch := make(chan string, 1)
	unsubscribe := session.On(func(event copilot.SessionEvent) {
		if data, ok := event.Data.(*copilot.AssistantMessageData); ok {
			ch <- data.Content
		}
	})
	return ch, unsubscribe
}

func receiveAssistant(ctx context.Context, ch <-chan string) (string, error) {
	select {
	case content := <-ch:
		return content, nil
	case <-ctx.Done():
		return "", ctx.Err()
	}
}

func pause() {
	fmt.Println("Simulating time passing...")
	fmt.Println()
	time.Sleep(time.Second)
}

func main() {
	ctx := context.Background()
	tool := manualTool()

	// 1. Create a session with a declaration-only tool, then stop after the permission prompt.
	client1 := newClient()
	if err := client1.Start(ctx); err != nil {
		panic(err)
	}
	session1, err := client1.CreateSession(ctx, &copilot.SessionConfig{
		Tools: []copilot.Tool{tool},
	})
	if err != nil {
		panic(err)
	}
	sessionID := session1.SessionID

	waitCtx, cancel := context.WithTimeout(ctx, timeout)
	// Subscribe before sending so the permission event cannot be missed.
	permissionCh, unsubscribePermission := watchPermission(session1)
	if _, err := session1.Send(ctx, copilot.MessageOptions{
		Prompt: "Use the manual_resume_status tool with id 'alpha', then tell me the status.",
	}); err != nil {
		unsubscribePermission()
		cancel()
		panic(err)
	}
	permission, err := receivePermission(waitCtx, permissionCh)
	unsubscribePermission()
	if err != nil {
		cancel()
		panic(err)
	}
	cancel()
	client1.ForceStop()
	pause()

	// 2. Resume pending work and grant permission to invoke the tool.
	client2 := newClient()
	if err := client2.Start(ctx); err != nil {
		panic(err)
	}
	session2, err := client2.ResumeSession(ctx, sessionID, &copilot.ResumeSessionConfig{
		Tools:               []copilot.Tool{tool},
		ContinuePendingWork: true,
	})
	if err != nil {
		panic(err)
	}

	waitCtx, cancel = context.WithTimeout(ctx, timeout)
	// Subscribe before approving so the external tool request cannot be missed.
	toolCh, unsubscribeTool := watchTool(session2)
	if _, err := session2.RPC.Permissions.HandlePendingPermissionRequest(ctx, &rpc.PermissionDecisionRequest{
		RequestID: permission.RequestID,
		Result:    &rpc.PermissionDecisionApproveOnce{},
	}); err != nil {
		unsubscribeTool()
		cancel()
		panic(err)
	}
	toolRequest, err := receiveTool(waitCtx, toolCh)
	unsubscribeTool()
	if err != nil {
		cancel()
		panic(err)
	}
	cancel()
	client2.ForceStop()
	pause()

	// 3. Resume again and manually provide the pending tool result.
	client3 := newClient()
	if err := client3.Start(ctx); err != nil {
		panic(err)
	}
	session3, err := client3.ResumeSession(ctx, sessionID, &copilot.ResumeSessionConfig{
		Tools:               []copilot.Tool{tool},
		ContinuePendingWork: true,
	})
	if err != nil {
		panic(err)
	}
	defer client3.ForceStop()

	waitCtx, cancel = context.WithTimeout(ctx, timeout)
	defer cancel()
	answerCh, unsubscribeAssistant := watchAssistant(session3)
	defer unsubscribeAssistant()
	if _, err := session3.RPC.Tools.HandlePendingToolCall(ctx, &rpc.HandlePendingToolCallRequest{
		RequestID: toolRequest.RequestID,
		Result:    rpc.ExternalToolStringResult("MANUAL_STATUS_READY"),
	}); err != nil {
		panic(err)
	}

	answer, err := receiveAssistant(waitCtx, answerCh)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		return
	}
	fmt.Println(answer)
}
