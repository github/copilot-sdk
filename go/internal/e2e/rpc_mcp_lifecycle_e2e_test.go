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
