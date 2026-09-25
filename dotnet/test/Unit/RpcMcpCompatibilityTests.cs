/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Text.Json;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Fact]
    public void Existing_Mcp_Overload_Signatures_And_Defaults_Are_Preserved()
    {
        (Type Owner, string Name, Type[] Parameters, int Required)[] signatures =
        [
            (typeof(McpApi), "EnableAsync", [typeof(string), typeof(CancellationToken)], 1),
            (typeof(McpApi), "DisableAsync", [typeof(string), typeof(CancellationToken)], 1),
            (typeof(McpApi), "StopServerAsync", [typeof(string), typeof(CancellationToken)], 1),
            (typeof(McpApi), "StartServerAsync", [typeof(string), typeof(object), typeof(CancellationToken)], 1),
            (typeof(McpApi), "RestartServerAsync", [typeof(string), typeof(object), typeof(CancellationToken)], 1),
            (typeof(McpOauthApi), "ProbeAsync", [typeof(string), typeof(CancellationToken)], 1),
            (typeof(McpOauthApi), "LoginAsync",
                [typeof(string), typeof(bool?), typeof(string), typeof(string), typeof(string),
                    typeof(string), typeof(bool?), typeof(McpOauthLoginGrantType?), typeof(CancellationToken)], 1),
            (typeof(ServerMcpApi), "PlanInstallAsync",
                [typeof(CatalogClientContract), typeof(McpPlanInstallSource), typeof(McpPlanScope?),
                    typeof(CancellationToken)], 2),
            (typeof(ServerCatalogApi), "SearchAsync",
                [typeof(CatalogClientContract), typeof(string), typeof(int?), typeof(IList<CatalogCandidateKind>),
                    typeof(CatalogSearchPage), typeof(CancellationToken)], 2),
        ];
        foreach (var (owner, name, types, required) in signatures)
        {
            var method = owner.GetMethod(name, types);
            Assert.NotNull(method);
            var parameters = method.GetParameters();
            Assert.Equal("cancellationToken", parameters[^1].Name);
            Assert.All(parameters.Take(required), parameter => Assert.False(parameter.IsOptional));
            Assert.All(parameters.Skip(required), parameter =>
            {
                Assert.True(parameter.IsOptional);
                Assert.True(parameter.HasDefaultValue);
                Assert.Null(parameter.DefaultValue);
            });
        }
    }

    [Fact]
    public async Task Existing_Positional_Mcp_Calls_Omit_Owned_Identity()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = McpCompatibilityResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();
        var cancellation = CancellationToken.None;
        using var config = JsonDocument.Parse("""{ "command": "node" }""");

        await session.Rpc.Mcp.EnableAsync("manual", cancellation);
        await session.Rpc.Mcp.DisableAsync("manual", cancellation);
        await session.Rpc.Mcp.StartServerAsync("manual", config.RootElement, cancellation);
        await session.Rpc.Mcp.RestartServerAsync("manual", default, cancellation);
        await session.Rpc.Mcp.StopServerAsync("manual", cancellation);
        await session.Rpc.Mcp.Oauth.ProbeAsync("manual", cancellation);
        await session.Rpc.Mcp.Oauth.LoginAsync("manual", false, "client", "success", null, null, true, null, cancellation);
        await client.Rpc.Mcp.PlanInstallAsync(
            new CatalogClientContract { ProtocolVersion = 3, RequiredCapabilities = ["mcp-install-planning"] },
            new McpPlanInstallSourceCandidate { CandidateHandle = "candidate", SearchId = "search" },
            McpPlanScope.User,
            cancellation);
        await client.Rpc.Catalog.SearchAsync(
            new CatalogClientContract { ProtocolVersion = 3, RequiredCapabilities = ["mcp-install-planning"] },
            "catalogue query",
            4,
            null,
            null,
            cancellation);

        var requests = server.Requests.ToArray();
        Assert.Equal(9, requests.Length);
        foreach (var request in requests)
        {
            Assert.False(request.Params.TryGetProperty("expectedInstallationId", out _));
            Assert.False(request.Params.TryGetProperty("loginId", out _));
            Assert.False(request.Params.TryGetProperty("policySessionId", out _));
            if (request.Method.StartsWith("session.", StringComparison.Ordinal))
            {
                Assert.Equal(session.SessionId, request.Params.GetProperty("sessionId").GetString());
            }
        }
        var start = Assert.Single(requests, request => request.Method == "session.mcp.startServer");
        Assert.Equal("node", start.Params.GetProperty("config").GetProperty("command").GetString());
        var restart = Assert.Single(requests, request => request.Method == "session.mcp.restartServer");
        Assert.False(restart.Params.TryGetProperty("config", out _));
        var plan = Assert.Single(requests, request => request.Method == "mcp.planInstall");
        Assert.Equal("user", plan.Params.GetProperty("scope").GetString());
        var search = Assert.Single(requests, request => request.Method == "catalog.search");
        Assert.Equal("catalogue query", search.Params.GetProperty("query").GetString());
        Assert.Equal(4, search.Params.GetProperty("limit").GetInt32());
    }

    [Fact]
    public async Task Expanded_Mcp_Requests_Keep_Identity_And_Sdk_Owned_Session_Binding()
    {
        // This fixture checks SDK wire encoding, not runtime admission.
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = McpCompatibilityResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();
        const string installationId = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        const string loginId = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

        await session.Rpc.Mcp.StartServerAsync(new McpStartServerRequest
        {
            ServerName = "owned",
            ExpectedInstallationId = installationId,
        });
        await session.Rpc.Mcp.Oauth.LoginAsync(new McpOauthLoginRequest
        {
            ServerName = "owned",
            ExpectedInstallationId = installationId,
            LoginId = loginId,
        });
        await client.Rpc.Mcp.PlanInstallAsync(new McpPlanInstallRequest
        {
            Contract = new CatalogClientContract { ProtocolVersion = 3, RequiredCapabilities = ["mcp-install-planning"] },
            Source = new McpPlanInstallSourceCandidate { CandidateHandle = "candidate", SearchId = "search" },
            Scope = McpPlanScope.User,
            PolicySessionId = session.SessionId,
        });
        await client.Rpc.Catalog.SearchAsync(new CatalogSearchRequest
        {
            Contract = new CatalogClientContract { ProtocolVersion = 3, RequiredCapabilities = ["mcp-install-planning"] },
            Query = "catalogue query",
            PolicySessionId = session.SessionId,
        });

        Assert.Null(typeof(McpApi).GetMethod("StartServerWithRequestAsync"));
        Assert.Null(typeof(McpOauthApi).GetMethod("LoginWithRequestAsync"));
        Assert.Null(typeof(McpStartServerRequest).GetProperty("SessionId"));
        Assert.Null(typeof(McpOauthLoginRequest).GetProperty("SessionId"));
        var requests = server.Requests.ToArray();
        Assert.Equal(4, requests.Length);
        foreach (var request in requests.Take(2))
        {
            Assert.Equal(session.SessionId, request.Params.GetProperty("sessionId").GetString());
            Assert.Equal(installationId, request.Params.GetProperty("expectedInstallationId").GetString());
        }
        var login = requests[1];
        Assert.Equal("session.mcp.oauth.login", login.Method);
        Assert.Equal(loginId, login.Params.GetProperty("loginId").GetString());
        var plan = requests[2];
        Assert.Equal("mcp.planInstall", plan.Method);
        Assert.Equal(session.SessionId, plan.Params.GetProperty("policySessionId").GetString());
        var search = requests[3];
        Assert.Equal("catalog.search", search.Method);
        Assert.Equal("catalogue query", search.Params.GetProperty("query").GetString());
        Assert.Equal(session.SessionId, search.Params.GetProperty("policySessionId").GetString());
    }

    private static Dictionary<string, object?> McpCompatibilityResponse(RpcRequestRecord request) =>
        request.Method switch
        {
            "mcp.planInstall" or "catalog.search" => new Dictionary<string, object?>
            {
                ["kind"] = "unavailable",
                ["reason"] = "host-not-available",
                ["message"] = "SDK wire fixture",
            },
            "session.mcp.oauth.probe" => new Dictionary<string, object?>
            {
                ["status"] = "failed",
                ["error"] = "SDK wire fixture",
            },
            "session.mcp.oauth.login" or "session.mcp.startServer" or "session.mcp.restartServer"
                or "session.mcp.stopServer" or "session.mcp.enable" or "session.mcp.disable" =>
                new Dictionary<string, object?>(),
            _ => throw new InvalidOperationException($"Unexpected MCP compatibility request '{request.Method}'."),
        };
}
#endif
