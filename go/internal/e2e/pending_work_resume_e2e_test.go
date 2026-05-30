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
// Most subtests spawn a TCP server client, connect a "suspended" client through CLIUrl,
// trigger pending work, then ForceStop the suspended client (preserving session state)
// and resume from a fresh client with ContinuePendingWork=true. Warm-join coverage keeps
// the original client connected while a second client resumes the same session.
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
		releasePermission := make(chan rpc.PermissionDecision, 1)

		suspendedClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.Connection = copilot.UriConnection{URL: cliURL, ConnectionToken: sharedTcpToken}
		})
		session1, err := suspendedClient.CreateSession(t.Context(), &copilot.SessionConfig{
			Tools: []copilot.Tool{originalTool},
			OnPermissionRequest: func(req copilot.PermissionRequest, _ copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
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
			if evt.Type() == copilot.SessionEventTypePermissionRequested {
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
			opts.Connection = copilot.UriConnection{URL: cliURL, ConnectionToken: sharedTcpToken}
		})
		t.Cleanup(func() { resumedClient.ForceStop() })

		session2, err := resumedClient.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			ContinuePendingWork: true,
			OnPermissionRequest: func(_ copilot.PermissionRequest, _ copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
				return &rpc.PermissionDecisionNoResult{}, nil
			},
			Tools: []copilot.Tool{resumedTool},
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		permResult, err := session2.RPC.Permissions.HandlePendingPermissionRequest(t.Context(), &rpc.PermissionDecisionRequest{
			RequestID: permData.RequestID,
			Result:    &rpc.PermissionDecisionApproveOnce{},
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
		case releasePermission <- &rpc.PermissionDecisionUserNotAvailable{}:
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
			opts.Connection = copilot.UriConnection{URL: cliURL, ConnectionToken: sharedTcpToken}
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
			opts.Connection = copilot.UriConnection{URL: cliURL, ConnectionToken: sharedTcpToken}
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
			Result:    rpc.ExternalToolStringResult("EXTERNAL_RESUMED_BETA"),
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
			opts.Connection = copilot.UriConnection{URL: cliURL, ConnectionToken: sharedTcpToken}
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
			opts.Connection = copilot.UriConnection{URL: cliURL, ConnectionToken: sharedTcpToken}
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
			Result:    rpc.ExternalToolStringResult("PARALLEL_B_BETA"),
		})
		if err != nil || !resB.Success {
			t.Fatalf("HandlePendingToolCall(B) failed: err=%v result=%+v", err, resB)
		}
		resA, err := session2.RPC.Tools.HandlePendingToolCall(t.Context(), &rpc.HandlePendingToolCallRequest{
			RequestID: toolEvents["pending_lookup_a"].RequestID,
			Result:    rpc.ExternalToolStringResult("PARALLEL_A_ALPHA"),
		})
		if err != nil || !resA.Success {
			t.Fatalf("HandlePendingToolCall(A) failed: err=%v result=%+v", err, resA)
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
				opts.Connection = copilot.UriConnection{URL: cliURL, ConnectionToken: sharedTcpToken}
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
			opts.Connection = copilot.UriConnection{URL: cliURL, ConnectionToken: sharedTcpToken}
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

	for _, scenario := range []struct {
		name                     string
		disconnectOriginalClient bool
		expectedSessionWasActive bool
		expectedHandleResult     bool
	}{
		{name: "warm", disconnectOriginalClient: false, expectedSessionWasActive: true, expectedHandleResult: true},
		{name: "cold", disconnectOriginalClient: true, expectedSessionWasActive: false, expectedHandleResult: false},
	} {
		scenario := scenario
		t.Run(fmt.Sprintf("should keep pending external tool handleable on %s resume when continuependingwork is false", scenario.name), func(t *testing.T) {
			ctx.ConfigureForTest(t)

			_, cliURL := startTcpServer(t, ctx)

			type ValueParams struct {
				Value string `json:"value" jsonschema:"Value to look up"`
			}
			toolStarted := make(chan string, 1)
			releaseTool := make(chan string, 1)

			originalTool := copilot.DefineTool("resume_external_tool", "Looks up a value after resumption",
				func(params ValueParams, inv copilot.ToolInvocation) (string, error) {
					select {
					case toolStarted <- params.Value:
					default:
					}
					return <-releaseTool, nil
				})

			suspendedClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
				opts.Connection = copilot.UriConnection{URL: cliURL, ConnectionToken: sharedTcpToken}
			})
			if !scenario.disconnectOriginalClient {
				defer suspendedClient.ForceStop()
			}
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

			if scenario.disconnectOriginalClient {
				suspendedClient.ForceStop()
			}

			resumedClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
				opts.Connection = copilot.UriConnection{URL: cliURL, ConnectionToken: sharedTcpToken}
			})
			t.Cleanup(func() { resumedClient.ForceStop() })

			// In warm mode the original client still owns the tool registration;
			// re-registering it from the resumed client would cause a name-clash. In
			// cold mode the original is gone, so we register a fresh throwing handler
			// to assert the runtime doesn't re-invoke the tool on resume (orphan
			// auto-completion happens internally).
			resumeConfig := &copilot.ResumeSessionConfig{
				ContinuePendingWork: false,
				OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			}
			if scenario.disconnectOriginalClient {
				resumeConfig.Tools = []copilot.Tool{
					copilot.DefineTool("resume_external_tool", "Looks up a value after resumption",
						func(_ ValueParams, _ copilot.ToolInvocation) (string, error) {
							t.Errorf("Resumed-session handler should not be invoked")
							return "", fmt.Errorf("resumed-session handler should not be invoked")
						}),
				}
			}

			session2, err := resumedClient.ResumeSession(t.Context(), sessionID, resumeConfig)
			if err != nil {
				t.Fatalf("Failed to resume session: %v", err)
			}

			messages, err := session2.GetEvents(t.Context())
			if err != nil {
				t.Fatalf("GetEvents failed: %v", err)
			}
			var resumeEvent *copilot.SessionResumeData
			for _, msg := range messages {
				if msg.Type() == copilot.SessionEventTypeSessionResume {
					if d, ok := msg.Data.(*copilot.SessionResumeData); ok {
						resumeEvent = d
						break
					}
				}
			}
			if resumeEvent == nil {
				t.Fatal("Expected a session.resume event")
				return
			}
			if resumeEvent.ContinuePendingWork != nil && *resumeEvent.ContinuePendingWork {
				t.Errorf("Expected ContinuePendingWork=false in resume event, got %v", resumeEvent.ContinuePendingWork)
			}
			if resumeEvent.SessionWasActive == nil || *resumeEvent.SessionWasActive != scenario.expectedSessionWasActive {
				t.Errorf("Expected SessionWasActive=%t in resume event, got %v", scenario.expectedSessionWasActive, resumeEvent.SessionWasActive)
			}

			// In warm mode the runtime still has the pending request; in cold mode the
			// runtime auto-completed the orphan with a synthetic interrupt result during
			// resume, so HandlePendingToolCall is expected to report Success=false.
			toolResult, err := session2.RPC.Tools.HandlePendingToolCall(t.Context(), &rpc.HandlePendingToolCallRequest{
				RequestID: toolEvent.RequestID,
				Result:    rpc.ExternalToolStringResult("EXTERNAL_RESUMED_BETA"),
			})
			if err != nil {
				t.Fatalf("Failed to handle pending tool call: %v", err)
			}
			if toolResult.Success != scenario.expectedHandleResult {
				t.Errorf("Expected HandlePendingToolCall Success=%t, got %+v", scenario.expectedHandleResult, toolResult)
			}

			if scenario.expectedHandleResult {
				// Warm path: the result flows through to the LLM and the assistant should
				// echo it in its final reply.
				ctxFinal, cancel := context.WithTimeout(t.Context(), pendingWorkTimeout)
				defer cancel()
				answer, err := testharness.GetFinalAssistantMessage(ctxFinal, session2)
				if err != nil {
					t.Fatalf("Failed to wait for final assistant message: %v", err)
				}
				if assistant, ok := answer.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(assistant.Content, "EXTERNAL_RESUMED_BETA") {
					t.Errorf("Expected answer to contain 'EXTERNAL_RESUMED_BETA', got %v", answer.Data)
				}
			} else {
				// Cold path: orphan auto-completion does not trigger an LLM turn on its
				// own, but the session should remain healthy for new work.
				followUp, err := session2.SendAndWait(t.Context(), copilot.MessageOptions{
					Prompt: "Reply with exactly: COLD_RESUMED_FOLLOWUP",
				})
				if err != nil {
					t.Fatalf("Failed to send follow-up turn: %v", err)
				}
				if assistant, ok := followUp.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(assistant.Content, "COLD_RESUMED_FOLLOWUP") {
					t.Errorf("Expected follow-up answer to contain 'COLD_RESUMED_FOLLOWUP', got %v", followUp.Data)
				}
			}

			select {
			case releaseTool <- "ORIGINAL_SHOULD_NOT_WIN":
			default:
			}

			session2.Disconnect()
		})
	}

	t.Run("should report continuependingwork true in resume event", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		_, cliURL := startTcpServer(t, ctx)

		var sessionID string
		func() {
			firstClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
				opts.Connection = copilot.UriConnection{URL: cliURL, ConnectionToken: sharedTcpToken}
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
				Prompt: "Reply with exactly: CONTINUE_PENDING_WORK_TRUE_TURN_ONE",
			})
			if err != nil {
				t.Fatalf("Failed to send first turn: %v", err)
			}
			if assistant, ok := answer.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(assistant.Content, "CONTINUE_PENDING_WORK_TRUE_TURN_ONE") {
				t.Errorf("Expected first answer to contain 'CONTINUE_PENDING_WORK_TRUE_TURN_ONE', got %v", answer.Data)
			}

			firstSession.Disconnect()
		}()

		resumedClient := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.Connection = copilot.UriConnection{URL: cliURL, ConnectionToken: sharedTcpToken}
		})
		t.Cleanup(func() { resumedClient.ForceStop() })

		resumedSession, err := resumedClient.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			ContinuePendingWork: true,
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		// Verify resume event reflects ContinuePendingWork=true and SessionWasActive=false (cold resume)
		messages, err := resumedSession.GetEvents(t.Context())
		if err != nil {
			t.Fatalf("GetEvents failed: %v", err)
		}
		var resumeEvent *copilot.SessionResumeData
		for _, msg := range messages {
			if msg.Type() == copilot.SessionEventTypeSessionResume {
				if d, ok := msg.Data.(*copilot.SessionResumeData); ok {
					resumeEvent = d
					break
				}
			}
		}
		if resumeEvent == nil {
			t.Fatal("Expected a session.resume event")
			return
		}
		if resumeEvent.ContinuePendingWork == nil || *resumeEvent.ContinuePendingWork != true {
			t.Errorf("Expected ContinuePendingWork=true in resume event, got %v", resumeEvent.ContinuePendingWork)
		}
		if resumeEvent.SessionWasActive != nil && *resumeEvent.SessionWasActive != false {
			t.Errorf("Expected SessionWasActive=false (or nil) for cold resume, got %v", resumeEvent.SessionWasActive)
		}

		followUp, err := resumedSession.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Reply with exactly: CONTINUE_PENDING_WORK_TRUE_TURN_TWO",
		})
		if err != nil {
			t.Fatalf("Failed to send follow-up turn: %v", err)
		}
		if assistant, ok := followUp.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(assistant.Content, "CONTINUE_PENDING_WORK_TRUE_TURN_TWO") {
			t.Errorf("Expected follow-up answer to contain 'CONTINUE_PENDING_WORK_TRUE_TURN_TWO', got %v", followUp.Data)
		}

		resumedSession.Disconnect()
	})
}

// serverCliURL extracts the local CLI URL from a TCP-mode server client.
// The server must already be started; this function panics with a fatal
// test failure if the port is not yet available.
func serverCliURL(t *testing.T, server *copilot.Client) string {
	t.Helper()
	port := server.RuntimePort()
	if port == 0 {
		t.Fatal("Expected non-zero RuntimePort from TCP server client; ensure the server is started before calling serverCliURL")
	}
	return fmt.Sprintf("localhost:%d", port)
}

// sharedTcpToken is the connection token used by startTcpServer and any sibling
// client that connects via the resulting CLI URL. Tests use a fixed token rather
// than the auto-generated one because the second client is constructed without
// access to the first client's internal state.
const sharedTcpToken = "tcp-shared-test-token"

// startTcpServer starts a TCP-mode server client and returns its CLI URL.
// It triggers an initial connection so RuntimePort is populated.
func startTcpServer(t *testing.T, ctx *testharness.TestContext) (*copilot.Client, string) {
	t.Helper()
	server := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.Connection = copilot.TcpConnection{Path: opts.Connection.(copilot.StdioConnection).Path, ConnectionToken: sharedTcpToken}
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
		if evt.Type() != copilot.SessionEventTypeExternalToolRequested {
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
