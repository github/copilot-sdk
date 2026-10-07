/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Collections.Concurrent;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// Representative scenario coverage for provider and model selection.
/// </summary>
[Trait(E2ETestTraits.Backend, E2ETestTraits.SelfConfiguredBackend)]
public class ScenarioTestingProvidersE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_providers", output)
{
    [Fact]
    public async Task Should_Route_Scenario_Models_With_Provider_Auth_Headers_Wire_Ids_And_Capabilities()
    {
        var handler = new ScenarioProviderRequestHandler();
        await using var client = CreateProviderClient(handler);
        await using var session = await Ctx.CreateSessionAsync(
            client,
            CreateScenarioProviderConfig("alpha/large"));

        var alphaResponse = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with the configured provider response.",
        });
        Assert.Contains(ScenarioProviderRequestHandler.SyntheticText, alphaResponse?.Data.Content ?? string.Empty);

        await session.SetModelAsync("beta/fast");
        var betaResponse = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with the configured provider response again.",
        });
        Assert.Contains(ScenarioProviderRequestHandler.SyntheticText, betaResponse?.Data.Content ?? string.Empty);

        var alpha = Assert.Single(handler.InferenceRequests, request => request.Host == "alpha.scenario.invalid");
        Assert.Contains("\"model\":\"alpha-wire-large\"", alpha.Body, StringComparison.Ordinal);
        Assert.Equal("alpha-scenario", alpha.Headers["X-Scenario-Provider"]);
        Assert.Contains("alpha-static-key", alpha.Headers["Authorization"], StringComparison.Ordinal);

        var beta = Assert.Single(handler.InferenceRequests, request => request.Host == "beta.scenario.invalid");
        Assert.Contains("\"model\":\"beta-wire-fast\"", beta.Body, StringComparison.Ordinal);
        Assert.Equal("beta-scenario", beta.Headers["X-Scenario-Provider"]);
        Assert.Equal("Bearer beta-static-token", beta.Headers["Authorization"]);

        var listed = await session.Rpc.Model.ListAsync();
        var alphaModel = Assert.Single(
            listed.List,
            model => model.GetRawText().Contains("\"id\":\"alpha/large\"", StringComparison.Ordinal));
        var alphaJson = alphaModel.GetRawText();
        Assert.Contains("\"max_context_window_tokens\":120000", alphaJson, StringComparison.Ordinal);
        Assert.Contains("\"max_prompt_tokens\":100000", alphaJson, StringComparison.Ordinal);
        Assert.Contains("\"reasoningEffort\":true", alphaJson, StringComparison.Ordinal);
        Assert.Contains("\"vision\":false", alphaJson, StringComparison.Ordinal);
        Assert.Contains(
            listed.List,
            model => model.GetRawText().Contains("\"id\":\"alpha/small\"", StringComparison.Ordinal));
        Assert.Contains(
            listed.List,
            model => model.GetRawText().Contains("\"id\":\"beta/fast\"", StringComparison.Ordinal));
    }

    [Fact]
    public async Task Should_Use_Dynamic_Scenario_Bearer_Callback_For_Selected_Provider()
    {
        const string token = "scenario-client-dynamic-token";
        ProviderTokenArgs? observedArgs = null;
        var handler = new ScenarioProviderRequestHandler();
        await using var client = CreateProviderClient(handler);
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            ClientName = "scenario-client",
            Model = "managed/default",
            Providers =
            [
                new NamedProviderConfig
                {
                    Name = "managed",
                    Type = "openai",
                    WireApi = "completions",
                    BaseUrl = "https://managed.scenario.invalid/v1",
                    ApiKey = "must-not-win",
                    BearerToken = "must-not-win-either",
                    BearerTokenProvider = args =>
                    {
                        observedArgs = args;
                        return Task.FromResult(token);
                    },
                },
            ],
            Models =
            [
                new ProviderModelConfig
                {
                    Id = "default",
                    Provider = "managed",
                    ModelId = "claude-sonnet-5",
                    WireModel = "managed-wire-model",
                },
            ],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with the configured provider response.",
        });

        Assert.NotNull(observedArgs);
        Assert.Equal("managed", observedArgs!.ProviderName);
        Assert.Equal(session.SessionId, observedArgs.SessionId);
        var request = Assert.Single(handler.InferenceRequests);
        Assert.Equal("Bearer " + token, request.Headers["Authorization"]);
        Assert.DoesNotContain("must-not-win", request.Headers["Authorization"], StringComparison.Ordinal);
    }

    [Fact]
    public async Task Should_Apply_Reasoning_Context_And_Auto_Atomically_Without_Implicit_Reset()
    {
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = "scenario-client",
            Model = "claude-sonnet-5",
        });

        await session.SetModelAsync("claude-sonnet-5", new SetModelOptions
        {
            ReasoningEffort = "high",
            ContextTier = ContextTier.LongContext,
        });

        var atomic = await session.Rpc.Model.GetCurrentAsync();
        Assert.Equal("claude-sonnet-5", atomic.ModelId);
        Assert.Equal("high", atomic.ReasoningEffort);
        Assert.Equal(ContextTier.LongContext, atomic.ContextTier);

        await session.SetModelAsync("auto", new SetModelOptions
        {
            AutoTier = AutoTier.Intelligence,
        });
        var auto = await session.Rpc.Model.GetCurrentAsync();
        Assert.Equal("auto", auto.ModelId);
        Assert.Equal(AutoTier.Intelligence, auto.PendingAutoTier);

        var invalid = await Assert.ThrowsAnyAsync<Exception>(() =>
            session.SetModelAsync("claude-sonnet-5", new SetModelOptions
            {
                ReasoningEffort = "low",
                ContextTier = ContextTier.Default,
                AutoTier = AutoTier.Fast,
            }));
        Assert.Contains("auto", invalid.ToString(), StringComparison.OrdinalIgnoreCase);

        var afterRejected = await session.Rpc.Model.GetCurrentAsync();
        Assert.Equal("auto", afterRejected.ModelId);
        Assert.Equal(AutoTier.Intelligence, afterRejected.PendingAutoTier);

        await session.SetModelAsync("auto", new SetModelOptions
        {
        });

        var omittedTier = await session.Rpc.Model.GetCurrentAsync();
        Assert.Equal("auto", omittedTier.ModelId);
        Assert.Equal(AutoTier.Intelligence, omittedTier.PendingAutoTier);
    }

    [Fact]
    public async Task Should_Resolve_Legacy_Bare_Model_Id_When_Scenario_Resumes_With_Named_Provider()
    {
        var initialHandler = new ScenarioProviderRequestHandler();
        var initialClient = CreateProviderClient(initialHandler);
        var initialSession = await Ctx.CreateSessionAsync(initialClient, new SessionConfig
        {
            ClientName = "scenario-client",
            Model = "legacy-scenario-model",
            Provider = new ProviderConfig
            {
                Type = "openai",
                WireApi = "completions",
                BaseUrl = "https://legacy.scenario.invalid/v1",
                ApiKey = "legacy-key",
                ModelId = "legacy-scenario-model",
                WireModel = "legacy-wire-model",
            },
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        var sessionId = initialSession.SessionId;
        await initialSession.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Persist this scenario session.",
        });
        await initialSession.Rpc.SuspendAsync();
        await initialSession.DisposeAsync();
        await initialClient.ForceStopAsync();

        var resumedHandler = new ScenarioProviderRequestHandler();
        await using var resumedClient = CreateProviderClient(resumedHandler);
        await using var resumed = await Ctx.ResumeSessionAsync(resumedClient, sessionId, new ResumeSessionConfig
        {
            ClientName = "scenario-client",
            Providers =
            [
                new NamedProviderConfig
                {
                    Name = "scenario-provider",
                    Type = "openai",
                    WireApi = "completions",
                    BaseUrl = "https://legacy.scenario.invalid/v1",
                    ApiKey = "resumed-key",
                },
            ],
            Models =
            [
                new ProviderModelConfig
                {
                    Id = "legacy-scenario-model",
                    Provider = "scenario-provider",
                    ModelId = "legacy-scenario-model",
                    WireModel = "legacy-wire-model",
                },
            ],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        Assert.Equal("legacy-scenario-model", (await resumed.Rpc.Model.GetCurrentAsync()).ModelId);
        var response = await resumed.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Continue the legacy scenario session.",
        });
        Assert.Contains(ScenarioProviderRequestHandler.SyntheticText, response?.Data.Content ?? string.Empty);
        var routed = Assert.Single(resumedHandler.InferenceRequests);
        Assert.NotEqual("legacy.scenario.invalid", routed.Host);
        Assert.Contains("\"model\":\"claude-sonnet-5\"", routed.Body, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Should_Ignore_Failing_Unselected_Provider_But_Surface_Selected_Provider_Failure()
    {
        var handler = new ScenarioProviderRequestHandler(failingHost: "offline.scenario.invalid");
        await using var client = CreateProviderClient(handler);
        var config = CreateScenarioProviderConfig("alpha/large");
        config.Providers!.Add(new NamedProviderConfig
        {
            Name = "offline",
            Type = "openai",
            WireApi = "completions",
            BaseUrl = "https://offline.scenario.invalid/v1",
            ApiKey = "offline-key",
        });
        config.Models!.Add(new ProviderModelConfig
        {
            Id = "broken",
            Provider = "offline",
            ModelId = "claude-sonnet-5",
            WireModel = "offline-wire-model",
        });

        await using var session = await Ctx.CreateSessionAsync(client, config);
        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with the configured provider response.",
        });
        Assert.Contains(ScenarioProviderRequestHandler.SyntheticText, response?.Data.Content ?? string.Empty);
        Assert.DoesNotContain(handler.InferenceRequests, request => request.Host == "offline.scenario.invalid");

        await session.SetModelAsync("offline/broken");
        var failure = await Assert.ThrowsAnyAsync<Exception>(() =>
            session.SendAndWaitAsync(new MessageOptions
            {
                Prompt = "This selected provider should fail.",
            }));
        Assert.Contains("offline", failure.ToString(), StringComparison.OrdinalIgnoreCase);
        Assert.Contains(handler.InferenceRequests, request => request.Host == "offline.scenario.invalid");
    }

    private CopilotClient CreateProviderClient(ScenarioProviderRequestHandler handler) =>
        Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(),
            RequestHandler = handler,
        });

    private static SessionConfig CreateScenarioProviderConfig(string model) => new()
    {
        ClientName = "scenario-client",
        Model = model,
        Providers =
        [
            new NamedProviderConfig
            {
                Name = "alpha",
                Type = "openai",
                WireApi = "completions",
                BaseUrl = "https://alpha.scenario.invalid/v1",
                ApiKey = "alpha-static-key",
                Headers = new Dictionary<string, string> { ["X-Scenario-Provider"] = "alpha-scenario" },
            },
            new NamedProviderConfig
            {
                Name = "beta",
                Type = "openai",
                WireApi = "responses",
                BaseUrl = "https://beta.scenario.invalid/v1",
                BearerToken = "beta-static-token",
                Headers = new Dictionary<string, string> { ["X-Scenario-Provider"] = "beta-scenario" },
            },
        ],
        Models =
        [
            new ProviderModelConfig
            {
                Id = "large",
                Provider = "alpha",
                Name = "Scenario Large",
                ModelId = "claude-sonnet-5",
                WireModel = "alpha-wire-large",
                MaxContextWindowTokens = 120_000,
                MaxPromptTokens = 100_000,
                MaxOutputTokens = 8_000,
                Capabilities = new ModelCapabilitiesOverride
                {
                    Supports = new ModelCapabilitiesOverrideSupports
                    {
                        ReasoningEffort = true,
                        Vision = false,
                    },
                },
            },
            new ProviderModelConfig
            {
                Id = "small",
                Provider = "alpha",
                ModelId = "claude-sonnet-5",
                WireModel = "alpha-wire-small",
            },
            new ProviderModelConfig
            {
                Id = "fast",
                Provider = "beta",
                ModelId = "claude-sonnet-5",
                WireModel = "beta-wire-fast",
            },
        ],
        OnPermissionRequest = PermissionHandler.ApproveAll,
    };
}

