package e2e

import (
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// TestMultiProviderRegistryE2E exercises the experimental multi-provider BYOK
// registry (Providers / Models on the session config). It validates that
// several named providers, several models per provider, and custom agents
// bound to those provider-qualified models can coexist in one session, be
// launched, and route inference to the configured provider with the configured
// wire model and headers.
func TestMultiProviderRegistryE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	t.Run("should register multiple providers with custom agents bound to their models", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// A heterogeneous registry: two providers of different types, with
		// multiple models each. Provider-qualified selection ids are
		// alpha/sonnet, alpha/haiku, beta/opus, beta/haiku.
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Providers: []copilot.NamedProviderConfig{
				{
					Name:    "alpha",
					Type:    "openai",
					WireAPI: "completions",
					BaseURL: "https://alpha.example.test/v1",
					APIKey:  "alpha-secret",
					Headers: map[string]string{"X-Provider": "alpha"},
				},
				{
					Name:        "beta",
					Type:        "anthropic",
					BaseURL:     "https://beta.example.test",
					BearerToken: "beta-bearer",
					Headers:     map[string]string{"X-Provider": "beta"},
				},
			},
			Models: []copilot.ProviderModelConfig{
				{ID: "sonnet", Provider: "alpha", WireModel: "byok-gpt-4o", MaxPromptTokens: 111111},
				{ID: "haiku", Provider: "alpha", WireModel: "byok-gpt-4o-mini"},
				{ID: "opus", Provider: "beta", WireModel: "byok-claude-3-opus"},
				{ID: "haiku", Provider: "beta", WireModel: "byok-claude-3-haiku"},
			},
			CustomAgents: []copilot.CustomAgentConfig{
				{Name: "orchestrator", DisplayName: "Orchestrator", Description: "Top-level planner.", Prompt: "Plan and delegate.", Model: "alpha/sonnet"},
				{Name: "researcher", DisplayName: "Researcher", Description: "Deep research subagent.", Prompt: "Research thoroughly.", Model: "beta/opus"},
				{Name: "fast-helper", DisplayName: "Fast Helper", Description: "Quick subagent.", Prompt: "Answer quickly.", Model: "alpha/haiku"},
				{Name: "summarizer", DisplayName: "Summarizer", Description: "Summarizing subagent.", Prompt: "Summarize.", Model: "beta/haiku"},
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		result, err := session.RPC.Agent.List(t.Context())
		if err != nil {
			t.Fatalf("Agent.List failed: %v", err)
		}

		// All four custom agents coexist in a single session.
		if len(result.Agents) != 4 {
			t.Fatalf("Expected 4 agents, got %d", len(result.Agents))
		}

		// Each agent is bound to its configured provider-qualified BYOK model.
		boundModels := map[string]string{}
		for _, agent := range result.Agents {
			model := ""
			if agent.Model != nil {
				model = *agent.Model
			}
			boundModels[agent.Name] = model
		}
		expected := map[string]string{
			"orchestrator": "alpha/sonnet",
			"researcher":   "beta/opus",
			"fast-helper":  "alpha/haiku",
			"summarizer":   "beta/haiku",
		}
		for name, want := range expected {
			if got := boundModels[name]; got != want {
				t.Errorf("Expected agent %q bound to model %q, got %q", name, want, got)
			}
		}

		// Models from BOTH providers are represented, proving the two providers
		// and their models coexist within the same session.
		var hasAlpha, hasBeta bool
		for _, model := range boundModels {
			if strings.HasPrefix(model, "alpha/") {
				hasAlpha = true
			}
			if strings.HasPrefix(model, "beta/") {
				hasBeta = true
			}
		}
		if !hasAlpha || !hasBeta {
			t.Errorf("Expected both providers represented; hasAlpha=%v hasBeta=%v", hasAlpha, hasBeta)
		}
	})

	assertRouting := func(t *testing.T, selectionID, expectedWireModel, expectedProviderHeader string) {
		ctx.ConfigureForTest(t)

		// Two OpenAI-compatible providers, both pointed at the replay proxy so
		// their /chat/completions traffic is captured. They are distinguished
		// on the wire by their per-provider X-Provider header. "alpha" carries
		// two models (multiple models per provider); "delta" carries one.
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Model:               selectionID,
			Providers: []copilot.NamedProviderConfig{
				{
					Name:    "alpha",
					Type:    "openai",
					WireAPI: "completions",
					BaseURL: ctx.ProxyURL,
					APIKey:  "alpha-secret",
					Headers: map[string]string{"X-Provider": "alpha"},
				},
				{
					Name:    "delta",
					Type:    "openai",
					WireAPI: "completions",
					BaseURL: ctx.ProxyURL,
					APIKey:  "delta-secret",
					Headers: map[string]string{"X-Provider": "delta"},
				},
			},
			Models: []copilot.ProviderModelConfig{
				{ID: "sonnet", Provider: "alpha", WireModel: "byok-gpt-4o"},
				{ID: "haiku", Provider: "alpha", WireModel: "byok-gpt-4o-mini"},
				{ID: "turbo", Provider: "delta", WireModel: "byok-gpt-4-turbo"},
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 5+5?"}); err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		exchanges, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("GetExchanges failed: %v", err)
		}
		if len(exchanges) != 1 {
			t.Fatalf("Expected exactly 1 exchange, got %d", len(exchanges))
		}
		exchange := exchanges[0]

		// The wire model sent to the provider is the selected model's WireModel,
		// not its provider-qualified selection id.
		if exchange.Request.Model != expectedWireModel {
			t.Errorf("Expected request model %q, got %q", expectedWireModel, exchange.Request.Model)
		}

		// The request carried the owning provider's custom header, proving the
		// turn was dispatched against the correct provider connection.
		if !exchangeHasHeader(exchange, "X-Provider", expectedProviderHeader) {
			t.Errorf("Expected X-Provider header %q to be present", expectedProviderHeader)
		}

		// The provider's API key was applied as an Authorization header.
		if !exchangeHasHeader(exchange, "Authorization", "Bearer") {
			t.Error("Expected an Authorization header on the dispatched request")
		}
	}

	t.Run("should route alpha sonnet turn to its provider and wire model", func(t *testing.T) {
		assertRouting(t, "alpha/sonnet", "byok-gpt-4o", "alpha")
	})

	t.Run("should route alpha haiku turn to its provider and wire model", func(t *testing.T) {
		assertRouting(t, "alpha/haiku", "byok-gpt-4o-mini", "alpha")
	})

	t.Run("should route delta turbo turn to its provider and wire model", func(t *testing.T) {
		assertRouting(t, "delta/turbo", "byok-gpt-4-turbo", "delta")
	})
}
