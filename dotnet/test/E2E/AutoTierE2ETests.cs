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
        var superseded = await session.SetAutoTierAsync(AutoTier.Fast);
        Assert.Equal(ModelSwitchAutoTierStatus.Pending, superseded.Status);
        Assert.Equal(AutoTier.Fast, superseded.PendingAutoTier);
        Assert.Equal(AutoTier.Efficiency, superseded.SupersededAutoTier);
        await AssertPendingAutoTierAsync(session, AutoTier.Fast);

        var replacedFast = await session.SetAutoTierAsync(AutoTier.Intelligence);
        Assert.Equal(ModelSwitchAutoTierStatus.Pending, replacedFast.Status);
        Assert.Equal(AutoTier.Intelligence, replacedFast.PendingAutoTier);
        Assert.Equal(AutoTier.Fast, replacedFast.SupersededAutoTier);
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
        await session.SetModelAsync("auto", new SetModelOptions { AutoTier = AutoTier.Fast });
        await AssertPendingAutoTierAsync(session, AutoTier.Fast);

        // ResetAutoTier clears it. Omission, a value, and a reset are three distinct
        // outcomes, which is why a single nullable property cannot express the request.
        await session.SetModelAsync("auto", new SetModelOptions { ResetAutoTier = true });
        await AssertPendingAutoTierAsync(session, null);
    }

    [Fact]
    public async Task Should_Restore_And_Override_Fast_Auto_Tier_On_Cold_Resume()
    {
        var initialClient = Ctx.CreateClient();
        string fastSessionId;
        string tierlessSessionId;

        await using (initialClient)
        {
            await using (var fastSession = await Ctx.CreateSessionAsync(initialClient, new SessionConfig
            {
                Model = "auto",
                OnPermissionRequest = PermissionHandler.ApproveAll,
                Capi = new CapiSessionOptions
                {
                    AutoTier = AutoTier.Fast,
                    EnableWebSocketResponses = false,
                },
            }))
            await using (var tierlessSession = await Ctx.CreateSessionAsync(initialClient, new SessionConfig
            {
                Model = "auto",
                OnPermissionRequest = PermissionHandler.ApproveAll,
                Capi = new CapiSessionOptions { EnableWebSocketResponses = false },
            }))
            {
                fastSessionId = fastSession.SessionId;
                tierlessSessionId = tierlessSession.SessionId;

                await fastSession.SendAndWaitAsync(new MessageOptions
                {
                    Prompt = "Reply with exactly AUTO_TIER_COLD_RESUME_READY.",
                });
                await tierlessSession.SendAndWaitAsync(new MessageOptions
                {
                    Prompt = "Reply with exactly AUTO_TIER_TIERLESS_READY.",
                });

                var fastCurrent = await fastSession.Rpc.Model.GetCurrentAsync();
                Assert.Equal(AutoTier.Fast, fastCurrent.AutoTier);
                var tierlessCurrent = await tierlessSession.Rpc.Model.GetCurrentAsync();
                Assert.Null(tierlessCurrent.AutoTier);
            }

            await initialClient.StopAsync();
        }

        var restoredClient = Ctx.CreateClient();
        await using (restoredClient)
        {
            await using (var restoredFast = await Ctx.ResumeSessionAsync(
                restoredClient,
                fastSessionId,
                new ResumeSessionConfig { OnPermissionRequest = PermissionHandler.ApproveAll }))
            await using (var restoredTierless = await Ctx.ResumeSessionAsync(
                restoredClient,
                tierlessSessionId,
                new ResumeSessionConfig { OnPermissionRequest = PermissionHandler.ApproveAll }))
            {
                var restoredFastCurrent = await restoredFast.Rpc.Model.GetCurrentAsync();
                Assert.Equal(AutoTier.Fast, restoredFastCurrent.AutoTier);
                var restoredTierlessCurrent = await restoredTierless.Rpc.Model.GetCurrentAsync();
                Assert.Null(restoredTierlessCurrent.AutoTier);
            }

            await restoredClient.StopAsync();
        }

        var overrideClient = Ctx.CreateClient();
        await using (overrideClient)
        {
            await using var overridden = await Ctx.ResumeSessionAsync(
                overrideClient,
                fastSessionId,
                new ResumeSessionConfig
                {
                    Model = "auto",
                    OnPermissionRequest = PermissionHandler.ApproveAll,
                    Capi = new CapiSessionOptions
                    {
                        AutoTier = AutoTier.Balance,
                        EnableWebSocketResponses = false,
                    },
                });

            var overriddenCurrent = await overridden.Rpc.Model.GetCurrentAsync();
            Assert.Equal(AutoTier.Balance, overriddenCurrent.AutoTier);
        }
    }

    [Fact]
    [Trait(E2ETestTraits.Backend, E2ETestTraits.CapiOnly)]
    public async Task Should_Commit_Fast_Auto_Tier_After_Successful_Turn()
    {
        await using var client = Ctx.CreateClient();
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            Model = "auto",
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Capi = new CapiSessionOptions
            {
                AutoTier = AutoTier.Efficiency,
                EnableWebSocketResponses = false,
            },
        });

        var modelChangedTask = TestHelper.GetNextEventOfTypeAsync<SessionModelChangeEvent>(
            session,
            evt => evt.Data.AutoTier == AutoTier.Fast);

        var staged = await session.SetAutoTierAsync(AutoTier.Fast);
        Assert.Equal(ModelSwitchAutoTierStatus.Pending, staged.Status);
        Assert.Equal(AutoTier.Efficiency, staged.EffectiveAutoTier);
        Assert.Equal(AutoTier.Fast, staged.PendingAutoTier);

        var beforeTurn = await session.Rpc.Model.GetCurrentAsync();
        Assert.Equal(AutoTier.Efficiency, beforeTurn.AutoTier);
        Assert.Equal(AutoTier.Fast, beforeTurn.PendingAutoTier);

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with exactly AUTO_TIER_FAST_COMMITTED.",
        });

        var modelChanged = await modelChangedTask;
        Assert.Equal("auto", modelChanged.Data.PreviousModel);
        Assert.Equal("auto", modelChanged.Data.NewModel);
        Assert.Equal(AutoTier.Efficiency, modelChanged.Data.PreviousAutoTier);
        Assert.Equal(AutoTier.Fast, modelChanged.Data.AutoTier);

        var committed = await session.Rpc.Model.GetCurrentAsync();
        Assert.Equal(AutoTier.Fast, committed.AutoTier);
        Assert.Null(committed.PendingAutoTier);
        Assert.Null(committed.ActivatingAutoTier);
    }

    [Fact]
    [Trait(E2ETestTraits.Backend, E2ETestTraits.CapiOnly)]
    public async Task Should_Preserve_Effective_Tier_When_Fast_Activation_Fails()
    {
        string sessionId;
        var initialClient = Ctx.CreateClient();

        await using (initialClient)
        {
            await using (var session = await Ctx.CreateSessionAsync(initialClient, new SessionConfig
            {
                Model = "auto",
                OnPermissionRequest = PermissionHandler.ApproveAll,
                Capi = new CapiSessionOptions
                {
                    AutoTier = AutoTier.Efficiency,
                    EnableWebSocketResponses = false,
                },
            }))
            {
                sessionId = session.SessionId;
                await session.SendAndWaitAsync(new MessageOptions
                {
                    Prompt = "Reply with exactly AUTO_TIER_INITIAL_READY.",
                });

                var failureTask = TestHelper.GetNextEventOfTypeAsync<SessionAutoTierSwitchFailedEvent>(session);
                var fastCommit = new TaskCompletionSource<SessionModelChangeEvent>(
                    TaskCreationOptions.RunContinuationsAsynchronously);
                using var modelChangeSubscription = session.On<SessionModelChangeEvent>(evt =>
                {
                    if (evt.Data.AutoTier == AutoTier.Fast)
                    {
                        fastCommit.TrySetResult(evt);
                    }
                });

                var staged = await session.SetAutoTierAsync(AutoTier.Fast);
                Assert.Equal(ModelSwitchAutoTierStatus.Pending, staged.Status);
                Assert.Equal(AutoTier.Efficiency, staged.EffectiveAutoTier);
                Assert.Equal(AutoTier.Fast, staged.PendingAutoTier);

                await session.SendAndWaitAsync(new MessageOptions
                {
                    Prompt = "Reply with exactly AUTO_TIER_FAILURE_RECOVERED.",
                });

                var failure = await failureTask;
                Assert.True(failure.Ephemeral);
                Assert.Equal(AutoTier.Efficiency, failure.Data.EffectiveAutoTier);
                Assert.Equal(AutoTier.Fast, failure.Data.RequestedAutoTier);
                Assert.Equal(AutoTierSwitchFailureReason.RequestFailed, failure.Data.Reason);
                var noFastCommit = await Task.WhenAny(fastCommit.Task, Task.Delay(TimeSpan.FromMilliseconds(100)));
                Assert.NotSame(fastCommit.Task, noFastCommit);

                var current = await session.Rpc.Model.GetCurrentAsync();
                Assert.Equal(AutoTier.Efficiency, current.AutoTier);
                Assert.Null(current.PendingAutoTier);
                Assert.Null(current.ActivatingAutoTier);
            }

            await initialClient.StopAsync();
        }

        await using var resumedClient = Ctx.CreateClient();
        await using var resumed = await Ctx.ResumeSessionAsync(
            resumedClient,
            sessionId,
            new ResumeSessionConfig { OnPermissionRequest = PermissionHandler.ApproveAll });

        var resumedCurrent = await resumed.Rpc.Model.GetCurrentAsync();
        Assert.Equal(AutoTier.Efficiency, resumedCurrent.AutoTier);
        Assert.Null(resumedCurrent.PendingAutoTier);
        Assert.Null(resumedCurrent.ActivatingAutoTier);

        var persisted = await resumed.Rpc.EventLog.ReadAsync(max: 100, waitMs: TimeSpan.Zero);
        Assert.DoesNotContain(persisted.Events, evt => evt is SessionAutoTierSwitchFailedEvent);
    }
}
