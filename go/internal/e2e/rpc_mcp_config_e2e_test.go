package e2e

import (
	"fmt"
	"testing"

	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors dotnet/test/RpcMcpConfigTests.cs (snapshot category "rpc_mcp_config").
// Tests server-scoped MCP configuration management via mcp.config.* RPCs.
func TestRpcMcpConfigE2E(t *testing.T) {
	t.Run("should call server mcp config rpcs", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })
		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		serverName := fmt.Sprintf("sdk-test-%s", randomHex(t))

		nodeCmd := "node"
		baseConfig := rpc.MCPServerConfig{
			Command: &nodeCmd,
			Args:    []string{"-v"},
		}
		updatedConfig := rpc.MCPServerConfig{
			Command: &nodeCmd,
			Args:    []string{"--version"},
		}

		initial, err := client.RPC.Mcp.Config().List(t.Context())
		if err != nil {
			t.Fatalf("Mcp.Config.List (initial) failed: %v", err)
		}
		if _, present := initial.Servers[serverName]; present {
			t.Fatalf("Did not expect %q to be present initially", serverName)
		}

		// Best-effort cleanup if a subtest assertion fails mid-flight.
		t.Cleanup(func() {
			_, _ = client.RPC.Mcp.Config().Remove(t.Context(), &rpc.MCPConfigRemoveRequest{Name: serverName})
		})

		if _, err := client.RPC.Mcp.Config().Add(t.Context(), &rpc.MCPConfigAddRequest{
			Name:   serverName,
			Config: baseConfig,
		}); err != nil {
			t.Fatalf("Mcp.Config.Add failed: %v", err)
		}

		afterAdd, err := client.RPC.Mcp.Config().List(t.Context())
		if err != nil {
			t.Fatalf("Mcp.Config.List (after add) failed: %v", err)
		}
		if _, present := afterAdd.Servers[serverName]; !present {
			t.Fatalf("Expected %q to be present after Add", serverName)
		}

		if _, err := client.RPC.Mcp.Config().Update(t.Context(), &rpc.MCPConfigUpdateRequest{
			Name:   serverName,
			Config: updatedConfig,
		}); err != nil {
			t.Fatalf("Mcp.Config.Update failed: %v", err)
		}

		afterUpdate, err := client.RPC.Mcp.Config().List(t.Context())
		if err != nil {
			t.Fatalf("Mcp.Config.List (after update) failed: %v", err)
		}
		updated, present := afterUpdate.Servers[serverName]
		if !present {
			t.Fatalf("Expected %q to still be present after Update", serverName)
		}
		if updated.Command == nil || *updated.Command != "node" {
			t.Errorf("Expected command='node', got %v", updated.Command)
		}
		if len(updated.Args) == 0 || updated.Args[0] != "--version" {
			t.Errorf("Expected args[0]='--version', got %v", updated.Args)
		}

		if _, err := client.RPC.Mcp.Config().Disable(t.Context(), &rpc.MCPConfigDisableRequest{Names: []string{serverName}}); err != nil {
			t.Fatalf("Mcp.Config.Disable failed: %v", err)
		}
		if _, err := client.RPC.Mcp.Config().Enable(t.Context(), &rpc.MCPConfigEnableRequest{Names: []string{serverName}}); err != nil {
			t.Fatalf("Mcp.Config.Enable failed: %v", err)
		}

		if _, err := client.RPC.Mcp.Config().Remove(t.Context(), &rpc.MCPConfigRemoveRequest{Name: serverName}); err != nil {
			t.Fatalf("Mcp.Config.Remove failed: %v", err)
		}

		afterRemove, err := client.RPC.Mcp.Config().List(t.Context())
		if err != nil {
			t.Fatalf("Mcp.Config.List (after remove) failed: %v", err)
		}
		if _, present := afterRemove.Servers[serverName]; present {
			t.Errorf("Expected %q to be removed", serverName)
		}
	})

	t.Run("should round trip http mcp oauth config rpc", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })
		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		serverName := fmt.Sprintf("sdk-http-oauth-%s", randomHex(t))

		httpType := rpc.MCPServerConfigTypeHTTP
		urlBase := "https://example.com/mcp"
		urlUpdated := "https://example.com/updated-mcp"
		clientID := "client-id"
		clientIDUpdated := "updated-client-id"
		grantClientCreds := rpc.MCPServerConfigHTTPOauthGrantTypeClientCredentials
		grantAuthCode := rpc.MCPServerConfigHTTPOauthGrantTypeAuthorizationCode
		var publicFalse = false
		var publicTrue = true
		var timeoutBase int64 = 3000
		var timeoutUpdated int64 = 4000

		baseConfig := rpc.MCPServerConfig{
			Type:              &httpType,
			URL:               &urlBase,
			Headers:           map[string]string{"Authorization": "Bearer token"},
			OauthClientID:     &clientID,
			OauthPublicClient: &publicFalse,
			OauthGrantType:    &grantClientCreds,
			Tools:             []string{"*"},
			Timeout:           &timeoutBase,
		}
		updatedConfig := rpc.MCPServerConfig{
			Type:              &httpType,
			URL:               &urlUpdated,
			OauthClientID:     &clientIDUpdated,
			OauthPublicClient: &publicTrue,
			OauthGrantType:    &grantAuthCode,
			Tools:             []string{"updated-tool"},
			Timeout:           &timeoutUpdated,
		}

		t.Cleanup(func() {
			_, _ = client.RPC.Mcp.Config().Remove(t.Context(), &rpc.MCPConfigRemoveRequest{Name: serverName})
		})

		if _, err := client.RPC.Mcp.Config().Add(t.Context(), &rpc.MCPConfigAddRequest{
			Name:   serverName,
			Config: baseConfig,
		}); err != nil {
			t.Fatalf("Mcp.Config.Add failed: %v", err)
		}

		afterAdd, err := client.RPC.Mcp.Config().List(t.Context())
		if err != nil {
			t.Fatalf("Mcp.Config.List (after add) failed: %v", err)
		}
		added, present := afterAdd.Servers[serverName]
		if !present {
			t.Fatalf("Expected %q to be present after Add", serverName)
		}
		if added.Type == nil || *added.Type != "http" {
			t.Errorf("Expected type='http', got %v", added.Type)
		}
		if added.URL == nil || *added.URL != "https://example.com/mcp" {
			t.Errorf("Expected url='https://example.com/mcp', got %v", added.URL)
		}
		if got := added.Headers["Authorization"]; got != "Bearer token" {
			t.Errorf("Expected Authorization='Bearer token', got %q", got)
		}
		if added.OauthClientID == nil || *added.OauthClientID != "client-id" {
			t.Errorf("Expected oauthClientId='client-id', got %v", added.OauthClientID)
		}
		if added.OauthPublicClient == nil || *added.OauthPublicClient {
			t.Errorf("Expected oauthPublicClient=false, got %v", added.OauthPublicClient)
		}
		if added.OauthGrantType == nil || *added.OauthGrantType != "client_credentials" {
			t.Errorf("Expected oauthGrantType='client_credentials', got %v", added.OauthGrantType)
		}

		if _, err := client.RPC.Mcp.Config().Update(t.Context(), &rpc.MCPConfigUpdateRequest{
			Name:   serverName,
			Config: updatedConfig,
		}); err != nil {
			t.Fatalf("Mcp.Config.Update failed: %v", err)
		}
		afterUpdate, err := client.RPC.Mcp.Config().List(t.Context())
		if err != nil {
			t.Fatalf("Mcp.Config.List (after update) failed: %v", err)
		}
		updated, present := afterUpdate.Servers[serverName]
		if !present {
			t.Fatalf("Expected %q to still be present after Update", serverName)
		}
		if updated.URL == nil || *updated.URL != "https://example.com/updated-mcp" {
			t.Errorf("Expected url='https://example.com/updated-mcp', got %v", updated.URL)
		}
		if updated.OauthClientID == nil || *updated.OauthClientID != "updated-client-id" {
			t.Errorf("Expected oauthClientId='updated-client-id', got %v", updated.OauthClientID)
		}
		if updated.OauthPublicClient == nil || !*updated.OauthPublicClient {
			t.Errorf("Expected oauthPublicClient=true, got %v", updated.OauthPublicClient)
		}
		if updated.OauthGrantType == nil || *updated.OauthGrantType != "authorization_code" {
			t.Errorf("Expected oauthGrantType='authorization_code', got %v", updated.OauthGrantType)
		}
		if len(updated.Tools) == 0 || updated.Tools[0] != "updated-tool" {
			t.Errorf("Expected tools[0]='updated-tool', got %v", updated.Tools)
		}
		if updated.Timeout == nil || *updated.Timeout != 4000 {
			t.Errorf("Expected timeout=4000, got %v", updated.Timeout)
		}

		if _, err := client.RPC.Mcp.Config().Remove(t.Context(), &rpc.MCPConfigRemoveRequest{Name: serverName}); err != nil {
			t.Fatalf("Mcp.Config.Remove failed: %v", err)
		}

		afterRemove, err := client.RPC.Mcp.Config().List(t.Context())
		if err != nil {
			t.Fatalf("Mcp.Config.List (after remove) failed: %v", err)
		}
		if _, present := afterRemove.Servers[serverName]; present {
			t.Errorf("Expected %q to be removed", serverName)
		}
	})
}
