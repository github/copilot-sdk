/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using System.Text.Json.Serialization;
using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public partial class ReplayProxyTests
{
    [Theory]
    [InlineData("37", 37L)]
    [InlineData("0", 0L)]
    [InlineData("", null)]
    [InlineData("null", null)]
    public void CapturedCompactionUsagePreservesInputCounts(string inputTokens, long? expectedInputTokens)
    {
        var inputProperty = inputTokens.Length == 0 ? string.Empty : ",\"inputTokens\":" + inputTokens;
        var json = "[{\"request\":{\"model\":\"test-model\",\"messages\":[]},"
            + "\"response\":{\"id\":\"compaction-response\",\"model\":\"test-model\","
            + "\"choices\":[]},"
            + "\"compactionUsage\":{\"interactionId\":\"compaction-interaction\","
            + "\"summary\":\"summary\",\"responseCount\":2" + inputProperty + "},"
            + "\"requestHeaders\":{\"x-interaction-type\":\"conversation-compaction\"}}]";
        var exchanges = JsonSerializer.Deserialize(json, ReplayProxyTestJsonContext.Default.ListParsedHttpExchange);
        Assert.NotNull(exchanges);
        var exchange = Assert.Single(exchanges);
        Assert.NotNull(exchange.RequestHeaders);
        Assert.Equal("conversation-compaction", exchange.RequestHeaders["x-interaction-type"].GetString());
        Assert.NotNull(exchange.CompactionUsage);
        Assert.Equal("compaction-interaction", exchange.CompactionUsage.InteractionId);
        Assert.Equal("summary", exchange.CompactionUsage.Summary);
        Assert.Equal(2, exchange.CompactionUsage.ResponseCount);
        Assert.Equal(expectedInputTokens, exchange.CompactionUsage.InputTokens);
    }

    [JsonSourceGenerationOptions(JsonSerializerDefaults.Web)]
    [JsonSerializable(typeof(List<ParsedHttpExchange>))]
    private partial class ReplayProxyTestJsonContext : JsonSerializerContext;
}
