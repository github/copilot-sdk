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

    [Fact]
    public void ProviderConfig_CanSerializeHeaders_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var original = new ProviderConfig
        {
            BaseUrl = "https://example.com/provider",
            Headers = new Dictionary<string, string> { ["Authorization"] = "Bearer provider-token" }
        };

        var json = JsonSerializer.Serialize(original, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("https://example.com/provider", root.GetProperty("baseUrl").GetString());
        Assert.Equal("Bearer provider-token", root.GetProperty("headers").GetProperty("Authorization").GetString());

        var deserialized = JsonSerializer.Deserialize<ProviderConfig>(json, options);
        Assert.NotNull(deserialized);
        Assert.Equal("https://example.com/provider", deserialized.BaseUrl);
        Assert.Equal("Bearer provider-token", deserialized.Headers!["Authorization"]);
    }

    [Fact]
    public void MessageOptions_CanSerializeRequestHeaders_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var original = new MessageOptions
        {
            Prompt = "real prompt",
            Mode = "plan",
            RequestHeaders = new Dictionary<string, string> { ["X-Trace"] = "trace-value" }
        };

        var json = JsonSerializer.Serialize(original, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("real prompt", root.GetProperty("prompt").GetString());
        Assert.Equal("plan", root.GetProperty("mode").GetString());
        Assert.Equal("trace-value", root.GetProperty("requestHeaders").GetProperty("X-Trace").GetString());

        var deserialized = JsonSerializer.Deserialize<MessageOptions>(json, options);
        Assert.NotNull(deserialized);
        Assert.Equal("real prompt", deserialized.Prompt);
        Assert.Equal("plan", deserialized.Mode);
        Assert.Equal("trace-value", deserialized.RequestHeaders!["X-Trace"]);
    }

    [Fact]
    public void SendMessageRequest_CanSerializeRequestHeaders_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotSession), "SendMessageRequest");
        var request = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("Prompt", "real prompt"),
            ("Mode", "plan"),
            ("RequestHeaders", new Dictionary<string, string> { ["X-Trace"] = "trace-value" }));

        var json = JsonSerializer.Serialize(request, requestType, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("session-id", root.GetProperty("sessionId").GetString());
        Assert.Equal("real prompt", root.GetProperty("prompt").GetString());
        Assert.Equal("plan", root.GetProperty("mode").GetString());
        Assert.Equal("trace-value", root.GetProperty("requestHeaders").GetProperty("X-Trace").GetString());
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

    private static Type GetNestedType(Type containingType, string name)
    {
        var type = containingType.GetNestedType(name, System.Reflection.BindingFlags.NonPublic);
        Assert.NotNull(type);
        return type!;
    }

    private static object CreateInternalRequest(Type type, params (string Name, object? Value)[] properties)
    {
        var instance = System.Runtime.CompilerServices.RuntimeHelpers.GetUninitializedObject(type);

        foreach (var (name, value) in properties)
        {
            var property = type.GetProperty(name, System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.Public | System.Reflection.BindingFlags.NonPublic);
            Assert.NotNull(property);

            if (property!.SetMethod is not null)
            {
                property.SetValue(instance, value);
                continue;
            }

            var field = type.GetField($"<{name}>k__BackingField", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic);
            Assert.NotNull(field);
            field!.SetValue(instance, value);
        }

        return instance;
    }
}
