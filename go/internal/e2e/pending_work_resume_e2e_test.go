package e2e

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

const pendingWorkTimeout = 60 * time.Second

// Mirrors dotnet/test/PendingWorkResumeTests.cs (snapshot category "pending_work_resume").
//
// Each subtest spawns a TCP server client, connects a "suspended" client through CLIUrl,
// triggers some pending work (permission request or external tool call), then ForceStops
// the suspended client (preserving session state) and resumes from a fresh client with
// ContinuePendingWork=true.
func TestPendingWorkResumeE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)

	t.Run("should continue pending permission request after resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		_, cliURL := startTcpServer(t, ctx)

		type ValueParams struct {
			Value string `json:"value" jsonschema:"Value to transform"`
		}
		// Original tool: should NOT actually run because we ForceStop before approving.
		originalTool := copilot.DefineTool("resume_permission_tool", "Transforms a value after permission is granted",
			func(params ValueParams, inv copilot.ToolInvocation) (string, error) {
				return "ORIGINAL_SHOULD_NOT_RUN_" + params.Value, nil
			})

		permissionRequested := make(chan copilot.PermissionRequest, 1)
		releasePermission := make(chan copilot.PermissionRequestResult, 1)

		suspendedClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.CLIUrl = cliURL
			opts.CLIPath = ""
			opts.TCPConnectionToken = sharedTcpToken
		})
		session1, err := suspendedClient.CreateSession(t.Context(), &copilot.SessionConfig{
			Tools: []copilot.Tool{originalTool},
			OnPermissionRequest: func(req copilot.PermissionRequest, _ copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				select {
				case permissionRequested <- req:
				default:
				}
				return <-releasePermission, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		// Subscribe to the permission.requested event before sending the prompt.
		permissionEventCh := make(chan *copilot.SessionEvent, 1)
		unsub := session1.On(func(evt copilot.SessionEvent) {
			if evt.Type == copilot.SessionEventTypePermissionRequested {
				select {
				case permissionEventCh <- &evt:
				default:
				}
			}
		})
		defer unsub()

		if _, err := session1.Send(t.Context(), copilot.MessageOptions{
			Prompt: "Use resume_permission_tool with value 'alpha', then reply with the result.",
		}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		select {
		case <-permissionRequested:
		case <-time.After(pendingWorkTimeout):
			t.Fatal("Timed out waiting for original permission handler invocation")
		}
		var permissionEvent *copilot.SessionEvent
		select {
		case permissionEvent = <-permissionEventCh:
		case <-time.After(pendingWorkTimeout):
			t.Fatal("Timed out waiting for permission.requested event")
		}
		permData, ok := permissionEvent.Data.(*copilot.PermissionRequestedData)
		if !ok {
			t.Fatalf("Expected PermissionRequestedData, got %T", permissionEvent.Data)
		}

		// Snap the suspended client offline before the original handler resolves.
		suspendedClient.ForceStop()

		var resumedToolInvoked bool
		var mu sync.Mutex
		resumedTool := copilot.DefineTool("resume_permission_tool", "Transforms a value after permission is granted",
			func(params ValueParams, inv copilot.ToolInvocation) (string, error) {
				mu.Lock()
				resumedToolInvoked = true
				mu.Unlock()
				return "PERMISSION_RESUMED_" + strings.ToUpper(params.Value), nil
			})

		resumedClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.CLIUrl = cliURL
			opts.CLIPath = ""
			opts.TCPConnectionToken = sharedTcpToken
		})
		t.Cleanup(func() { resumedClient.ForceStop() })

		session2, err := resumedClient.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			ContinuePendingWork: true,
			OnPermissionRequest: func(_ copilot.PermissionRequest, _ copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				return copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindNoResult}, nil
			},
			Tools: []copilot.Tool{resumedTool},
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		permResult, err := session2.RPC.Permissions.HandlePendingPermissionRequest(t.Context(), &rpc.PermissionDecisionRequest{
			RequestID: permData.RequestID,
			Result: rpc.PermissionDecision{
				Kind: rpc.PermissionDecisionKindApproveOnce,
			},
		})
		if err != nil {
			t.Fatalf("Failed to handle pending permission request: %v", err)
		}
		if !permResult.Success {
			t.Fatalf("Expected HandlePendingPermissionRequest to succeed, got %+v", permResult)
		}

		ctxFinal, cancel := context.WithTimeout(t.Context(), pendingWorkTimeout)
		defer cancel()
		answer, err := testharness.GetFinalAssistantMessage(ctxFinal, session2)
		if err != nil {
			t.Fatalf("Failed to wait for final assistant message: %v", err)
		}

		mu.Lock()
		invoked := resumedToolInvoked
		mu.Unlock()
		if !invoked {
			t.Error("Expected resumed tool implementation to be invoked")
		}

		if assistant, ok := answer.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(assistant.Content, "PERMISSION_RESUMED_ALPHA") {
			t.Errorf("Expected response to contain 'PERMISSION_RESUMED_ALPHA', got %v", answer.Data)
		}

		// Allow original handler to unblock so cleanup proceeds.
		select {
		case releasePermission <- copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindUserNotAvailable}:
		default:
		}

		session2.Disconnect()
	})

	t.Run("should continue pending external tool request after resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		_, cliURL := startTcpServer(t, ctx)

		type ValueParams struct {
			Value string `json:"value" jsonschema:"Value to look up"`
		}
		toolStarted := make(chan string, 1)
		releaseTool := make(chan string, 1)

		// Original tool blocks until we release it; we ForceStop before that happens.
		originalTool := copilot.DefineTool("resume_external_tool", "Looks up a value after resumption",
			func(params ValueParams, inv copilot.ToolInvocation) (string, error) {
				select {
				case toolStarted <- params.Value:
				default:
				}
				return <-releaseTool, nil
			})

		suspendedClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.CLIUrl = cliURL
			opts.CLIPath = ""
			opts.TCPConnectionToken = sharedTcpToken
		})
		session1, err := suspendedClient.CreateSession(t.Context(), &copilot.SessionConfig{
			Tools:               []copilot.Tool{originalTool},
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		toolEventCh := waitForExternalToolRequests(session1, []string{"resume_external_tool"})

		if _, err := session1.Send(t.Context(), copilot.MessageOptions{
			Prompt: "Use resume_external_tool with value 'beta', then reply with the result.",
		}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		toolEvents, err := waitForExternalToolResults(toolEventCh, pendingWorkTimeout)
		if err != nil {
			t.Fatalf("waiting for external tool requests: %v", err)
		}
		toolEvent := toolEvents["resume_external_tool"]
		select {
		case v := <-toolStarted:
			if v != "beta" {
				t.Errorf("Expected original tool started with 'beta', got %q", v)
			}
		case <-time.After(pendingWorkTimeout):
			t.Fatal("Timed out waiting for original tool to start")
		}

		suspendedClient.ForceStop()

		resumedClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.CLIUrl = cliURL
			opts.CLIPath = ""
			opts.TCPConnectionToken = sharedTcpToken
		})
		t.Cleanup(func() { resumedClient.ForceStop() })

		session2, err := resumedClient.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			ContinuePendingWork: true,
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		toolResult, err := session2.RPC.Tools.HandlePendingToolCall(t.Context(), &rpc.HandlePendingToolCallRequest{
			RequestID: toolEvent.RequestID,
			Result: &rpc.ExternalToolResult{
				String: copilot.String("EXTERNAL_RESUMED_BETA"),
			},
		})
		if err != nil {
			t.Fatalf("Failed to handle pending tool call: %v", err)
		}
		if !toolResult.Success {
			t.Errorf("Expected HandlePendingToolCall to succeed, got %+v", toolResult)
		}

		ctxFinal, cancel := context.WithTimeout(t.Context(), pendingWorkTimeout)
		defer cancel()
		answer, err := testharness.GetFinalAssistantMessage(ctxFinal, session2)
		if err != nil {
			t.Fatalf("Failed to wait for final assistant message: %v", err)
		}
		if assistant, ok := answer.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(assistant.Content, "EXTERNAL_RESUMED_BETA") {
			t.Errorf("Expected response to contain 'EXTERNAL_RESUMED_BETA', got %v", answer.Data)
		}

		select {
		case releaseTool <- "ORIGINAL_SHOULD_NOT_WIN":
		default:
		}

		session2.Disconnect()
	})

	t.Run("should continue parallel pending external tool requests after resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		_, cliURL := startTcpServer(t, ctx)

		type ValueParams struct {
			Value string `json:"value" jsonschema:"Value to look up"`
		}
		startedA := make(chan string, 1)
		startedB := make(chan string, 1)
		releaseA := make(chan string, 1)
		releaseB := make(chan string, 1)

		originalA := copilot.DefineTool("pending_lookup_a", "Looks up the first value after resumption",
			func(params ValueParams, inv copilot.ToolInvocation) (string, error) {
				select {
				case startedA <- params.Value:
				default:
				}
				return <-releaseA, nil
			})
		originalB := copilot.DefineTool("pending_lookup_b", "Looks up the second value after resumption",
			func(params ValueParams, inv copilot.ToolInvocation) (string, error) {
				select {
				case startedB <- params.Value:
				default:
				}
				return <-releaseB, nil
			})

		suspendedClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.CLIUrl = cliURL
			opts.CLIPath = ""
			opts.TCPConnectionToken = sharedTcpToken
		})
		session1, err := suspendedClient.CreateSession(t.Context(), &copilot.SessionConfig{
			Tools:               []copilot.Tool{originalA, originalB},
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		toolEventCh := waitForExternalToolRequests(session1, []string{"pending_lookup_a", "pending_lookup_b"})

		if _, err := session1.Send(t.Context(), copilot.MessageOptions{
			Prompt: "Call pending_lookup_a with value 'alpha' and pending_lookup_b with value 'beta', then reply with both results.",
		}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		toolEvents, err := waitForExternalToolResults(toolEventCh, pendingWorkTimeout)
		if err != nil {
			t.Fatalf("waiting for external tool requests: %v", err)
		}
		select {
		case v := <-startedA:
			if v != "alpha" {
				t.Errorf("Expected pending_lookup_a started with 'alpha', got %q", v)
			}
		case <-time.After(pendingWorkTimeout):
			t.Fatal("Timed out waiting for pending_lookup_a to start")
		}
		select {
		case v := <-startedB:
			if v != "beta" {
				t.Errorf("Expected pending_lookup_b started with 'beta', got %q", v)
			}
		case <-time.After(pendingWorkTimeout):
			t.Fatal("Timed out waiting for pending_lookup_b to start")
		}

		suspendedClient.ForceStop()

		resumedClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.CLIUrl = cliURL
			opts.CLIPath = ""
			opts.TCPConnectionToken = sharedTcpToken
		})
		t.Cleanup(func() { resumedClient.ForceStop() })

		session2, err := resumedClient.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			ContinuePendingWork: true,
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		// Resolve B first to verify ordering doesn't matter.
		resB, err := session2.RPC.Tools.HandlePendingToolCall(t.Context(), &rpc.HandlePendingToolCallRequest{
			RequestID: toolEvents["pending_lookup_b"].RequestID,
			Result:    &rpc.ExternalToolResult{String: copilot.String("PARALLEL_B_BETA")},
		})
		if err != nil || !resB.Success {
			t.Fatalf("HandlePendingToolCall(B) failed: err=%v result=%+v", err, resB)
		}
		resA, err := session2.RPC.Tools.HandlePendingToolCall(t.Context(), &rpc.HandlePendingToolCallRequest{
			RequestID: toolEvents["pending_lookup_a"].RequestID,
			Result:    &rpc.ExternalToolResult{String: copilot.String("PARALLEL_A_ALPHA")},
		})
		if err != nil || !resA.Success {
			t.Fatalf("HandlePendingToolCall(A) failed: err=%v result=%+v", err, resA)
		}

		ctxFinal, cancel := context.WithTimeout(t.Context(), pendingWorkTimeout)
		defer cancel()
		answer, err := testharness.GetFinalAssistantMessage(ctxFinal, session2)
		if err != nil {
			t.Fatalf("Failed to wait for final assistant message: %v", err)
		}
		assistant, ok := answer.Data.(*copilot.AssistantMessageData)
		if !ok {
			t.Fatalf("Expected AssistantMessageData, got %T", answer.Data)
		}
		if !strings.Contains(assistant.Content, "PARALLEL_A_ALPHA") {
			t.Errorf("Expected response to contain 'PARALLEL_A_ALPHA', got %q", assistant.Content)
		}
		if !strings.Contains(assistant.Content, "PARALLEL_B_BETA") {
			t.Errorf("Expected response to contain 'PARALLEL_B_BETA', got %q", assistant.Content)
		}

		select {
		case releaseA <- "ORIGINAL_A_SHOULD_NOT_WIN":
		default:
		}
		select {
		case releaseB <- "ORIGINAL_B_SHOULD_NOT_WIN":
		default:
		}

		session2.Disconnect()
	})

	t.Run("should resume successfully when no pending work exists", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		_, cliURL := startTcpServer(t, ctx)

		var sessionID string
		func() {
			firstClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
				opts.CLIUrl = cliURL
				opts.CLIPath = ""
				opts.TCPConnectionToken = sharedTcpToken
			})
			defer firstClient.ForceStop()

			firstSession, err := firstClient.CreateSession(t.Context(), &copilot.SessionConfig{
				OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			})
			if err != nil {
				t.Fatalf("Failed to create first session: %v", err)
			}
			sessionID = firstSession.SessionID

			answer, err := firstSession.SendAndWait(t.Context(), copilot.MessageOptions{
				Prompt: "Reply with exactly: NO_PENDING_TURN_ONE",
			})
			if err != nil {
				t.Fatalf("Failed to send first turn: %v", err)
			}
			if assistant, ok := answer.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(assistant.Content, "NO_PENDING_TURN_ONE") {
				t.Errorf("Expected first answer to contain 'NO_PENDING_TURN_ONE', got %v", answer.Data)
			}

			firstSession.Disconnect()
		}()

		resumedClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.CLIUrl = cliURL
			opts.CLIPath = ""
			opts.TCPConnectionToken = sharedTcpToken
		})
		t.Cleanup(func() { resumedClient.ForceStop() })

		resumedSession, err := resumedClient.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			ContinuePendingWork: true,
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		followUp, err := resumedSession.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Reply with exactly: NO_PENDING_TURN_TWO",
		})
		if err != nil {
			t.Fatalf("Failed to send follow-up turn: %v", err)
		}
		if assistant, ok := followUp.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(assistant.Content, "NO_PENDING_TURN_TWO") {
			t.Errorf("Expected follow-up answer to contain 'NO_PENDING_TURN_TWO', got %v", followUp.Data)
		}

		resumedSession.Disconnect()
	})
}

