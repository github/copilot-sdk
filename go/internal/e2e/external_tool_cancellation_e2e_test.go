package e2e

import (
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestExternalToolCancellationE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should_cancel_tool_handler_when_session_disconnects", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type ValueParams struct {
			Value string `json:"value" jsonschema:"Value to analyze"`
		}
		toolStarted := make(chan struct{}, 1)
		toolCancelled := make(chan struct{}, 1)
		releaseTool := make(chan string, 1)

		slowTool := copilot.DefineTool("slow_analysis", "A slow analysis tool that blocks until released",
			func(_ ValueParams, inv copilot.ToolInvocation) (string, error) {
				select {
				case toolStarted <- struct{}{}:
				default:
				}
				select {
				case value := <-releaseTool:
					return value, nil
				case <-inv.TraceContext.Done():
					select {
					case toolCancelled <- struct{}{}:
					default:
					}
					return "", inv.TraceContext.Err()
				}
			})
		slowTool.SkipPermission = true

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools:               []copilot.Tool{slowTool},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		go func() {
			_, _ = session.Send(t.Context(), copilot.MessageOptions{
				Prompt: "Use slow_analysis with value 'test_abort'. Wait for the result.",
			})
		}()

		select {
		case <-toolStarted:
		case <-time.After(60 * time.Second):
			t.Fatal("Timed out waiting for tool handler to start")
		}

		if err := session.Disconnect(); err != nil {
			t.Fatalf("Disconnect failed: %v", err)
		}

		select {
		case <-toolCancelled:
		case <-time.After(60 * time.Second):
			t.Fatal("Timed out waiting for tool handler cancellation")
		}

	})
}
