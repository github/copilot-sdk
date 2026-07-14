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
        => Assert.Throws<InvalidOperationException>(
            () => E2ETestBackendConfiguration.Parse("unknown"));

    [Theory]
    [InlineData("anthropic-messages", "anthropic", null)]
    [InlineData("openai-responses", "openai", "responses")]
    [InlineData("openai-completions", "openai", "completions")]
    public void AppliesProvider(
        string backendValue,
        string expectedType,
        string? expectedWireApi)
    {
        var backend = E2ETestBackendConfiguration.Parse(backendValue);
        var config = new SessionConfig();
        backend.ApplyProvider(config, "http://localhost:1234");

        Assert.Equal("claude-sonnet-4.5", config.Model);
        Assert.Equal("http://localhost:1234", config.Provider!.BaseUrl);
        Assert.Equal(expectedType, config.Provider.Type);
        Assert.Equal(expectedWireApi, config.Provider.WireApi);
        Assert.False(string.IsNullOrEmpty(config.Provider.BearerToken));
    }
}
