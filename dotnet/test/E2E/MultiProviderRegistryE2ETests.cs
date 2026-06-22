/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// End-to-end coverage for the experimental multi-provider BYOK registry
/// (<see cref="SessionConfig.Providers"/> / <see cref="SessionConfig.Models"/>).
/// Validates that several named providers, several models per provider, and
/// custom agents bound to those provider-qualified models can coexist in one
/// session, be launched, and route inference to the configured provider with
/// the configured wire model and headers.
/// </summary>
public class MultiProviderRegistryE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "multi_provider_registry", output)
{
    /// <summary>
    /// Builds a heterogeneous registry: two providers of different types, with
    /// multiple models each. Provider-qualified selection ids are
    /// <c>alpha/sonnet</c>, <c>alpha/haiku</c>, <c>beta/opus</c>, <c>beta/haiku</c>.
    /// </summary>
    private static IList<NamedProviderConfig> RegistryProviders() =>
    [
        new()
        {
            Name = "alpha",
            Type = "openai",
            WireApi = "completions",
            BaseUrl = "https://alpha.example.test/v1",
            ApiKey = "alpha-secret",
            Headers = new Dictionary<string, string> { ["X-Provider"] = "alpha" },
        },
        new()
        {
            Name = "beta",
            Type = "anthropic",
            BaseUrl = "https://beta.example.test",
            BearerToken = "beta-bearer",
            Headers = new Dictionary<string, string> { ["X-Provider"] = "beta" },
        },
    ];

    private static IList<ProviderModelConfig> RegistryModels() =>
    [
        new() { Id = "sonnet", Provider = "alpha", WireModel = "byok-gpt-4o", MaxPromptTokens = 111111 },
        new() { Id = "haiku", Provider = "alpha", WireModel = "byok-gpt-4o-mini" },
        new() { Id = "opus", Provider = "beta", WireModel = "byok-claude-3-opus" },
        new() { Id = "haiku", Provider = "beta", WireModel = "byok-claude-3-haiku" },
    ];

    private static IList<CustomAgentConfig> RegistryAgents() =>
    [
        new() { Name = "orchestrator", DisplayName = "Orchestrator", Description = "Top-level planner.", Prompt = "Plan and delegate.", Model = "alpha/sonnet" },
        new() { Name = "researcher", DisplayName = "Researcher", Description = "Deep research subagent.", Prompt = "Research thoroughly.", Model = "beta/opus" },
        new() { Name = "fast-helper", DisplayName = "Fast Helper", Description = "Quick subagent.", Prompt = "Answer quickly.", Model = "alpha/haiku" },
        new() { Name = "summarizer", DisplayName = "Summarizer", Description = "Summarizing subagent.", Prompt = "Summarize.", Model = "beta/haiku" },
    ];

