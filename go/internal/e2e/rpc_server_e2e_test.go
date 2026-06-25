package e2e

import (
	"fmt"
	"path/filepath"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
	"github.com/google/uuid"
)

// Mirrors dotnet/test/RpcServerTests.cs (snapshot category "rpc_server").
// Tests server-scoped (non-session) RPCs.
func TestRPCServerE2E(t *testing.T) {
	t.Run("should call rpc ping with typed params and result", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		message := "typed rpc test"
		result, err := client.RPC.Ping(t.Context(), &rpc.PingRequest{Message: &message})
		if err != nil {
			t.Fatalf("RPC.Ping failed: %v", err)
		}
		if !strings.Contains(result.Message, "typed rpc test") {
			t.Errorf("Expected ping response to contain 'typed rpc test', got %q", result.Message)
		}
		if result.Timestamp.IsZero() {
			t.Errorf("Expected non-zero Timestamp, got %s", result.Timestamp)
		}
	})

	t.Run("should call rpc models list with typed result", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)
		const token = "rpc-models-token"
		registerProxyUser(t, ctx, token, "rpc-user", nil)
		client := newAuthenticatedClient(ctx, token)
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		result, err := client.RPC.Models.List(t.Context(), &rpc.ModelsListRequest{})
		if err != nil {
			t.Fatalf("Models.List failed: %v", err)
		}
		if result.Models == nil {
			t.Fatal("Expected non-nil Models list")
		}
		var hasClaude bool
		for _, model := range result.Models {
			if strings.TrimSpace(model.Name) == "" {
				t.Errorf("Model %q has empty Name", model.ID)
			}
			if model.ID == "claude-sonnet-4.5" {
				hasClaude = true
			}
		}
		if !hasClaude {
			t.Errorf("Expected models list to contain 'claude-sonnet-4.5'")
		}
	})

	t.Run("should call rpc account get quota when authenticated", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)
		const token = "rpc-quota-token"
		registerProxyUser(t, ctx, token, "rpc-user", map[string]any{
			"chat": map[string]any{
				"entitlement":       100,
				"overage_count":     2,
				"overage_permitted": true,
				"percent_remaining": 75,
				"timestamp_utc":     "2026-04-30T00:00:00Z",
			},
		})
		client := newAuthenticatedClient(ctx, token)
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		tokenCopy := token
		result, err := client.RPC.Account.GetQuota(t.Context(), &rpc.AccountGetQuotaRequest{GitHubToken: &tokenCopy})
		if err != nil {
			t.Fatalf("Account.GetQuota failed: %v", err)
		}
		chat, present := result.QuotaSnapshots["chat"]
		if !present {
			t.Fatalf("Expected 'chat' quota in snapshots, got %+v", result.QuotaSnapshots)
		}
		if chat.EntitlementRequests != 100 {
			t.Errorf("Expected EntitlementRequests=100, got %d", chat.EntitlementRequests)
		}
		if chat.UsedRequests != 25 {
			t.Errorf("Expected UsedRequests=25, got %d", chat.UsedRequests)
		}
		if chat.RemainingPercentage != 75 {
			t.Errorf("Expected RemainingPercentage=75, got %v", chat.RemainingPercentage)
		}
		if chat.Overage != 2 {
			t.Errorf("Expected Overage=2, got %v", chat.Overage)
		}
		if !chat.UsageAllowedWithExhaustedQuota {
			t.Errorf("Expected UsageAllowedWithExhaustedQuota=true")
		}
		if !chat.OverageAllowedWithExhaustedQuota {
			t.Errorf("Expected OverageAllowedWithExhaustedQuota=true")
		}
		expectedResetDate, err := time.Parse(time.RFC3339, "2026-04-30T00:00:00Z")
		if err != nil {
			t.Fatalf("Parse expected reset date: %v", err)
		}
		if chat.ResetDate == nil || !chat.ResetDate.Equal(expectedResetDate) {
			t.Errorf("Expected ResetDate='2026-04-30T00:00:00Z', got %v", chat.ResetDate)
		}
	})

	t.Run("should call rpc tools list with typed result", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		result, err := client.RPC.Tools.List(t.Context(), &rpc.ToolsListRequest{})
		if err != nil {
			t.Fatalf("Tools.List failed: %v", err)
		}
		if len(result.Tools) == 0 {
			t.Fatal("Expected non-empty Tools list")
		}
		for i, tool := range result.Tools {
			if strings.TrimSpace(tool.Name) == "" {
				t.Errorf("Tool[%d] has empty Name", i)
			}
		}
	})

	t.Run("should call rpc session fs set provider with typed result", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		result, err := client.RPC.SessionFS.SetProvider(t.Context(), &rpc.SessionFSSetProviderRequest{
			InitialCwd:       "/",
			SessionStatePath: "/session-state",
			Conventions:      rpc.SessionFSSetProviderConventionsPosix,
			Capabilities:     &rpc.SessionFSSetProviderCapabilities{Sqlite: rpcPtr(true)},
		})
		if err != nil {
			t.Fatalf("SessionFS.SetProvider failed: %v", err)
		}
		if !result.Success {
			t.Fatalf("Expected SessionFS.SetProvider Success=true, got %+v", result)
		}
	})

	t.Run("should add secret filter values", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.Env = append(opts.Env, "COPILOT_ENABLE_SECRET_FILTERING=true")
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		secret := "rpc-secret-" + randomHex(t)
		result, err := client.RPC.Secrets.AddFilterValues(t.Context(), &rpc.SecretsAddFilterValuesRequest{Values: []string{secret}})
		if err != nil {
			t.Fatalf("Secrets.AddFilterValues failed: %v", err)
		}
		if !result.Ok {
			t.Fatalf("Expected AddFilterValues Ok=true, got %+v", result)
		}
	})

	t.Run("should list find and inspect persisted session state", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		token := "rpc-server-list-token-" + randomHex(t)
		registerProxyUser(t, ctx, token, "rpc-user", nil)
		client := newAuthenticatedClient(ctx, token)
		t.Cleanup(func() { client.ForceStop() })

		sessionID := uuid.NewString()
		workingDirectory := createUniqueRPCWorkDirectory(t, ctx, "server-rpc-list")
		missingSessionID := uuid.NewString()
		missingTaskID := "missing-task-" + randomHex(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			SessionID:           sessionID,
			WorkingDirectory:    workingDirectory,
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })
		if err := session.Log(t.Context(), "SERVER_RPC_LIST_READY", nil); err != nil {
			t.Fatalf("Log failed: %v", err)
		}

		saveSession(t, client, sessionID)

		metadataLimit := int64(0)
		filter := &rpc.SessionListFilter{Cwd: &workingDirectory}
		listed, err := client.RPC.Sessions.List(t.Context(), &rpc.SessionsListRequest{
			MetadataLimit: &metadataLimit,
			Filter:        filter,
		})
		if err != nil {
			t.Fatalf("Sessions.List failed: %v", err)
		}
		if listed.Sessions == nil {
			t.Fatal("Expected non-nil sessions list")
		}
		for _, metadata := range listed.Sessions {
			local, ok := metadata.(*rpc.LocalSessionMetadataValue)
			if ok && local.Context != nil {
				assertRPCPathEqual(t, workingDirectory, local.Context.Cwd)
			}
		}

		byPrefix, err := client.RPC.Sessions.FindByPrefix(t.Context(), &rpc.SessionsFindByPrefixRequest{Prefix: sessionID[:8]})
		if err != nil {
			t.Fatalf("Sessions.FindByPrefix failed: %v", err)
		}
		if byPrefix.SessionID != nil && *byPrefix.SessionID != sessionID {
			t.Fatalf("Expected prefix lookup to return %q or nil, got %q", sessionID, *byPrefix.SessionID)
		}

		byTask, err := client.RPC.Sessions.FindByTaskId(t.Context(), &rpc.SessionsFindByTaskIDRequest{TaskID: missingTaskID})
		if err != nil {
			t.Fatalf("Sessions.FindByTaskId failed: %v", err)
		}
		if byTask.SessionID != nil {
			t.Fatalf("Expected missing task ID lookup to return nil, got %q", *byTask.SessionID)
		}

		lastForContext, err := client.RPC.Sessions.GetLastForContext(t.Context(), &rpc.SessionsGetLastForContextRequest{
			Context: &rpc.SessionContext{Cwd: workingDirectory},
		})
		if err != nil {
			t.Fatalf("Sessions.GetLastForContext failed: %v", err)
		}
		if lastForContext.SessionID != nil && *lastForContext.SessionID != sessionID {
			t.Fatalf("Expected last session for context to be %q or nil, got %q", sessionID, *lastForContext.SessionID)
		}

		sizes, err := client.RPC.Sessions.GetSizes(t.Context())
		if err != nil {
			t.Fatalf("Sessions.GetSizes failed: %v", err)
		}
		if sizes.Sizes == nil {
			t.Fatal("Expected non-nil session sizes map")
		}
		if size, present := sizes.Sizes[sessionID]; present && size < 0 {
			t.Fatalf("Expected non-negative size for %q, got %d", sessionID, size)
		}

		inUse, err := client.RPC.Sessions.CheckInUse(t.Context(), &rpc.SessionsCheckInUseRequest{SessionIDs: []string{sessionID, missingSessionID}})
		if err != nil {
			t.Fatalf("Sessions.CheckInUse failed: %v", err)
		}
		if containsString(inUse.InUse, missingSessionID) {
			t.Fatalf("Did not expect missing session %q to be in use: %+v", missingSessionID, inUse.InUse)
		}

	})

	t.Run("should enrich basic session metadata", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		token := "rpc-server-enrich-token-" + randomHex(t)
		registerProxyUser(t, ctx, token, "rpc-user", nil)
		client := newAuthenticatedClient(ctx, token)
		t.Cleanup(func() { client.ForceStop() })

		sessionID := uuid.NewString()
		workingDirectory := createUniqueRPCWorkDirectory(t, ctx, "server-rpc-enrich")
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			SessionID:           sessionID,
			WorkingDirectory:    workingDirectory,
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })
		if err := session.Log(t.Context(), "SERVER_RPC_ENRICH_READY", nil); err != nil {
			t.Fatalf("Log failed: %v", err)
		}
		saveSession(t, client, sessionID)

		now := time.Now().UTC().Format(time.RFC3339Nano)
		result, err := client.RPC.Sessions.EnrichMetadata(t.Context(), &rpc.SessionsEnrichMetadataRequest{
			Sessions: []rpc.LocalSessionMetadataValue{{
				SessionID:    sessionID,
				StartTime:    now,
				ModifiedTime: now,
				IsRemote:     false,
				Name:         rpcPtr("Basic metadata"),
				Context:      &rpc.SessionContext{Cwd: workingDirectory},
			}},
		})
		if err != nil {
			t.Fatalf("Sessions.EnrichMetadata failed: %v", err)
		}
		if len(result.Sessions) != 1 {
			t.Fatalf("Expected one enriched session, got %+v", result.Sessions)
		}
		enriched := result.Sessions[0]
		if enriched.SessionID != sessionID {
			t.Fatalf("Expected enriched session ID %q, got %q", sessionID, enriched.SessionID)
		}
		if enriched.Context == nil {
			t.Fatal("Expected enriched context")
		}
		assertRPCPathEqual(t, workingDirectory, enriched.Context.Cwd)
		if enriched.IsRemote {
			t.Fatal("Expected local enriched session")
		}
	})

	t.Run("should close active session and release lock", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		token := "rpc-server-close-token-" + randomHex(t)
		registerProxyUser(t, ctx, token, "rpc-user", nil)
		client := newAuthenticatedClient(ctx, token)
		t.Cleanup(func() { client.ForceStop() })

		sessionID := uuid.NewString()
		workingDirectory := createUniqueRPCWorkDirectory(t, ctx, "server-rpc-close")
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			SessionID:           sessionID,
			WorkingDirectory:    workingDirectory,
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		if err := session.Log(t.Context(), "SERVER_RPC_CLOSE_READY", nil); err != nil {
			t.Fatalf("Log failed: %v", err)
		}
		saveSession(t, client, sessionID)

		if _, err := client.RPC.Sessions.Close(t.Context(), &rpc.SessionsCloseRequest{SessionID: sessionID}); err != nil {
			t.Fatalf("Sessions.Close failed: %v", err)
		}
		if _, err := client.RPC.Sessions.ReleaseLock(t.Context(), &rpc.SessionsReleaseLockRequest{SessionID: sessionID}); err != nil {
			t.Fatalf("Sessions.ReleaseLock failed: %v", err)
		}
		inUse, err := client.RPC.Sessions.CheckInUse(t.Context(), &rpc.SessionsCheckInUseRequest{SessionIDs: []string{sessionID}})
		if err != nil {
			t.Fatalf("Sessions.CheckInUse failed: %v", err)
		}
		if containsString(inUse.InUse, sessionID) {
			t.Fatalf("Expected %q not to be in use after close/release", sessionID)
		}
	})

	t.Run("should prune dry run and bulk delete persisted session", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		token := "rpc-server-delete-token-" + randomHex(t)
		registerProxyUser(t, ctx, token, "rpc-user", nil)
		client := newAuthenticatedClient(ctx, token)
		t.Cleanup(func() { client.ForceStop() })

		sessionID := uuid.NewString()
		missingSessionID := uuid.NewString()
		workingDirectory := createUniqueRPCWorkDirectory(t, ctx, "server-rpc-delete")
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			SessionID:           sessionID,
			WorkingDirectory:    workingDirectory,
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		if err := session.Log(t.Context(), "SERVER_RPC_DELETE_READY", nil); err != nil {
			t.Fatalf("Log failed: %v", err)
		}

		saveSession(t, client, sessionID)
		if _, err := client.RPC.Sessions.Close(t.Context(), &rpc.SessionsCloseRequest{SessionID: sessionID}); err != nil {
			t.Fatalf("Sessions.Close failed: %v", err)
		}

		prune, err := client.RPC.Sessions.PruneOld(t.Context(), &rpc.SessionsPruneOldRequest{
			OlderThanDays:     0,
			DryRun:            rpcPtr(true),
			IncludeNamed:      rpcPtr(true),
			ExcludeSessionIDs: []string{},
		})
		if err != nil {
			t.Fatalf("Sessions.PruneOld failed: %v", err)
		}
		if !prune.DryRun {
			t.Fatalf("Expected prune DryRun=true, got %+v", prune)
		}
		if containsString(prune.Deleted, sessionID) {
			t.Fatalf("Dry run should not delete %q", sessionID)
		}
		if prune.FreedBytes < 0 {
			t.Fatalf("Expected non-negative freed bytes, got %d", prune.FreedBytes)
		}

		deleted, err := client.RPC.Sessions.BulkDelete(t.Context(), &rpc.SessionsBulkDeleteRequest{
			SessionIDs: []string{sessionID, missingSessionID},
		})
		if err != nil {
			t.Fatalf("Sessions.BulkDelete failed: %v", err)
		}
		freed, present := deleted.FreedBytes[sessionID]
		if !present {
			t.Fatalf("Expected BulkDelete to include %q in freedBytes, got %+v", sessionID, deleted.FreedBytes)
		}
		if freed < 0 {
			t.Fatalf("Expected non-negative freed bytes for %q, got %d", sessionID, freed)
		}
		if missingFreed, present := deleted.FreedBytes[missingSessionID]; present && missingFreed != 0 {
			t.Fatalf("Expected missing session freed bytes to be 0 when present, got %d", missingFreed)
		}

		_ = session
	})

	t.Run("should set additional plugins and reload deferred hooks", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })
		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		if _, err := client.RPC.Sessions.SetAdditionalPlugins(t.Context(), &rpc.SessionsSetAdditionalPluginsRequest{Plugins: []rpc.InstalledPlugin{}}); err != nil {
			t.Fatalf("Sessions.SetAdditionalPlugins(clear) failed: %v", err)
		}
		t.Cleanup(func() {
			_, _ = client.RPC.Sessions.SetAdditionalPlugins(t.Context(), &rpc.SessionsSetAdditionalPluginsRequest{Plugins: []rpc.InstalledPlugin{}})
		})

		sessionID := uuid.NewString()
		workingDirectory := createUniqueRPCWorkDirectory(t, ctx, "server-rpc-hooks")
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			SessionID:             sessionID,
			WorkingDirectory:      workingDirectory,
			EnableConfigDiscovery: copilot.Bool(false),
			OnPermissionRequest:   copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		if _, err := client.RPC.Sessions.ReloadPluginHooks(t.Context(), &rpc.SessionsReloadPluginHooksRequest{
			SessionID:      sessionID,
			DeferRepoHooks: rpcPtr(true),
		}); err != nil {
			t.Fatalf("Sessions.ReloadPluginHooks failed: %v", err)
		}
		loaded, err := client.RPC.Sessions.LoadDeferredRepoHooks(t.Context(), &rpc.SessionsLoadDeferredRepoHooksRequest{SessionID: sessionID})
		if err != nil {
			t.Fatalf("Sessions.LoadDeferredRepoHooks failed: %v", err)
		}
		if loaded.StartupPrompts == nil {
			t.Fatal("Expected non-nil StartupPrompts")
		}
		if loaded.HookCount != 0 || len(loaded.StartupPrompts) != 0 {
			t.Fatalf("Expected no deferred hooks for isolated directory, got %+v", loaded)
		}
	})

	t.Run("should report implemented error when connecting unknown remote session", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })
		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		_, err := client.RPC.Sessions.Connect(t.Context(), &rpc.ConnectRemoteSessionParams{SessionID: "remote-" + randomHex(t)})
		if err == nil {
			t.Fatal("Expected Sessions.Connect to fail for an unknown remote session")
		}
		text := strings.ToLower(err.Error())
		if strings.Contains(text, "unhandled method sessions.connect") {
			t.Fatalf("Expected implemented error for sessions.connect, got %v", err)
		}
		if !strings.Contains(text, "session") {
			t.Fatalf("Expected remote connect error to mention session, got %v", err)
		}
	})

	t.Run("should discover server mcp and skills", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		skillName := fmt.Sprintf("server-rpc-skill-%s", randomHex(t))
		skillsDir := createMCPSkillsRPCDirectory(t, ctx.WorkDir, "server-rpc-skills", skillName, "Skill discovered by server-scoped RPC tests.")

		workingDir := ctx.WorkDir
		mcp, err := client.RPC.MCP.Discover(t.Context(), &rpc.MCPDiscoverRequest{WorkingDirectory: &workingDir})
		if err != nil {
			t.Fatalf("MCP.Discover failed: %v", err)
		}
		if mcp.Servers == nil {
			t.Errorf("Expected non-nil Servers")
		}

		skills, err := client.RPC.Skills.Discover(t.Context(), &rpc.SkillsDiscoverRequest{SkillDirectories: []string{skillsDir}})
		if err != nil {
			t.Fatalf("Skills.Discover failed: %v", err)
		}
		discovered := findServerSkill(skills.Skills, skillName)
		if discovered == nil {
			t.Fatalf("Expected to discover skill %q", skillName)
			return
		}
		if discovered.Description != "Skill discovered by server-scoped RPC tests." {
			t.Errorf("Expected description to match, got %q", discovered.Description)
		}
		if !discovered.Enabled {
			t.Errorf("Expected discovered skill to be Enabled")
		}
		expectedSuffix := filepath.Join(skillName, "SKILL.md")
		if discovered.Path == nil || !strings.HasSuffix(filepath.ToSlash(*discovered.Path), filepath.ToSlash(expectedSuffix)) {
			t.Errorf("Expected skill path to end with %q, got %v", expectedSuffix, discovered.Path)
		}

		// Disable the skill globally and re-discover.
		if _, err := client.RPC.Skills.Config().SetDisabledSkills(t.Context(), &rpc.SkillsConfigSetDisabledSkillsRequest{
			DisabledSkills: []string{skillName},
		}); err != nil {
			t.Fatalf("Skills.Config.SetDisabledSkills failed: %v", err)
		}
		t.Cleanup(func() {
			_, _ = client.RPC.Skills.Config().SetDisabledSkills(t.Context(), &rpc.SkillsConfigSetDisabledSkillsRequest{
				DisabledSkills: []string{},
			})
		})

		disabled, err := client.RPC.Skills.Discover(t.Context(), &rpc.SkillsDiscoverRequest{SkillDirectories: []string{skillsDir}})
		if err != nil {
			t.Fatalf("Skills.Discover (after disable) failed: %v", err)
		}
		disabledSkill := findServerSkill(disabled.Skills, skillName)
		if disabledSkill == nil {
			t.Fatalf("Expected to find skill %q after disable", skillName)
			return
		}
		if disabledSkill.Enabled {
			t.Errorf("Expected skill %q to be Enabled=false after global disable", skillName)
		}
	})
}

