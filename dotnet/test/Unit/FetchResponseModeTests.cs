// Copyright (c) Microsoft Corporation. All rights reserved.

using System.Text.Json;
using System.Text.Json.Serialization;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class FetchResponseModeTests
{
    [Theory]
    [InlineData("stream")]
    [InlineData("text")]
    [InlineData("file")]
    public void ResultModeMatchesRequestModeAndRoundTrips(string mode)
    {
        var typeInfo = FetchResponseModeJsonContext.Default.FetchSendResult;
        var modeFields = mode switch
        {
            "text" => ""","bodyText":"hello","endedAtUnverifiedEof":false""",
            "file" => ""","path":"response.txt","sizeBytes":5,"endedAtUnverifiedEof":false""",
            _ => ""
        };
        var json = $$"""{"responseMode":"{{mode}}","url":"https://example.com","status":200,"statusText":"OK","headers":[]{{modeFields}}}""";
        var result = JsonSerializer.Deserialize(json, typeInfo)!;
        var requestMode = mode switch
        {
            "text" => FetchResponseMode.Text,
            "file" => FetchResponseMode.File,
            _ => FetchResponseMode.Stream
        };
        Assert.True(result.ResponseMode == requestMode);
        using var expected = JsonDocument.Parse(json);
        using var actual = JsonDocument.Parse(JsonSerializer.Serialize(result, typeInfo));
        Assert.Equal(expected.RootElement.EnumerateObject().Count(), actual.RootElement.EnumerateObject().Count());
        foreach (var property in expected.RootElement.EnumerateObject())
        {
            Assert.Equal(property.Value.GetRawText(), actual.RootElement.GetProperty(property.Name).GetRawText());
        }
    }
}

[JsonSerializable(typeof(FetchSendResult))]
internal partial class FetchResponseModeJsonContext : JsonSerializerContext;
