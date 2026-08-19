package e2e

import (
	"strings"
	"testing"

	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestRpcUiEphemeralQuery(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should_answer_ephemeral_query", func(t *testing.T) {
		// TODO(cli-1.0.81-2): CLI 1.0.81-2 fails session.ui.ephemeralQuery against the
		// recorded snapshot ("Failed to get response from the AI model"). Re-enable once
		// the runtime fix ships.
		t.Skip("blocked on CLI 1.0.81-2 session.ui.ephemeralQuery regression")

		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()

		result, err := session.RPC.UI.EphemeralQuery(t.Context(), &rpc.UIEphemeralQueryRequest{
			Question: "In one word, what is the primary color of a clear daytime sky?",
		})
		if err != nil {
			t.Fatalf("UI.EphemeralQuery failed: %v", err)
		}
		if result == nil {
			t.Fatal("Expected non-nil ephemeral query result")
			return
		}
		if strings.TrimSpace(result.Answer) == "" {
			t.Fatal("Expected non-empty ephemeral query answer")
		}
		if !strings.Contains(strings.ToLower(result.Answer), "blue") {
			t.Fatalf("Expected answer to contain blue, got %q", result.Answer)
		}
	})
}
