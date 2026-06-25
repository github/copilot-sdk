/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"net/http"
	"strconv"
	"strings"
	"sync"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// Fake BYOK provider base URLs. These hosts are never actually dialed: the
// capturing RoundTripper fully answers any request aimed at a `.invalid` host,
// so they only need to be syntactically valid, non-resolving URLs. Distinct
// hosts let the per-provider test assert routing by host.
const (
	byokPrimaryHost    = "byok-endpoint.invalid"
	byokPrimaryBaseURL = "https://" + byokPrimaryHost + "/v1"
	byokRedHost        = "byok-red.invalid"
	byokRedBaseURL     = "https://" + byokRedHost + "/v1"
	byokBlueHost       = "byok-blue.invalid"
	byokBlueBaseURL    = "https://" + byokBlueHost + "/v1"
)

// capturedBYOKRequest records the host and Authorization header of one outbound
// HTTP request the runtime aimed at a fake BYOK provider endpoint.
type capturedBYOKRequest struct {
	host          string
	authorization string
}

// byokCapturingRoundTripper stands in for a real HTTP upstream. It records the
// `Authorization` header the runtime applied (after calling the provider's
// BearerTokenProvider callback over the session-scoped `providerToken.getToken` RPC)
// for every request aimed at a fake `.invalid` BYOK host, answering them with a
// synthetic 404 (a non-retryable status, so each outbound model request yields
// exactly one capture). Every other request (CAPI bootstrap: model catalog,
// policy, session) is fabricated locally so the test never touches the network.
type byokCapturingRoundTripper struct {
	mu       sync.Mutex
	captures []capturedBYOKRequest
}

func (rt *byokCapturingRoundTripper) RoundTrip(req *http.Request) (*http.Response, error) {
	if strings.HasSuffix(req.URL.Hostname(), ".invalid") {
		rt.mu.Lock()
		rt.captures = append(rt.captures, capturedBYOKRequest{
			host:          req.URL.Host,
			authorization: req.Header.Get("Authorization"),
		})
		rt.mu.Unlock()
		if req.Body != nil {
			_ = req.Body.Close()
		}
		return buildJSONResponse(http.StatusNotFound, `{"error":{"message":"fake byok endpoint"}}`), nil
	}
	return buildNonInferenceResponse(req.URL.String()), nil
}

// authHeaders returns the captured Authorization headers in arrival order.
func (rt *byokCapturingRoundTripper) authHeaders() []string {
	rt.mu.Lock()
	defer rt.mu.Unlock()
	headers := make([]string, 0, len(rt.captures))
	for _, c := range rt.captures {
		if c.authorization != "" {
			headers = append(headers, c.authorization)
		}
	}
	return headers
}

// authHeaderForHost returns the Authorization header captured for requests aimed
// at host, if any.
func (rt *byokCapturingRoundTripper) authHeaderForHost(host string) string {
	rt.mu.Lock()
	defer rt.mu.Unlock()
	for _, c := range rt.captures {
		if c.host == host {
			return c.authorization
		}
	}
	return ""
}

func (rt *byokCapturingRoundTripper) reset() {
	rt.mu.Lock()
	defer rt.mu.Unlock()
	rt.captures = nil
}

