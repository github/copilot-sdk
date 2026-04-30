/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class SkillsRpcTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "skills_rpc", output)
{
    [Fact]
    public async Task Should_List_Skills()
    {
        var session = await CreateSessionAsync();

        var result = await session.Rpc.Skills.ListAsync();

        Assert.NotNull(result.Skills);
    }

    [Fact]
    public async Task Should_Enable_And_Disable_Skill()
    {
        var session = await CreateSessionAsync();

        await session.Rpc.Skills.EnableAsync("nonexistent-test-skill");
        await session.Rpc.Skills.DisableAsync("nonexistent-test-skill");
    }

    [Fact]
    public async Task Should_Reload_Skills()
    {
        var session = await CreateSessionAsync();

        await session.Rpc.Skills.ReloadAsync();
    }
}
