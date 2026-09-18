package e2e

import (
	"sync/atomic"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors nodejs/test/e2e/auto_tier.e2e.test.ts (snapshot category "auto_tier").
//
// The runtime stages an Auto routing preference instead of applying it immediately: a
// request stays unclaimed until a later turn using the "auto" model mints a usable
// model and token pair. These tests observe that staged state through Model.GetCurrent,
// so they assert what the runtime actually recorded rather than what the SDK serialized.
func TestAutoTierE2E(t *testing.T) {
	autoTier := func(tier copilot.AutoTier) *copilot.AutoTier { return &tier }

	pendingTier := func(t *testing.T, session *copilot.Session) *rpc.AutoTier {
		t.Helper()
		current, err := session.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("Model.GetCurrent failed: %v", err)
		}
		return current.PendingAutoTier
	}

	assertPending := func(t *testing.T, session *copilot.Session, want rpc.AutoTier) {
		t.Helper()
		got := pendingTier(t, session)
		if got == nil || *got != want {
			t.Fatalf("Expected pending auto tier %q, got %v", want, got)
		}
	}

	assertNoPending := func(t *testing.T, session *copilot.Session) {
		t.Helper()
		if got := pendingTier(t, session); got != nil {
			t.Fatalf("Expected no pending auto tier, got %q", *got)
		}
	}

	newAutoClient := func(t *testing.T) (*testharness.TestContext, *copilot.Client) {
		t.Helper()
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })
		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}
		return ctx, client
	}

	newAutoSession := func(t *testing.T) *copilot.Session {
		t.Helper()
		_, client := newAutoClient(t)
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model:               "auto",
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		return session
	}

	t.Run("should stage and reset auto tier preference", func(t *testing.T) {
		if testharness.RunInIsolatedProcess(t) {
			return
		}
		session := newAutoSession(t)
		assertNoPending(t, session)

		staged, err := session.SetAutoTier(t.Context(), autoTier(copilot.AutoTierEfficiency))
		if err != nil {
			t.Fatalf("SetAutoTier(efficiency) failed: %v", err)
		}
		if staged.Status != rpc.ModelSwitchAutoTierStatusPending {
			t.Fatalf("Expected status pending, got %q", staged.Status)
		}
		if staged.PendingAutoTier == nil || *staged.PendingAutoTier != rpc.AutoTierEfficiency {
			t.Fatalf("Expected pending efficiency in result, got %+v", staged)
		}
		assertPending(t, session, rpc.AutoTierEfficiency)

		// A second request replaces the first and reports the one it displaced.
		superseded, err := session.SetAutoTier(t.Context(), autoTier(copilot.AutoTierFast))
		if err != nil {
			t.Fatalf("SetAutoTier(fast) failed: %v", err)
		}
		if superseded.Status != rpc.ModelSwitchAutoTierStatusPending {
			t.Fatalf("Expected status pending, got %q", superseded.Status)
		}
		if superseded.SupersededAutoTier == nil || *superseded.SupersededAutoTier != rpc.AutoTierEfficiency {
			t.Fatalf("Expected superseded efficiency, got %+v", superseded)
		}
		assertPending(t, session, rpc.AutoTierFast)

		replacedFast, err := session.SetAutoTier(t.Context(), autoTier(copilot.AutoTierIntelligence))
		if err != nil {
			t.Fatalf("SetAutoTier(intelligence) failed: %v", err)
		}
		if replacedFast.Status != rpc.ModelSwitchAutoTierStatusPending {
			t.Fatalf("Expected status pending, got %q", replacedFast.Status)
		}
		if replacedFast.SupersededAutoTier == nil || *replacedFast.SupersededAutoTier != rpc.AutoTierFast {
			t.Fatalf("Expected superseded fast, got %+v", replacedFast)
		}
		assertPending(t, session, rpc.AutoTierIntelligence)

		// A nil tier returns the session to provider-default routing. The status is
		// unchanged because provider-default was already the committed preference;
		// the request's effect is cancelling the staged one.
		reset, err := session.SetAutoTier(t.Context(), nil)
		if err != nil {
			t.Fatalf("SetAutoTier(nil) failed: %v", err)
		}
		if reset.Status != rpc.ModelSwitchAutoTierStatusUnchanged {
			t.Fatalf("Expected status unchanged, got %q", reset.Status)
		}
		if reset.SupersededAutoTier == nil || *reset.SupersededAutoTier != rpc.AutoTierIntelligence {
			t.Fatalf("Expected superseded intelligence, got %+v", reset)
		}
		assertNoPending(t, session)
	})

	t.Run("should preserve auto tier when set model omits it", func(t *testing.T) {
		if testharness.RunInIsolatedProcess(t) {
			return
		}
		session := newAutoSession(t)

		if _, err := session.SetAutoTier(t.Context(), autoTier(copilot.AutoTierBalance)); err != nil {
			t.Fatalf("SetAutoTier(balance) failed: %v", err)
		}
		assertPending(t, session, rpc.AutoTierBalance)

		// Leaving AutoTier nil without asking for a reset leaves the staged preference alone.
		if err := session.SetModel(t.Context(), "auto", nil); err != nil {
			t.Fatalf("SetModel without options failed: %v", err)
		}
		assertPending(t, session, rpc.AutoTierBalance)

		// Supplying a tier replaces it.
		if err := session.SetModel(t.Context(), "auto", &copilot.SetModelOptions{
			AutoTier: autoTier(copilot.AutoTierFast),
		}); err != nil {
			t.Fatalf("SetModel with AutoTier failed: %v", err)
		}
		assertPending(t, session, rpc.AutoTierFast)

		// ResetAutoTier clears it. Omission, a value, and a reset are three distinct
		// outcomes, which is why a single nillable field cannot express the request.
		if err := session.SetModel(t.Context(), "auto", &copilot.SetModelOptions{
			ResetAutoTier: true,
		}); err != nil {
			t.Fatalf("SetModel with ResetAutoTier failed: %v", err)
		}
		assertNoPending(t, session)
	})

	t.Run("should restore and override fast auto tier on cold resume", func(t *testing.T) {
		if testharness.RunInIsolatedProcess(t) {
			return
		}
		ctx, client := newAutoClient(t)
		fastSession, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model:               "auto",
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Capi: &copilot.CapiSessionOptions{
				AutoTier:                 copilot.AutoTierFast,
				EnableWebSocketResponses: copilot.Bool(false),
			},
		})
		if err != nil {
			t.Fatalf("CreateSession(fast) failed: %v", err)
		}
		tierlessSession, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model:               "auto",
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Capi: &copilot.CapiSessionOptions{
				EnableWebSocketResponses: copilot.Bool(false),
			},
		})
		if err != nil {
			t.Fatalf("CreateSession(tierless) failed: %v", err)
		}
		fastSessionID := fastSession.SessionID
		tierlessSessionID := tierlessSession.SessionID

		if _, err := fastSession.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Reply with exactly AUTO_TIER_COLD_RESUME_READY.",
		}); err != nil {
			t.Fatalf("Fast session turn failed: %v", err)
		}
		if _, err := tierlessSession.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Reply with exactly AUTO_TIER_TIERLESS_READY.",
		}); err != nil {
			t.Fatalf("Tierless session turn failed: %v", err)
		}
		fastCurrent, err := fastSession.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("GetCurrent(fast) failed: %v", err)
		}
		if fastCurrent.AutoTier == nil || *fastCurrent.AutoTier != rpc.AutoTierFast {
			t.Fatalf("Expected effective fast tier, got %+v", fastCurrent)
		}
		tierlessCurrent, err := tierlessSession.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("GetCurrent(tierless) failed: %v", err)
		}
		if tierlessCurrent.AutoTier != nil {
			t.Fatalf("Expected no effective tier, got %+v", tierlessCurrent)
		}

		fastSession.Disconnect()
		tierlessSession.Disconnect()
		if err := client.Stop(); err != nil {
			t.Fatalf("Stop initial client failed: %v", err)
		}

		restoredClient := ctx.NewClient()
		t.Cleanup(func() { restoredClient.ForceStop() })
		if err := restoredClient.Start(t.Context()); err != nil {
			t.Fatalf("Start restored client failed: %v", err)
		}
		restoredFast, err := restoredClient.ResumeSession(t.Context(), fastSessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("ResumeSession(fast) failed: %v", err)
		}
		restoredTierless, err := restoredClient.ResumeSession(t.Context(), tierlessSessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("ResumeSession(tierless) failed: %v", err)
		}
		restoredFastCurrent, err := restoredFast.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("GetCurrent(restored fast) failed: %v", err)
		}
		if restoredFastCurrent.AutoTier == nil || *restoredFastCurrent.AutoTier != rpc.AutoTierFast {
			t.Fatalf("Expected restored fast tier, got %+v", restoredFastCurrent)
		}
		restoredTierlessCurrent, err := restoredTierless.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("GetCurrent(restored tierless) failed: %v", err)
		}
		if restoredTierlessCurrent.AutoTier != nil {
			t.Fatalf("Expected restored tierless state, got %+v", restoredTierlessCurrent)
		}
		restoredFast.Disconnect()
		restoredTierless.Disconnect()
		if err := restoredClient.Stop(); err != nil {
			t.Fatalf("Stop restored client failed: %v", err)
		}

		overrideClient := ctx.NewClient()
		t.Cleanup(func() { overrideClient.ForceStop() })
		if err := overrideClient.Start(t.Context()); err != nil {
			t.Fatalf("Start override client failed: %v", err)
		}
		overridden, err := overrideClient.ResumeSession(t.Context(), fastSessionID, &copilot.ResumeSessionConfig{
			Model:               "auto",
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Capi: &copilot.CapiSessionOptions{
				AutoTier:                 copilot.AutoTierBalance,
				EnableWebSocketResponses: copilot.Bool(false),
			},
		})
		if err != nil {
			t.Fatalf("ResumeSession(override) failed: %v", err)
		}
		overriddenCurrent, err := overridden.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("GetCurrent(overridden) failed: %v", err)
		}
		if overriddenCurrent.AutoTier == nil || *overriddenCurrent.AutoTier != rpc.AutoTierBalance {
			t.Fatalf("Expected balance override, got %+v", overriddenCurrent)
		}
		overridden.Disconnect()
		if err := overrideClient.Stop(); err != nil {
			t.Fatalf("Stop override client failed: %v", err)
		}
	})

	t.Run("should commit fast auto tier after successful turn", func(t *testing.T) {
		if testharness.RunInIsolatedProcess(t) {
			return
		}
		_, client := newAutoClient(t)
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model:               "auto",
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Capi: &copilot.CapiSessionOptions{
				AutoTier:                 copilot.AutoTierEfficiency,
				EnableWebSocketResponses: copilot.Bool(false),
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		modelChanged := make(chan *copilot.SessionModelChangeData, 1)
		unsubscribe := session.On(func(event copilot.SessionEvent) {
			if data, ok := event.Data.(*copilot.SessionModelChangeData); ok && data.AutoTier != nil && *data.AutoTier == copilot.AutoTierFast {
				select {
				case modelChanged <- data:
				default:
				}
			}
		})
		defer unsubscribe()

		staged, err := session.SetAutoTier(t.Context(), autoTier(copilot.AutoTierFast))
		if err != nil {
			t.Fatalf("SetAutoTier(fast) failed: %v", err)
		}
		if staged.Status != rpc.ModelSwitchAutoTierStatusPending ||
			staged.EffectiveAutoTier == nil || *staged.EffectiveAutoTier != rpc.AutoTierEfficiency ||
			staged.PendingAutoTier == nil || *staged.PendingAutoTier != rpc.AutoTierFast {
			t.Fatalf("Unexpected staged result: %+v", staged)
		}

		beforeTurn, err := session.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("GetCurrent before turn failed: %v", err)
		}
		if beforeTurn.AutoTier == nil || *beforeTurn.AutoTier != rpc.AutoTierEfficiency ||
			beforeTurn.PendingAutoTier == nil || *beforeTurn.PendingAutoTier != rpc.AutoTierFast {
			t.Fatalf("Unexpected state before turn: %+v", beforeTurn)
		}

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Reply with exactly AUTO_TIER_FAST_COMMITTED.",
		}); err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		select {
		case data := <-modelChanged:
			if data.PreviousModel == nil || *data.PreviousModel != "auto" ||
				data.NewModel != "auto" ||
				data.PreviousAutoTier == nil || *data.PreviousAutoTier != copilot.AutoTierEfficiency {
				t.Fatalf("Unexpected model change: %+v", data)
			}
		case <-time.After(30 * time.Second):
			t.Fatal("Timed out waiting for Fast model change")
		}

		committed, err := session.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("GetCurrent after turn failed: %v", err)
		}
		if committed.AutoTier == nil || *committed.AutoTier != rpc.AutoTierFast ||
			committed.PendingAutoTier != nil || committed.ActivatingAutoTier != nil {
			t.Fatalf("Unexpected committed state: %+v", committed)
		}
	})

	t.Run("should preserve effective tier when fast activation fails", func(t *testing.T) {
		if testharness.RunInIsolatedProcess(t) {
			return
		}
		ctx, client := newAutoClient(t)
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model:               "auto",
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Capi: &copilot.CapiSessionOptions{
				AutoTier:                 copilot.AutoTierEfficiency,
				EnableWebSocketResponses: copilot.Bool(false),
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		sessionID := session.SessionID
		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Reply with exactly AUTO_TIER_INITIAL_READY.",
		}); err != nil {
			t.Fatalf("Initial SendAndWait failed: %v", err)
		}

		failureObserved := make(chan struct {
			data      *copilot.SessionAutoTierSwitchFailedData
			ephemeral bool
		}, 1)
		var fastCommitted atomic.Bool
		unsubscribe := session.On(func(event copilot.SessionEvent) {
			switch data := event.Data.(type) {
			case *copilot.SessionAutoTierSwitchFailedData:
				select {
				case failureObserved <- struct {
					data      *copilot.SessionAutoTierSwitchFailedData
					ephemeral bool
				}{data: data, ephemeral: event.Ephemeral != nil && *event.Ephemeral}:
				default:
				}
			case *copilot.SessionModelChangeData:
				if data.AutoTier != nil && *data.AutoTier == copilot.AutoTierFast {
					fastCommitted.Store(true)
				}
			}
		})

		staged, err := session.SetAutoTier(t.Context(), autoTier(copilot.AutoTierFast))
		if err != nil {
			t.Fatalf("SetAutoTier(fast) failed: %v", err)
		}
		if staged.Status != rpc.ModelSwitchAutoTierStatusPending ||
			staged.EffectiveAutoTier == nil || *staged.EffectiveAutoTier != rpc.AutoTierEfficiency ||
			staged.PendingAutoTier == nil || *staged.PendingAutoTier != rpc.AutoTierFast {
			t.Fatalf("Unexpected staged result: %+v", staged)
		}

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Reply with exactly AUTO_TIER_FAILURE_RECOVERED.",
		}); err != nil {
			t.Fatalf("Failure-path SendAndWait failed: %v", err)
		}

		select {
		case observed := <-failureObserved:
			if !observed.ephemeral ||
				observed.data.EffectiveAutoTier == nil || *observed.data.EffectiveAutoTier != copilot.AutoTierEfficiency ||
				observed.data.RequestedAutoTier == nil || *observed.data.RequestedAutoTier != copilot.AutoTierFast ||
				observed.data.Reason != copilot.AutoTierSwitchFailureReasonRequestFailed {
				t.Fatalf("Unexpected failure event: %+v", observed)
			}
		case <-time.After(30 * time.Second):
			t.Fatal("Timed out waiting for Fast activation failure")
		}
		if fastCommitted.Load() {
			t.Fatal("Fast tier committed after failed activation")
		}

		current, err := session.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("GetCurrent after failure failed: %v", err)
		}
		if current.AutoTier == nil || *current.AutoTier != rpc.AutoTierEfficiency ||
			current.PendingAutoTier != nil || current.ActivatingAutoTier != nil {
			t.Fatalf("Unexpected state after failure: %+v", current)
		}

		unsubscribe()
		session.Disconnect()
		if err := client.Stop(); err != nil {
			t.Fatalf("Stop failed client failed: %v", err)
		}

		resumedClient := ctx.NewClient()
		t.Cleanup(func() { resumedClient.ForceStop() })
		if err := resumedClient.Start(t.Context()); err != nil {
			t.Fatalf("Start resumed client failed: %v", err)
		}
		resumed, err := resumedClient.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		resumedCurrent, err := resumed.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("GetCurrent after resume failed: %v", err)
		}
		if resumedCurrent.AutoTier == nil || *resumedCurrent.AutoTier != rpc.AutoTierEfficiency ||
			resumedCurrent.PendingAutoTier != nil || resumedCurrent.ActivatingAutoTier != nil {
			t.Fatalf("Unexpected resumed state: %+v", resumedCurrent)
		}
		max := int64(100)
		waitMs := int32(0)
		persisted, err := resumed.RPC.EventLog.Read(t.Context(), &rpc.EventLogReadRequest{
			Max:    &max,
			WaitMs: &waitMs,
		})
		if err != nil {
			t.Fatalf("EventLog.Read failed: %v", err)
		}
		for _, event := range persisted.Events {
			if event.Type() == copilot.SessionEventTypeSessionAutoTierSwitchFailed {
				t.Fatal("Ephemeral failure event was replayed after cold resume")
			}
		}
		resumed.Disconnect()
		if err := resumedClient.Stop(); err != nil {
			t.Fatalf("Stop resumed client failed: %v", err)
		}
	})
}
