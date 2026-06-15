/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Text.RegularExpressions;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ProviderEndpointE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "provider-endpoint", output)
{
    /// <summary>
    /// Creates a client with the provider-endpoint API opt-in env var
    /// (COPILOT_ALLOW_GET_PROVIDER_ENDPOINT) set on the CLI subprocess.
    /// </summary>
    private CopilotClient CreateProviderEndpointClient()
    {
        var env = new Dictionary<string, string>(Ctx.GetEnvironment())
        {
            ["COPILOT_ALLOW_GET_PROVIDER_ENDPOINT"] = "true",
        };
        return Ctx.CreateClient(options: new CopilotClientOptions { Environment = env });
    }

    [Fact]
    public async Task ShouldReturnByokProviderEndpointWhenCustomProviderIsConfigured()
    {
        var client = CreateProviderEndpointClient();

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Provider = new ProviderConfig
            {
                Type = "openai",
                WireApi = "completions",
                BaseUrl = "https://api.example.test/v1",
                ApiKey = "byok-secret",
                Headers = new Dictionary<string, string> { ["X-Custom-Header"] = "byok-yes" },
            },
        });

        try
        {
            var endpoint = await session.Rpc.Provider.GetEndpointAsync();

            Assert.Equal(ProviderEndpointType.Openai, endpoint.Type);
            Assert.Equal(ProviderEndpointWireApi.Completions, endpoint.WireApi);
            Assert.Equal("https://api.example.test/v1", endpoint.BaseUrl);
            Assert.Equal("byok-secret", endpoint.ApiKey);
            Assert.Equal("byok-yes", endpoint.Headers["X-Custom-Header"]);
            // BYOK sessions never issue a CAPI session token.
            Assert.Null(endpoint.SessionToken);
        }
        finally
        {
            try { await session.DisposeAsync(); }
            catch { /* disconnect may fail since the BYOK provider URL is fake */ }
        }
    }

    [Fact]
    public async Task ShouldReturnCapiProviderEndpointForOAuthAuthenticatedSession()
    {
        var client = CreateProviderEndpointClient();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var endpoint = await session.Rpc.Provider.GetEndpointAsync();

        Assert.True(
            endpoint.Type == ProviderEndpointType.Openai
            || endpoint.Type == ProviderEndpointType.Azure
            || endpoint.Type == ProviderEndpointType.Anthropic,
            $"unexpected endpoint.Type {endpoint.Type}");
        // wireApi is omitted for anthropic; otherwise one of the OpenAI shapes.
        if (endpoint.Type != ProviderEndpointType.Anthropic)
        {
            Assert.True(
                endpoint.WireApi == ProviderEndpointWireApi.Completions
                || endpoint.WireApi == ProviderEndpointWireApi.Responses,
                $"unexpected endpoint.WireApi {endpoint.WireApi}");
        }

        // CAPI baseUrl is the (proxy) Copilot API URL injected by the harness.
        Assert.Matches(@"^https?://", endpoint.BaseUrl);

        // For CAPI OAuth sessions the apiKey is the resolved GitHub bearer.
        Assert.False(string.IsNullOrEmpty(endpoint.ApiKey));

        // Standard CAPI headers should be present, and Authorization is
        // surfaced as the runtime sends it (`Bearer <apiKey>`).
        Assert.False(string.IsNullOrEmpty(endpoint.Headers["Copilot-Integration-Id"]));
        Assert.Matches(new Regex("Copilot", RegexOptions.IgnoreCase), endpoint.Headers["User-Agent"]);
        Assert.False(string.IsNullOrEmpty(endpoint.Headers["X-GitHub-Api-Version"]));
        Assert.Matches(@"[0-9a-f-]{8,}", endpoint.Headers["X-Interaction-Id"]);
        Assert.Equal($"Bearer {endpoint.ApiKey}", endpoint.Headers["Authorization"]);

        // When the omit-modelId path returned an auto-mode session token, it
        // must use the documented header name. The harness may have a non-auto
        // model selected, in which case the field is simply omitted.
        if (endpoint.SessionToken != null)
        {
            Assert.Equal("Copilot-Session-Token", endpoint.SessionToken.Header);
            Assert.False(string.IsNullOrEmpty(endpoint.SessionToken.Token));
            if (endpoint.SessionToken.ExpiresAt.HasValue)
            {
                Assert.True(endpoint.SessionToken.ExpiresAt.Value > DateTimeOffset.MinValue);
            }
        }
    }
}
