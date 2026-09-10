/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class SessionEventSerializationTests
{
    public static TheoryData<AutoTier?, string?> AutoTiers => new()
    {
        { AutoTier.Efficiency, "efficiency" },
        { AutoTier.Balance, "balance" },
        { AutoTier.Intelligence, "intelligence" },
        { null, null },
    };

    [Theory]
    [MemberData(nameof(AutoTiers))]
    public void SessionEvent_Deserializes_AutoTier(AutoTier? expectedTier, string? wireTier)
    {
        foreach (var eventType in new[] { "session.start", "session.resume" })
        {
            var autoTierProperty = wireTier is null ? "" : $""", "autoTier": "{wireTier}" """;
            var json = $$"""
                {
                    "id": "11111111-1111-1111-1111-111111111111",
                    "timestamp": "2026-08-28T00:00:00Z",
                    "parentId": null,
                    "type": "{{eventType}}",
                    "data": {
                        "sessionId": "test-session", "version": 1,
                        "producer": "copilot", "copilotVersion": "1.0.82-1",
                        "startTime": "2026-08-28T00:00:00Z",
                        "resumeTime": "2026-08-28T00:00:00Z", "eventCount": 1
                        {{autoTierProperty}}
                    }
                }
                """;

            var sessionEvent = SessionEvent.FromJson(json);
            var actualTier = eventType == "session.start"
                ? Assert.IsType<SessionStartEvent>(sessionEvent).Data.AutoTier
                : Assert.IsType<SessionResumeEvent>(sessionEvent).Data.AutoTier;
            Assert.Equal(expectedTier, actualTier);
        }
    }

    [Theory]
    [InlineData("message-1")]
    [InlineData(null)]
    public void UserMessageEvent_MessageId_UsesCamelCaseAndIsOptional(string? messageId)
    {
        var messageIdProperty = messageId is null ? "" : $""", "messageId": "{messageId}" """;
        var json = $$"""
            {
                "id": "11111111-1111-1111-1111-111111111111",
                "timestamp": "2026-08-28T00:00:00Z",
                "parentId": null,
                "type": "user.message",
                "data": {
                    "content": "hello"
                    {{messageIdProperty}}
                }
            }
            """;

        var sessionEvent = Assert.IsType<UserMessageEvent>(SessionEvent.FromJson(json));
        Assert.Equal(messageId, sessionEvent.Data.MessageId);

        using var document = JsonDocument.Parse(sessionEvent.ToJson());
        var data = document.RootElement.GetProperty("data");
        if (messageId is null)
        {
            Assert.False(data.TryGetProperty("messageId", out _));
        }
        else
        {
            Assert.Equal(messageId, data.GetProperty("messageId").GetString());
        }
    }

    public static TheoryData<string> AutoTierSwitchFailureReasons =>
    [
        "policy_rejected",
        "request_failed",
        "setup_failed",
        "unsupported",
    ];

    [Theory]
    [MemberData(nameof(AutoTierSwitchFailureReasons))]
    public void SessionEvent_Deserializes_AutoTierSwitchFailed(string wireReason)
    {
        var json = $$"""
            {
                "id": "11111111-1111-1111-1111-111111111111",
                "timestamp": "2026-08-28T00:00:00Z",
                "parentId": null,
                "type": "session.auto_tier_switch_failed",
                "data": {
                    "effectiveAutoTier": "balance",
                    "requestedAutoTier": "intelligence",
                    "reason": "{{wireReason}}"
                }
            }
            """;

        var sessionEvent = SessionEvent.FromJson(json);

        var data = Assert.IsType<SessionAutoTierSwitchFailedEvent>(sessionEvent).Data;
        Assert.Equal(new AutoTierSwitchFailureReason(wireReason), data.Reason);
        Assert.Equal(AutoTier.Balance, data.EffectiveAutoTier);
        Assert.Equal(AutoTier.Intelligence, data.RequestedAutoTier);
    }

    [Fact]
    public void SessionEvent_Deserializes_AutoTierSwitchFailed_WithNullRequestedTier()
    {
        // A null requested tier means the attempt to return to provider-default
        // Auto routing is what failed.
        var json = """
            {
                "id": "11111111-1111-1111-1111-111111111111",
                "timestamp": "2026-08-28T00:00:00Z",
                "parentId": null,
                "type": "session.auto_tier_switch_failed",
                "data": {
                    "effectiveAutoTier": "efficiency",
                    "requestedAutoTier": null,
                    "reason": "unsupported"
                }
            }
            """;

        var sessionEvent = SessionEvent.FromJson(json);

        var data = Assert.IsType<SessionAutoTierSwitchFailedEvent>(sessionEvent).Data;
        Assert.Null(data.RequestedAutoTier);
        Assert.Equal(AutoTier.Efficiency, data.EffectiveAutoTier);
        Assert.Equal(AutoTierSwitchFailureReason.Unsupported, data.Reason);
    }

    public static TheoryData<SessionEvent, string> JsonElementBackedEvents => new()
    {
        {
            new AssistantMessageEvent
            {
                Id = Guid.Parse("11111111-1111-1111-1111-111111111111"),
                Timestamp = DateTimeOffset.Parse("2026-03-15T21:26:02.642Z"),
                ParentId = Guid.Parse("22222222-2222-2222-2222-222222222222"),
                AgentId = "agent-1",
                Data = new AssistantMessageData
                {
                    MessageId = "msg-1",
                    Content = "",
                    ToolRequests =
                    [
                        new AssistantMessageToolRequest
                        {
                            ToolCallId = "call-1",
                            Name = "view",
                            Arguments = ParseJsonElement("""{"path":"README.md"}"""),
                            Type = AssistantMessageToolRequestType.Function,
                        },
                    ],
                },
            },
            "assistant.message"
        },
        {
            new ToolExecutionStartEvent
            {
                Id = Guid.Parse("33333333-3333-3333-3333-333333333333"),
                Timestamp = DateTimeOffset.Parse("2026-03-15T21:26:02.642Z"),
                ParentId = Guid.Parse("44444444-4444-4444-4444-444444444444"),
                Data = new ToolExecutionStartData
                {
                    ToolCallId = "call-1",
                    ToolName = "view",
                    Arguments = ParseJsonElement("""{"path":"README.md"}"""),
                },
            },
            "tool.execution_start"
        },
        {
            new ToolExecutionCompleteEvent
            {
                Id = Guid.Parse("55555555-5555-5555-5555-555555555555"),
                Timestamp = DateTimeOffset.Parse("2026-03-15T21:26:02.642Z"),
                ParentId = Guid.Parse("66666666-6666-6666-6666-666666666666"),
                Data = new ToolExecutionCompleteData
                {
                    ToolCallId = "call-1",
                    Success = true,
                    Result = new ToolExecutionCompleteResult
                    {
                        Content = "ok",
                        DetailedContent = "ok",
                    },
                    ToolTelemetry = new Dictionary<string, JsonElement>
                    {
                        ["properties"] = ParseJsonElement("""{"command":"view"}"""),
                        ["metrics"] = ParseJsonElement("""{"resultLength":2}"""),
                    },
                },
            },
            "tool.execution_complete"
        },
        {
            new SessionShutdownEvent
            {
                Id = Guid.Parse("77777777-7777-7777-7777-777777777777"),
                Timestamp = DateTimeOffset.Parse("2026-03-15T21:26:52.987Z"),
                ParentId = Guid.Parse("88888888-8888-8888-8888-888888888888"),
                Data = new SessionShutdownData
                {
                    ShutdownType = ShutdownType.Routine,
                    TotalApiDuration = TimeSpan.FromMilliseconds(100),
                    SessionStartTime = 1773609948932,
                    CodeChanges = new ShutdownCodeChanges
                    {
                        LinesAdded = 1,
                        LinesRemoved = 0,
                        FilesModified = ["README.md"],
                    },
                    ModelMetrics = new Dictionary<string, ShutdownModelMetric>
                    {
                        ["gpt-5.4"] = new ShutdownModelMetric
                        {
                            Requests = new ShutdownModelMetricRequests { Count = 1, Cost = 1 },
                            TokenDetails = new Dictionary<string, ShutdownModelMetricTokenDetail>
                            {
                                ["input"] = new ShutdownModelMetricTokenDetail { TokenCount = 10 },
                            },
                            TotalNanoAiu = 123,
                            Usage = new ShutdownModelMetricUsage
                            {
                                InputTokens = 10,
                                OutputTokens = 5,
                                CacheReadTokens = 0,
                                CacheWriteTokens = 0,
                            },
                        },
                    },
                    CurrentModel = "gpt-5.4",
                    TokenDetails = new Dictionary<string, ShutdownTokenDetail>
                    {
                        ["input"] = new ShutdownTokenDetail { TokenCount = 10 },
                    },
                    TotalNanoAiu = 123,
                },
            },
            "session.shutdown"
        },
        {
            new SystemNotificationEvent
            {
                Id = Guid.Parse("99999999-9999-9999-9999-999999999999"),
                Timestamp = DateTimeOffset.Parse("2026-03-15T21:26:53.987Z"),
                ParentId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                Data = new SystemNotificationData
                {
                    Content = "<system_notification>Instruction discovered</system_notification>",
                    Kind = new SystemNotificationInstructionDiscovered
                    {
                        Description = "AGENTS.md from src/",
                        SourcePath = "src/AGENTS.md",
                        TriggerFile = "src/Program.cs",
                        TriggerTool = "view",
                    },
                },
            },
            "system.notification"
        },
        {
            new McpOauthRequiredEvent
            {
                Id = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                Timestamp = DateTimeOffset.Parse("2026-03-15T21:26:54.987Z"),
                ParentId = Guid.Parse("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                Data = new McpOauthRequiredData
                {
                    RequestId = "oauth-request",
                    Reason = McpOauthRequestReason.Initial,
                    ServerName = "oauth-server",
                    ServerUrl = "https://example.com/mcp",
                    StaticClientConfig = new McpOauthRequiredStaticClientConfig
                    {
                        ClientId = "client-id",
                        ClientSecret = "static-secret",
                        GrantType = "client_credentials",
                        PublicClient = false,
                    },
                    WwwAuthenticateParams = new McpOauthWWWAuthenticateParams
                    {
                        ResourceMetadataUrl = "https://example.com/.well-known/oauth-protected-resource",
                    },
                    ResourceMetadata = """{"resource":"https://example.com/mcp"}""",
                },
            },
            "mcp.oauth_required"
        },
        {
            new AssistantMessageStartEvent
            {
                Id = Guid.Parse("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                Timestamp = DateTimeOffset.Parse("2026-03-15T21:26:55.987Z"),
                ParentId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"),
                Data = new AssistantMessageStartData
                {
                    MessageId = "msg-start-1",
                    Phase = "main",
                },
            },
            "assistant.message_start"
        }
    };

    private static JsonElement ParseJsonElement(string json)
    {
        using var document = JsonDocument.Parse(json);
        return document.RootElement.Clone();
    }

    [Theory]
    [MemberData(nameof(JsonElementBackedEvents))]
    public void SessionEvent_ToJson_RoundTrips_JsonElementBackedPayloads(SessionEvent sessionEvent, string expectedType)
    {
        var serialized = sessionEvent.ToJson();

        using var document = JsonDocument.Parse(serialized);
        var root = document.RootElement;

        Assert.Equal(expectedType, root.GetProperty("type").GetString());

        switch (expectedType)
        {
            case "assistant.message":
                Assert.Equal("agent-1", root.GetProperty("agentId").GetString());
                Assert.Equal(
                    "README.md",
                    root.GetProperty("data")
                        .GetProperty("toolRequests")[0]
                        .GetProperty("arguments")
                        .GetProperty("path")
                        .GetString());
                break;

            case "tool.execution_start":
                Assert.Equal(
                    "README.md",
                    root.GetProperty("data")
                        .GetProperty("arguments")
                        .GetProperty("path")
                        .GetString());
                break;

            case "tool.execution_complete":
                Assert.Equal(
                    "view",
                    root.GetProperty("data")
                        .GetProperty("toolTelemetry")
                        .GetProperty("properties")
                        .GetProperty("command")
                        .GetString());
                break;

            case "session.shutdown":
                Assert.Equal(
                    1,
                    root.GetProperty("data")
                        .GetProperty("modelMetrics")
                        .GetProperty("gpt-5.4")
                        .GetProperty("requests")
                        .GetProperty("count")
                        .GetInt32());
                Assert.Equal(
                    123,
                    root.GetProperty("data")
                        .GetProperty("totalNanoAiu")
                        .GetInt32());
                Assert.Equal(
                    10,
                    root.GetProperty("data")
                        .GetProperty("tokenDetails")
                        .GetProperty("input")
                        .GetProperty("tokenCount")
                        .GetInt32());
                Assert.Equal(
                    10,
                    root.GetProperty("data")
                        .GetProperty("modelMetrics")
                        .GetProperty("gpt-5.4")
                        .GetProperty("tokenDetails")
                        .GetProperty("input")
                        .GetProperty("tokenCount")
                        .GetInt32());
                break;

            case "system.notification":
                Assert.Equal(
                    "instruction_discovered",
                    root.GetProperty("data")
                        .GetProperty("kind")
                        .GetProperty("type")
                        .GetString());
                Assert.Equal(
                    "src/AGENTS.md",
                    root.GetProperty("data")
                        .GetProperty("kind")
                        .GetProperty("sourcePath")
                        .GetString());
                break;

            case "mcp.oauth_required":
                Assert.Equal(
                    "client_credentials",
                    root.GetProperty("data")
                        .GetProperty("staticClientConfig")
                        .GetProperty("grantType")
                        .GetString());
                Assert.Equal(
                    "static-secret",
                    root.GetProperty("data")
                        .GetProperty("staticClientConfig")
                        .GetProperty("clientSecret")
                        .GetString());
                Assert.Equal(
                    """{"resource":"https://example.com/mcp"}""",
                    root.GetProperty("data")
                        .GetProperty("resourceMetadata")
                        .GetString());
                break;

            case "assistant.message_start":
                Assert.Equal(
                    "msg-start-1",
                    root.GetProperty("data")
                        .GetProperty("messageId")
                        .GetString());
                Assert.Equal(
                    "main",
                    root.GetProperty("data")
                        .GetProperty("phase")
                        .GetString());
                break;
        }
    }

    [Fact]
    public void McpOauthRequiredData_Allows_Missing_Optional_Metadata()
    {
        const string json = """
            {
              "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "timestamp": "2026-03-15T21:26:54.987Z",
              "parentId": null,
              "type": "mcp.oauth_required",
              "data": {
                "requestId": "oauth-request",
                "reason": "initial",
                "serverName": "oauth-server",
                "serverUrl": "https://example.com/mcp"
              }
            }
            """;

        var authEvent = Assert.IsType<McpOauthRequiredEvent>(SessionEvent.FromJson(json));
        Assert.Null(authEvent.Data.WwwAuthenticateParams);
        Assert.Null(authEvent.Data.ResourceMetadata);
    }

    [Fact]
    public void McpOauthRequiredData_Preserves_Static_Client_Secret()
    {
        const string json = """
            {
              "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "timestamp": "2026-03-15T21:26:54.987Z",
              "parentId": null,
              "type": "mcp.oauth_required",
              "data": {
                "requestId": "oauth-request",
                "reason": "initial",
                "serverName": "oauth-server",
                "serverUrl": "https://example.com/mcp",
                "staticClientConfig": {
                  "clientId": "static-client",
                  "clientSecret": "static-secret",
                  "grantType": "client_credentials",
                  "publicClient": false
                }
              }
            }
            """;

        var authEvent = Assert.IsType<McpOauthRequiredEvent>(SessionEvent.FromJson(json));

        Assert.NotNull(authEvent.Data.StaticClientConfig);
        Assert.Equal("static-secret", authEvent.Data.StaticClientConfig.ClientSecret);
    }

    [Fact]
    public void ManagedSettingsResolvedData_Preserves_Client_Provenance()
    {
        Assert.Equal("server", ManagedSettingsResolvedSource.Server.Value);
        Assert.Equal("device", ManagedSettingsResolvedSource.Device.Value);
        Assert.Equal("client", ManagedSettingsResolvedSource.Client.Value);
        Assert.Equal("mixed", ManagedSettingsResolvedSource.Mixed.Value);
        Assert.Equal("none", ManagedSettingsResolvedSource.None.Value);

        const string clientJson = """
            {
              "id": "11111111-1111-1111-1111-111111111111",
              "timestamp": "2026-03-15T21:26:54.987Z",
              "parentId": null,
              "type": "session.managed_settings_resolved",
              "data": {
                "source": "client",
                "serverManaged": false,
                "deviceManaged": false,
                "clientManaged": true,
                "failClosed": false,
                "bypassPermissionsDisabled": true,
                "managedKeys": ["permissions"]
              }
            }
            """;

        var clientEvent = Assert.IsType<SessionManagedSettingsResolvedEvent>(
            SessionEvent.FromJson(clientJson));
        Assert.Equal(ManagedSettingsResolvedSource.Client, clientEvent.Data.Source);
        Assert.True(clientEvent.Data.ClientManaged);
        using (var document = JsonDocument.Parse(clientEvent.ToJson()))
        {
            Assert.True(document.RootElement.GetProperty("data").GetProperty("clientManaged").GetBoolean());
        }

        const string mixedJson = """
            {
              "id": "22222222-2222-2222-2222-222222222222",
              "timestamp": "2026-03-15T21:26:54.987Z",
              "parentId": null,
              "type": "session.managed_settings_resolved",
              "data": {
                "source": "mixed",
                "serverManaged": true,
                "deviceManaged": true,
                "failClosed": false,
                "bypassPermissionsDisabled": true,
                "managedKeys": ["permissions"]
              }
            }
            """;

        var mixedEvent = Assert.IsType<SessionManagedSettingsResolvedEvent>(
            SessionEvent.FromJson(mixedJson));
        Assert.Equal(ManagedSettingsResolvedSource.Mixed, mixedEvent.Data.Source);
        Assert.Null(mixedEvent.Data.ClientManaged);
        using var mixedDocument = JsonDocument.Parse(mixedEvent.ToJson());
        Assert.False(mixedDocument.RootElement.GetProperty("data").TryGetProperty("clientManaged", out _));
    }
}