// serverCliURL extracts the local CLI URL from a TCP-mode server client.
// The server must already be started; this function panics with a fatal
// test failure if the port is not yet available.
func serverCliURL(t *testing.T, server *copilot.Client) string {
	t.Helper()
	port := server.ActualPort()
	if port == 0 {
		t.Fatal("Expected non-zero ActualPort from TCP server client; ensure the server is started before calling serverCliURL")
	}
	return fmt.Sprintf("localhost:%d", port)
}

// sharedTcpToken is the connection token used by startTcpServer and any sibling
// client that connects via the resulting CLI URL. Tests use a fixed token rather
// than the auto-generated one because the second client is constructed without
// access to the first client's internal state.
const sharedTcpToken = "tcp-shared-test-token"

// startTcpServer starts a TCP-mode server client and returns its CLI URL.
// It triggers an initial connection so ActualPort is populated.
func startTcpServer(t *testing.T, ctx *testharness.TestContext) (*copilot.Client, string) {
	t.Helper()
	server := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.UseStdio = copilot.Bool(false)
		opts.TCPConnectionToken = sharedTcpToken
	})
	t.Cleanup(func() { server.ForceStop() })
	// Trigger connection so we can read the port. CreateSession+Disconnect is the
	// established pattern (see multi_client_test.go).
	initSession, err := server.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("Failed to start TCP server client: %v", err)
	}
	initSession.Disconnect()
	return server, serverCliURL(t, server)
}

