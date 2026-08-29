package e2e

import (
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestAbortE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	// Verifies that Abort cleanly interrupts an active turn during streaming
	// without leaving dangling state or causing exceptions in the event delivery pipeline.
	t.Run("should abort during active streaming", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Streaming:           copilot.Bool(true),
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		var mu sync.Mutex
		var events []copilot.SessionEvent
		firstDelta := make(chan *copilot.AssistantMessageDeltaData, 1)
		sessionIdle := make(chan struct{}, 1)

		session.On(func(event copilot.SessionEvent) {
			mu.Lock()
			events = append(events, event)
			mu.Unlock()
			if d, ok := event.Data.(*copilot.AssistantMessageDeltaData); ok {
				select {
				case firstDelta <- d:
				default:
				}
			}
			if _, ok := event.Data.(*copilot.SessionIdleData); ok {
				select {
				case sessionIdle <- struct{}{}:
				default:
				}
			}
		})

		// Fire-and-forget — we'll abort before it finishes
		go func() {
			_, _ = session.Send(t.Context(), copilot.MessageOptions{
				Prompt: "Write a very long essay about the history of computing, covering every decade from the 1940s to the 2020s in great detail.",
			})
		}()

		// Wait for at least one delta to arrive (proves streaming started)
		var delta *copilot.AssistantMessageDeltaData
		select {
		case delta = <-firstDelta:
		case <-time.After(60 * time.Second):
			t.Fatal("Timed out waiting for first streaming delta")
		}
		if delta.DeltaContent == "" {
			t.Error("Expected first delta to have content")
		}

		// Now abort mid-stream
		if err := session.Abort(t.Context()); err != nil {
			t.Fatalf("Abort failed: %v", err)
		}
		select {
		case <-sessionIdle:
		case <-time.After(60 * time.Second):
			t.Fatal("Timed out waiting for session to become idle after abort")
		}

		mu.Lock()
		snapshot := make([]copilot.SessionEvent, len(events))
		copy(snapshot, events)
		mu.Unlock()

		// Key contract: at least one delta arrived before abort
		hasDelta := false
		for _, e := range snapshot {
			if _, ok := e.Data.(*copilot.AssistantMessageDeltaData); ok {
				hasDelta = true
				break
			}
		}
		if !hasDelta {
			t.Error("Expected at least one assistant.message_delta event before abort")
		}

		// Session should be usable after abort.
		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Say 'abort_recovery_ok'.",
		})
		if err != nil {
			t.Fatalf("Recovery SendAndWait failed after abort: %v", err)
		}
		if content := assistantContent(t, msg); !strings.Contains(strings.ToLower(content), "abort_recovery_ok") {
			t.Errorf("Expected recovery message to contain 'abort_recovery_ok', got %q", content)
		}
	})

	// Verifies that Abort cleanly interrupts an active turn during tool execution.
	t.Run("should abort during active tool execution", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type ValueParams struct {
			Value string `json:"value" jsonschema:"Value to analyze"`
		}
		toolStarted := make(chan string, 1)
		releaseTool := make(chan string, 1)

		slowTool := copilot.DefineTool("slow_analysis", "A slow analysis tool that blocks until released",
			func(params ValueParams, inv copilot.ToolInvocation) (string, error) {
				select {
				case toolStarted <- params.Value:
				default:
				}
				return <-releaseTool, nil
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

		sessionIdle := make(chan struct{}, 1)
		session.On(func(event copilot.SessionEvent) {
			if _, ok := event.Data.(*copilot.SessionIdleData); ok {
				select {
				case sessionIdle <- struct{}{}:
				default:
				}
			}
		})

		// Fire-and-forget
		go func() {
			_, _ = session.Send(t.Context(), copilot.MessageOptions{
				Prompt: "Use slow_analysis with value 'test_abort'. Wait for the result.",
			})
		}()

		// Wait for the tool to start executing
		var toolValue string
		select {
		case toolValue = <-toolStarted:
		case <-time.After(60 * time.Second):
			t.Fatal("Timed out waiting for slow_analysis tool to start")
		}
		if toolValue != "test_abort" {
			t.Errorf("Expected tool value 'test_abort', got %q", toolValue)
		}

		// Abort while the tool is running
		if err := session.Abort(t.Context()); err != nil {
			t.Fatalf("Abort failed: %v", err)
		}

		// Release the tool so its goroutine doesn't leak
		select {
		case releaseTool <- "RELEASED_AFTER_ABORT":
		default:
		}
		select {
		case <-sessionIdle:
		case <-time.After(60 * time.Second):
			t.Fatal("Timed out waiting for session to become idle after tool abort")
		}

		// Session should be usable after abort.
		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Say 'tool_abort_recovery_ok'.",
		})
		if err != nil {
			t.Fatalf("Recovery SendAndWait failed after tool abort: %v", err)
		}
		if content := assistantContent(t, msg); !strings.Contains(content, "tool_abort_recovery_ok") {
			t.Errorf("Expected recovery message to contain 'tool_abort_recovery_ok', got %q", content)
		}
	})
}
