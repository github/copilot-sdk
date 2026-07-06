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
			_, _ = session.RPC.Permissions.SetAllowAll(t.Context(), &rpc.PermissionsSetAllowAllRequest{Enabled: copilot.Bool(false)})
		}()

		initial, err := session.RPC.Permissions.GetAllowAll(t.Context())
		if err != nil {
			t.Fatalf("Permissions.GetAllowAll initial failed: %v", err)
		}
		if initial.Enabled {
			t.Fatal("Allow-all should be disabled on a fresh session")
		}

		enable, err := session.RPC.Permissions.SetAllowAll(t.Context(), &rpc.PermissionsSetAllowAllRequest{Enabled: copilot.Bool(true)})
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

		disable, err := session.RPC.Permissions.SetAllowAll(t.Context(), &rpc.PermissionsSetAllowAllRequest{Enabled: copilot.Bool(false)})
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

	t.Run("should_add_byok_provider_and_model_at_runtime", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()

		apiKey := "provider-key"
		providerType := rpc.ProviderConfigTypeOpenai
		wireAPI := rpc.ProviderConfigWireAPICompletions
		modelName := "Go Added Model"
		maxPromptTokens := float64(4096)
		result, err := session.RPC.Provider.Add(t.Context(), &rpc.ProviderAddRequest{
			Providers: []rpc.NamedProviderConfig{{
				Name:    "go-e2e-provider",
				Type:    &providerType,
				BaseURL: "https://models.example.test/v1",
				APIKey:  &apiKey,
				Headers: map[string]string{"x-provider": "go"},
				WireAPI: &wireAPI,
			}},
			Models: []rpc.ProviderModelConfig{{
				ID:              "small",
				Provider:        "go-e2e-provider",
				Name:            &modelName,
				MaxPromptTokens: &maxPromptTokens,
			}},
		})
		if err != nil {
			t.Fatalf("Provider.Add failed: %v", err)
		}
		if len(result.Models) != 1 {
			t.Fatalf("Expected one added provider model, got %+v", result.Models)
		}

		selectionID := "go-e2e-provider/small"
		if _, err := session.RPC.Model.SwitchTo(t.Context(), &rpc.ModelSwitchToRequest{ModelID: selectionID}); err != nil {
			t.Fatalf("Model.SwitchTo added model failed: %v", err)
		}
		current, err := session.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("Model.GetCurrent after provider add failed: %v", err)
		}
		if current.ModelID == nil || *current.ModelID != selectionID {
			t.Fatalf("Expected current model %q, got %+v", selectionID, current)
		}
	})

	t.Run("should_return_empty_completions_when_host_does_not_provide_them", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()

		result, err := session.RPC.Completions.Request(t.Context(), &rpc.CompletionsRequestRequest{
			Text:   "Use @ to mention context",
			Offset: 5,
		})
		if err != nil {
			t.Fatalf("Completions.Request failed: %v", err)
		}
		if result.Items == nil {
			t.Fatal("Expected non-nil completion items list")
		}
	})

	t.Run("should_report_visibility_as_unsynced_for_local_session", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()

		status := rpc.SessionVisibilityStatusUnshared
		set, err := session.RPC.Visibility.Set(t.Context(), &rpc.VisibilitySetRequest{Status: status})
		if err != nil {
			t.Fatalf("Visibility.Set failed: %v", err)
		}
		if set.Synced || set.Status != nil || set.ShareURL != nil {
			t.Fatalf("Expected unsynced visibility set result, got %+v", set)
		}
		get, err := session.RPC.Visibility.Get(t.Context())
		if err != nil {
			t.Fatalf("Visibility.Get failed: %v", err)
		}
		if get.Synced || get.Status != nil || get.ShareURL != nil {
			t.Fatalf("Expected unsynced visibility get result, got %+v", get)
		}
	})

	t.Run("should_get_context_attribution_and_heaviest_messages_after_turn", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()

		answer, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say CONTEXT_METADATA_OK exactly."})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if answer == nil {
			t.Fatal("Expected final assistant message")
		}

		attribution, err := session.RPC.Metadata.GetContextAttribution(t.Context())
		if err != nil {
			t.Fatalf("Metadata.GetContextAttribution failed: %v", err)
		}
		if attribution == nil {
			t.Fatal("Expected attribution result")
		}
		limit := int64(5)
		heaviest, err := session.RPC.Metadata.GetContextHeaviestMessages(t.Context(), &rpc.MetadataContextHeaviestMessagesRequest{Limit: &limit})
		if err != nil {
			t.Fatalf("Metadata.GetContextHeaviestMessages failed: %v", err)
		}
		if heaviest.Messages == nil {
			t.Fatal("Expected non-nil heaviest messages list")
		}
	})

	t.Run("should_update_and_clear_live_subagent_settings", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()

		contextTier := rpc.SubagentSettingsEntryContextTierLongContext
		model := "gpt-5-mini"
		reasoningEffort := "low"
		update, err := session.RPC.Tools.UpdateSubagentSettings(t.Context(), &rpc.UpdateSubagentSettingsRequest{
			Subagents: &rpc.SubagentSettings{
				DisabledSubagents: []string{"legacy-agent"},
				Agents: map[string]rpc.SubagentSettingsEntry{
					"general-purpose": {
						ContextTier: &contextTier,
						Model:       &model,
						EffortLevel: &reasoningEffort,
					},
				},
			},
		})
		if err != nil {
			t.Fatalf("Tools.UpdateSubagentSettings failed: %v", err)
		}
		if update == nil {
			t.Fatal("Expected update result")
		}

		clear, err := session.RPC.Tools.UpdateSubagentSettings(t.Context(), &rpc.UpdateSubagentSettingsRequest{})
		if err != nil {
			t.Fatalf("Tools.UpdateSubagentSettings clear failed: %v", err)
		}
		if clear == nil {
			t.Fatal("Expected clear result")
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
