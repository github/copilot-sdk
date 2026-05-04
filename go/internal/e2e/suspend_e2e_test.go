package e2e

import (
	"context"
	"strings"
	"sync/atomic"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

const suspendTimeout = 60 * time.Second

func TestSuspendE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)

	t.Run("should suspend idle session without throwing", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Reply with: SUSPEND_IDLE_OK",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if content := assistantContent(t, msg); !strings.Contains(content, "SUSPEND_IDLE_OK") {
			t.Fatalf("Expected response to contain SUSPEND_IDLE_OK, got %q", content)
		}

		if err := suspendSession(t.Context(), session); err != nil {
			t.Fatalf("Suspend failed: %v", err)
		}
	})

	t.Run("should allow resume and continue conversation after suspend", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		_, cliURL := startTcpServer(t, ctx)

		client1 := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.CLIUrl = cliURL
			opts.CLIPath = ""
			opts.TCPConnectionToken = sharedTcpToken
		})
		t.Cleanup(func() { client1.ForceStop() })

		session1, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		if _, err := session1.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Remember the magic word: SUSPENSE. Reply with: SUSPEND_TURN_ONE",
		}); err != nil {
			t.Fatalf("First SendAndWait failed: %v", err)
		}

		if err := suspendSession(t.Context(), session1); err != nil {
			t.Fatalf("Suspend failed: %v", err)
		}
		client1.ForceStop()

		client2 := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.CLIUrl = cliURL
			opts.CLIPath = ""
			opts.TCPConnectionToken = sharedTcpToken
		})
		t.Cleanup(func() { client2.ForceStop() })

		session2, err := client2.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}
		t.Cleanup(func() { _ = session2.Disconnect() })

		followUp, err := session2.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "What was the magic word I asked you to remember? Reply with just the word.",
		})
		if err != nil {
			t.Fatalf("Follow-up SendAndWait failed: %v", err)
		}
		if content := strings.ToUpper(assistantContent(t, followUp)); !strings.Contains(content, "SUSPENSE") {
			t.Fatalf("Expected response to contain SUSPENSE, got %q", content)
		}
	})

	t.Run("should cancel pending permission request when suspending", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		type ValueParams struct {
			Value string `json:"value" jsonschema:"Value to transform"`
		}

		permissionRequested := make(chan copilot.PermissionRequest, 1)
		releasePermission := make(chan copilot.PermissionRequestResult, 1)
		var toolInvoked atomic.Bool

		tool := copilot.DefineTool("suspend_cancel_permission_tool", "Transforms a value (should not run when suspend cancels permission)",
			func(params ValueParams, inv copilot.ToolInvocation) (string, error) {
				toolInvoked.Store(true)
				return "SHOULD_NOT_RUN_" + params.Value, nil
			})

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Tools: []copilot.Tool{tool},
			OnPermissionRequest: func(request copilot.PermissionRequest, _ copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				select {
				case permissionRequested <- request:
				default:
				}
				return <-releasePermission, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		defer func() {
			select {
			case releasePermission <- copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindUserNotAvailable}:
			default:
			}
		}()

		if _, err := session.Send(t.Context(), copilot.MessageOptions{
			Prompt: "Use suspend_cancel_permission_tool with value 'omega', then reply with the result.",
		}); err != nil {
			t.Fatalf("Send failed: %v", err)
		}

		var request copilot.PermissionRequest
		select {
		case request = <-permissionRequested:
		case <-time.After(suspendTimeout):
			t.Fatal("Timed out waiting for permission request")
		}
		if request.Kind != copilot.PermissionRequestKindCustomTool {
			t.Fatalf("Expected custom-tool permission request, got %q", request.Kind)
		}
		if request.ToolName == nil || *request.ToolName != "suspend_cancel_permission_tool" {
			t.Fatalf("Expected permission request for suspend_cancel_permission_tool, got %#v", request.ToolName)
		}

		if err := suspendSession(t.Context(), session); err != nil {
			t.Fatalf("Suspend failed: %v", err)
		}

		if toolInvoked.Load() {
			t.Fatal("Tool should not have been invoked after suspend cancelled its pending permission")
		}
	})

	t.Run("should reject pending external tool when suspending", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		type ValueParams struct {
			Value string `json:"value" jsonschema:"Value to look up"`
		}

		toolStarted := make(chan string, 1)
		releaseTool := make(chan string, 1)

		tool := copilot.DefineTool("suspend_reject_external_tool", "Looks up a value externally",
			func(params ValueParams, inv copilot.ToolInvocation) (string, error) {
				select {
				case toolStarted <- params.Value:
				default:
				}
				return <-releaseTool, nil
			})

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Tools:               []copilot.Tool{tool},
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		defer func() {
			select {
			case releaseTool <- "RELEASED_AFTER_SUSPEND":
			default:
			}
		}()

		toolEventCh := waitForExternalToolRequests(session, []string{"suspend_reject_external_tool"})

		if _, err := session.Send(t.Context(), copilot.MessageOptions{
			Prompt: "Use suspend_reject_external_tool with value 'sigma', then reply with the result.",
		}); err != nil {
			t.Fatalf("Send failed: %v", err)
		}

		toolEvents, err := waitForExternalToolResults(toolEventCh, suspendTimeout)
		if err != nil {
			t.Fatalf("waiting for external tool request: %v", err)
		}
		requestID := toolEvents["suspend_reject_external_tool"].RequestID
		if requestID == "" {
			t.Fatal("Expected external tool request id to be populated")
		}

		select {
		case value := <-toolStarted:
			if value != "sigma" {
				t.Fatalf("Expected tool to start with value sigma, got %q", value)
			}
		case <-time.After(suspendTimeout):
			t.Fatal("Timed out waiting for tool to start")
		}

		if err := suspendSession(t.Context(), session); err != nil {
			t.Fatalf("Suspend failed: %v", err)
		}
	})
}

func suspendSession(ctx context.Context, session *copilot.Session) error {
	ctx, cancel := context.WithTimeout(ctx, suspendTimeout)
	defer cancel()
	_, err := session.RPC.Suspend(ctx)
	return err
}