// newAuthenticatedClient builds a client that resolves auth through the test proxy.
func newAuthenticatedClient(ctx *testharness.TestContext, token string) *copilot.Client {
	return ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.Env = append(opts.Env, "COPILOT_DEBUG_GITHUB_API_URL="+ctx.ProxyURL)
		opts.GitHubToken = token
	})
}

// registerProxyUser configures the proxy with a fake CopilotUser response for the given token.
func registerProxyUser(t *testing.T, ctx *testharness.TestContext, token, login string, quotaSnapshots map[string]any) {
	t.Helper()
	user := map[string]any{
		"login":                 login,
		"copilot_plan":          "individual_pro",
		"endpoints":             map[string]any{"api": ctx.ProxyURL, "telemetry": "https://localhost:1/telemetry"},
		"analytics_tracking_id": login + "-tracking-id",
	}
	if quotaSnapshots != nil {
		user["quota_snapshots"] = quotaSnapshots
	}
	if err := ctx.SetCopilotUserByToken(token, user); err != nil {
		t.Fatalf("SetCopilotUserByToken failed: %v", err)
	}
}

func findServerSkill(skills []rpc.ServerSkill, name string) *rpc.ServerSkill {
	for i, skill := range skills {
		if skill.Name == name {
			return &skills[i]
		}
	}
	return nil
}

func saveSession(t *testing.T, client *copilot.Client, sessionID string) {
	t.Helper()
	if _, err := client.RPC.Sessions.Save(t.Context(), &rpc.SessionsSaveRequest{SessionID: sessionID}); err != nil {
		t.Fatalf("Sessions.Save failed: %v", err)
	}
}
