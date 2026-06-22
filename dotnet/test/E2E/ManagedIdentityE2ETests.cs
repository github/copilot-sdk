/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// End-to-end coverage for Azure managed identity (MI) authentication on a BYOK
/// provider. Proves the full SDK → runtime → Rust credential chain wiring without
/// any real network:
///
///  - The shared <b>mock identity endpoint</b> (<c>test/harness/mockIdentityServer.ts</c>,
///    spawned via <see cref="MockIdentityServer"/>) plays the App Service / Functions
///    managed identity contract (<c>IDENTITY_ENDPOINT</c> + <c>IDENTITY_HEADER</c>). It
///    returns a fixed fake AAD token and records the <c>resource</c> + identity query
///    parameters the runtime asked for.
///  - The shared <b>mock model endpoint</b> (<c>test/harness/mockModelServer.ts</c>,
///    spawned via <see cref="MockModelServer"/>) is the BYOK provider's <c>baseUrl</c>. It
///    records the <c>Authorization</c> header the runtime sent and replies with a minimal
///    chat completion so the turn finishes cleanly.
///
/// Both mock servers are the same shared harness servers the Node SDK uses. The session is
/// configured with <see cref="ProviderConfig.ManagedIdentity"/> (no apiKey/bearerToken), runs
/// one real turn, and we assert the model request carried
/// <c>Authorization: Bearer &lt;fake-token&gt;</c> and that the identity endpoint was asked for
/// the right resource + identity. Because the BYOK base URL is the mock model server (not the
/// replay proxy), the test needs no recorded snapshot and never touches the network.
/// </summary>
public class ManagedIdentityE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "managed_identity", output)
{
    /// <summary>
    /// Spawns both shared mock servers, injects the standard Azure managed
    /// identity env vars (pointing the runtime's credential chain at the mock
    /// identity endpoint), and hands the caller a client whose runtime subprocess
    /// resolves managed identities against those mocks.
    /// </summary>
    private async Task<(CopilotClient Client, MockIdentityServer Identity, MockModelServer Model)> CreateManagedIdentityClientAsync()
    {
        var identity = new MockIdentityServer();
        var model = new MockModelServer();
        try
        {
            await identity.StartAsync();
            await model.StartAsync();

            var env = new Dictionary<string, string>(Ctx.GetEnvironment())
            {
                ["IDENTITY_ENDPOINT"] = identity.Endpoint,
                ["IDENTITY_HEADER"] = identity.Header,
                ["AZURE_TOKEN_CREDENTIALS"] = "ManagedIdentityCredential",
                // Ensure no ambient user-assigned id leaks in from the host environment.
                ["AZURE_CLIENT_ID"] = "",
            };

            var client = Ctx.CreateClient(options: new CopilotClientOptions { Environment = env });
            return (client, identity, model);
        }
        catch
        {
            await identity.DisposeAsync();
            await model.DisposeAsync();
            throw;
        }
    }

