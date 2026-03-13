/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using System.Text.Json;
using System.Text.Json.Serialization;
using StreamJsonRpc;

namespace GitHub.Copilot.SDK.Test;

/// <summary>
/// Tests for JSON serialization compatibility, particularly for StreamJsonRpc types
/// that are needed when CancellationTokens fire during JSON-RPC operations.
/// This test suite verifies the fix for https://github.com/PureWeen/PolyPilot/issues/319
/// </summary>
public class SerializationTests
{
    /// <summary>
    /// Verifies that StreamJsonRpc.RequestId can be round-tripped using the SDK's configured
    /// JsonSerializerOptions. This is critical for preventing NotSupportedException when
    /// StandardCancellationStrategy fires during JSON-RPC operations.
    /// </summary>
    [Fact]
    public void RequestId_CanBeSerializedAndDeserialized_WithSdkOptions()
    {
        var options = GetSerializerOptions();

        // Long id
        var jsonLong = JsonSerializer.Serialize(new RequestId(42L), options);
        Assert.Equal("42", jsonLong);
        Assert.Equal(new RequestId(42L), JsonSerializer.Deserialize<RequestId>(jsonLong, options));

        // String id
        var jsonStr = JsonSerializer.Serialize(new RequestId("req-1"), options);
        Assert.Equal("\"req-1\"", jsonStr);
        Assert.Equal(new RequestId("req-1"), JsonSerializer.Deserialize<RequestId>(jsonStr, options));

        // Null id
        var jsonNull = JsonSerializer.Serialize(RequestId.Null, options);
        Assert.Equal("null", jsonNull);
        Assert.Equal(RequestId.Null, JsonSerializer.Deserialize<RequestId>(jsonNull, options));
    }

    [Theory]
    [InlineData(0L)]
    [InlineData(-1L)]
    [InlineData(long.MaxValue)]
    public void RequestId_NumericEdgeCases_RoundTrip(long id)
    {
        var options = GetSerializerOptions();
        var requestId = new RequestId(id);
        var json = JsonSerializer.Serialize(requestId, options);
        Assert.Equal(requestId, JsonSerializer.Deserialize<RequestId>(json, options));
    }

    /// <summary>
    /// Verifies the SDK's options can resolve type info for RequestId,
    /// ensuring AOT-safe serialization without falling back to reflection.
    /// </summary>
    [Fact]
    public void SerializerOptions_CanResolveRequestIdTypeInfo()
    {
        var options = GetSerializerOptions();
        var typeInfo = options.GetTypeInfo(typeof(RequestId));
        Assert.NotNull(typeInfo);
        Assert.Equal(typeof(RequestId), typeInfo.Type);
    }

    private static JsonSerializerOptions GetSerializerOptions()
    {
        var prop = typeof(CopilotClient)
            .GetProperty("SerializerOptionsForMessageFormatter",
                System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Static);

        var options = (JsonSerializerOptions?)prop?.GetValue(null);
        Assert.NotNull(options);
        return options;
    }
}
