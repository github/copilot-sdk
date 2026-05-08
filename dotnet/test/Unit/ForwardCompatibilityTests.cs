/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using System.Text.Json.Serialization;
using Xunit;

namespace GitHub.Copilot.SDK.Test.Unit;

/// <summary>
/// Tests for forward-compatible handling of unknown session event types.
/// Verifies that the SDK gracefully handles event types introduced by newer CLI versions.
/// </summary>
public class ForwardCompatibilityTests
{
    [Fact]
    public void FromJson_KnownEventType_DeserializesNormally()
    {
        var json = """
        {
            "id": "00000000-0000-0000-0000-000000000001",
            "timestamp": "2026-01-01T00:00:00Z",
            "parentId": null,
            "agentId": "agent-1",
            "type": "user.message",
            "data": {
                "content": "Hello"
            }
        }
        """;

        var result = SessionEvent.FromJson(json);

        Assert.IsType<UserMessageEvent>(result);
        Assert.Equal("user.message", result.Type);
        Assert.Equal("agent-1", result.AgentId);
    }

    [Fact]
    public void FromJson_UnknownEventType_ReturnsBaseSessionEvent()
    {
        var json = """
        {
            "id": "12345678-1234-1234-1234-123456789abc",
            "timestamp": "2026-06-15T10:30:00Z",
            "parentId": "abcdefab-abcd-abcd-abcd-abcdefabcdef",
            "agentId": "future-agent",
            "type": "future.feature_from_server",
            "data": { "key": "value" }
        }
        """;

        var result = SessionEvent.FromJson(json);

        Assert.IsType<SessionEvent>(result);
        Assert.Equal("unknown", result.Type);
        Assert.Equal("future-agent", result.AgentId);
    }

    [Fact]
    public void FromJson_UnknownEventType_PreservesBaseMetadata()
    {
        var json = """
        {
            "id": "12345678-1234-1234-1234-123456789abc",
            "timestamp": "2026-06-15T10:30:00Z",
            "parentId": "abcdefab-abcd-abcd-abcd-abcdefabcdef",
            "type": "future.feature_from_server",
            "data": {}
        }
        """;

        var result = SessionEvent.FromJson(json);

        Assert.Equal(Guid.Parse("12345678-1234-1234-1234-123456789abc"), result.Id);
        Assert.Equal(DateTimeOffset.Parse("2026-06-15T10:30:00Z"), result.Timestamp);
        Assert.Equal(Guid.Parse("abcdefab-abcd-abcd-abcd-abcdefabcdef"), result.ParentId);
    }

    [Fact]
    public void FromJson_MultipleEvents_MixedKnownAndUnknown()
    {
        var events = new[]
        {
            """{"id":"00000000-0000-0000-0000-000000000001","timestamp":"2026-01-01T00:00:00Z","parentId":null,"type":"user.message","data":{"content":"Hi"}}""",
            """{"id":"00000000-0000-0000-0000-000000000002","timestamp":"2026-01-01T00:00:00Z","parentId":null,"type":"future.unknown_type","data":{}}""",
            """{"id":"00000000-0000-0000-0000-000000000003","timestamp":"2026-01-01T00:00:00Z","parentId":null,"type":"user.message","data":{"content":"Bye"}}""",
        };

        var results = events.Select(SessionEvent.FromJson).ToList();

        Assert.Equal(3, results.Count);
        Assert.IsType<UserMessageEvent>(results[0]);
        Assert.IsType<SessionEvent>(results[1]);
        Assert.IsType<UserMessageEvent>(results[2]);
    }

    [Fact]
    public void FromJson_KnownEventType_WithExtraUnknownFields_IgnoresExtras()
    {
        // Forward-compat: when the runtime adds new fields to a known event,
        // older SDK versions must ignore them and still successfully parse the event.
        var json = """
        {
            "id": "00000000-0000-0000-0000-000000000001",
            "timestamp": "2026-01-01T00:00:00Z",
            "parentId": null,
            "agentId": "agent-1",
            "type": "user.message",
            "futureEnvelopeField": {"someShape": [1,2,3]},
            "data": {
                "content": "Hello",
                "futureDataField": "ignored",
                "anotherFutureField": {"nested": true}
            }
        }
        """;

        var result = SessionEvent.FromJson(json);

        var msg = Assert.IsType<UserMessageEvent>(result);
        Assert.Equal("Hello", msg.Data.Content);
    }

    [Fact]
    public void FromJson_KnownEventType_WithExtraUnknownEnvelopeFields_IgnoresExtras()
    {
        // Pure envelope-level extra field (no inner data extras).
        var json = """
        {
            "id": "00000000-0000-0000-0000-000000000001",
            "timestamp": "2026-01-01T00:00:00Z",
            "parentId": null,
            "agentId": "agent-1",
            "type": "session.idle",
            "newServerOnlyField": 42,
            "data": {}
        }
        """;

        var result = SessionEvent.FromJson(json);

        Assert.IsType<SessionIdleEvent>(result);
        Assert.Equal("agent-1", result.AgentId);
    }

