/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class FleetRpcTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "fleet_rpc", output)
{
    [Fact]
    public async Task Should_Start_Fleet()
    {
        var session = await CreateSessionAsync();

        var result = await session.Rpc.Fleet.StartAsync("Start fleet mode for this test session.");

        Assert.True(result.Started);
    }
}
