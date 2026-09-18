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

        var reconciled = await session.Rpc.Connectors.ReconcileAsync("account-1", refreshCatalog: true);
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

    private static Dictionary<string, object?> CreateConnectorRpcResponse(RpcRequestRecord request) =>
        request.Method switch
        {
            "session.connectors.getCapabilities" => new Dictionary<string, object?>
            {
                ["apiVersion"] = 1L,
                ["availability"] = "enabled",
                ["consentContinuation"] = true,
                ["maxDeadlineMs"] = 30_000L,
                ["maxPollAttempts"] = 5L,
                ["maxPollIntervalMs"] = 2_000L,
                ["opaqueAccountSelection"] = true
            },
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

    private static Dictionary<string, object?> CreateConnectorCatalogResponse(long revision, string status) =>
        new()
        {
            ["connectors"] = new object?[]
            {
                new Dictionary<string, object?>
                {
                    ["description"] = "Slack workspace search",
                    ["displayName"] = "Slack",
                    ["name"] = "slack",
                    ["runtimeServerIds"] = new object?[] { "connector-slack" },
                    ["status"] = status
                }
            },
            ["refreshedAtMs"] = 1_750_000_000_000L,
            ["revision"] = revision
        };

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
#endif
