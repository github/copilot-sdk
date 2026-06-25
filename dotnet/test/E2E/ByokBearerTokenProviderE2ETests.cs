/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using System.Net;
using System.Net.Http;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// End-to-end coverage for the experimental BYOK bearer-token-provider surface
/// (<c>BearerTokenProvider</c> on a provider config). The callback stays entirely on
/// the SDK/client side: the SDK strips it from the wire config, sets the
/// <c>hasBearerTokenProvider</c> flag, and the runtime calls back over the
/// session-scoped <c>providerToken.getToken</c> RPC before each outbound model
/// request, applying the returned token as the <c>Authorization</c> header.
/// </summary>
/// <remarks>
/// <para>
/// These tests mirror the Node SDK's <c>byok_bearer_token_provider.e2e.test.ts</c>.
/// Rather than standing up a real HTTP listener, each test installs a
/// <see cref="CapturingRequestHandler"/> that intercepts the runtime's outbound
/// model request in-process, captures the <c>Authorization</c> header, and
/// returns a synthetic response — so nothing touches the network and there is no
/// CAPI proxy acting as the inference endpoint. They validate, against a real
/// runtime:
/// </para>
/// <list type="number">
///   <item>the callback's token reaches the model request as <c>Authorization: Bearer &lt;token&gt;</c>;</item>
///   <item>the runtime re-acquires a token per request (no runtime-side caching);</item>
///   <item>per-provider dispatch routes each provider's turn to its own callback,
///   and the resulting token reaches that provider's endpoint.</item>
/// </list>
/// </remarks>
public class ByokBearerTokenProviderE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "byok_bearer_token_provider", output)
{
    // Fake BYOK provider hosts. These are never actually dialed: the request
    // handler fully answers any request aimed at a `.invalid` host, so they only
    // need to be syntactically valid, non-resolving URLs. Distinct hosts let the
    // per-provider test assert routing by host.
    private const string PrimaryHost = "byok-endpoint.invalid";
    private const string PrimaryBaseUrl = $"https://{PrimaryHost}/v1";
    private const string RedHost = "byok-red.invalid";
    private const string RedBaseUrl = $"https://{RedHost}/v1";
    private const string BlueHost = "byok-blue.invalid";
    private const string BlueBaseUrl = $"https://{BlueHost}/v1";

    private CopilotClient CreateClientWith(CapturingRequestHandler handler) =>
        Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(),
            RequestHandler = handler,
        });

    /// <summary>
    /// Drives one BYOK turn against the given providers/models. The capturing
    /// handler 404s the BYOK request, which errors the turn after the runtime has
    /// already applied the (token-bearing) <c>Authorization</c> header — which is
    /// all these tests assert on. The resulting error is swallowed.
    /// </summary>
    private static async Task RunTurnAsync(
        CopilotClient client,
        IList<NamedProviderConfig> providers,
        IList<ProviderModelConfig> models,
        string selectionId,
        string prompt)
    {
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Model = selectionId,
            Providers = providers,
            Models = models,
        });
        try
        {
            await session.SendAndWaitAsync(new MessageOptions { Prompt = prompt });
        }
        catch (InvalidOperationException)
        {
            // The handler always 404s the BYOK endpoint, so the turn errors after
            // the token-bearing request was already captured. Expected.
        }
        finally
        {
            await session.DisposeAsync();
        }
    }

    [Fact]
    public async Task Applies_The_Callbacks_Token_As_The_Authorization_Header()
    {
        const string sentinel = "sentinel-bearer-token-abc123";
        var calls = 0;

        var handler = new CapturingRequestHandler();
        await using var client = CreateClientWith(handler);
        await client.StartAsync();

        var providers = new List<NamedProviderConfig>
        {
            new()
            {
                Name = "mi",
                Type = "openai",
                WireApi = "completions",
                BaseUrl = PrimaryBaseUrl,
                BearerTokenProvider = _ =>
                {
                    Interlocked.Increment(ref calls);
                    return Task.FromResult(sentinel);
                },
            },
        };
        var models = new List<ProviderModelConfig>
        {
            new() { Id = "default", Provider = "mi", WireModel = "byok-gpt-4o" },
        };

        await RunTurnAsync(client, providers, models, "mi/default", "What is 5+5?");

        // The runtime acquired a token via the callback and applied it verbatim as
        // the bearer credential on the outbound model request.
        Assert.Contains($"Bearer {sentinel}", handler.AuthHeaders());
        Assert.True(calls >= 1, "Expected the bearer-token callback to be invoked at least once.");
    }

    [Fact]
    public async Task Re_Acquires_A_Fresh_Token_For_Each_Request()
    {
        var calls = 0;

        var handler = new CapturingRequestHandler();
        await using var client = CreateClientWith(handler);
        await client.StartAsync();

        var providers = new List<NamedProviderConfig>
        {
            new()
            {
                Name = "mi",
                Type = "openai",
                WireApi = "completions",
                BaseUrl = PrimaryBaseUrl,
                // A distinct token per acquisition proves the runtime re-invokes
                // the callback per request rather than caching a previous token.
                BearerTokenProvider = _ =>
                {
                    var n = Interlocked.Increment(ref calls);
                    return Task.FromResult($"rotating-token-{n}");
                },
            },
        };
        var models = new List<ProviderModelConfig>
        {
            new() { Id = "default", Provider = "mi", WireModel = "byok-gpt-4o" },
        };

        await RunTurnAsync(client, providers, models, "mi/default", "What is 1+1?");
        await RunTurnAsync(client, providers, models, "mi/default", "What is 2+2?");

        // Each outbound request carries a freshly-acquired, distinct token.
        var auths = handler.AuthHeaders();
        Assert.True(auths.Count >= 2, $"Expected at least 2 captured Authorization headers, saw {auths.Count}.");
        Assert.Matches(@"^Bearer rotating-token-\d+$", auths[0]);
        Assert.Matches(@"^Bearer rotating-token-\d+$", auths[1]);
        Assert.NotEqual(auths[0], auths[1]);
        Assert.True(calls >= 2, "Expected the bearer-token callback to be invoked at least twice.");
    }

    [Fact]
    public async Task Dispatches_Token_Acquisition_Per_Provider()
    {
        var tokenByProvider = new Dictionary<string, string>
        {
            ["red"] = "token-for-red",
            ["blue"] = "token-for-blue",
        };
        var acquiredFor = new ConcurrentBag<string>();

        Func<ProviderTokenArgs, Task<string>> MakeCallback(string providerName) =>
            args =>
            {
                // The runtime forwards the requesting provider's name so the client
                // can dispatch to the right credential.
                Assert.Equal(providerName, args.ProviderName);
                // The runtime also forwards the owning session id so a
                // client-level shared callback can resolve the session.
                Assert.False(string.IsNullOrEmpty(args.SessionId));
                acquiredFor.Add(providerName);
                return Task.FromResult(tokenByProvider[providerName]);
            };

        var handler = new CapturingRequestHandler();
        await using var client = CreateClientWith(handler);
        await client.StartAsync();

        var providers = new List<NamedProviderConfig>
        {
            new()
            {
                Name = "red",
                Type = "openai",
                WireApi = "completions",
                BaseUrl = RedBaseUrl,
                BearerTokenProvider = MakeCallback("red"),
            },
            new()
            {
                Name = "blue",
                Type = "openai",
                WireApi = "completions",
                BaseUrl = BlueBaseUrl,
                BearerTokenProvider = MakeCallback("blue"),
            },
        };
        var models = new List<ProviderModelConfig>
        {
            new() { Id = "default", Provider = "red", WireModel = "byok-gpt-4o" },
            new() { Id = "default", Provider = "blue", WireModel = "byok-gpt-4o" },
        };

        await RunTurnAsync(client, providers, models, "red/default", "What is 3+3?");
        await RunTurnAsync(client, providers, models, "blue/default", "What is 4+4?");

        // Each provider's turn was authenticated with its own token AND that token
        // was delivered to that provider's endpoint, proving per-provider dispatch
        // (not a single session-global credential).
        Assert.Equal($"Bearer {tokenByProvider["red"]}", handler.AuthHeaderForHost(RedHost));
        Assert.Equal($"Bearer {tokenByProvider["blue"]}", handler.AuthHeaderForHost(BlueHost));
        Assert.Contains("red", acquiredFor);
        Assert.Contains("blue", acquiredFor);
    }
}

