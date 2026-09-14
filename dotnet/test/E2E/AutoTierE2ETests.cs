/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// Mirrors nodejs/test/e2e/auto_tier.e2e.test.ts (snapshot category "auto_tier").
/// </summary>
/// <remarks>
/// The runtime stages an Auto routing preference instead of applying it immediately: a
/// request stays unclaimed until a later turn using the <c>auto</c> model mints a usable
/// model and token pair. These tests observe that staged state through
/// <c>Model.GetCurrentAsync</c>, so they assert what the runtime actually recorded rather
/// than what the SDK serialized.
/// </remarks>
public class AutoTierE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "auto_tier", output)
{
    private static async Task AssertPendingAutoTierAsync(CopilotSession session, AutoTier? expected)
    {
        var current = await session.Rpc.Model.GetCurrentAsync();
        Assert.Equal(expected, current.PendingAutoTier);
    }

    [Fact]
    public async Task Should_Stage_And_Reset_Auto_Tier_Preference()
    {
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Model = "auto",
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await AssertPendingAutoTierAsync(session, null);

        var staged = await session.SetAutoTierAsync(AutoTier.Efficiency);
        Assert.Equal(ModelSwitchAutoTierStatus.Pending, staged.Status);
        Assert.Equal(AutoTier.Efficiency, staged.PendingAutoTier);
        await AssertPendingAutoTierAsync(session, AutoTier.Efficiency);

        // A second request replaces the first and reports the one it displaced.
        var superseded = await session.SetAutoTierAsync(AutoTier.Intelligence);
        Assert.Equal(ModelSwitchAutoTierStatus.Pending, superseded.Status);
        Assert.Equal(AutoTier.Intelligence, superseded.PendingAutoTier);
        Assert.Equal(AutoTier.Efficiency, superseded.SupersededAutoTier);
        await AssertPendingAutoTierAsync(session, AutoTier.Intelligence);

        // A null tier returns the session to provider-default routing. The status is
        // Unchanged because provider-default was already the committed preference; the
        // request's effect is cancelling the staged one.
        var reset = await session.SetAutoTierAsync(null);
        Assert.Equal(ModelSwitchAutoTierStatus.Unchanged, reset.Status);
        Assert.Equal(AutoTier.Intelligence, reset.SupersededAutoTier);
        await AssertPendingAutoTierAsync(session, null);
    }

    [Fact]
    public async Task Should_Preserve_Auto_Tier_When_Set_Model_Omits_It()
    {
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Model = "auto",
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await session.SetAutoTierAsync(AutoTier.Balance);
        await AssertPendingAutoTierAsync(session, AutoTier.Balance);

        // Leaving AutoTier unset without asking for a reset leaves the staged preference alone.
        await session.SetModelAsync("auto", new SetModelOptions());
        await AssertPendingAutoTierAsync(session, AutoTier.Balance);

        // Supplying a tier replaces it.
        await session.SetModelAsync("auto", new SetModelOptions { AutoTier = AutoTier.Intelligence });
        await AssertPendingAutoTierAsync(session, AutoTier.Intelligence);

        // ResetAutoTier clears it. Omission, a value, and a reset are three distinct
        // outcomes, which is why a single nullable property cannot express the request.
        await session.SetModelAsync("auto", new SetModelOptions { ResetAutoTier = true });
        await AssertPendingAutoTierAsync(session, null);
    }
}
