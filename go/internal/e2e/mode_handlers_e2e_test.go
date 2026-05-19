package e2e

import (
	"fmt"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

const (
	modeHandlerToken = "mode-handler-token"
	planSummary      = "Greeting file implementation plan"
	planPrompt       = "Create a brief implementation plan for adding a greeting.txt file, then request approval with exit_plan_mode."
	autoModePrompt   = "Explain that auto mode recovered from a rate limit in one short sentence."
)

func TestModeHandlersE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)

	client := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.Env = append(opts.Env, "COPILOT_DEBUG_GITHUB_API_URL="+ctx.ProxyURL)
	})
	t.Cleanup(func() { client.ForceStop() })

	if err := ctx.SetCopilotUserByToken(modeHandlerToken, map[string]interface{}{
		"login":                 "mode-handler-user",
		"copilot_plan":          "individual_pro",
		"endpoints":             map[string]interface{}{"api": ctx.ProxyURL, "telemetry": "https://localhost:1/telemetry"},
		"analytics_tracking_id": "mode-handler-tracking-id",
	}); err != nil {
		t.Fatalf("Failed to set copilot user for mode handler test: %v", err)
	}

	t.Run("should invoke exit plan mode handler when model uses tool", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var mu sync.Mutex
		var exitPlanModeRequests []copilot.ExitPlanModeRequest

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			GitHubToken:         modeHandlerToken,
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnExitPlanMode: func(request copilot.ExitPlanModeRequest, invocation copilot.ExitPlanModeInvocation) (copilot.ExitPlanModeResult, error) {
				mu.Lock()
				exitPlanModeRequests = append(exitPlanModeRequests, request)
				mu.Unlock()

				if invocation.SessionID == "" {
					t.Error("Expected non-empty session ID in invocation")
				}

				return copilot.ExitPlanModeResult{
					Approved:       true,
					SelectedAction: "interactive",
					Feedback:       "Approved by the Go E2E test",
				}, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		defer session.Disconnect()

		awaitRequested := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeExitPlanModeRequested,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.ExitPlanModeRequestedData)
				return ok && data.Summary == planSummary
			},
			"exit_plan_mode.requested event",
		)
		awaitCompleted := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeExitPlanModeCompleted,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.ExitPlanModeCompletedData)
				return ok && data.Approved != nil && *data.Approved && data.SelectedAction != nil && *data.SelectedAction == "interactive"
			},
			"exit_plan_mode.completed event",
		)

		response, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: planPrompt,
			Mode:   "plan",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		if len(exitPlanModeRequests) != 1 {
			t.Fatalf("Expected one exit-plan-mode request, got %d", len(exitPlanModeRequests))
		}
		request := exitPlanModeRequests[0]
		mu.Unlock()

		if request.Summary != planSummary {
			t.Fatalf("Expected summary %q, got %q", planSummary, request.Summary)
		}
		if len(request.Actions) != 3 || request.Actions[0] != "interactive" || request.Actions[1] != "autopilot" || request.Actions[2] != "exit_only" {
			t.Fatalf("Unexpected actions: %#v", request.Actions)
		}
		if request.RecommendedAction != "interactive" {
			t.Fatalf("Expected recommended action interactive, got %q", request.RecommendedAction)
		}
		requested := awaitEvent(t, awaitRequested)
		if data := requested.Data.(*copilot.ExitPlanModeRequestedData); data.Summary != planSummary {
			t.Fatalf("Expected requested event summary %q, got %+v", planSummary, data)
		}

		completed := awaitEvent(t, awaitCompleted)
		completedData := completed.Data.(*copilot.ExitPlanModeCompletedData)
		if completedData.Feedback == nil || *completedData.Feedback != "Approved by the Go E2E test" {
			t.Fatalf("Unexpected completed event feedback: %+v", completedData)
		}

		if response == nil {
			t.Fatal("Expected non-nil response")
		}
	})

	t.Run("should invoke auto mode switch handler when rate limited", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var mu sync.Mutex
		var autoModeSwitchRequests []copilot.AutoModeSwitchRequest

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			GitHubToken:         modeHandlerToken,
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnAutoModeSwitch: func(request copilot.AutoModeSwitchRequest, invocation copilot.AutoModeSwitchInvocation) (copilot.AutoModeSwitchResponse, error) {
				mu.Lock()
				autoModeSwitchRequests = append(autoModeSwitchRequests, request)
				mu.Unlock()

				if invocation.SessionID == "" {
					t.Error("Expected non-empty session ID in invocation")
				}

				return copilot.AutoModeSwitchResponseYes, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		defer session.Disconnect()

		awaitRequested := waitForMatchingEventAllowingRateLimit(
			session,
			copilot.SessionEventTypeAutoModeSwitchRequested,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.AutoModeSwitchRequestedData)
				return ok &&
					data.ErrorCode != nil && *data.ErrorCode == "user_weekly_rate_limited" &&
					data.RetryAfterSeconds != nil && *data.RetryAfterSeconds == 1
			},
			"auto_mode_switch.requested event",
		)
		awaitCompleted := waitForMatchingEventAllowingRateLimit(
			session,
			copilot.SessionEventTypeAutoModeSwitchCompleted,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.AutoModeSwitchCompletedData)
				return ok && data.Response == "yes"
			},
			"auto_mode_switch.completed event",
		)
		awaitModelChange := waitForMatchingEventAllowingRateLimit(
			session,
			copilot.SessionEventTypeSessionModelChange,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionModelChangeData)
				return ok && data.Cause != nil && *data.Cause == "rate_limit_auto_switch"
			},
			"rate-limit auto-mode model change",
		)
		awaitIdle := waitForMatchingEventAllowingRateLimit(
			session,
			copilot.SessionEventTypeSessionIdle,
			func(event copilot.SessionEvent) bool {
				_, ok := event.Data.(*copilot.SessionIdleData)
				return ok
			},
			"session.idle after auto-mode switch",
		)

		messageID, err := session.Send(t.Context(), copilot.MessageOptions{
			Prompt: autoModePrompt,
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if messageID == "" {
			t.Fatal("Expected non-empty message ID")
		}

		requested := awaitEvent(t, awaitRequested)
		requestedData := requested.Data.(*copilot.AutoModeSwitchRequestedData)
		if requestedData.ErrorCode == nil || *requestedData.ErrorCode != "user_weekly_rate_limited" {
			t.Fatalf("Unexpected requested event error code: %+v", requestedData)
		}

		completed := awaitEvent(t, awaitCompleted)
		if data := completed.Data.(*copilot.AutoModeSwitchCompletedData); data.Response != "yes" {
			t.Fatalf("Unexpected completed event response: %+v", data)
		}

		modelChange := awaitEvent(t, awaitModelChange)
		if data := modelChange.Data.(*copilot.SessionModelChangeData); data.Cause == nil || *data.Cause != "rate_limit_auto_switch" {
			t.Fatalf("Unexpected model change event: %+v", data)
		}
		awaitEvent(t, awaitIdle)

		mu.Lock()
		if len(autoModeSwitchRequests) != 1 {
			t.Fatalf("Expected one auto-mode-switch request, got %d", len(autoModeSwitchRequests))
		}
		request := autoModeSwitchRequests[0]
		mu.Unlock()

		if request.ErrorCode == nil || *request.ErrorCode != "user_weekly_rate_limited" {
			t.Fatalf("Unexpected auto-mode-switch request error code: %+v", request)
		}
		if request.RetryAfterSeconds == nil || *request.RetryAfterSeconds != 1 {
			t.Fatalf("Unexpected auto-mode-switch retry-after value: %+v", request)
		}
	})
}

func waitForMatchingEventAllowingRateLimit(session *copilot.Session, eventType copilot.SessionEventType, predicate func(copilot.SessionEvent) bool, description string) func() (*copilot.SessionEvent, error) {
	result := make(chan *copilot.SessionEvent, 1)
	errCh := make(chan error, 1)
	unsubscribe := session.On(func(event copilot.SessionEvent) {
		if event.Type() == eventType && predicate(event) {
			select {
			case result <- &event:
			default:
			}
		} else if event.Type() == copilot.SessionEventTypeSessionError {
			if data, ok := event.Data.(*copilot.SessionErrorData); ok && data.ErrorType == "rate_limit" {
				return
			}
			msg := "session error"
			if data, ok := event.Data.(*copilot.SessionErrorData); ok {
				msg = data.Message
			}
			select {
			case errCh <- fmt.Errorf("%s while waiting for %s", msg, description):
			default:
			}
		}
	})

	return func() (*copilot.SessionEvent, error) {
		defer unsubscribe()
		select {
		case event := <-result:
			return event, nil
		case err := <-errCh:
			return nil, err
		case <-time.After(30 * time.Second):
			return nil, fmt.Errorf("timed out waiting for %s", description)
		}
	}
}
