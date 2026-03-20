/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;

namespace GitHub.Copilot.SDK.Test;

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
            "type": "user.message",
            "data": {
                "content": "Hello"
            }
        }
        """;

        var result = SessionEvent.FromJson(json);

        Assert.IsType<UserMessageEvent>(result);
        Assert.Equal("user.message", result.Type);
    }

    [Fact]
    public void FromJson_UnknownEventType_ReturnsBaseSessionEvent()
    {
        var json = """
        {
            "id": "12345678-1234-1234-1234-123456789abc",
            "timestamp": "2026-06-15T10:30:00Z",
            "parentId": "abcdefab-abcd-abcd-abcd-abcdefabcdef",
            "type": "future.feature_from_server",
            "data": { "key": "value" }
        }
        """;

        var result = SessionEvent.FromJson(json);

        Assert.IsType<SessionEvent>(result);
        Assert.Equal("unknown", result.Type);
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
    public void SessionEvent_Type_DefaultsToUnknown()
    {
        var evt = new SessionEvent();

        Assert.Equal("unknown", evt.Type);
    }
}
