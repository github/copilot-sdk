package e2e

import (
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestPerSessionAuth(t *testing.T) {
	ctx := testharness.NewTestContext(t)

	// Create client with COPILOT_DEBUG_GITHUB_API_URL redirected to the proxy
	// so per-session auth token resolution (fetchCopilotUser) is intercepted.
	client := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.Env = append(opts.Env, "COPILOT_DEBUG_GITHUB_API_URL="+ctx.ProxyURL)
	})
	t.Cleanup(func() { client.ForceStop() })
	// Register per-token user configs on the proxy
	if err := ctx.SetCopilotUserByToken("token-alice", map[string]interface{}{
		"login":                 "alice",
		"copilot_plan":          "individual_pro",
		"endpoints":             map[string]interface{}{"api": ctx.ProxyURL, "telemetry": "https://localhost:1/telemetry"},
		"analytics_tracking_id": "alice-tracking-id",
	}); err != nil {
		t.Fatalf("Failed to set copilot user for alice: %v", err)
	}

	if err := ctx.SetCopilotUserByToken("token-bob", map[string]interface{}{
		"login":                 "bob",
		"copilot_plan":          "business",
		"endpoints":             map[string]interface{}{"api": ctx.ProxyURL, "telemetry": "https://localhost:1/telemetry"},
		"analytics_tracking_id": "bob-tracking-id",
	}); err != nil {
		t.Fatalf("Failed to set copilot user for bob: %v", err)
	}

	t.Run("should authenticate with per-session token", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			GitHubToken:         "token-alice",
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		authStatus, err := session.RPC.Auth.GetStatus(t.Context())
		if err != nil {
			t.Fatalf("Failed to get auth status: %v", err)
		}

		if !authStatus.IsAuthenticated {
			t.Errorf("Expected session to be authenticated")
		}
		if authStatus.Login == nil || *authStatus.Login != "alice" {
			t.Errorf("Expected login to be 'alice', got %v", authStatus.Login)
		}
	})

	t.Run("should isolate auth between sessions", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		sessionA, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			GitHubToken:         "token-alice",
		})
		if err != nil {
			t.Fatalf("Failed to create session A: %v", err)
		}

		sessionB, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			GitHubToken:         "token-bob",
		})
		if err != nil {
			t.Fatalf("Failed to create session B: %v", err)
		}

		statusA, err := sessionA.RPC.Auth.GetStatus(t.Context())
		if err != nil {
			t.Fatalf("Failed to get auth status for session A: %v", err)
		}

		statusB, err := sessionB.RPC.Auth.GetStatus(t.Context())
		if err != nil {
			t.Fatalf("Failed to get auth status for session B: %v", err)
		}

		if statusA.Login == nil || *statusA.Login != "alice" {
			t.Errorf("Expected session A login to be 'alice', got %v", statusA.Login)
		}
		if statusB.Login == nil || *statusB.Login != "bob" {
			t.Errorf("Expected session B login to be 'bob', got %v", statusB.Login)
		}
	})

	t.Run("should be unauthenticated without token", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		authStatus, err := session.RPC.Auth.GetStatus(t.Context())
		if err != nil {
			t.Fatalf("Failed to get auth status: %v", err)
		}

		// Without a per-session token, there is no per-session identity.
		// In CI the process-level fake token may still authenticate globally,
		// so we check Login rather than IsAuthenticated.
		if authStatus.Login != nil && *authStatus.Login != "" {
			t.Errorf("Expected no per-session login without token, got %q", *authStatus.Login)
		}
	})

	t.Run("should fail with invalid token", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		_, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			GitHubToken:         "invalid-token",
		})
		if err == nil {
			t.Fatal("Expected session creation to fail with invalid token")
		}
		t.Logf("Got expected error: %v", err)
	})
}
