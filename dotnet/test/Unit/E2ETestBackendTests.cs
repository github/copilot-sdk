/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class E2ETestBackendTests
{
    [Theory]
    [InlineData(null, "capi")]
    [InlineData("", "capi")]
    [InlineData("capi", "capi")]
    [InlineData("ANTHROPIC-MESSAGES", "anthropic-messages")]
    [InlineData("openai-responses", "openai-responses")]
    [InlineData("openai-completions", "openai-completions")]
    public void ParsesBackend(string? value, string expected)
        => Assert.Equal(expected, E2ETestBackendConfiguration.Parse(value).ToWireName());

    [Fact]
    public void RejectsUnknownBackend()
        => Assert.Throws<ArgumentOutOfRangeException>(
            () => E2ETestBackendConfiguration.Parse("unknown"));

    [Theory]
    [InlineData("anthropic-messages", "anthropic", null, "claude-sonnet-4.5")]
    [InlineData("openai-responses", "openai", "responses", "gpt-4.1")]
    [InlineData("openai-completions", "openai", "completions", "gpt-4.1")]
    public void AppliesProvider(
        string backendValue,
        string expectedType,
        string? expectedWireApi,
        string expectedModel)
    {
        var backend = E2ETestBackendConfiguration.Parse(backendValue);
        var config = new SessionConfig();
        backend.ApplyProvider(config, "http://localhost:1234");

        Assert.Equal(expectedModel, config.Model);
        Assert.Equal("http://localhost:1234", config.Provider!.BaseUrl);
        Assert.Equal(expectedType, config.Provider.Type);
        Assert.Equal(expectedWireApi, config.Provider.WireApi);
        Assert.Equal(expectedModel, config.Provider.ModelId);
        Assert.Equal(expectedModel, config.Provider.WireModel);
        Assert.False(string.IsNullOrEmpty(config.Provider.BearerToken));
    }

    [Fact]
    public void PreservesExplicitModel()
    {
        var config = new SessionConfig { Model = "test-model" };
        E2ETestBackend.OpenAIResponses.ApplyProvider(config, "http://localhost:1234");

        Assert.Equal("test-model", config.Model);
        Assert.Equal("test-model", config.Provider!.ModelId);
        Assert.Equal("test-model", config.Provider.WireModel);
    }

    [Fact]
    public void PreservesExplicitProvider()
    {
        var provider = new ProviderConfig
        {
            Type = "custom",
            ModelId = "provider-model",
        };
        var config = new SessionConfig
        {
            Model = "session-model",
            Provider = provider,
        };

        E2ETestBackend.OpenAIResponses.ApplyProvider(config, "http://localhost:1234");

        Assert.Equal("session-model", config.Model);
        Assert.Same(provider, config.Provider);
    }
}
