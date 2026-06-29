/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"regexp"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// session.provider.getEndpoint is gated behind COPILOT_ALLOW_GET_PROVIDER_ENDPOINT;
// the harness env passed to the CLI subprocess opts in for this test file.
func TestProviderEndpointE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)

	client := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.Env = append(opts.Env, "COPILOT_ALLOW_GET_PROVIDER_ENDPOINT=true")
	})
	t.Cleanup(func() { client.ForceStop() })

	t.Run("returns the BYOK provider endpoint when a custom provider is configured", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Provider: &copilot.ProviderConfig{
				Type:    "openai",
				WireAPI: "completions",
				BaseURL: "https://api.example.test/v1",
				APIKey:  "byok-secret",
				Headers: map[string]string{"X-Custom-Header": "byok-yes"},
			},
		})
		if err != nil {
			t.Fatalf("create session: %v", err)
		}
		// disconnect may fail since the BYOK provider URL is fake.
		defer func() { _ = session.Disconnect() }()

		endpoint, err := session.RPC.Provider.GetEndpoint(t.Context())
		if err != nil {
			t.Fatalf("getEndpoint: %v", err)
		}

		if endpoint.Type != rpc.ProviderEndpointTypeOpenai {
			t.Errorf("Type: want %q, got %q", rpc.ProviderEndpointTypeOpenai, endpoint.Type)
		}
		if endpoint.WireAPI == nil || *endpoint.WireAPI != rpc.ProviderEndpointWireAPICompletions {
			t.Errorf("WireAPI: want %q, got %v", rpc.ProviderEndpointWireAPICompletions, endpoint.WireAPI)
		}
		if endpoint.BaseURL != "https://api.example.test/v1" {
			t.Errorf("BaseURL: got %q", endpoint.BaseURL)
		}
		if endpoint.APIKey == nil || *endpoint.APIKey != "byok-secret" {
			t.Errorf("APIKey: got %v", endpoint.APIKey)
		}
		if got := endpoint.Headers["X-Custom-Header"]; got != "byok-yes" {
			t.Errorf("X-Custom-Header: got %q", got)
		}
		// BYOK sessions never issue a CAPI session token.
		if endpoint.SessionToken != nil {
			t.Errorf("SessionToken: expected nil, got %+v", endpoint.SessionToken)
		}
	})

	t.Run("returns the CAPI provider endpoint for an OAuth-authenticated session", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("create session: %v", err)
		}
		defer func() {
			if err := session.Disconnect(); err != nil {
				t.Errorf("disconnect: %v", err)
			}
		}()

		endpoint, err := session.RPC.Provider.GetEndpoint(t.Context())
		if err != nil {
			t.Fatalf("getEndpoint: %v", err)
		}

		switch endpoint.Type {
		case rpc.ProviderEndpointTypeOpenai, rpc.ProviderEndpointTypeAzure, rpc.ProviderEndpointTypeAnthropic:
		default:
			t.Errorf("unexpected Type %q", endpoint.Type)
		}
		// wireApi is omitted for anthropic; otherwise one of the OpenAI shapes.
		if endpoint.Type != rpc.ProviderEndpointTypeAnthropic {
			if endpoint.WireAPI == nil ||
				(*endpoint.WireAPI != rpc.ProviderEndpointWireAPICompletions &&
					*endpoint.WireAPI != rpc.ProviderEndpointWireAPIResponses) {
				t.Errorf("unexpected WireAPI %v for type %q", endpoint.WireAPI, endpoint.Type)
			}
		}

		// CAPI baseUrl is the (proxy) Copilot API URL injected by the harness.
		if !strings.HasPrefix(endpoint.BaseURL, "http://") && !strings.HasPrefix(endpoint.BaseURL, "https://") {
			t.Errorf("BaseURL not an http(s) URL: %q", endpoint.BaseURL)
		}

		// For CAPI OAuth sessions the apiKey is the resolved GitHub bearer.
		if endpoint.APIKey == nil || len(*endpoint.APIKey) == 0 {
			t.Fatalf("APIKey should be a non-empty string, got %v", endpoint.APIKey)
		}

		// Standard CAPI headers must be present, and Authorization is surfaced
		// as the runtime sends it (`Bearer <apiKey>`).
		if endpoint.Headers["Copilot-Integration-Id"] == "" {
			t.Errorf("Copilot-Integration-Id header missing")
		}
		if ua := endpoint.Headers["User-Agent"]; !regexp.MustCompile(`(?i)Copilot`).MatchString(ua) {
			t.Errorf("User-Agent should mention Copilot, got %q", ua)
		}
		if endpoint.Headers["X-GitHub-Api-Version"] == "" {
			t.Errorf("X-GitHub-Api-Version header missing")
		}
		if !regexp.MustCompile(`[0-9a-f-]{8,}`).MatchString(endpoint.Headers["X-Interaction-Id"]) {
			t.Errorf("X-Interaction-Id should match interaction-id format, got %q", endpoint.Headers["X-Interaction-Id"])
		}
		if want, got := "Bearer "+*endpoint.APIKey, endpoint.Headers["Authorization"]; want != got {
			t.Errorf("Authorization: want %q, got %q", want, got)
		}

		// When the omit-modelId path returned an auto-mode session token, it
		// must use the documented header name. The harness may have a non-auto
		// model selected, in which case the field is simply omitted.
		if endpoint.SessionToken != nil {
			if endpoint.SessionToken.Header != "Copilot-Session-Token" {
				t.Errorf("SessionToken.Header: got %q", endpoint.SessionToken.Header)
			}
			if endpoint.SessionToken.Token == "" {
				t.Errorf("SessionToken.Token should be non-empty")
			}
			if endpoint.SessionToken.ExpiresAt != nil && endpoint.SessionToken.ExpiresAt.IsZero() {
				t.Errorf("SessionToken.ExpiresAt should be a valid time when present")
			}
		}
	})
}