type collectedExternalRequests struct {
	mu   sync.Mutex
	seen map[string]*copilot.ExternalToolRequestedData
	want map[string]struct{}
	done chan struct{}
}

// waitForExternalToolRequests subscribes to a session and returns a struct that
// blocks until all requested tool names have been observed via external_tool.requested.
func waitForExternalToolRequests(session *copilot.Session, names []string) *collectedExternalRequests {
	c := &collectedExternalRequests{
		seen: make(map[string]*copilot.ExternalToolRequestedData),
		want: make(map[string]struct{}, len(names)),
		done: make(chan struct{}),
	}
	for _, n := range names {
		c.want[n] = struct{}{}
	}
	session.On(func(evt copilot.SessionEvent) {
		if evt.Type != copilot.SessionEventTypeExternalToolRequested {
			return
		}
		d, ok := evt.Data.(*copilot.ExternalToolRequestedData)
		if !ok {
			return
		}
		c.mu.Lock()
		defer c.mu.Unlock()
		if _, want := c.want[d.ToolName]; !want {
			return
		}
		if _, dup := c.seen[d.ToolName]; dup {
			return
		}
		c.seen[d.ToolName] = d
		if len(c.seen) == len(c.want) {
			select {
			case <-c.done:
			default:
				close(c.done)
			}
		}
	})
	return c
}

func waitForExternalToolResults(c *collectedExternalRequests, timeout time.Duration) (map[string]*copilot.ExternalToolRequestedData, error) {
	select {
	case <-c.done:
	case <-time.After(timeout):
		c.mu.Lock()
		got := make([]string, 0, len(c.seen))
		for name := range c.seen {
			got = append(got, name)
		}
		c.mu.Unlock()
		return nil, errors.New("timed out waiting for external tool requests; got: " + strings.Join(got, ", "))
	}
	c.mu.Lock()
	defer c.mu.Unlock()
	out := make(map[string]*copilot.ExternalToolRequestedData, len(c.seen))
	for k, v := range c.seen {
		out[k] = v
	}
	return out, nil
}
