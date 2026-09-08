package e2e

import (
	"testing"

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

	newAutoSession := func(t *testing.T) *copilot.Session {
		t.Helper()
		ctx := testharness.NewTestContext(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })
		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}
		ctx.ConfigureForTest(t)

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
		superseded, err := session.SetAutoTier(t.Context(), autoTier(copilot.AutoTierIntelligence))
		if err != nil {
			t.Fatalf("SetAutoTier(intelligence) failed: %v", err)
		}
		if superseded.Status != rpc.ModelSwitchAutoTierStatusPending {
			t.Fatalf("Expected status pending, got %q", superseded.Status)
		}
		if superseded.SupersededAutoTier == nil || *superseded.SupersededAutoTier != rpc.AutoTierEfficiency {
			t.Fatalf("Expected superseded efficiency, got %+v", superseded)
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
			AutoTier: autoTier(copilot.AutoTierIntelligence),
		}); err != nil {
			t.Fatalf("SetModel with AutoTier failed: %v", err)
		}
		assertPending(t, session, rpc.AutoTierIntelligence)

		// ResetAutoTier clears it. Omission, a value, and a reset are three distinct
		// outcomes, which is why a single nillable field cannot express the request.
		if err := session.SetModel(t.Context(), "auto", &copilot.SetModelOptions{
			ResetAutoTier: true,
		}); err != nil {
			t.Fatalf("SetModel with ResetAutoTier failed: %v", err)
		}
		assertNoPending(t, session)
	})
}