// TestBYOKBearerTokenProvider is end-to-end coverage for the experimental BYOK
// bearer-token-provider surface (BearerTokenProvider on a provider config). The
// callback stays entirely on the SDK/client side: the SDK strips it from the
// wire config, sets the `hasBearerTokenProvider` flag, and the runtime calls
// back over the session-scoped `providerToken.getToken` RPC before each outbound
// model request, applying the returned token as the `Authorization` header.
//
// Rather than standing up a real HTTP listener, the test installs a capturing
// RoundTripper that intercepts the runtime's outbound model request in-process,
// captures the `Authorization` header, and returns a synthetic response. It
// validates, against a real runtime:
//  1. the callback's token reaches the model request as `Authorization: Bearer <token>`;
//  2. the runtime re-acquires a token per request (no runtime-side caching);
//  3. per-provider dispatch routes each provider's turn to its own callback, and
//     the resulting token reaches that provider's endpoint.
func TestBYOKBearerTokenProvider(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	rt := &byokCapturingRoundTripper{}
	handler := &copilot.CopilotRequestHandler{Transport: rt}

	client := newCopilotRequestClient(ctx, handler)
	t.Cleanup(func() { client.ForceStop() })
	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	// runTurn drives one BYOK turn; the synthetic 404 errors the turn after the
	// runtime has already sent the token-bearing request, which is all the test
	// asserts on, so the resulting error is expected and swallowed.
	runTurn := func(providers []copilot.NamedProviderConfig, models []copilot.ProviderModelConfig, selectionID, prompt string) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Model:               selectionID,
			Providers:           providers,
			Models:              models,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		_, _ = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: prompt})
		_ = session.Disconnect()
	}

	t.Run("applies the callback's token as the Authorization header", func(t *testing.T) {
		rt.reset()
		const sentinel = "sentinel-bearer-token-abc123"
		var mu sync.Mutex
		calls := 0
		getBearerToken := func(args copilot.ProviderTokenArgs) (string, error) {
			mu.Lock()
			calls++
			mu.Unlock()
			return sentinel, nil
		}

		providers := []copilot.NamedProviderConfig{{
			Name:                "mi",
			Type:                "openai",
			WireAPI:             "completions",
			BaseURL:             byokPrimaryBaseURL,
			BearerTokenProvider: getBearerToken,
		}}
		models := []copilot.ProviderModelConfig{{ID: "default", Provider: "mi", WireModel: "byok-gpt-4o"}}

		runTurn(providers, models, "mi/default", "What is 5+5?")

		// The runtime acquired a token via the callback and applied it verbatim
		// as the bearer credential on the outbound model request.
		if !containsString(rt.authHeaders(), "Bearer "+sentinel) {
			t.Fatalf("Expected captured Authorization headers to contain %q, got %v", "Bearer "+sentinel, rt.authHeaders())
		}
		mu.Lock()
		gotCalls := calls
		mu.Unlock()
		if gotCalls < 1 {
			t.Fatalf("Expected the callback to be invoked at least once, got %d", gotCalls)
		}
	})

	t.Run("re-acquires a fresh token for each request (no runtime caching)", func(t *testing.T) {
		rt.reset()
		var mu sync.Mutex
		calls := 0
		getBearerToken := func(args copilot.ProviderTokenArgs) (string, error) {
			mu.Lock()
			calls++
			token := "rotating-token-" + strconv.Itoa(calls)
			mu.Unlock()
			// A distinct token per acquisition proves the runtime re-invokes the
			// callback per request rather than caching a previous token.
			return token, nil
		}

		providers := []copilot.NamedProviderConfig{{
			Name:                "mi",
			Type:                "openai",
			WireAPI:             "completions",
			BaseURL:             byokPrimaryBaseURL,
			BearerTokenProvider: getBearerToken,
		}}
		models := []copilot.ProviderModelConfig{{ID: "default", Provider: "mi", WireModel: "byok-gpt-4o"}}

		runTurn(providers, models, "mi/default", "What is 1+1?")
		runTurn(providers, models, "mi/default", "What is 2+2?")

		// Each outbound request carries a freshly-acquired, distinct token.
		auths := rt.authHeaders()
		if len(auths) < 2 {
			t.Fatalf("Expected at least 2 captured Authorization headers, got %d: %v", len(auths), auths)
		}
		if !strings.HasPrefix(auths[0], "Bearer rotating-token-") || !strings.HasPrefix(auths[1], "Bearer rotating-token-") {
			t.Fatalf("Expected rotating-token bearer headers, got %v", auths)
		}
		if auths[0] == auths[1] {
			t.Fatalf("Expected distinct tokens per request, both were %q", auths[0])
		}
		mu.Lock()
		gotCalls := calls
		mu.Unlock()
		if gotCalls < 2 {
			t.Fatalf("Expected the callback to be invoked at least twice, got %d", gotCalls)
		}
	})

	t.Run("dispatches token acquisition per provider", func(t *testing.T) {
		rt.reset()
		tokenByProvider := map[string]string{
			"red":  "token-for-red",
			"blue": "token-for-blue",
		}
		var mu sync.Mutex
		var acquiredFor []string
		makeCallback := func(providerName string) copilot.BearerTokenProvider {
			return func(args copilot.ProviderTokenArgs) (string, error) {
				// The runtime forwards the requesting provider's name so the
				// client can dispatch to the right credential.
				if args.ProviderName != providerName {
					t.Errorf("Expected providerName %q, got %q", providerName, args.ProviderName)
				}
				// The runtime also forwards the owning session id so a
				// client-level shared callback can resolve the session.
				if args.SessionID == "" {
					t.Errorf("Expected a non-empty session id in token args")
				}
				mu.Lock()
				acquiredFor = append(acquiredFor, providerName)
				mu.Unlock()
				return tokenByProvider[providerName], nil
			}
		}

		providers := []copilot.NamedProviderConfig{
			{
				Name:                "red",
				Type:                "openai",
				WireAPI:             "completions",
				BaseURL:             byokRedBaseURL,
				BearerTokenProvider: makeCallback("red"),
			},
			{
				Name:                "blue",
				Type:                "openai",
				WireAPI:             "completions",
				BaseURL:             byokBlueBaseURL,
				BearerTokenProvider: makeCallback("blue"),
			},
		}
		models := []copilot.ProviderModelConfig{
			{ID: "default", Provider: "red", WireModel: "byok-gpt-4o"},
			{ID: "default", Provider: "blue", WireModel: "byok-gpt-4o"},
		}

		runTurn(providers, models, "red/default", "What is 3+3?")
		runTurn(providers, models, "blue/default", "What is 4+4?")

		// Each provider's turn was authenticated with its own token AND that
		// token was delivered to that provider's endpoint, proving per-provider
		// dispatch (not a single session-global credential).
		if got := rt.authHeaderForHost(byokRedHost); got != "Bearer "+tokenByProvider["red"] {
			t.Fatalf("Expected red host to receive %q, got %q", "Bearer "+tokenByProvider["red"], got)
		}
		if got := rt.authHeaderForHost(byokBlueHost); got != "Bearer "+tokenByProvider["blue"] {
			t.Fatalf("Expected blue host to receive %q, got %q", "Bearer "+tokenByProvider["blue"], got)
		}
		mu.Lock()
		got := append([]string(nil), acquiredFor...)
		mu.Unlock()
		if !containsString(got, "red") || !containsString(got, "blue") {
			t.Fatalf("Expected both providers to acquire tokens, got %v", got)
		}
	})
}
