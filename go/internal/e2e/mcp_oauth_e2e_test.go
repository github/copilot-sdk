package e2e

import (
	"bufio"
	"encoding/json"
	"net/http"
	"os"
	"os/exec"
	"slices"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

const expectedMCPOAuthToken = "sdk-host-token"
const refreshMCPOAuthToken = expectedMCPOAuthToken + "-refresh"
const upscopeMCPOAuthToken = expectedMCPOAuthToken + "-upscope"
const reauthMCPOAuthToken = expectedMCPOAuthToken + "-reauth"

func TestMCPOAuthE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("satisfy MCP OAuth using host-provided token", func(t *testing.T) {
		baseURL := startOAuthMCPServer(t)
		serverName := "oauth-protected-mcp"
		tokenType := "Bearer"
		expiresIn := int64(3600)
		var observedRequest copilot.MCPAuthRequest

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnMCPAuthRequest: func(request copilot.MCPAuthRequest, _ copilot.MCPAuthInvocation) (*copilot.MCPAuthResult, error) {
				observedRequest = request
				return copilot.MCPAuthResultToken(&copilot.MCPAuthToken{
					AccessToken: expectedMCPOAuthToken,
					TokenType:   &tokenType,
					ExpiresIn:   &expiresIn,
				}), nil
			},
			MCPServers: map[string]copilot.MCPServerConfig{
				serverName: copilot.MCPHTTPServerConfig{
					URL:   baseURL + "/mcp",
					Tools: []string{"*"},
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { session.Disconnect() })

		waitForMCPServerStatus(t, session, serverName, rpc.MCPServerStatusConnected)
		tools, err := session.RPC.MCP.ListTools(t.Context(), &rpc.MCPListToolsRequest{ServerName: serverName})
		if err != nil {
			t.Fatalf("Failed to list MCP tools: %v", err)
		}
		if len(tools.Tools) != 1 || tools.Tools[0].Name != "whoami" {
			t.Fatalf("Expected whoami tool, got %#v", tools.Tools)
		}

		if observedRequest.ServerName != serverName {
			t.Fatalf("Expected serverName %q, got %q", serverName, observedRequest.ServerName)
		}
		if observedRequest.ServerURL != baseURL+"/mcp" {
			t.Fatalf("Expected serverUrl %q, got %q", baseURL+"/mcp", observedRequest.ServerURL)
		}
		if observedRequest.WwwAuthenticateParams == nil {
			t.Fatal("Expected WWW-Authenticate params")
		}
		if observedRequest.Reason != "initial" {
			t.Fatalf("Unexpected auth request reason: %q", observedRequest.Reason)
		}
		if observedRequest.WwwAuthenticateParams.ResourceMetadataURL == nil ||
			*observedRequest.WwwAuthenticateParams.ResourceMetadataURL != baseURL+"/.well-known/oauth-protected-resource" {
			t.Fatalf("Unexpected resource metadata URL: %v", observedRequest.WwwAuthenticateParams.ResourceMetadataURL)
		}
		if stringValue(observedRequest.WwwAuthenticateParams.Scope) != "mcp.read" || stringValue(observedRequest.WwwAuthenticateParams.Error) != "invalid_token" {
			t.Fatalf("Unexpected WWW-Authenticate params: %#v", observedRequest.WwwAuthenticateParams)
		}

		var metadata map[string]any
		if observedRequest.ResourceMetadata == nil {
			t.Fatal("Expected resource metadata to be propagated")
		}
		if err := json.Unmarshal([]byte(*observedRequest.ResourceMetadata), &metadata); err != nil {
			t.Fatalf("Failed to parse resource metadata: %v", err)
		}
		if metadata["resource"] != baseURL+"/mcp" {
			t.Fatalf("Expected resource %q, got %#v", baseURL+"/mcp", metadata["resource"])
		}

		requests := fetchOAuthMCPRequests(t, baseURL)
		if !hasAuthorization(requests, "") {
			t.Fatal("Expected at least one unauthenticated MCP request")
		}
		if !hasAuthorization(requests, "Bearer "+expectedMCPOAuthToken) {
			t.Fatal("Expected at least one MCP request with host-provided token")
		}
	})

	t.Run("request replacement tokens across MCP OAuth lifecycle", func(t *testing.T) {
		baseURL := startOAuthMCPServer(t)
		serverName := "oauth-lifecycle-mcp"
		var mu sync.Mutex
		var observedReasons []copilot.MCPOauthRequestReason
		refreshCount := 0

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			EnableMCPApps:       true,
			OnMCPAuthRequest: func(request copilot.MCPAuthRequest, _ copilot.MCPAuthInvocation) (*copilot.MCPAuthResult, error) {
				mu.Lock()
				observedReasons = append(observedReasons, request.Reason)
				refreshOrdinal := 0
				if request.Reason == copilot.MCPOauthRequestReasonRefresh {
					refreshCount++
					refreshOrdinal = refreshCount
				}
				mu.Unlock()

				token := expectedMCPOAuthToken
				switch request.Reason {
				case copilot.MCPOauthRequestReasonRefresh:
					if request.WwwAuthenticateParams == nil ||
						request.WwwAuthenticateParams.ResourceMetadataURL != nil ||
						stringValue(request.WwwAuthenticateParams.Error) != "invalid_token" {
						t.Fatalf("Unexpected refresh WWW-Authenticate params: %#v", request.WwwAuthenticateParams)
					}
					if refreshOrdinal > 1 {
						return copilot.MCPAuthResultCancelled(), nil
					}
					token = refreshMCPOAuthToken
				case copilot.MCPOauthRequestReasonUpscope:
					token = upscopeMCPOAuthToken
					if request.WwwAuthenticateParams == nil ||
						request.WwwAuthenticateParams.ResourceMetadataURL == nil ||
						*request.WwwAuthenticateParams.ResourceMetadataURL != baseURL+"/.well-known/oauth-protected-resource" ||
						stringValue(request.WwwAuthenticateParams.Scope) != "mcp.write" ||
						stringValue(request.WwwAuthenticateParams.Error) != "insufficient_scope" {
						t.Fatalf("Unexpected upscope WWW-Authenticate params: %#v", request.WwwAuthenticateParams)
					}
				case copilot.MCPOauthRequestReasonReauth:
					token = reauthMCPOAuthToken
				}
				return copilot.MCPAuthResultToken(&copilot.MCPAuthToken{AccessToken: token}), nil
			},
			MCPServers: map[string]copilot.MCPServerConfig{
				serverName: copilot.MCPHTTPServerConfig{
					URL:   baseURL + "/mcp",
					Tools: []string{"*"},
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { session.Disconnect() })

		waitForMCPServerStatus(t, session, serverName, rpc.MCPServerStatusConnected)
		callWhoami(t, session, serverName, "refresh")
		callWhoami(t, session, serverName, "upscope")
		callWhoami(t, session, serverName, "reauth")

		mu.Lock()
		reasons := append([]copilot.MCPOauthRequestReason(nil), observedReasons...)
		mu.Unlock()
		expectedReasons := []copilot.MCPOauthRequestReason{
			copilot.MCPOauthRequestReasonInitial,
			copilot.MCPOauthRequestReasonRefresh,
			copilot.MCPOauthRequestReasonUpscope,
			copilot.MCPOauthRequestReasonRefresh,
			copilot.MCPOauthRequestReasonReauth,
		}
		if !slices.Equal(reasons, expectedReasons) {
			t.Fatalf("Unexpected auth request reasons: %#v", reasons)
		}

		requests := fetchOAuthMCPRequests(t, baseURL)
		if !hasAuthorization(requests, "Bearer "+refreshMCPOAuthToken) {
			t.Fatal("Expected at least one MCP request with refresh token")
		}
		if !hasAuthorization(requests, "Bearer "+upscopeMCPOAuthToken) {
			t.Fatal("Expected at least one MCP request with upscope token")
		}
		if !hasAuthorization(requests, "Bearer "+reauthMCPOAuthToken) {
			t.Fatal("Expected at least one MCP request with reauth token")
		}
	})

	t.Run("cancel pending MCP OAuth request", func(t *testing.T) {
		baseURL := startOAuthMCPServer(t)
		serverName := "oauth-cancelled-mcp"
		var mu sync.Mutex
		var observedRequest copilot.MCPAuthRequest
		var observed bool

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnMCPAuthRequest: func(request copilot.MCPAuthRequest, _ copilot.MCPAuthInvocation) (*copilot.MCPAuthResult, error) {
				mu.Lock()
				observedRequest = request
				observed = true
				mu.Unlock()
				return copilot.MCPAuthResultCancelled(), nil
			},
			MCPServers: map[string]copilot.MCPServerConfig{
				serverName: copilot.MCPHTTPServerConfig{
					URL:   baseURL + "/mcp",
					Tools: []string{"*"},
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { session.Disconnect() })

		waitForMCPServerStatus(t, session, serverName, rpc.MCPServerStatusNeedsAuth)

		// The MCP connection is kicked off by session.create, but the SDK only registers its
		// `mcp.oauth_required` event interest once create returns. If the server's initial 401
		// wins that race, the runtime records `needs-auth` WITHOUT invoking the host callback,
		// so `observedRequest` is briefly unset even after `needs-auth` is observed. A later
		// auth retry (now that interest is registered) invokes the callback with the same
		// `Initial` reason. Wait for the callback rather than sampling it the instant
		// `needs-auth` first appears, which is what made this test flaky.
		var request copilot.MCPAuthRequest
		deadline := time.Now().Add(60 * time.Second)
		for {
			mu.Lock()
			got := observed
			request = observedRequest
			mu.Unlock()
			if got {
				break
			}
			if time.Now().After(deadline) {
				t.Fatalf("%s OAuth request did not reach the host callback", serverName)
			}
			time.Sleep(200 * time.Millisecond)
		}

		if request.ServerName != serverName {
			t.Fatalf("Expected serverName %q, got %q", serverName, request.ServerName)
		}
		if request.Reason != copilot.MCPOauthRequestReasonInitial {
			t.Fatalf("Unexpected auth request reason: %q", request.Reason)
		}
	})

	t.Run("resolve pending MCP OAuth request through RPC", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureWithoutSnapshot(t)
		client := ctx.NewClient()
		defer client.ForceStop()

		baseURL := startOAuthMCPServer(t)
		serverName := "oauth-direct-rpc-mcp"
		requests := make(chan copilot.MCPAuthRequest, 1)
		releaseHandler := make(chan struct{})

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			EnableMCPApps:       true,
			OnMCPAuthRequest: func(request copilot.MCPAuthRequest, _ copilot.MCPAuthInvocation) (*copilot.MCPAuthResult, error) {
				requests <- request
				<-releaseHandler
				return copilot.MCPAuthResultCancelled(), nil
			},
			MCPServers: map[string]copilot.MCPServerConfig{
				serverName: copilot.MCPHTTPServerConfig{
					URL:   baseURL + "/mcp",
					Tools: []string{"*"},
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { session.Disconnect() })

		connected := make(chan error, 1)
		go func() {
			connected <- waitForMCPServerStatusResult(t.Context(), session, serverName, rpc.MCPServerStatusConnected, 60*time.Second)
		}()

		var request copilot.MCPAuthRequest
		select {
		case request = <-requests:
		case <-time.After(30 * time.Second):
			t.Fatal("Timed out waiting for MCP OAuth request")
		}

		tokenType := "Bearer"
		expiresIn := int64(3600)
		result, err := session.RPC.MCP.Oauth().HandlePendingRequest(t.Context(), &rpc.MCPOauthHandlePendingRequest{
			RequestID: request.RequestID,
			Result: rpc.MCPOauthPendingRequestResponseToken{
				AccessToken: expectedMCPOAuthToken,
				TokenType:   &tokenType,
				ExpiresIn:   &expiresIn,
			},
		})
		if err != nil {
			close(releaseHandler)
			t.Fatalf("HandlePendingRequest failed: %v", err)
		}
		close(releaseHandler)
		if !result.Success {
			t.Fatal("Expected direct MCP OAuth pending request resolution to succeed")
		}

		if err := <-connected; err != nil {
			t.Fatal(err)
		}
		requestLog := fetchOAuthMCPRequests(t, baseURL)
		if !hasAuthorization(requestLog, "Bearer "+expectedMCPOAuthToken) {
			t.Fatal("Expected MCP request with token supplied through direct RPC")
		}
	})
}

type oauthMCPRequest struct {
	Authorization *string `json:"authorization"`
}

func startOAuthMCPServer(t *testing.T) string {
	t.Helper()

	serverPath := testharness.RepoPath("test", "harness", "test-mcp-oauth-server.mjs")
	cmd := exec.Command("node", serverPath)
	cmd.Env = append(os.Environ(), "EXPECTED_TOKEN="+expectedMCPOAuthToken)
	stdout, err := cmd.StdoutPipe()
	if err != nil {
		t.Fatalf("Failed to pipe OAuth MCP server stdout: %v", err)
	}
	var stderr syncBuffer
	cmd.Stderr = &stderr
	if err := cmd.Start(); err != nil {
		t.Fatalf("Failed to start OAuth MCP server: %v", err)
	}
	t.Cleanup(func() {
		if cmd.ProcessState != nil && cmd.ProcessState.Exited() {
			return
		}
		_ = cmd.Process.Kill()
		_, _ = cmd.Process.Wait()
	})

	lines := make(chan string, 1)
	go func() {
		scanner := bufio.NewScanner(stdout)
		for scanner.Scan() {
			lines <- scanner.Text()
			return
		}
		close(lines)
	}()

	select {
	case line, ok := <-lines:
		if !ok {
			t.Fatalf("OAuth MCP server exited before listening: %s", stderr.String())
		}
		const prefix = "Listening: "
		if !strings.HasPrefix(line, prefix) {
			t.Fatalf("Unexpected OAuth MCP server startup line %q. stderr=%s", line, stderr.String())
		}
		return strings.TrimPrefix(line, prefix)
	case <-time.After(10 * time.Second):
		t.Fatalf("Timed out waiting for OAuth MCP server: %s", stderr.String())
	}
	return ""
}

func stringValue(value *string) string {
	if value == nil {
		return ""
	}
	return *value
}

// syncBuffer is a minimal io.Writer whose contents can be read concurrently.
// os/exec writes to cmd.Stderr on a separate goroutine, so reading a plain
// strings.Builder while the process is running is a data race (caught by -race).
type syncBuffer struct {
	mu  sync.Mutex
	buf strings.Builder
}

func (b *syncBuffer) Write(p []byte) (int, error) {
	b.mu.Lock()
	defer b.mu.Unlock()
	return b.buf.Write(p)
}

func (b *syncBuffer) String() string {
	b.mu.Lock()
	defer b.mu.Unlock()
	return b.buf.String()
}

func fetchOAuthMCPRequests(t *testing.T, baseURL string) []oauthMCPRequest {
	t.Helper()

	response, err := http.Get(baseURL + "/__requests")
	if err != nil {
		t.Fatalf("Failed to fetch OAuth MCP requests: %v", err)
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		t.Fatalf("Failed to fetch OAuth MCP requests: %s", response.Status)
	}
	var requests []oauthMCPRequest
	if err := json.NewDecoder(response.Body).Decode(&requests); err != nil {
		t.Fatalf("Failed to decode OAuth MCP requests: %v", err)
	}
	return requests
}

func hasAuthorization(requests []oauthMCPRequest, expected string) bool {
	for _, request := range requests {
		if request.Authorization == nil && expected == "" {
			return true
		}
		if request.Authorization != nil && *request.Authorization == expected {
			return true
		}
	}
	return false
}

func callWhoami(t *testing.T, session *copilot.Session, serverName string, scenario string) {
	t.Helper()

	result, err := session.RPC.MCP.Apps().CallTool(t.Context(), &rpc.MCPAppsCallToolRequest{
		OriginServerName: serverName,
		ServerName:       serverName,
		ToolName:         "whoami",
		Arguments:        map[string]any{"scenario": scenario},
	})
	if err != nil {
		t.Fatalf("Failed to call whoami for %s: %v", scenario, err)
	}
	content, ok := (*result)["content"].([]any)
	if !ok || len(content) != 1 {
		t.Fatalf("Unexpected whoami result: %#v", result)
	}
}