internal sealed class ScenarioProviderRequestHandler(string? failingHost = null) : CopilotRequestHandler
{
    internal const string SyntheticText = "SCENARIO_PROVIDER_RESPONSE";
    private static readonly Regex WantsStreamRegex = new("\"stream\"\\s*:\\s*true", RegexOptions.Compiled);
    private readonly ConcurrentQueue<ScenarioProviderRequest> _requests = new();

    internal IReadOnlyList<ScenarioProviderRequest> InferenceRequests =>
        [.. _requests.Where(request => RecordingRequestHandler.IsInferenceUrl(request.Url))];

    protected override async Task<HttpResponseMessage> SendRequestAsync(
        HttpRequestMessage request,
        CopilotRequestContext ctx)
    {
        var body = request.Content is null
            ? string.Empty
#if NET8_0_OR_GREATER
            : await request.Content.ReadAsStringAsync(ctx.CancellationToken).ConfigureAwait(false);
#else
            : await request.Content.ReadAsStringAsync().ConfigureAwait(false);
#endif
        var headers = request.Headers.ToDictionary(
            pair => pair.Key,
            pair => string.Join(", ", pair.Value),
            StringComparer.OrdinalIgnoreCase);
        var uri = request.RequestUri!;
        _requests.Enqueue(new ScenarioProviderRequest(uri.ToString(), uri.Host, body, headers));

        if (string.Equals(uri.Host, failingHost, StringComparison.Ordinal))
        {
            return new HttpResponseMessage(HttpStatusCode.BadGateway)
            {
                Content = new StringContent(
                    "{\"error\":{\"message\":\"offline scenario provider\"}}",
                    Encoding.UTF8,
                    "application/json"),
            };
        }

        if (!RecordingRequestHandler.IsInferenceUrl(uri.ToString()))
        {
            return RecordingRequestHandler.BuildNonInferenceResponse(uri.ToString());
        }

        var wantsStream = WantsStreamRegex.IsMatch(body);
        if (uri.AbsolutePath.EndsWith("/responses", StringComparison.OrdinalIgnoreCase))
        {
            return wantsStream
                ? Sse(
                    "event: response.created\ndata: {\"type\":\"response.created\",\"response\":{\"id\":\"scenario-response\",\"object\":\"response\",\"status\":\"in_progress\",\"output\":[]}}\n\n" +
                    "event: response.output_item.added\ndata: {\"type\":\"response.output_item.added\",\"output_index\":0,\"item\":{\"id\":\"scenario-message\",\"type\":\"message\",\"role\":\"assistant\",\"content\":[]}}\n\n" +
                    "event: response.content_part.added\ndata: {\"type\":\"response.content_part.added\",\"output_index\":0,\"content_index\":0,\"part\":{\"type\":\"output_text\",\"text\":\"\"}}\n\n" +
                    $"event: response.output_text.delta\ndata: {{\"type\":\"response.output_text.delta\",\"output_index\":0,\"content_index\":0,\"delta\":\"{SyntheticText}\"}}\n\n" +
                    $"event: response.output_text.done\ndata: {{\"type\":\"response.output_text.done\",\"output_index\":0,\"content_index\":0,\"text\":\"{SyntheticText}\"}}\n\n" +
                    $"event: response.completed\ndata: {{\"type\":\"response.completed\",\"response\":{{\"id\":\"scenario-response\",\"object\":\"response\",\"status\":\"completed\",\"output\":[{{\"id\":\"scenario-message\",\"type\":\"message\",\"role\":\"assistant\",\"content\":[{{\"type\":\"output_text\",\"text\":\"{SyntheticText}\"}}]}}],\"usage\":{{\"input_tokens\":5,\"output_tokens\":3,\"total_tokens\":8}}}}}}\n\n")
                : Json(
                    $"{{\"id\":\"scenario-response\",\"object\":\"response\",\"status\":\"completed\",\"output\":[{{\"id\":\"scenario-message\",\"type\":\"message\",\"role\":\"assistant\",\"content\":[{{\"type\":\"output_text\",\"text\":\"{SyntheticText}\"}}]}}],\"usage\":{{\"input_tokens\":5,\"output_tokens\":3,\"total_tokens\":8}}}}");
        }

        return wantsStream
            ? Sse(
                $"data: {{\"id\":\"scenario-chat\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"scenario\",\"choices\":[{{\"index\":0,\"delta\":{{\"role\":\"assistant\",\"content\":\"{SyntheticText}\"}},\"finish_reason\":null}}]}}\n\n" +
                "data: {\"id\":\"scenario-chat\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"scenario\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":3,\"total_tokens\":8}}\n\n" +
                "data: [DONE]\n\n")
            : Json(
                $"{{\"id\":\"scenario-chat\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"scenario\",\"choices\":[{{\"index\":0,\"message\":{{\"role\":\"assistant\",\"content\":\"{SyntheticText}\"}},\"finish_reason\":\"stop\"}}],\"usage\":{{\"prompt_tokens\":5,\"completion_tokens\":3,\"total_tokens\":8}}}}");
    }

    private static HttpResponseMessage Json(string body) => new(HttpStatusCode.OK)
    {
        Content = new StringContent(body, Encoding.UTF8, "application/json"),
    };

    private static HttpResponseMessage Sse(string body) => new(HttpStatusCode.OK)
    {
        Content = new StringContent(body, Encoding.UTF8, "text/event-stream"),
    };
}

internal sealed record ScenarioProviderRequest(
    string Url,
    string Host,
    string Body,
    IReadOnlyDictionary<string, string> Headers);
