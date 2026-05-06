/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace GitHub.Copilot.SDK.Test.Unit;

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
            Headers = new Dictionary<string, string> { ["Authorization"] = "Bearer provider-token" },
            ModelId = "gpt-4o",
            WireModel = "my-finetune-v3",
            MaxInputTokens = 100_000,
            MaxOutputTokens = 4096
        };

        var json = JsonSerializer.Serialize(original, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("https://example.com/provider", root.GetProperty("baseUrl").GetString());
        Assert.Equal("Bearer provider-token", root.GetProperty("headers").GetProperty("Authorization").GetString());
        Assert.Equal("gpt-4o", root.GetProperty("modelId").GetString());
        Assert.Equal("my-finetune-v3", root.GetProperty("wireModel").GetString());
        Assert.Equal(100_000, root.GetProperty("maxPromptTokens").GetInt32());
        Assert.Equal(4096, root.GetProperty("maxOutputTokens").GetInt32());

        var deserialized = JsonSerializer.Deserialize<ProviderConfig>(json, options);
        Assert.NotNull(deserialized);
        Assert.Equal("https://example.com/provider", deserialized.BaseUrl);
        Assert.Equal("Bearer provider-token", deserialized.Headers!["Authorization"]);
        Assert.Equal("gpt-4o", deserialized.ModelId);
        Assert.Equal("my-finetune-v3", deserialized.WireModel);
        Assert.Equal(100_000, deserialized.MaxInputTokens);
        Assert.Equal(4096, deserialized.MaxOutputTokens);
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

    [Fact]
    public void CreateSessionRequest_CanSerializeInstructionDirectories_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");
        var request = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("InstructionDirectories", new List<string> { "C:\\extra-instructions", "C:\\more-instructions" }));

        var json = JsonSerializer.Serialize(request, requestType, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("C:\\extra-instructions", root.GetProperty("instructionDirectories")[0].GetString());
        Assert.Equal("C:\\more-instructions", root.GetProperty("instructionDirectories")[1].GetString());
    }

    [Fact]
    public void ResumeSessionRequest_CanSerializeInstructionDirectories_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");
        var request = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("InstructionDirectories", new List<string> { "C:\\resume-instructions" }));

        var json = JsonSerializer.Serialize(request, requestType, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("C:\\resume-instructions", root.GetProperty("instructionDirectories")[0].GetString());
    }

    [Fact]
    public void McpHttpServerConfig_CanSerializeOauthOptions_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        McpServerConfig original = new McpHttpServerConfig
        {
            Url = "https://example.com/mcp",
            Headers = new Dictionary<string, string> { ["Authorization"] = "Bearer token" },
            OauthClientId = "client-id",
            OauthPublicClient = false,
            OauthGrantType = McpHttpServerConfigOauthGrantType.ClientCredentials,
            Tools = ["*"],
            Timeout = 3000
        };

        var json = JsonSerializer.Serialize(original, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("http", root.GetProperty("type").GetString());
        Assert.Equal("https://example.com/mcp", root.GetProperty("url").GetString());
        Assert.Equal("Bearer token", root.GetProperty("headers").GetProperty("Authorization").GetString());
        Assert.Equal("client-id", root.GetProperty("oauthClientId").GetString());
        Assert.False(root.GetProperty("oauthPublicClient").GetBoolean());
        Assert.Equal("client_credentials", root.GetProperty("oauthGrantType").GetString());
        Assert.Equal("*", root.GetProperty("tools")[0].GetString());
        Assert.Equal(3000, root.GetProperty("timeout").GetInt32());

        var deserialized = JsonSerializer.Deserialize<McpServerConfig>(json, options);
        var httpConfig = Assert.IsType<McpHttpServerConfig>(deserialized);
        Assert.Equal("https://example.com/mcp", httpConfig.Url);
        Assert.Equal("Bearer token", httpConfig.Headers!["Authorization"]);
        Assert.Equal("client-id", httpConfig.OauthClientId);
        Assert.False(httpConfig.OauthPublicClient);
        Assert.Equal(McpHttpServerConfigOauthGrantType.ClientCredentials, httpConfig.OauthGrantType);
        Assert.Equal("*", Assert.Single(httpConfig.Tools));
        Assert.Equal(3000, httpConfig.Timeout);
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