/// <summary>
/// A <see cref="CopilotRequestHandler"/> used in place of a real HTTP listener.
/// The runtime invokes <see cref="SendRequestAsync"/> for every model-layer HTTP
/// request. Requests aimed at a fake BYOK host (<c>*.invalid</c>) are captured —
/// recording the <c>Authorization</c> header the runtime applied after calling
/// the provider's <c>BearerTokenProvider</c> callback over the session-scoped
/// <c>providerToken.getToken</c> RPC — and answered with a synthetic <c>404</c>
/// (a non-retryable status, so each outbound model request yields exactly one
/// capture). Every other request (CAPI bootstrap: model catalog, policy, …) is
/// served a synthetic well-formed response so the bootstrap never touches the
/// network.
/// </summary>
internal sealed class CapturingRequestHandler : CopilotRequestHandler
{
    private readonly ConcurrentQueue<CapturedRequest> _captures = new();

    protected override Task<HttpResponseMessage> SendRequestAsync(HttpRequestMessage request, CopilotRequestContext ctx)
    {
        var uri = request.RequestUri!;
        if (uri.Host.EndsWith(".invalid", StringComparison.Ordinal))
        {
            _captures.Enqueue(new CapturedRequest(
                uri.Host,
                request.Headers.TryGetValues("Authorization", out var values)
                    ? string.Join(", ", values)
                    : null));

            var response = new HttpResponseMessage(HttpStatusCode.NotFound)
            {
                Content = new StringContent(
                    "{\"error\":{\"message\":\"fake byok endpoint\"}}",
                    System.Text.Encoding.UTF8,
                    "application/json"),
            };
            return Task.FromResult(response);
        }

        // CAPI bootstrap (model catalog, policy, …) — answered off-network.
        return Task.FromResult(RecordingRequestHandler.BuildNonInferenceResponse(uri.ToString()));
    }

    /// <summary>The <c>Authorization</c> headers captured across BYOK requests, in arrival order.</summary>
    public IReadOnlyList<string> AuthHeaders() =>
        [.. _captures.Select(c => c.Authorization).Where(v => v is not null).Cast<string>()];

    /// <summary>The <c>Authorization</c> header captured for requests aimed at <paramref name="host"/>, if any.</summary>
    public string? AuthHeaderForHost(string host) =>
        _captures.FirstOrDefault(c => string.Equals(c.Host, host, StringComparison.Ordinal))?.Authorization;

    private sealed record CapturedRequest(string Host, string? Authorization);
}
