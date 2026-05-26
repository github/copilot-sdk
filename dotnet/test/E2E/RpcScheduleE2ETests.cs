/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class RpcScheduleE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_schedule", output)
{
    [Fact]
    public async Task Should_List_No_Schedules_For_Fresh_Session()
    {
        await using var session = await CreateSessionAsync();

        var result = await session.Rpc.Schedule.ListAsync();

        Assert.NotNull(result.Entries);
        Assert.Empty(result.Entries);
    }

    [Fact]
    public async Task Should_Return_Null_Entry_When_Stopping_Unknown_Schedule()
    {
        await using var session = await CreateSessionAsync();

        var result = await session.Rpc.Schedule.StopAsync(long.MaxValue);

        Assert.Null(result.Entry);
        Assert.Empty((await session.Rpc.Schedule.ListAsync()).Entries);
    }
}
