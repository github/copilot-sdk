package e2e

import (
	"path/filepath"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/rpc"
)

func testMCPServers(t *testing.T, serverNames ...string) map[string]copilot.MCPServerConfig {
	t.Helper()

	mcpServerPath, err := filepath.Abs("../../../test/harness/test-mcp-server.mjs")
	if err != nil {
		t.Fatalf("Failed to resolve test-mcp-server path: %v", err)
	}

	mcpServerDir := filepath.Dir(mcpServerPath)
	mcpServers := make(map[string]copilot.MCPServerConfig, len(serverNames))
	for _, serverName := range serverNames {
		mcpServers[serverName] = copilot.MCPStdioServerConfig{
			Command:          "node",
			Args:             []string{mcpServerPath},
			Tools:            &[]string{"*"},
			WorkingDirectory: mcpServerDir,
		}
	}
	return mcpServers
}

func waitForMCPServerStatus(t *testing.T, session *copilot.Session, serverName string, expectedStatus rpc.MCPServerStatus) {
	t.Helper()

	var lastStatus string
	deadline := time.Now().Add(60 * time.Second)
	for time.Now().Before(deadline) {
		result, err := session.RPC.MCP.List(t.Context())
		if err != nil {
			lastStatus = err.Error()
		} else {
			lastStatus = "<not listed>"
			for _, server := range result.Servers {
				if server.Name != serverName {
					continue
				}
				if server.Status == expectedStatus {
					return
				}
				lastStatus = string(server.Status)
				break
			}
		}
		time.Sleep(200 * time.Millisecond)
	}

	t.Fatalf("%s did not reach %s; last status was %s", serverName, expectedStatus, lastStatus)
}
