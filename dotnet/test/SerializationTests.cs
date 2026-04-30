/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace GitHub.Copilot.SDK.Test;

/// <summary>
/// Tests for JSON serialization compatibility with the SDK's configured options.
/// </summary>
public class SerializationTests
{
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
