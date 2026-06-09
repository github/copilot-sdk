package e2e

import (
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestRpcMcpLifecycle(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should_list_tools_and_report_running_status_for_connected_server", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		const serverName = "rpc-lifecycle-list-server"
		session := createPortedSession(t, client, &copilot.SessionConfig{MCPServers: testMCPServers(t, serverName)})
		defer session.Disconnect()
		waitForPortedMCPServerStatus(t, session, serverName, rpc.MCPServerStatusConnected)

		tools, err := session.RPC.MCP.ListTools(t.Context(), &rpc.MCPListToolsRequest{ServerName: serverName})
		if err != nil {
			t.Fatalf("MCP.ListTools failed: %v", err)
		}
		if len(tools.Tools) == 0 {
			t.Fatal("Expected connected MCP server to expose at least one tool")
		}
		for _, tool := range tools.Tools {
			if strings.TrimSpace(tool.Name) == "" {
				t.Fatalf("Expected non-empty MCP tool name, got %+v", tool)
			}
		}

		running, err := session.RPC.MCP.IsServerRunning(t.Context(), &rpc.MCPIsServerRunningRequest{ServerName: serverName})
		if err != nil {
			t.Fatalf("MCP.IsServerRunning(%s) failed: %v", serverName, err)
		}
		if !running.Running {
			t.Fatalf("Expected %s to be running", serverName)
		}
		missing, err := session.RPC.MCP.IsServerRunning(t.Context(), &rpc.MCPIsServerRunningRequest{ServerName: "missing-" + randomHex(t)})
		if err != nil {
			t.Fatalf("MCP.IsServerRunning(missing) failed: %v", err)
		}
		if missing.Running {
			t.Fatal("Expected missing MCP server not to be running")
		}
	})

	t.Run("should_throw_when_listing_tools_for_unconnected_server", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		const serverName = "rpc-lifecycle-unconnected-host"
		session := createPortedSession(t, client, &copilot.SessionConfig{MCPServers: testMCPServers(t, serverName)})
		defer session.Disconnect()
		waitForPortedMCPServerStatus(t, session, serverName, rpc.MCPServerStatusConnected)

		_, err := session.RPC.MCP.ListTools(t.Context(), &rpc.MCPListToolsRequest{ServerName: "missing-" + randomHex(t)})
		if err == nil {
			t.Fatal("Expected MCP.ListTools for an unconnected server to fail")
		}
		message := err.Error()
		assertPortedNoUnhandledMethod(t, message)
		assertPortedContainsFold(t, message, "not connected")
	})

	t.Run("should_stop_running_mcp_server", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		const serverName = "rpc-lifecycle-stop-server"
		session := createPortedSession(t, client, &copilot.SessionConfig{MCPServers: testMCPServers(t, serverName)})
		defer session.Disconnect()
		waitForPortedMCPServerStatus(t, session, serverName, rpc.MCPServerStatusConnected)
		waitForPortedMCPRunning(t, session, serverName, true)

		if _, err := session.RPC.MCP.StopServer(t.Context(), &rpc.MCPStopServerRequest{ServerName: serverName}); err != nil {
			t.Fatalf("MCP.StopServer failed: %v", err)
		}
		waitForPortedMCPRunning(t, session, serverName, false)
	})

	t.Run("should_start_and_restart_mcp_server", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		const hostServer = "rpc-lifecycle-host-server"
		session := createPortedSession(t, client, &copilot.SessionConfig{MCPServers: testMCPServers(t, hostServer)})
		defer session.Disconnect()
		waitForPortedMCPServerStatus(t, session, hostServer, rpc.MCPServerStatusConnected)

		const startedServer = "rpc-lifecycle-started-server"
		config := testMCPServers(t, startedServer)[startedServer]
		if _, err := session.RPC.MCP.StartServer(t.Context(), &rpc.MCPStartServerRequest{ServerName: startedServer, Config: config}); err != nil {
			t.Fatalf("MCP.StartServer failed: %v", err)
		}
		waitForPortedMCPRunning(t, session, startedServer, true)

		tools, err := session.RPC.MCP.ListTools(t.Context(), &rpc.MCPListToolsRequest{ServerName: startedServer})
		if err != nil {
			t.Fatalf("MCP.ListTools(started) failed: %v", err)
		}
		if len(tools.Tools) == 0 {
			t.Fatal("Expected started MCP server to expose tools")
		}

		if _, err := session.RPC.MCP.RestartServer(t.Context(), &rpc.MCPRestartServerRequest{ServerName: startedServer, Config: config}); err != nil {
			t.Fatalf("MCP.RestartServer failed: %v", err)
		}
		waitForPortedMCPRunning(t, session, startedServer, true)
	})

	t.Run("should_register_and_unregister_external_mcp_client", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		const hostServer = "rpc-lifecycle-extclient-host"
		session := createPortedSession(t, client, &copilot.SessionConfig{MCPServers: testMCPServers(t, hostServer)})
		defer session.Disconnect()
		waitForPortedMCPServerStatus(t, session, hostServer, rpc.MCPServerStatusConnected)

		const externalName = "rpc-lifecycle-external-client"
		initial, err := session.RPC.MCP.IsServerRunning(t.Context(), &rpc.MCPIsServerRunningRequest{ServerName: externalName})
		if err != nil {
			t.Fatalf("MCP.IsServerRunning(initial external) failed: %v", err)
		}
		if initial.Running {
			t.Fatal("Expected external client to start as not running")
		}

		if _, err := session.RPC.MCP.RegisterExternalClient(t.Context(), &rpc.MCPRegisterExternalClientRequest{
			ServerName: externalName,
			Client:     map[string]any{"id": externalName},
			Transport:  map[string]any{"kind": "in-process"},
			Config:     map[string]any{"command": "noop"},
		}); err != nil {
			t.Fatalf("MCP.RegisterExternalClient failed: %v", err)
		}
		waitForPortedMCPRunning(t, session, externalName, true)

		if _, err := session.RPC.MCP.UnregisterExternalClient(t.Context(), &rpc.MCPUnregisterExternalClientRequest{ServerName: externalName}); err != nil {
			t.Fatalf("MCP.UnregisterExternalClient failed: %v", err)
		}
		waitForPortedMCPRunning(t, session, externalName, false)
	})

	t.Run("should_reload_mcp_servers_with_config", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		const hostServer = "rpc-lifecycle-reload-host"
		session := createPortedSession(t, client, &copilot.SessionConfig{MCPServers: testMCPServers(t, hostServer)})
		defer session.Disconnect()
		waitForPortedMCPServerStatus(t, session, hostServer, rpc.MCPServerStatusConnected)

		result, err := session.RPC.MCP.ReloadWithConfig(t.Context(), &rpc.MCPReloadWithConfigRequest{Config: map[string]any{
			"mcpServers":      map[string]any{},
			"disabledServers": []string{},
		}})
		if err != nil {
			t.Fatalf("MCP.ReloadWithConfig failed: %v", err)
		}
		if result == nil {
			t.Fatal("Expected non-nil reload result")
		}
		if result.FilteredServers == nil {
			t.Fatal("Expected non-nil FilteredServers")
		}
		if len(result.FilteredServers) != 0 {
			t.Fatalf("Expected no filtered servers, got %+v", result.FilteredServers)
		}
	})

	t.Run("should_configure_github_mcp_server", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		const hostServer = "rpc-lifecycle-configure-host"
		session := createPortedSession(t, client, &copilot.SessionConfig{MCPServers: testMCPServers(t, hostServer)})
		defer session.Disconnect()
		waitForPortedMCPServerStatus(t, session, hostServer, rpc.MCPServerStatusConnected)

		result, err := session.RPC.MCP.ConfigureGitHub(t.Context(), &rpc.MCPConfigureGitHubRequest{AuthInfo: map[string]any{"type": "api-key"}})
		if err != nil {
			t.Fatalf("MCP.ConfigureGitHub failed: %v", err)
		}
		if result == nil {
			t.Fatal("Expected non-nil configure result")
		}
		if result.Changed {
			t.Fatal("Expected Changed=false")
		}
	})

	t.Run("should_respond_to_mcp_oauth_request_without_pending_request", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		const hostServer = "rpc-lifecycle-oauth-host"
		session := createPortedSession(t, client, &copilot.SessionConfig{MCPServers: testMCPServers(t, hostServer)})
		defer session.Disconnect()
		waitForPortedMCPServerStatus(t, session, hostServer, rpc.MCPServerStatusConnected)

		result, err := session.RPC.MCP.Oauth().Respond(t.Context(), &rpc.MCPOauthRespondRequest{RequestID: "missing-" + randomHex(t)})
		if err != nil {
			t.Fatalf("MCP.Oauth.Respond failed: %v", err)
		}
		if result == nil {
			t.Fatal("Expected non-nil OAuth respond result")
		}
	})
}

func waitForPortedMCPServerStatus(t *testing.T, session *copilot.Session, serverName string, expectedStatus rpc.MCPServerStatus) {
	t.Helper()
	waitForRPCCondition(t, 60*time.Second, serverName+" reaching "+string(expectedStatus), func() (bool, error) {
		result, err := session.RPC.MCP.List(t.Context())
		if err != nil {
			return false, err
		}
		for _, server := range result.Servers {
			if server.Name == serverName {
				return server.Status == expectedStatus, nil
			}
		}
		return false, nil
	})
}

func waitForPortedMCPRunning(t *testing.T, session *copilot.Session, serverName string, expectedRunning bool) {
	t.Helper()
	waitForRPCCondition(t, 60*time.Second, serverName+" running state", func() (bool, error) {
		result, err := session.RPC.MCP.IsServerRunning(t.Context(), &rpc.MCPIsServerRunningRequest{ServerName: serverName})
		if err != nil {
			return false, err
		}
		return result.Running == expectedRunning, nil
	})
}
