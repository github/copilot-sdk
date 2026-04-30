/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class McpRuntimeRpcTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "mcp_runtime_rpc", output)
{
    private static async Task<Exception> AssertFailureAsync(Func<Task> action, string expectedMessage)
    {
        var ex = await Assert.ThrowsAnyAsync<Exception>(action);
        Assert.Contains(expectedMessage, ex.ToString(), StringComparison.OrdinalIgnoreCase);
        return ex;
    }

    [Fact]
    public async Task Should_List_Mcp_Servers()
    {
        var session = await CreateSessionAsync();

        var result = await session.Rpc.Mcp.ListAsync();

        Assert.NotNull(result.Servers);
    }

    [Fact]
    public async Task Should_List_Plugins()
    {
        var session = await CreateSessionAsync();

        var result = await session.Rpc.Plugins.ListAsync();

        Assert.NotNull(result.Plugins);
    }

    [Fact]
    public async Task Should_List_Extensions()
    {
        var session = await CreateSessionAsync();

        var result = await session.Rpc.Extensions.ListAsync();

        Assert.NotNull(result.Extensions);
    }

    [Fact]
    public async Task Should_Report_Error_When_Mcp_Host_Is_Not_Initialized()
    {
        var session = await CreateSessionAsync();

        await AssertFailureAsync(
            () => session.Rpc.Mcp.EnableAsync("missing-server"),
            "No MCP host initialized");
        await AssertFailureAsync(
            () => session.Rpc.Mcp.DisableAsync("missing-server"),
            "No MCP host initialized");
        await AssertFailureAsync(
            () => session.Rpc.Mcp.ReloadAsync(),
            "MCP config reload not available");
    }

    [Fact]
    public async Task Should_Report_Error_When_Extensions_Are_Not_Available()
    {
        var session = await CreateSessionAsync();

        await AssertFailureAsync(
            () => session.Rpc.Extensions.EnableAsync("missing-extension"),
            "Extensions not available");
        await AssertFailureAsync(
            () => session.Rpc.Extensions.DisableAsync("missing-extension"),
            "Extensions not available");
        await AssertFailureAsync(
            () => session.Rpc.Extensions.ReloadAsync(),
            "Extensions not available");
    }
}
