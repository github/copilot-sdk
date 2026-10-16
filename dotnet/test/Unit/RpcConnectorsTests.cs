/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Text.Json;
using System.Text.Json.Serialization;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Fact]
    public async Task Session_Rpc_Connectors_Reports_Capabilities_Status_And_Catalogs()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = CreateConnectorRpcResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        var connectors = session.Rpc.Connectors;
        Assert.Same(connectors, session.Rpc.Connectors);

        var capabilities = await connectors.GetCapabilitiesAsync();
        Assert.Equal(1, capabilities.ApiVersion);
        Assert.Equal(ConnectorAvailability.Enabled, capabilities.Availability);
        Assert.True(capabilities.ConsentContinuation);
        Assert.Equal(5, capabilities.MaxPollAttempts);
        Assert.Equal(2_000, capabilities.MaxPollIntervalMs);
        Assert.Equal(30_000, capabilities.MaxDeadlineMs);
        Assert.True(capabilities.OpaqueAccountSelection);
        Assert.Null(capabilities.SessionAccountSelection);
        Assert.Null(capabilities.TargetedReconcile);
        Assert.False(capabilities.SessionAccountSelection == true);
        Assert.False(capabilities.TargetedReconcile == true);

        var status = await connectors.GetStatusAsync();
        Assert.Equal("account-1", status.AccountId);
        Assert.Equal(2, status.PendingConnections);
        Assert.Equal(7, status.Catalog?.Revision);
        var runtimeServer = Assert.Single(status.RuntimeServers);
        Assert.Equal("slack", runtimeServer.ConnectorName);
        Assert.Equal("connector-slack", runtimeServer.RuntimeServerId);
        Assert.Equal(ConnectorMcpStatus.Connected, runtimeServer.Status);

        var listed = await connectors.ListAsync("account-1");
        AssertConnectorCatalog(listed, 3, ConnectorCatalogStatus.NotConnected);

        var refreshed = await connectors.RefreshAsync("account-1");
        AssertConnectorCatalog(refreshed, 4, ConnectorCatalogStatus.Connected);

        Assert.Collection(
            server.Requests,
            request => AssertConnectorRequest(
                request,
                "session.connectors.getCapabilities",
                session.SessionId),
            request => AssertConnectorRequest(
                request,
                "session.connectors.getStatus",
                session.SessionId),
            request => AssertConnectorRequest(
                request,
                "session.connectors.list",
                session.SessionId,
                ("accountId", "account-1")),
            request => AssertConnectorRequest(
                request,
                "session.connectors.refresh",
                session.SessionId,
                ("accountId", "account-1")));
    }

    [Fact]
    public async Task Session_Rpc_Connectors_Maps_Connection_Wire_And_Result_Variants()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = CreateConnectorRpcResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        var connected = Assert.IsType<ConnectorConnectResultConnected>(
            await session.Rpc.Connectors.ConnectAsync("account-1", "slack"));
        Assert.Equal("connected", connected.Kind);
        Assert.Equal(8, connected.Status.Catalog?.Revision);

        var consent = Assert.IsType<ConnectorConnectResultConsentRequired>(
            await session.Rpc.Connectors.ReconnectAsync("account-1", "slack"));
        Assert.Equal("consent_required", consent.Kind);
        Assert.Equal("https://example.com/consent", consent.ConsentUrl);
        Assert.Equal("continuation-reconnect", consent.ContinuationId);

        var pending = Assert.IsType<ConnectorConnectResultPending>(
            await session.Rpc.Connectors.ContinueConnectionAsync(
                "continuation-reconnect",
                maxAttempts: 3,
                pollIntervalMs: 1_000,
                deadlineMs: 10_000));
        Assert.Equal("pending", pending.Kind);
        Assert.Equal("continuation-next", pending.ContinuationId);

        Assert.Collection(
            server.Requests,
            request => AssertConnectorRequest(
                request,
                "session.connectors.connect",
                session.SessionId,
                ("accountId", "account-1"),
                ("connectorName", "slack")),
            request => AssertConnectorRequest(
                request,
                "session.connectors.reconnect",
                session.SessionId,
                ("accountId", "account-1"),
                ("connectorName", "slack")),
            request => AssertConnectorRequest(
                request,
                "session.connectors.continueConnection",
                session.SessionId,
                ("continuationId", "continuation-reconnect"),
                ("maxAttempts", 3L),
                ("pollIntervalMs", 1_000L),
                ("deadlineMs", 10_000L)));
    }

    [Fact]
    public async Task Session_Rpc_Connectors_Maps_Disconnect_And_Reconcile()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = CreateConnectorRpcResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        var disconnected = await session.Rpc.Connectors.DisconnectAsync("account-1", "slack");
        Assert.True(disconnected.Disconnected);
        Assert.Equal(9, disconnected.Status.Catalog?.Revision);

        var reconciled = await session.Rpc.Connectors.ReconcileAsync(
            "account-1",
            true,
            CancellationToken.None);
        Assert.Equal(10, reconciled.Catalog?.Revision);
        Assert.Empty(reconciled.RuntimeServers);

        Assert.Collection(
            server.Requests,
            request => AssertConnectorRequest(
                request,
                "session.connectors.disconnect",
                session.SessionId,
                ("accountId", "account-1"),
                ("connectorName", "slack")),
            request => AssertConnectorRequest(
                request,
                "session.connectors.reconcile",
                session.SessionId,
                ("accountId", "account-1"),
                ("refreshCatalog", true)));
    }

    [Theory]
    [InlineData("token")]
    [InlineData("token-provider")]
    [InlineData(null)]
    public async Task Session_Rpc_Connectors_GetAccount_Preserves_Credential_Free_And_Null_Results(string? authType)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = request => request.Method switch
        {
            "session.connectors.getCapabilities" => CreateConnectorCapabilitiesResponse(sessionAccountSelection: true),
            "session.connectors.getAccount" => authType is null ? null : CreateConnectorSessionAccountResponse(authType),
            _ => CreateConnectorRpcResponse(request)
        };
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        var capabilities = await session.Rpc.Connectors.GetCapabilitiesAsync();
        Assert.Equal(ConnectorAvailability.Enabled, capabilities.Availability);
        Assert.True(capabilities.SessionAccountSelection == true);

        var result = await session.Rpc.Connectors.GetAccountAsync();
        if (authType is null)
        {
            Assert.Null(result);
        }
        else
        {
            var account = Assert.IsType<ConnectorSessionAccount>(result);
            Assert.Equal("account-1", account.AccountId);
            Assert.Equal(new AuthInfoType(authType), account.AuthInfo.Type);
            Assert.Equal("github.com", account.AuthInfo.Host);
            Assert.Equal("octocat", account.AuthInfo.Login);

            var json = JsonSerializer.SerializeToElement(
                account, ConnectorSerializationJsonContext.Default.ConnectorSessionAccount);
            Assert.Equal(["accountId", "authInfo"], json.EnumerateObject().Select(property => property.Name).Order());
            var authInfo = json.GetProperty("authInfo");
            Assert.Equal(["host", "login", "type"], authInfo.EnumerateObject().Select(property => property.Name).Order());
            Assert.Equal("account-1", json.GetProperty("accountId").GetString());
            Assert.Equal(authType, authInfo.GetProperty("type").GetString());
            Assert.Equal("github.com", authInfo.GetProperty("host").GetString());
            Assert.Equal("octocat", authInfo.GetProperty("login").GetString());
            Assert.Equal(
                ["AccountId", "AuthInfo"],
                typeof(ConnectorSessionAccount).GetProperties().Select(property => property.Name).Order());
            Assert.Equal(
                ["Host", "Login", "Type"],
                typeof(AuthIdentityMetadata).GetProperties().Select(property => property.Name).Order());
        }

        Assert.Collection(
            server.Requests,
            request => AssertConnectorRequest(request, "session.connectors.getCapabilities", session.SessionId),
            request => AssertConnectorRequest(request, "session.connectors.getAccount", session.SessionId));
    }

    [Theory]
    [InlineData("enabled", null, null, false, false)]
    [InlineData("enabled", false, false, false, false)]
    [InlineData("enabled", true, null, true, false)]
    [InlineData("enabled", null, true, false, true)]
    [InlineData("enabled", true, false, true, false)]
    [InlineData("enabled", false, true, false, true)]
    [InlineData("enabled", true, true, true, true)]
    [InlineData("disabled", true, true, false, false)]
    [InlineData("unavailable", true, true, false, false)]
    public async Task Session_Rpc_Connectors_Capability_Gates_Preserve_Legacy_Requests(
        string availability,
        bool? sessionAccountSelection,
        bool? targetedReconcile,
        bool expectAccount,
        bool expectTarget)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = request => request.Method switch
        {
            "session.connectors.getCapabilities" => CreateConnectorCapabilitiesResponse(
                availability, sessionAccountSelection, targetedReconcile),
            "session.connectors.getAccount" => CreateConnectorSessionAccountResponse("token"),
            _ => CreateConnectorRpcResponse(request)
        };
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        var capabilities = await session.Rpc.Connectors.GetCapabilitiesAsync();
        Assert.Equal(availability, capabilities.Availability.Value);
        Assert.Equal(sessionAccountSelection, capabilities.SessionAccountSelection);
        Assert.Equal(targetedReconcile, capabilities.TargetedReconcile);

        if (capabilities.Availability == ConnectorAvailability.Enabled && capabilities.SessionAccountSelection == true)
        {
            Assert.NotNull(await session.Rpc.Connectors.GetAccountAsync());
        }
        if (capabilities.Availability == ConnectorAvailability.Enabled)
        {
            if (capabilities.TargetedReconcile == true)
            {
                await session.Rpc.Connectors.ReconcileAsync(new ConnectorReconcileRequest
                {
                    AccountId = "account-1",
                    RefreshCatalog = false,
                    ForceConnectorName = "slack",
                });
            }
            else
            {
                await session.Rpc.Connectors.ReconcileAsync("account-1", refreshCatalog: false);
            }
        }

        bool enabled = availability == "enabled";
        var requests = server.Requests;
        Assert.Equal(1 + (expectAccount ? 1 : 0) + (enabled ? 1 : 0), requests.Count);
        AssertConnectorRequest(requests[0], "session.connectors.getCapabilities", session.SessionId);
        int nextRequest = 1;
        if (expectAccount)
        {
            AssertConnectorRequest(requests[nextRequest++], "session.connectors.getAccount", session.SessionId);
        }
        if (enabled)
        {
            if (expectTarget)
            {
                AssertConnectorRequest(
                    requests[nextRequest],
                    "session.connectors.reconcile",
                    session.SessionId,
                    ("accountId", "account-1"),
                    ("refreshCatalog", false),
                    ("forceConnectorName", "slack"));
            }
            else
            {
                AssertConnectorRequest(
                    requests[nextRequest],
                    "session.connectors.reconcile",
                    session.SessionId,
                    ("accountId", "account-1"),
                    ("refreshCatalog", false));
            }
        }
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task Session_Rpc_Connectors_Catalog_Preserves_Optional_Presentation_Metadata(bool includeMetadata)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = request => request.Method == "session.connectors.list"
            ? CreateConnectorCatalogResponse(11, "not_connected", includeMetadata)
            : CreateConnectorRpcResponse(request);
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        var catalog = await session.Rpc.Connectors.ListAsync("account-1");
        AssertConnectorCatalog(catalog, 11, ConnectorCatalogStatus.NotConnected);
        var connector = Assert.Single(catalog.Connectors);
        Assert.Equal(includeMetadata ? "https://example.com/slack.svg" : null, connector.Logo);
        Assert.Equal(includeMetadata ? "standard" : null, connector.Tier);
        Assert.Equal(includeMetadata ? "preview" : null, connector.ReleaseTag);

        var json = JsonSerializer.SerializeToElement(
            connector, ConnectorSerializationJsonContext.Default.ConnectorCatalogEntry);
        string[] expectedFields = includeMetadata
            ? ["description", "displayName", "logo", "name", "releaseTag", "runtimeServerIds", "status", "tier"]
            : ["description", "displayName", "name", "runtimeServerIds", "status"];
        Assert.Equal(expectedFields, json.EnumerateObject().Select(property => property.Name).Order());
        if (includeMetadata)
        {
            Assert.Equal("https://example.com/slack.svg", json.GetProperty("logo").GetString());
            Assert.Equal("standard", json.GetProperty("tier").GetString());
            Assert.Equal("preview", json.GetProperty("releaseTag").GetString());
        }

        AssertConnectorRequest(
            Assert.Single(server.Requests),
            "session.connectors.list",
            session.SessionId,
            ("accountId", "account-1"));
    }

    [Fact]
    public async Task Session_Rpc_Connectors_Unknown_Continuation_Outcome_Is_A_Decode_Error()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = request => request.Method == "session.connectors.continueConnection"
            ? new Dictionary<string, object?>
            {
                ["kind"] = "verification_required",
                ["continuationId"] = "continuation-1",
                ["status"] = CreateConnectorStatusResponse(8)
            }
            : CreateConnectorRpcResponse(request);
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        var failure = await Assert.ThrowsAsync<JsonException>(() => session.Rpc.Connectors.ContinueConnectionAsync(
            "continuation-1",
            maxAttempts: 3,
            pollIntervalMs: 1_000,
            deadlineMs: 10_000).WaitAsync(TimeSpan.FromSeconds(5)));
        Assert.Contains("verification_required", failure.Message);
        AssertConnectorRequest(
            Assert.Single(server.Requests),
            "session.connectors.continueConnection",
            session.SessionId,
            ("continuationId", "continuation-1"),
            ("maxAttempts", 3L),
            ("pollIntervalMs", 1_000L),
            ("deadlineMs", 10_000L));
    }

    private static Dictionary<string, object?> CreateConnectorRpcResponse(RpcRequestRecord request) =>
        request.Method switch
        {
            "session.connectors.getCapabilities" => CreateConnectorCapabilitiesResponse(),
            "session.connectors.getStatus" => CreateConnectorStatusResponse(7, pendingConnections: 2),
            "session.connectors.list" => CreateConnectorCatalogResponse(3, "not_connected"),
            "session.connectors.refresh" => CreateConnectorCatalogResponse(4, "connected"),
            "session.connectors.connect" => new Dictionary<string, object?>
            {
                ["kind"] = "connected",
                ["status"] = CreateConnectorStatusResponse(8)
            },
            "session.connectors.reconnect" => new Dictionary<string, object?>
            {
                ["kind"] = "consent_required",
                ["consentUrl"] = "https://example.com/consent",
                ["continuationId"] = "continuation-reconnect"
            },
            "session.connectors.continueConnection" => new Dictionary<string, object?>
            {
                ["kind"] = "pending",
                ["continuationId"] = "continuation-next"
            },
            "session.connectors.disconnect" => new Dictionary<string, object?>
            {
                ["disconnected"] = true,
                ["status"] = CreateConnectorStatusResponse(9)
            },
            "session.connectors.reconcile" => CreateConnectorStatusResponse(
                10,
                includeRuntimeServer: false),
            _ => throw new InvalidOperationException($"Unexpected Connector RPC method '{request.Method}'.")
        };

    private static Dictionary<string, object?> CreateConnectorCapabilitiesResponse(
        string availability = "enabled",
        bool? sessionAccountSelection = null,
        bool? targetedReconcile = null)
    {
        var result = new Dictionary<string, object?>
        {
            ["apiVersion"] = 1L,
            ["availability"] = availability,
            ["consentContinuation"] = true,
            ["maxDeadlineMs"] = 30_000L,
            ["maxPollAttempts"] = 5L,
            ["maxPollIntervalMs"] = 2_000L,
            ["opaqueAccountSelection"] = true
        };
        if (sessionAccountSelection is not null)
        {
            result["sessionAccountSelection"] = sessionAccountSelection;
        }
        if (targetedReconcile is not null)
        {
            result["targetedReconcile"] = targetedReconcile;
        }
        return result;
    }

    private static Dictionary<string, object?> CreateConnectorSessionAccountResponse(string authType) =>
        new()
        {
            ["accountId"] = "account-1",
            ["authInfo"] = new Dictionary<string, object?>
            {
                ["type"] = authType,
                ["host"] = "github.com",
                ["login"] = "octocat"
            }
        };

    private static Dictionary<string, object?> CreateConnectorStatusResponse(
        long revision,
        long pendingConnections = 0,
        bool includeRuntimeServer = true) =>
        new()
        {
            ["accountId"] = "account-1",
            ["apiVersion"] = 1L,
            ["availability"] = "enabled",
            ["catalog"] = CreateConnectorCatalogResponse(revision, "connected"),
            ["pendingConnections"] = pendingConnections,
            ["runtimeServers"] = includeRuntimeServer
                ? new object?[]
                {
                    new Dictionary<string, object?>
                    {
                        ["connectorName"] = "slack",
                        ["runtimeServerId"] = "connector-slack",
                        ["status"] = "connected"
                    }
                }
                : Array.Empty<object?>()
        };

    private static Dictionary<string, object?> CreateConnectorCatalogResponse(
        long revision,
        string status,
        bool includeMetadata = false)
    {
        var connector = new Dictionary<string, object?>
        {
            ["description"] = "Slack workspace search",
            ["displayName"] = "Slack",
            ["name"] = "slack",
            ["runtimeServerIds"] = new object?[] { "connector-slack" },
            ["status"] = status
        };
        if (includeMetadata)
        {
            connector["logo"] = "https://example.com/slack.svg";
            connector["tier"] = "standard";
            connector["releaseTag"] = "preview";
        }
        return new()
        {
            ["connectors"] = new object?[] { connector },
            ["refreshedAtMs"] = 1_750_000_000_000L,
            ["revision"] = revision
        };
    }

    private static void AssertConnectorCatalog(
        ConnectorCatalogResult catalog,
        long revision,
        ConnectorCatalogStatus status)
    {
        Assert.Equal(revision, catalog.Revision);
        Assert.Equal(1_750_000_000_000L, catalog.RefreshedAtMs);
        var connector = Assert.Single(catalog.Connectors);
        Assert.Equal("Slack", connector.DisplayName);
        Assert.Equal("slack", connector.Name);
        Assert.Equal(status, connector.Status);
        Assert.Equal(["connector-slack"], connector.RuntimeServerIds);
    }

    private static void AssertConnectorRequest(
        RpcRequestRecord request,
        string method,
        string sessionId,
        params (string Name, object Value)[] expectedProperties)
    {
        Assert.Equal(method, request.Method);
        Assert.Equal(expectedProperties.Length + 1, request.Params.EnumerateObject().Count());
        Assert.Equal(sessionId, request.Params.GetProperty("sessionId").GetString());

        foreach (var (name, expectedValue) in expectedProperties)
        {
            var actualValue = request.Params.GetProperty(name);
            switch (expectedValue)
            {
                case string stringValue:
                    Assert.Equal(stringValue, actualValue.GetString());
                    break;
                case long longValue:
                    Assert.Equal(longValue, actualValue.GetInt64());
                    break;
                case bool boolValue:
                    Assert.Equal(boolValue, actualValue.GetBoolean());
                    break;
                default:
                    throw new InvalidOperationException(
                        $"Unsupported expected JSON value type '{expectedValue.GetType().Name}'.");
            }
        }
    }
}

[JsonSourceGenerationOptions(DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
[JsonSerializable(typeof(ConnectorSessionAccount))]
[JsonSerializable(typeof(ConnectorCatalogEntry))]
internal partial class ConnectorSerializationJsonContext : JsonSerializerContext;
#endif