    [Fact]
    public void FromJson_UnknownEventType_WithUnknownEnumInData_DoesNotThrow()
    {
        // Unknown event types are mapped to base SessionEvent which does not parse data.
        // So unknown enum values inside the data of an unknown event must not throw.
        var json = """
        {
            "id": "00000000-0000-0000-0000-000000000001",
            "timestamp": "2026-01-01T00:00:00Z",
            "parentId": null,
            "type": "future.event_with_enum",
            "data": {
                "futureMode": "future_value_not_in_sdk_enum"
            }
        }
        """;

        var result = SessionEvent.FromJson(json);

        Assert.IsType<SessionEvent>(result);
        Assert.Equal("unknown", result.Type);
    }

    [Fact]
    public void FromJson_KnownEventType_WithUnknownEnumInData_PreservesValue()
    {
        var json = """
        {
            "id": "00000000-0000-0000-0000-000000000001",
            "timestamp": "2026-01-01T00:00:00Z",
            "parentId": null,
            "type": "abort",
            "data": {
                "reason": "future_abort_reason"
            }
        }
        """;

        var result = SessionEvent.FromJson(json);

        var abort = Assert.IsType<AbortEvent>(result);
        Assert.Equal("future_abort_reason", abort.Data.Reason.Value);
    }

    [Fact]
    public void FromJson_KnownEventType_WithNonStringEnumInData_ThrowsJsonException()
    {
        var json = """
        {
            "id": "00000000-0000-0000-0000-000000000001",
            "timestamp": "2026-01-01T00:00:00Z",
            "parentId": null,
            "type": "abort",
            "data": {
                "reason": false
            }
        }
        """;

        var exception = Assert.Throws<JsonException>(() => SessionEvent.FromJson(json));
        Assert.Contains("AbortReason", exception.Message);
    }

    [Fact]
    public void RpcEnum_WithUnknownValue_PreservesValue()
    {
        var mode = JsonSerializer.Deserialize(
            """
            "future_mode"
            """,
            ForwardCompatibilityJsonContext.Default.SessionMode);

        Assert.Equal("future_mode", mode.Value);
        Assert.Equal(
            """
            "future_mode"
            """,
            JsonSerializer.Serialize(mode, ForwardCompatibilityJsonContext.Default.SessionMode));
    }

    [Fact]
    public void RpcEnum_WithNonStringValue_ThrowsJsonException()
    {
        var exception = Assert.Throws<JsonException>(() => JsonSerializer.Deserialize(
            """
            42
            """,
            ForwardCompatibilityJsonContext.Default.SessionMode));

        Assert.Contains("SessionMode", exception.Message);
    }

    [Fact]
    public void RpcEnum_DefaultValue_HasEmptyStringValue()
    {
        GitHub.Copilot.SDK.Rpc.SessionMode mode = default;

        Assert.Equal(string.Empty, mode.Value);
        Assert.Equal(string.Empty, mode.ToString());
    }

    [Fact]
    public void RpcEnum_DefaultValueSerialization_ThrowsJsonException()
    {
        GitHub.Copilot.SDK.Rpc.SessionMode mode = default;

        var exception = Assert.Throws<JsonException>(() => JsonSerializer.Serialize(
            mode,
            ForwardCompatibilityJsonContext.Default.SessionMode));

        Assert.Contains("SessionMode", exception.Message);
    }

    [Fact]
    public void FromJson_KnownEventType_WithNullOptionalFields_DoesNotThrow()
    {
        // The CLI may emit null for optional fields. Verify parsing doesn't throw.
        var json = """
        {
            "id": "00000000-0000-0000-0000-000000000001",
            "timestamp": "2026-01-01T00:00:00Z",
            "parentId": null,
            "agentId": null,
            "type": "user.message",
            "data": {
                "content": "Hello"
            }
        }
        """;

        var result = SessionEvent.FromJson(json);

        var msg = Assert.IsType<UserMessageEvent>(result);
        Assert.Null(msg.AgentId);
        Assert.Null(msg.ParentId);
        Assert.Equal("Hello", msg.Data.Content);
    }

    [Fact]
    public void FromJson_UnknownEventType_PreservesAgentIdNull()
    {
        // Some events legitimately have no agent id. Verify it round-trips as null.
        var json = """
        {
            "id": "00000000-0000-0000-0000-000000000001",
            "timestamp": "2026-01-01T00:00:00Z",
            "parentId": null,
            "type": "future.something",
            "data": {}
        }
        """;

        var result = SessionEvent.FromJson(json);

        Assert.Equal("unknown", result.Type);
        Assert.Null(result.AgentId);
    }
}

[JsonSerializable(typeof(GitHub.Copilot.SDK.Rpc.SessionMode))]
internal partial class ForwardCompatibilityJsonContext : JsonSerializerContext;
