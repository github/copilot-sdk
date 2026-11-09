/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization;
using System.Text.Json.Serialization.Metadata;
using GitHub.Copilot.Rpc;
using Xunit;
using RpcNamedProviderConfig = GitHub.Copilot.Rpc.NamedProviderConfig;

namespace GitHub.Copilot.Test;

public class ProviderConfigurationTests
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
        TypeInfoResolver = new DefaultJsonTypeInfoResolver(),
    };

    [Theory]
    [InlineData("openai")]
    [InlineData("azure")]
    [InlineData("anthropic")]
    [InlineData("winml")]
    [InlineData(null)]
    public void PreservesSettingsWithoutCredentials(string? kind)
    {
        var settings = JsonNode.Parse("""
            {
                "name": "discovered", "baseUrl": "http://127.0.0.1:1234",
                "wireApi": "responses", "transport": "http",
                "azure": { "apiVersion": "2025-01-01" },
                "localModel": { "device": "gpu", "future": [1, true] },
                "modelProvider": "azure_openai"
            }
            """)!;
        if (kind is not null)
        {
            settings["type"] = kind;
        }
        var input = settings.DeepClone();
        input["apiKey"] = "fixture-key";
        input["bearerToken"] = "fixture-token";
        input["headers"] = new JsonObject { ["Authorization"] = "fixture-header" };
        input["hasBearerTokenProvider"] = true;
        var provider = input.Deserialize<RpcNamedProviderConfig>(JsonOptions)!;
        var original = JsonSerializer.SerializeToNode(provider, JsonOptions);

        var saved = provider.ToSavedProviderConfiguration();

        Assert.True(JsonNode.DeepEquals(settings, JsonSerializer.SerializeToNode(saved, JsonOptions)));
        Assert.True(JsonNode.DeepEquals(original, JsonSerializer.SerializeToNode(provider, JsonOptions)));
    }

    [Fact]
    public void OmitsAbsentOptionalSettings()
    {
        var provider = new RpcNamedProviderConfig { Name = "minimal", BaseUrl = "http://127.0.0.1:1234" };
        var saved = JsonSerializer.SerializeToNode(provider.ToSavedProviderConfiguration(), JsonOptions);

        Assert.True(JsonNode.DeepEquals(
            JsonNode.Parse("""{"name":"minimal","baseUrl":"http://127.0.0.1:1234"}"""), saved));
    }
}