    /// <summary>Runs one turn against a BYOK provider that authenticates via managed identity.</summary>
    private static async Task RunTurnAsync(CopilotClient client, ProviderConfig provider)
    {
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Provider = provider,
        });
        try
        {
            await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 5+5?" }, TimeSpan.FromMinutes(3));
        }
        finally
        {
            // disconnect may fail since the BYOK provider is a local mock
            try { await session.DisposeAsync(); } catch { /* ignore */ }
        }
    }

    [Fact]
    public async Task Should_Acquire_System_Assigned_Token_And_Inject_It_As_A_Bearer()
    {
        var (client, identity, model) = await CreateManagedIdentityClientAsync();
        await using var _identity = identity;
        await using var _model = model;

        await RunTurnAsync(client, new ProviderConfig
        {
            Type = "openai",
            WireApi = "completions",
            BaseUrl = model.BaseUrl,
            ModelId = "claude-sonnet-4.5",
            ManagedIdentity = new ManagedIdentityConfig(),
        });

        List<RecordedModelRequest> modelRequests = [];
        await TestHelper.WaitForConditionAsync(
            async () => { modelRequests = await model.GetRecordedRequestsAsync(); return modelRequests.Count >= 1; },
            timeout: TimeSpan.FromSeconds(10),
            timeoutMessage: "Timed out waiting for a model request");

        // The runtime acquired the fake token from the identity endpoint and
        // injected it as the model request's bearer credential.
        Assert.Equal($"Bearer {identity.Token}", modelRequests[0].Authorization);

        // The identity endpoint was hit with the App Service secret header, the
        // default cognitiveservices resource, and NO identity selector (system assigned).
        var identityRequests = await identity.GetRecordedRequestsAsync();
        Assert.NotEmpty(identityRequests);
        Assert.Equal(identity.Header, identityRequests[0].IdentityHeader);
        Assert.Equal("https://cognitiveservices.azure.com", identityRequests[0].Resource);
        Assert.Empty(identityRequests[0].IdentityParams);
    }

    [Fact]
    public async Task Should_Acquire_User_Assigned_ClientId_Token_With_Custom_Scope()
    {
        var (client, identity, model) = await CreateManagedIdentityClientAsync();
        await using var _identity = identity;
        await using var _model = model;

        await RunTurnAsync(client, new ProviderConfig
        {
            Type = "openai",
            WireApi = "completions",
            BaseUrl = model.BaseUrl,
            ModelId = "claude-sonnet-4.5",
            ManagedIdentity = new ManagedIdentityConfig
            {
                ClientId = "11111111-2222-3333-4444-555555555555",
                Scope = "https://gateway.example.test/.default",
            },
        });

        List<RecordedModelRequest> modelRequests = [];
        await TestHelper.WaitForConditionAsync(
            async () => { modelRequests = await model.GetRecordedRequestsAsync(); return modelRequests.Count >= 1; },
            timeout: TimeSpan.FromSeconds(10),
            timeoutMessage: "Timed out waiting for a model request");

        Assert.Equal($"Bearer {identity.Token}", modelRequests[0].Authorization);

        var identityRequests = await identity.GetRecordedRequestsAsync();
        Assert.NotEmpty(identityRequests);
        Assert.Equal(identity.Header, identityRequests[0].IdentityHeader);
        // The custom scope's resource (scope minus the /.default suffix).
        Assert.Equal("https://gateway.example.test", identityRequests[0].Resource);
        // The user-assigned client id was sent as the App Service client_id param.
        Assert.Equal(
            new Dictionary<string, string> { ["client_id"] = "11111111-2222-3333-4444-555555555555" },
            identityRequests[0].IdentityParams);
    }

    [Fact]
    public async Task Should_Reuse_Cached_Token_Across_Turns_While_Valid()
    {
        var (client, identity, model) = await CreateManagedIdentityClientAsync();
        await using var _identity = identity;
        await using var _model = model;

        // A unique scope keeps this turn's cache key isolated (the runtime caches
        // process-wide by scope + identity). Default lifetime (1h) is well outside
        // the runtime's 5-minute refresh buffer, so the first token stays cached.
        var provider = new ProviderConfig
        {
            Type = "openai",
            WireApi = "completions",
            BaseUrl = model.BaseUrl,
            ModelId = "claude-sonnet-4.5",
            ManagedIdentity = new ManagedIdentityConfig { Scope = "https://cache-test.example.test/.default" },
        };

        await RunTurnAsync(client, provider);
        await RunTurnAsync(client, provider);

        // Two turns, but the identity endpoint was only hit once: the second turn
        // reused the cached token instead of re-acquiring one.
        var identityRequests = await identity.GetRecordedRequestsAsync();
        Assert.Single(identityRequests);

        // Every model request across both turns carried that one cached token.
        var modelRequests = await model.GetRecordedRequestsAsync();
        Assert.True(modelRequests.Count >= 2, $"expected >= 2 model requests, saw {modelRequests.Count}");
        Assert.All(modelRequests, request => Assert.Equal($"Bearer {identity.Token}", request.Authorization));
    }

    [Fact]
    public async Task Should_Refresh_Token_On_Next_Turn_Once_Within_Expiry_Buffer()
    {
        var (client, identity, model) = await CreateManagedIdentityClientAsync();
        await using var _identity = identity;
        await using var _model = model;

        // Mint short-lived, rotating tokens: a 1-second lifetime is inside the
        // runtime's 5-minute refresh buffer, so the cached token is treated as
        // stale immediately and re-acquired on the next turn. Rotation makes the
        // refreshed token observably different from the first one.
        await identity.ConfigureAsync(expiresInSeconds: 1, rotateTokens: true);

        var provider = new ProviderConfig
        {
            Type = "openai",
            WireApi = "completions",
            BaseUrl = model.BaseUrl,
            ModelId = "claude-sonnet-4.5",
            ManagedIdentity = new ManagedIdentityConfig { Scope = "https://refresh-test.example.test/.default" },
        };

        await RunTurnAsync(client, provider);
        var firstTurnRequests = await model.GetRecordedRequestsAsync();
        var firstTurnBearer = firstTurnRequests.Count > 0 ? firstTurnRequests[^1].Authorization : null;

        await RunTurnAsync(client, provider);
        var secondTurnRequests = await model.GetRecordedRequestsAsync();
        var secondTurnBearer = secondTurnRequests.Count > 0 ? secondTurnRequests[^1].Authorization : null;

        // The endpoint was hit again for the second turn rather than serving a cached token.
        var identityRequests = await identity.GetRecordedRequestsAsync();
        Assert.True(identityRequests.Count >= 2, $"expected >= 2 identity requests, saw {identityRequests.Count}");

        // The second turn's model request carried a freshly minted token, not the
        // one from the first turn — proving automatic refresh.
        Assert.NotEqual(firstTurnBearer, secondTurnBearer);
        Assert.Equal($"Bearer {identityRequests[^1].IssuedToken}", secondTurnBearer);
    }
}
