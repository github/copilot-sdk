/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Reflection;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization;
using System.Text.Json.Serialization.Metadata;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class SessionEventDispatchTests
{
    public static IEnumerable<object[]> EventTypes =>
        typeof(SessionEvent).GetCustomAttributes<JsonDerivedTypeAttribute>()
            .Select(attribute => new object[] { attribute.TypeDiscriminator!, attribute.DerivedType });

    public static IEnumerable<object[]> EventDiscriminators =>
        EventTypes.Select(eventType => new object[] { eventType[0] });

    [Theory]
    [MemberData(nameof(EventTypes))]
    public void FromJson_DispatchesEveryRegisteredEvent(string discriminator, Type expectedType)
    {
        var json = $$"""
            {
                "data":null,
                "agentId":"child-agent",
                "ephemeral":true,
                "id":"11111111-1111-1111-1111-111111111111",
                "parentId":"22222222-2222-2222-2222-222222222222",
                "timestamp":"2026-09-30T00:00:00Z",
                "type":"{{discriminator}}"
            }
            """;

        var result = SessionEvent.FromJson(json);

        Assert.Equal(expectedType, result.GetType());
        Assert.Equal(discriminator, result.Type);
        Assert.Equal("child-agent", result.AgentId);
        Assert.True(result.Ephemeral);
        Assert.Equal(Guid.Parse("11111111-1111-1111-1111-111111111111"), result.Id);
        Assert.Equal(Guid.Parse("22222222-2222-2222-2222-222222222222"), result.ParentId);
        Assert.Equal(DateTimeOffset.Parse("2026-09-30T00:00:00Z"), result.Timestamp);
    }

    [Theory]
    [MemberData(nameof(EventDiscriminators))]
    public void FromJson_DoesNotDispatchPartialDiscriminators(string discriminator)
    {
        var candidates = Enumerable.Range(0, discriminator.Length)
            .Select(index => discriminator[..index] + "?" + discriminator[(index + 1)..])
            .Prepend(discriminator[..^1])
            .Append(discriminator + "x");

        foreach (var candidate in candidates)
        {
            var json = $$"""{"type":"{{candidate}}","agentId":"child","data":null}""";
            var actual = SessionEvent.FromJson(json);

            Assert.IsType<SessionEvent>(actual);
            Assert.Equal("child", actual.AgentId);
            Assert.Equal(JsonSerializer.Deserialize(json, PolymorphicTypeInfo)?.ToJson(), actual.ToJson());
        }
    }

    [Theory]
    [InlineData("""{"type":"user.message","data":{"content":"hello"}}""")]
    [InlineData("""{"data":{"nested":[{"type":"session.idle"}],"content":"hello"},"type":"user.message"}""")]
    [InlineData("""{"ty\u0070e":"user.message","data":{"content":"hello"}}""")]
    [InlineData("""{"type":"user\u002Emessage","data":{"content":"hello"}}""")]
    [InlineData("""{"type":"future.\u00E9","agentId":"child"}""")]
    [InlineData("""{"TYPE":"user.message","data":{"content":"hello"}}""")]
    [InlineData("""{"type":"USER.MESSAGE","data":{"content":"hello"}}""")]
    [InlineData("""{"type":"future.event","agentId":"child","ephemeral":true,"data":[null,{"type":"user.message"}]}""")]
    [InlineData("""{"agentId":"child","ephemeral":true,"data":{"type":"user.message"}}""")]
    [InlineData("""{"type":42,"agentId":"child","ephemeral":true}""")]
    [InlineData("""{"type":-2147483648}""")]
    [InlineData("""{"type":2147483647}""")]
    [InlineData("""{"type":"user.message","DATA":{"CONTENT":"hello"}}""")]
    [InlineData("""{"type":"user.message","data":{"content":"hello"},"data":{"content":"last"}}""")]
    [InlineData("null")]
    public void FromJson_PreservesPolymorphicDeserialization(string json)
    {
        var expected = JsonSerializer.Deserialize(json, PolymorphicTypeInfo);
        var actual = SessionEvent.FromJson(json);

        Assert.Equal(expected?.GetType(), actual?.GetType());
        Assert.True(JsonNode.DeepEquals(
            JsonNode.Parse(expected?.ToJson() ?? "null"),
            JsonNode.Parse(actual?.ToJson() ?? "null")));
    }

    [Theory]
    [InlineData("""{"type":null}""")]
    [InlineData("""{"type":true}""")]
    [InlineData("""{"type":{}}""")]
    [InlineData("""{"type":[]}""")]
    [InlineData("""{"type":1.5}""")]
    [InlineData("""{"type":2147483648}""")]
    [InlineData("""{"type":"\uD800"}""")]
    [InlineData("""{"type":"session.idle","\uD800":true,"data":{}}""")]
    [InlineData("""{"type":"user.message","data":{"content":"hello"}""")]
    [InlineData("""{"type":"user.message","data":{"content":"hello"},}""")]
    [InlineData("""{"type":"future.event","ty\u0070e":"future.event"}""")]
    [InlineData("""{"\u0024future":true,"type":"session.idle","data":{}}""")]
    [InlineData("""{"type":"user.message","type":"session.idle","data":{"content":"hello"}}""")]
    [InlineData("""{"type":"user.message","data":{"content":"hello"},"type":"user.message"}""")]
    [InlineData("""{"type":"future.event","data":{},"type":"future.event"}""")]
    [InlineData("""{"type":"user.message","data":{}}""")]
    [InlineData("""{"type":"user.message"}""")]
    [InlineData("""{"$id":"1","type":"user.message","data":{"content":"hello"}}""")]
    [InlineData("""{"type":"user.message","data":{"content":"hello"},"$ref":"1"}""")]
    [InlineData("""{"type":"future.event","$values":[]}""")]
    [InlineData("""{"$future":"ignored","type":"user.message","data":{"content":"hello"}}""")]
    [InlineData("""{"type":"session.idle","id":"invalid","data":{}}""")]
    [InlineData("""{"type":"session.idle","timestamp":"invalid","data":{}}""")]
    [InlineData("""{"type":"session.idle","ephemeral":"invalid","data":{}}""")]
    [InlineData("""[{"type":"session.idle","data":{}}]""")]
    [InlineData("42")]
    [InlineData("true")]
    [InlineData("@")]
    [InlineData("")]
    [InlineData("\"text\"")]
    [InlineData("""{"type":"user.message","data":{"content":"hello"}} trailing""")]
    public void FromJson_RejectsInvalidEventsLikePolymorphicDeserialization(string json)
    {
        Assert.Throws<JsonException>(() => JsonSerializer.Deserialize(json, PolymorphicTypeInfo));
        Assert.Throws<JsonException>(() => SessionEvent.FromJson(json));
    }

    [Theory]
    [InlineData(0)]
    [InlineData(32)]
    [InlineData(256)]
    [InlineData(100_000)]
    public void FromJson_PreservesUnicodeAcrossBufferSizes(int repetitions)
    {
        var content = string.Concat(Enumerable.Repeat("a\u00E9\u4E2D\U0001F600", repetitions));
        var json = "{\"data\":{\"content\":\"" + content + "\"},\"ty\\u0070e\":\"user.message\"}";

        var actual = Assert.IsType<UserMessageEvent>(SessionEvent.FromJson(json));

        Assert.Equal(content, actual.Data.Content);
        Assert.Equal(JsonSerializer.Deserialize(json, PolymorphicTypeInfo)?.ToJson(), actual.ToJson());
    }

    [Theory]
    [InlineData('\uD800', 0)]
    [InlineData('\uDC00', 0)]
    [InlineData('\uD800', 1000)]
    [InlineData('\uDC00', 1000)]
    public void FromJson_RejectsInvalidUtf16LikePolymorphicDeserialization(char invalid, int padding)
    {
        var json = "{\"type\":\"user.message\",\"data\":{\"content\":\"" + new string('a', padding) + invalid + "\"}}";

        Assert.Throws<ArgumentException>(() => JsonSerializer.Deserialize(json, PolymorphicTypeInfo));
        Assert.Throws<ArgumentException>(() => SessionEvent.FromJson(json));
    }

    [Fact]
    public void FromJson_RejectsNullInputLikePolymorphicDeserialization()
    {
        Assert.Throws<ArgumentNullException>(() => JsonSerializer.Deserialize((string)null!, PolymorphicTypeInfo));
        Assert.Throws<ArgumentNullException>(() => SessionEvent.FromJson(null!));
    }

    [Fact]
    public async Task FromJson_IsSafeForConcurrentDeserialization()
    {
        var events = EventTypes.ToArray();
        var tasks = Enumerable.Range(0, 32).Select(i => Task.Run(() =>
        {
            foreach (var item in events)
            {
                var discriminator = (string)item[0];
                var expectedType = (Type)item[1];
                var json = $$"""{"type":"{{discriminator}}","agentId":"{{i}}","data":null}""";
                var actual = SessionEvent.FromJson(json);

                Assert.Equal(expectedType, actual.GetType());
                Assert.Equal(i.ToString(), actual.AgentId);
            }
        }));

        await Task.WhenAll(tasks);
    }

    [Fact]
    public void LiveOptions_SerializeEventsWithoutChangingTheWireFormat()
    {
        var options = (JsonSerializerOptions)typeof(CopilotClient).GetProperty(
            "SerializerOptionsForMessageFormatter", BindingFlags.Static | BindingFlags.NonPublic)!
            .GetValue(null)!;
        var sessionEvent = new UserMessageEvent
        {
            Id = Guid.Parse("11111111-1111-1111-1111-111111111111"),
            Timestamp = DateTimeOffset.Parse("2026-09-30T00:00:00Z"),
            Data = new UserMessageData { Content = "hello" },
        };

        var json = JsonSerializer.Serialize<SessionEvent>(sessionEvent, options);
        Assert.True(JsonNode.DeepEquals(JsonNode.Parse(sessionEvent.ToJson()), JsonNode.Parse(json)));
        var decoded = Assert.IsType<UserMessageEvent>(JsonSerializer.Deserialize<SessionEvent>(json, options));
        Assert.Equal("hello", decoded.Data.Content);
    }

    // The unchanged source-generated context is the reference contract, not reflection metadata.
    private static JsonTypeInfo<SessionEvent> PolymorphicTypeInfo
    {
        get
        {
            var contextType = typeof(SessionEvent).Assembly.GetType("GitHub.Copilot.SessionEventsJsonContext", true)!;
            var context = (JsonSerializerContext)contextType.GetProperty("Default")!.GetValue(null)!;
            return (JsonTypeInfo<SessionEvent>)context.GetTypeInfo(typeof(SessionEvent))!;
        }
    }
}
