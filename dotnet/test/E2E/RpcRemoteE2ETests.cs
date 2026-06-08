/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class RpcRemoteE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_remote", output)
{
    private static readonly TimeSpan EventTimeout = TimeSpan.FromSeconds(30);

    [Fact]
    public async Task Should_Treat_Remote_Off_As_No_Op_Or_Implemented_Error()
    {
        await using var session = await CreateSessionAsync();

        var result = await TryInvokeImplementedAsync(
            () => session.Rpc.Remote.EnableAsync(RemoteSessionMode.Off),
            "session.remote.enable");

        if (result is not null)
        {
            Assert.False(result.RemoteSteerable);
            Assert.True(string.IsNullOrEmpty(result.Url));
        }
    }

    [Fact]
    public async Task Should_Treat_Remote_Disable_As_No_Op_Or_Implemented_Error()
    {
        await using var session = await CreateSessionAsync();

        await TryInvokeImplementedAsync(
            () => session.Rpc.Remote.DisableAsync(),
            "session.remote.disable");
    }

    [Fact]
    public async Task Should_Notify_Steerable_Changed_Event_And_Persist_Flag()
    {
        await using var session = await CreateSessionAsync();

        await session.Rpc.Remote.NotifySteerableChangedAsync(true);

        await WaitForRemoteSteerableEventAsync(session, expected: true);

        await session.Rpc.Remote.NotifySteerableChangedAsync(false);

        await WaitForRemoteSteerableEventAsync(session, expected: false);
    }

    private static async Task WaitForRemoteSteerableEventAsync(CopilotSession session, bool expected)
    {
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var events = await session.GetEventsAsync();
                return events
                    .OfType<SessionRemoteSteerableChangedEvent>()
                    .Any(evt => evt.Data.RemoteSteerable == expected);
            },
            timeout: EventTimeout,
            timeoutMessage: $"Timed out waiting for session.remote_steerable_changed={expected}.");
    }

    private static async Task<T?> TryInvokeImplementedAsync<T>(Func<Task<T>> action, string method) where T : class
    {
        try
        {
            return await action();
        }
        catch (IOException ex) when (ex.ToString().Contains($"Unhandled method {method}", StringComparison.OrdinalIgnoreCase))
        {
            return null;
        }
    }

    private static async Task TryInvokeImplementedAsync(Func<Task> action, string method)
    {
        try
        {
            await action();
        }
        catch (IOException ex) when (ex.ToString().Contains($"Unhandled method {method}", StringComparison.OrdinalIgnoreCase))
        {
            // Older runtimes may not expose this RPC yet; that is the only accepted fallback path.
        }
    }
}
