/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Fact]
    public void Existing_Server_Mcp_Overload_Signatures_And_Defaults_Are_Preserved()
    {
        (Type Owner, string Name, Type[] Parameters, int Required)[] signatures =
        [
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
    public void Request_Overloads_Share_The_Legacy_Method_Name_And_Require_Mandatory_Inputs()
    {
        var plan = typeof(ServerMcpApi).GetMethod("PlanInstallAsync", [typeof(McpPlanInstallRequest), typeof(CancellationToken)]);
        var search = typeof(ServerCatalogApi).GetMethod("SearchAsync", [typeof(CatalogSearchRequest), typeof(CancellationToken)]);
        Assert.NotNull(plan);
        Assert.NotNull(search);
        Assert.Null(typeof(ServerMcpApi).GetMethod("PlanInstallWithRequestAsync"));
        Assert.Null(typeof(ServerCatalogApi).GetMethod("SearchWithRequestAsync"));
        foreach (var (type, name) in new[]
        {
            (typeof(McpPlanInstallRequest), nameof(McpPlanInstallRequest.Contract)),
            (typeof(McpPlanInstallRequest), nameof(McpPlanInstallRequest.Source)),
            (typeof(CatalogSearchRequest), nameof(CatalogSearchRequest.Contract)),
            (typeof(CatalogSearchRequest), nameof(CatalogSearchRequest.Query)),
        })
        {
            var property = type.GetProperty(name);
            Assert.NotNull(property);
            Assert.Contains(property.CustomAttributes, attribute =>
                attribute.AttributeType.FullName == "System.Runtime.CompilerServices.RequiredMemberAttribute");
        }
        foreach (var name in new[] { nameof(McpPlanInstallRequest.Scope), nameof(McpPlanInstallRequest.PolicySessionId) })
        {
            Assert.DoesNotContain(typeof(McpPlanInstallRequest).GetProperty(name)!.CustomAttributes, attribute =>
                attribute.AttributeType.FullName == "System.Runtime.CompilerServices.RequiredMemberAttribute");
        }
    }

    [Fact]
    public async Task Request_Overload_Rejects_Missing_Required_Inputs()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = ServerMcpCompatibilityResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await client.StartAsync();
        server.ClearRequests();
        await Assert.ThrowsAsync<ArgumentNullException>(() => client.Rpc.Mcp.PlanInstallAsync(new McpPlanInstallRequest
        {
            Contract = null!,
            Source = new McpPlanInstallSourceCandidate { CandidateHandle = "candidate", SearchId = "search" },
        }));
        await Assert.ThrowsAsync<ArgumentNullException>(() => client.Rpc.Mcp.PlanInstallAsync((McpPlanInstallRequest)null!));
        Assert.Empty(server.Requests);
    }

    [Fact]
    public async Task Existing_Positional_Server_Mcp_Calls_Omit_The_Policy_Session()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = ServerMcpCompatibilityResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();
        var cancellation = CancellationToken.None;

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
        Assert.Equal(2, requests.Length);
        Assert.All(requests, request => Assert.False(request.Params.TryGetProperty("policySessionId", out _)));
        var plan = Assert.Single(requests, request => request.Method == "mcp.planInstall");
        Assert.Equal("user", plan.Params.GetProperty("scope").GetString());
        var search = Assert.Single(requests, request => request.Method == "catalog.search");
        Assert.Equal("catalogue query", search.Params.GetProperty("query").GetString());
        Assert.Equal(4, search.Params.GetProperty("limit").GetInt32());
    }

    [Fact]
    public async Task Same_Name_Request_Overloads_Keep_The_Selected_Policy_Session()
    {
        // This fixture checks SDK wire encoding, not runtime admission.
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = ServerMcpCompatibilityResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

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

        var requests = server.Requests.ToArray();
        Assert.Equal(2, requests.Length);
        var plan = requests[0];
        Assert.Equal("mcp.planInstall", plan.Method);
        Assert.Equal("user", plan.Params.GetProperty("scope").GetString());
        Assert.Equal(session.SessionId, plan.Params.GetProperty("policySessionId").GetString());
        var search = requests[1];
        Assert.Equal("catalog.search", search.Method);
        Assert.Equal("catalogue query", search.Params.GetProperty("query").GetString());
        Assert.Equal(session.SessionId, search.Params.GetProperty("policySessionId").GetString());
    }

    private static Dictionary<string, object?> ServerMcpCompatibilityResponse(RpcRequestRecord request) =>
        request.Method switch
        {
            "mcp.planInstall" or "catalog.search" => new Dictionary<string, object?>
            {
                ["kind"] = "unavailable",
                ["reason"] = "host-not-available",
                ["message"] = "SDK wire fixture",
            },
            _ => throw new InvalidOperationException($"Unexpected server MCP compatibility request '{request.Method}'."),
        };
}
#endif
