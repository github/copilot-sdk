package e2e

import (
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestRpcServerMisc(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	sharedClient := ctx.NewClient()
	t.Cleanup(func() { sharedClient.ForceStop() })

	t.Run("should_reload_user_settings", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		if err := sharedClient.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		if _, err := sharedClient.RPC.User.Settings().Reload(t.Context()); err != nil {
			t.Fatalf("User.Settings.Reload failed: %v", err)
		}
	})

	t.Run("should_get_set_and_clear_user_settings", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedIsolatedPortedClient(t, ctx)
		defer client.ForceStop()

		initial, err := client.RPC.User.Settings().Get(t.Context())
		if err != nil {
			t.Fatalf("User.Settings.Get initial failed: %v", err)
		}
		if initial.Settings == nil {
			t.Fatal("Expected settings map")
		}
		var key string
		var value bool
		for candidateKey, setting := range initial.Settings {
			if candidateValue, ok := setting.Value.(bool); ok {
				key = candidateKey
				value = candidateValue
				break
			}
		}
		if key == "" {
			t.Fatalf("Expected at least one boolean setting, got %+v", initial.Settings)
		}
		toggledValue := !value

		set, err := client.RPC.User.Settings().Set(t.Context(), &rpc.UserSettingsSetRequest{
			Settings: map[string]any{key: toggledValue},
		})
		if err != nil {
			t.Fatalf("User.Settings.Set(toggle) failed: %v", err)
		}
		if len(set.ShadowedKeys) != 0 {
			t.Fatalf("Expected no shadowed settings keys, got %+v", set.ShadowedKeys)
		}
		if _, err := client.RPC.User.Settings().Reload(t.Context()); err != nil {
			t.Fatalf("User.Settings.Reload after set failed: %v", err)
		}
		afterSet, err := client.RPC.User.Settings().Get(t.Context())
		if err != nil {
			t.Fatalf("User.Settings.Get after set failed: %v", err)
		}
		metadata, ok := afterSet.Settings[key]
		if !ok {
			t.Fatalf("Expected setting %q in %+v", key, afterSet.Settings)
		}
		if metadata.Value != toggledValue || metadata.IsDefault {
			t.Fatalf("Expected explicit true setting, got %+v", metadata)
		}

		clear, err := client.RPC.User.Settings().Set(t.Context(), &rpc.UserSettingsSetRequest{
			Settings: map[string]any{key: nil},
		})
		if err != nil {
			t.Fatalf("User.Settings.Set(null) failed: %v", err)
		}
		if len(clear.ShadowedKeys) != 0 {
			t.Fatalf("Expected no shadowed settings keys from clear, got %+v", clear.ShadowedKeys)
		}
		if _, err := client.RPC.User.Settings().Reload(t.Context()); err != nil {
			t.Fatalf("User.Settings.Reload after clear failed: %v", err)
		}
		afterClear, err := client.RPC.User.Settings().Get(t.Context())
		if err != nil {
			t.Fatalf("User.Settings.Get after clear failed: %v", err)
		}
		metadata, ok = afterClear.Settings[key]
		if !ok {
			t.Fatalf("Expected setting %q after clear in %+v", key, afterClear.Settings)
		}
		if !metadata.IsDefault {
			t.Fatalf("Expected cleared setting to be default, got %+v", metadata)
		}
	})

	t.Run("should_login_list_getcurrentauth_and_logout_account", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		if err := ctx.SetCopilotUserByToken("go-account-token", map[string]interface{}{
			"login":        "go-account-user",
			"copilot_plan": "individual_pro",
			"endpoints": map[string]interface{}{
				"api":       ctx.ProxyURL,
				"telemetry": "https://localhost:1/telemetry",
			},
			"analytics_tracking_id": "go-account-user-tracking-id",
		}); err != nil {
			t.Fatalf("SetCopilotUserByToken failed: %v", err)
		}
		client := newNoTokenClient(t, ctx)
		defer client.ForceStop()

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		initial, err := client.RPC.Account.GetCurrentAuth(t.Context())
		if err != nil {
			t.Fatalf("Account.GetCurrentAuth initial failed: %v", err)
		}
		if initial.AuthInfo != nil {
			t.Fatalf("Expected no initial auth info, got %+v", initial.AuthInfo)
		}

		login, err := client.RPC.Account.Login(t.Context(), &rpc.AccountLoginRequest{
			Host:  "https://github.com",
			Login: "go-account-user",
			Token: "go-account-token",
		})
		if err != nil {
			t.Fatalf("Account.Login failed: %v", err)
		}
		if login == nil {
			t.Fatal("Expected login result")
		}

		current, err := client.RPC.Account.GetCurrentAuth(t.Context())
		if err != nil {
			t.Fatalf("Account.GetCurrentAuth after login failed: %v", err)
		}
		authInfo, ok := current.AuthInfo.(*rpc.UserAuthInfo)
		if !ok {
			t.Fatalf("Expected user auth info after login, got %#v", current.AuthInfo)
		}
		if authInfo.Login != "go-account-user" || authInfo.Host != "https://github.com" {
			t.Fatalf("Unexpected current auth info: %+v", authInfo)
		}

		users, err := client.RPC.Account.GetAllUsers(t.Context())
		if err != nil {
			t.Fatalf("Account.GetAllUsers failed: %v", err)
		}
		if users == nil {
			t.Fatal("Expected non-nil users result")
		}
		for _, user := range *users {
			userInfo, ok := user.AuthInfo.(*rpc.UserAuthInfo)
			if !ok {
				t.Fatalf("Expected user auth info in all users, got %#v", user.AuthInfo)
			}
			if userInfo.Login == "go-account-user" && (user.Token == nil || *user.Token != "go-account-token") {
				t.Fatalf("Expected logged-in user's token to round trip, got %+v", user)
			}
		}

		logout, err := client.RPC.Account.Logout(t.Context(), &rpc.AccountLogoutRequest{
			AuthInfo: authInfo,
		})
		if err != nil {
			t.Fatalf("Account.Logout failed: %v", err)
		}
		if logout.HasMoreUsers {
			t.Fatalf("Expected no users after isolated logout, got %+v", logout)
		}
		afterLogout, err := client.RPC.Account.GetCurrentAuth(t.Context())
		if err != nil {
			t.Fatalf("Account.GetCurrentAuth after logout failed: %v", err)
		}
		if afterLogout.AuthInfo != nil {
			t.Fatalf("Expected no auth after logout, got %+v", afterLogout.AuthInfo)
		}
	})

	t.Run("should_report_agent_registry_spawn_gate_closed", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedIsolatedPortedClient(t, ctx)
		defer client.ForceStop()

		_, err := client.RPC.AgentRegistry.Spawn(t.Context(), &rpc.AgentRegistrySpawnRequest{Cwd: ctx.WorkDir})
		if err == nil {
			t.Fatal("Expected AgentRegistry.Spawn to be rejected by the closed spawn gate")
		}
		message := err.Error()
		assertPortedNoUnhandledMethod(t, message)
		assertPortedContainsFold(t, message, "agentRegistry.spawn")
		if !strings.Contains(strings.ToLower(message), "not enabled") && !strings.Contains(strings.ToLower(message), "no delegate") {
			t.Fatalf("Expected agentRegistry.spawn gate error, got %s", message)
		}
	})

	t.Run("should_shut_down_owned_runtime", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedPortedClient(t, ctx)
		defer client.ForceStop()

		if _, err := client.RPC.User.Settings().Reload(t.Context()); err != nil {
			t.Fatalf("User.Settings.Reload before shutdown failed: %v", err)
		}
		if _, err := client.RPC.Runtime.Shutdown(t.Context()); err != nil {
			t.Fatalf("Runtime.Shutdown failed: %v", err)
		}

		waitForRPCCondition(t, 15*time.Second, "runtime to stop serving RPCs after shutdown", func() (bool, error) {
			_, err := client.RPC.User.Settings().Reload(t.Context())
			return err != nil, nil
		})
	})

	t.Run("should_report_not_found_when_opening_session_without_context", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedIsolatedPortedClient(t, ctx)
		defer client.ForceStop()

		result, err := client.RPC.Sessions.Open(t.Context(), nil)
		if err != nil {
			t.Fatalf("Sessions.Open failed: %v", err)
		}
		if result.Status != rpc.SessionsOpenStatusNotFound {
			t.Fatalf("Expected Sessions.Open status not_found, got %+v", result)
		}
		if result.SessionID != nil {
			t.Fatalf("Expected nil session ID for not_found, got %q", *result.SessionID)
		}
	})

	t.Run("should_reject_send_attachments_from_non_extension_connection", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, sharedClient, nil)
		defer session.Disconnect()

		_, err := session.RPC.Extensions.SendAttachmentsToMessage(t.Context(), &rpc.SendAttachmentsToMessageParams{Attachments: []rpc.PushAttachment{}})
		if err == nil {
			t.Fatal("Expected SendAttachmentsToMessage from a normal SDK connection to fail")
		}
		message := err.Error()
		assertPortedNoUnhandledMethod(t, message)
		assertPortedContainsFold(t, message, "extension")
	})
}

func newNoTokenClient(t *testing.T, ctx *testharness.TestContext) *copilot.Client {
	t.Helper()
	env := append([]string{}, ctx.Env()...)
	env = append(env,
		"COPILOT_HOME="+t.TempDir(),
		"GH_CONFIG_DIR="+t.TempDir(),
		"GH_TOKEN=",
		"GITHUB_TOKEN=",
		"COPILOT_SDK_AUTH_TOKEN=",
	)
	useLoggedInUser := false
	return copilot.NewClient(&copilot.ClientOptions{
		Connection:       copilot.StdioConnection{Path: ctx.CLIPath},
		WorkingDirectory: ctx.WorkDir,
		Env:              env,
		UseLoggedInUser:  &useLoggedInUser,
	})
}
