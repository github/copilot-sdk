/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class ClientApiE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "client_api", output)
{
    private static async Task<Exception> AssertFailureAsync(Func<Task> action, string expectedMessage)
    {
        var ex = await Assert.ThrowsAnyAsync<Exception>(action);
        Assert.Contains(expectedMessage, ex.ToString(), StringComparison.OrdinalIgnoreCase);
        return ex;
    }

    [Fact]
    public async Task Should_Delete_Session_By_Id()
    {
        var session = await CreateSessionAsync();
        var sessionId = session.SessionId;

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say OK." });
        await Task.Delay(200);
        await session.DisposeAsync();
        await Client.DeleteSessionAsync(sessionId);

        var metadata = await Client.GetSessionMetadataAsync(sessionId);
        Assert.Null(metadata);
    }

    [Fact]
    public async Task Should_Report_Error_When_Deleting_Unknown_Session_Id()
    {
        await Client.StartAsync();

        await AssertFailureAsync(
            () => Client.DeleteSessionAsync("00000000-0000-0000-0000-000000000000"),
            "Session file not found");
    }

    [Fact]
    public async Task Should_Get_Null_Foreground_Session_Id_In_Headless_Mode()
    {
        await Client.StartAsync();

        var sessionId = await Client.GetForegroundSessionIdAsync();

        Assert.Null(sessionId);
    }

    [Fact]
    public async Task Should_Report_Error_When_Setting_Foreground_Session_In_Headless_Mode()
    {
        var session = await CreateSessionAsync();

        await AssertFailureAsync(
            () => Client.SetForegroundSessionIdAsync(session.SessionId),
            "Not running in TUI+server mode");
    }

    [Fact]
    public async Task DisposeAsync_Disconnects_Client_And_Disposes_Rpc_Surface()
    {
        await using var client = Ctx.CreateClient();
        await client.StartAsync();

        Assert.Equal(ConnectionState.Connected, client.State);

        await client.DisposeAsync();

        Assert.Equal(ConnectionState.Disconnected, client.State);
        Assert.Throws<ObjectDisposedException>(() => client.Rpc);
    }

    [Fact]
    public async Task Dispose_Disconnects_Client_And_Disposes_Rpc_Surface()
    {
        using var client = Ctx.CreateClient();
        await client.StartAsync();

        Assert.Equal(ConnectionState.Connected, client.State);

        client.Dispose();

        Assert.Equal(ConnectionState.Disconnected, client.State);
        Assert.Throws<ObjectDisposedException>(() => client.Rpc);
    }
}
