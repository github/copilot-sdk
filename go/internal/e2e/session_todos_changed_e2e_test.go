package e2e

import (
	"context"
	"slices"
	"sort"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestFiresSessionTodosChangedAndExposesRowsAndDependencies(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("fires session.todos_changed and exposes rows and dependencies", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		defer session.Disconnect()

		awaitTodosChanged := waitForMatchingEvent(
			session,
			copilot.SessionEventType("session.todos_changed"),
			func(copilot.SessionEvent) bool { return true },
			"session.todos_changed event",
		)

		sendCtx, cancel := context.WithTimeout(t.Context(), 120*time.Second)
		defer cancel()
		_, err = session.SendAndWait(sendCtx, copilot.MessageOptions{
			Prompt: "Use the sql tool exactly once to execute all three of the following statements " +
				"together, in this exact order, in a single sql tool call (a single query string " +
				"containing all three statements):\n" +
				"1. INSERT INTO todos (id, title, status) VALUES ('alpha', 'First todo', 'pending');\n" +
				"2. INSERT INTO todos (id, title, status) VALUES ('beta', 'Second todo', 'done');\n" +
				"3. INSERT INTO todo_deps (todo_id, depends_on) VALUES ('beta', 'alpha');\n" +
				"Then stop. Do not insert any other rows or create any other tables.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		awaitEvent(t, awaitTodosChanged)

		result, err := session.RPC.Plan.ReadSqlTodosWithDependencies(t.Context())
		if err != nil {
			t.Fatalf("Plan.ReadSqlTodosWithDependencies failed: %v", err)
		}

		var ids []string
		for _, row := range result.Rows {
			if row.ID != nil && *row.ID != "" {
				ids = append(ids, *row.ID)
			}
		}
		sort.Strings(ids)
		if !slices.Equal(ids, []string{"alpha", "beta"}) {
			t.Fatalf("Expected todo ids [alpha beta], got %v", ids)
		}

		foundDependency := false
		for _, dependency := range result.Dependencies {
			if dependency.TodoID == "beta" && dependency.DependsOn == "alpha" {
				foundDependency = true
				break
			}
		}
		if !foundDependency {
			t.Fatalf("Expected dependency beta -> alpha, got %+v", result.Dependencies)
		}
	})
}
