/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// E2E coverage for the server-scoped remote-control RPC methods that were previously untested:
/// getRemoteControlStatus, setRemoteControlSteering, stopRemoteControl, transferRemoteControl, and
/// startRemoteControl. The remote-control singleton is per-runtime shared state, so every test uses
/// its own dedicated client process and leaves the singleton in the "off" state.
/// </summary>
public class RpcServerRemoteControlE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_server_remote_control", output)
{
    [Fact]
    public async Task Should_Report_Remote_Control_Status_As_Off()
    {
        await using var client = Ctx.CreateClient();
        await client.StartAsync();

        var result = await client.Rpc.Sessions.GetRemoteControlStatusAsync();

        // A runtime that has never attached remote control reports the off singleton state.
        Assert.IsType<RemoteControlStatusOff>(result.Status);
        Assert.Equal("off", result.Status.State);
    }

    [Fact]
    public async Task Should_Treat_Set_Steering_As_No_Op_When_Off()
    {
        await using var client = Ctx.CreateClient();
        await client.StartAsync();

        // Steering only applies to an active singleton; with remote control off it is a no-op that
        // returns the unchanged off status rather than failing.
        var result = await client.Rpc.Sessions.SetRemoteControlSteeringAsync(false);

        Assert.IsType<RemoteControlStatusOff>(result.Status);
    }

    [Fact]
    public async Task Should_Report_Not_Stopped_When_Remote_Control_Is_Off()
    {
        await using var client = Ctx.CreateClient();
        await client.StartAsync();

        var result = await client.Rpc.Sessions.StopRemoteControlAsync();

        // Nothing is attached, so there is nothing to tear down.
        Assert.False(result.Stopped);
        Assert.IsType<RemoteControlStatusOff>(result.Status);
    }

    [Fact]
    public async Task Should_Reject_Transfer_When_Off_With_Compare_And_Swap()
    {
        await using var client = Ctx.CreateClient();
        await client.StartAsync();

        // Compare-and-swap transfer is rejected because the singleton is off (it points at no
        // session), so the expected-from guard can never match and nothing is rebound.
        var result = await client.Rpc.Sessions.TransferRemoteControlAsync(
            toSessionId: $"rc-to-{Guid.NewGuid():N}",
            expectedFromSessionId: $"rc-from-{Guid.NewGuid():N}");

        Assert.False(result.Transferred);
        Assert.IsType<RemoteControlStatusOff>(result.Status);
    }

    [Fact]
    public async Task Should_Reach_Runtime_When_Starting_Remote_Control_For_Unknown_Session()
    {
        await using var client = Ctx.CreateClient();
        await client.StartAsync();

        try
        {
            // startRemoteControl attaches the singleton to a local session. A well-formed session id
            // that the runtime does not know is rejected at the runtime (not as an unhandled method),
            // proving the method is wired through without requiring a live Mission Control backend.
            var ex = await Assert.ThrowsAnyAsync<Exception>(
                () => client.Rpc.Sessions.StartRemoteControlAsync(
                    $"missing-session-{Guid.NewGuid():N}",
                    new RemoteControlConfig { Remote = false, Explicit = false, Silent = true, Steerable = false }));

            var message = ex.ToString();
            Assert.DoesNotContain("Unhandled method", message, StringComparison.OrdinalIgnoreCase);
            Assert.True(
                message.Contains("session", StringComparison.OrdinalIgnoreCase)
                    || message.Contains("remote", StringComparison.OrdinalIgnoreCase),
                message);
        }
        finally
        {
            // Force the singleton back to off regardless of how the start attempt resolved.
            try { await client.Rpc.Sessions.StopRemoteControlAsync(force: true); }
            catch { /* best-effort reset */ }
        }
    }
}
