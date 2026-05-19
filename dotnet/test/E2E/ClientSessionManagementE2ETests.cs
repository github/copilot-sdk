/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public class ClientSessionManagementE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
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
        await session.DisposeAsync();
        await Client.DeleteSessionAsync(sessionId);

        var metadata = await Client.GetSessionMetadataAsync(sessionId);
        Assert.Null(metadata);
    }

    [Fact]
    public async Task Should_Report_Error_When_Deleting_Unknown_Session_Id()
    {
        await Client.StartAsync();
        const string UnknownSessionId = "00000000-0000-0000-0000-000000000000";

        await AssertFailureAsync(
            () => Client.DeleteSessionAsync(UnknownSessionId),
            $"Failed to delete session {UnknownSessionId}");
    }

    [Fact]
    public async Task Should_Get_Null_Last_Session_Id_Before_Any_Sessions_Exist()
    {
        await Client.StartAsync();

        var result = await Client.GetLastSessionIdAsync();

        Assert.Null(result);
    }

    [Fact]
    public async Task Should_Track_Last_Session_Id_After_Session_Created()
    {
        var session = await CreateSessionAsync();
        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say OK." });
        var sessionId = session.SessionId;
        await session.DisposeAsync();

        var lastId = await Client.GetLastSessionIdAsync();

        Assert.Equal(sessionId, lastId);
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
}