    [Fact]
    public async Task Should_Register_Multiple_Providers_With_Custom_Agents_Bound_To_Their_Models()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            Providers = RegistryProviders(),
            Models = RegistryModels(),
            CustomAgents = RegistryAgents(),
        });

        var agents = (await session.Rpc.Agent.ListAsync()).Agents;

        // All four custom agents coexist in a single session.
        Assert.Equal(4, agents.Count);

        // Each agent is bound to its configured provider-qualified BYOK model.
        AssertAgentModel(agents, "orchestrator", "alpha/sonnet", "Orchestrator", "Top-level planner.");
        AssertAgentModel(agents, "researcher", "beta/opus", "Researcher", "Deep research subagent.");
        AssertAgentModel(agents, "fast-helper", "alpha/haiku", "Fast Helper", "Quick subagent.");
        AssertAgentModel(agents, "summarizer", "beta/haiku", "Summarizer", "Summarizing subagent.");

        // Models from BOTH providers are represented, proving the two providers
        // and their models coexist within the same session.
        var boundModels = agents.Select(a => a.Model).ToHashSet();
        Assert.Contains(boundModels, m => m!.StartsWith("alpha/", StringComparison.Ordinal));
        Assert.Contains(boundModels, m => m!.StartsWith("beta/", StringComparison.Ordinal));
    }

    [Fact]
    public async Task Should_Route_Alpha_Sonnet_Turn_To_Its_Provider_And_Wire_Model()
        => await AssertRoutingAsync("alpha/sonnet", "byok-gpt-4o", "alpha");

    [Fact]
    public async Task Should_Route_Alpha_Haiku_Turn_To_Its_Provider_And_Wire_Model()
        => await AssertRoutingAsync("alpha/haiku", "byok-gpt-4o-mini", "alpha");

    [Fact]
    public async Task Should_Route_Delta_Turbo_Turn_To_Its_Provider_And_Wire_Model()
        => await AssertRoutingAsync("delta/turbo", "byok-gpt-4-turbo", "delta");

    /// <summary>
    /// Selects <paramref name="selectionId"/> in a session whose registry holds
    /// two OpenAI-compatible providers (each pointed at the replay proxy), runs a
    /// turn, and asserts the captured request used the model's configured wire
    /// model and carried the owning provider's header and credential.
    /// </summary>
    private async Task AssertRoutingAsync(string selectionId, string expectedWireModel, string expectedProviderHeader)
    {
        // Two OpenAI-compatible providers, both pointed at the replay proxy so
        // their /chat/completions traffic is captured. They are distinguished on
        // the wire by their per-provider X-Provider header. "alpha" carries two
        // models (multiple models per provider); "delta" carries one.
        var providers = new List<NamedProviderConfig>
        {
            new()
            {
                Name = "alpha",
                Type = "openai",
                WireApi = "completions",
                BaseUrl = Ctx.ProxyUrl,
                ApiKey = "alpha-secret",
                Headers = new Dictionary<string, string> { ["X-Provider"] = "alpha" },
            },
            new()
            {
                Name = "delta",
                Type = "openai",
                WireApi = "completions",
                BaseUrl = Ctx.ProxyUrl,
                ApiKey = "delta-secret",
                Headers = new Dictionary<string, string> { ["X-Provider"] = "delta" },
            },
        };
        var models = new List<ProviderModelConfig>
        {
            new() { Id = "sonnet", Provider = "alpha", WireModel = "byok-gpt-4o" },
            new() { Id = "haiku", Provider = "alpha", WireModel = "byok-gpt-4o-mini" },
            new() { Id = "turbo", Provider = "delta", WireModel = "byok-gpt-4-turbo" },
        };

        var session = await CreateSessionAsync(new SessionConfig
        {
            Model = selectionId,
            Providers = providers,
            Models = models,
        });

        var exchanges = await SendAndWaitForExchangesAsync(
            session,
            new MessageOptions { Prompt = "What is 5+5?" });

        var exchange = Assert.Single(exchanges);

        // The wire model sent to the provider is the selected model's WireModel,
        // not its provider-qualified selection id.
        Assert.Equal(expectedWireModel, exchange.Request.Model);

        // The request carried the owning provider's custom header, proving the
        // turn was dispatched against the correct provider connection.
        Assert.Equal(expectedProviderHeader, GetHeaderValue(exchange, "X-Provider"));

        // The provider's API key was applied as an Authorization header.
        Assert.False(string.IsNullOrEmpty(GetHeaderValue(exchange, "Authorization")));
    }

    private static void AssertAgentModel(
        IEnumerable<AgentInfo> agents,
        string name,
        string expectedModel,
        string expectedDisplayName,
        string expectedDescription)
    {
        var agent = Assert.Single(agents, a => string.Equals(a.Name, name, StringComparison.Ordinal));
        Assert.Equal(expectedModel, agent.Model);
        Assert.Equal(expectedDisplayName, agent.DisplayName);
        Assert.Equal(expectedDescription, agent.Description);
    }

    private static string? GetHeaderValue(ParsedHttpExchange exchange, string name)
    {
        if (exchange.RequestHeaders == null)
        {
            return null;
        }

        foreach (var kv in exchange.RequestHeaders)
        {
            if (!string.Equals(kv.Key, name, StringComparison.OrdinalIgnoreCase))
            {
                continue;
            }

            return kv.Value.ValueKind switch
            {
                JsonValueKind.String => kv.Value.GetString(),
                JsonValueKind.Array when kv.Value.GetArrayLength() > 0 => kv.Value[0].GetString(),
                _ => kv.Value.ToString(),
            };
        }

        return null;
    }
}
