package e2e

import (
	"encoding/json"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestRpcSessionStateExtras(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should_list_models_for_session", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		const token = "rpc-session-model-list-token"
		registerProxyUser(t, ctx, token, "rpc-session-extras-user", nil)
		authClient := newAuthenticatedClient(ctx, token)
		defer authClient.ForceStop()

		session := createPortedSession(t, authClient, &copilot.SessionConfig{Model: "claude-sonnet-4.5"})
		defer session.Disconnect()

		result, err := session.RPC.Model.List(t.Context())
		if err != nil {
			t.Fatalf("Model.List failed: %v", err)
		}
		if result.List == nil {
			t.Fatal("Expected non-nil model list")
		}
		if len(result.List) == 0 {
			t.Fatal("Expected non-empty model list")
		}
		found := false
		for _, model := range result.List {
			data, err := json.Marshal(model)
			if err == nil && strings.Contains(string(data), "claude-sonnet-4.5") {
				found = true
				break
			}
		}
		if !found {
			t.Fatalf("Expected model list to include claude-sonnet-4.5, got %+v", result.List)
		}
	})

	t.Run("should_report_session_activity_when_idle", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()

		activity, err := session.RPC.Metadata.Activity(t.Context())
		if err != nil {
			t.Fatalf("Metadata.Activity failed: %v", err)
		}
		if activity.HasActiveWork {
			t.Fatal("Expected a fresh session to report no active work")
		}
		if activity.Abortable {
			t.Fatal("Expected a fresh session to have nothing abortable")
		}
	})

	t.Run("should_get_and_set_allowall_permissions", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()
		defer func() {
			_, _ = session.RPC.Permissions.SetAllowAll(t.Context(), &rpc.PermissionsSetAllowAllRequest{Enabled: false})
		}()

		initial, err := session.RPC.Permissions.GetAllowAll(t.Context())
		if err != nil {
			t.Fatalf("Permissions.GetAllowAll initial failed: %v", err)
		}
		if initial.Enabled {
			t.Fatal("Allow-all should be disabled on a fresh session")
		}

		enable, err := session.RPC.Permissions.SetAllowAll(t.Context(), &rpc.PermissionsSetAllowAllRequest{Enabled: true})
		if err != nil {
			t.Fatalf("Permissions.SetAllowAll(true) failed: %v", err)
		}
		if !enable.Success || !enable.Enabled {
			t.Fatalf("Expected successful enable, got %+v", enable)
		}
		afterEnable, err := session.RPC.Permissions.GetAllowAll(t.Context())
		if err != nil {
			t.Fatalf("Permissions.GetAllowAll after enable failed: %v", err)
		}
		if !afterEnable.Enabled {
			t.Fatal("Expected allow-all to be enabled")
		}

		disable, err := session.RPC.Permissions.SetAllowAll(t.Context(), &rpc.PermissionsSetAllowAllRequest{Enabled: false})
		if err != nil {
			t.Fatalf("Permissions.SetAllowAll(false) failed: %v", err)
		}
		if !disable.Success || disable.Enabled {
			t.Fatalf("Expected successful disable, got %+v", disable)
		}
		afterDisable, err := session.RPC.Permissions.GetAllowAll(t.Context())
		if err != nil {
			t.Fatalf("Permissions.GetAllowAll after disable failed: %v", err)
		}
		if afterDisable.Enabled {
			t.Fatal("Expected allow-all to be disabled")
		}
	})

	t.Run("should_read_empty_sql_todos_for_fresh_session", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()

		result, err := session.RPC.Plan.ReadSqlTodos(t.Context())
		if err != nil {
			t.Fatalf("Plan.ReadSqlTodos failed: %v", err)
		}
		if result.Rows == nil {
			t.Fatal("Expected non-nil SQL todo rows")
		}
		if len(result.Rows) != 0 {
			t.Fatalf("Expected empty SQL todo rows, got %+v", result.Rows)
		}
	})

	t.Run("should_get_telemetry_engagement_id", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()

		result, err := session.RPC.Telemetry.GetEngagementId(t.Context())
		if err != nil {
			t.Fatalf("Telemetry.GetEngagementId failed: %v", err)
		}
		if result == nil {
			t.Fatal("Expected non-nil telemetry engagement result")
		}
	})

	t.Run("should_get_current_tool_metadata_after_initialization", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()

		answer, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 2+2?"})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if answer == nil {
			t.Fatal("Expected a final assistant message")
		}

		result, err := session.RPC.Tools.GetCurrentMetadata(t.Context())
		if err != nil {
			t.Fatalf("Tools.GetCurrentMetadata failed: %v", err)
		}
		if result.Tools == nil {
			t.Fatal("Expected non-nil current tool metadata")
		}
		if len(result.Tools) == 0 {
			t.Fatal("Expected non-empty current tool metadata")
		}
		for _, tool := range result.Tools {
			if strings.TrimSpace(tool.Name) == "" {
				t.Fatalf("Expected non-empty tool name, got %+v", tool)
			}
			if strings.TrimSpace(tool.Description) == "" {
				t.Fatalf("Expected non-empty tool description, got %+v", tool)
			}
		}
	})

	t.Run("should_reload_session_plugins", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()

		if _, err := session.RPC.Plugins.Reload(t.Context()); err != nil {
			t.Fatalf("Plugins.Reload failed: %v", err)
		}
		plugins, err := session.RPC.Plugins.List(t.Context())
		if err != nil {
			t.Fatalf("Plugins.List failed: %v", err)
		}
		if plugins.Plugins == nil {
			t.Fatal("Expected non-nil session plugin list")
		}
		for _, plugin := range plugins.Plugins {
			if strings.TrimSpace(plugin.Name) == "" {
				t.Fatalf("Expected non-empty plugin name, got %+v", plugin)
			}
		}
	})
}
