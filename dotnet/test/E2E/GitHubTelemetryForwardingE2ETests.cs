/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

#pragma warning disable GHCP001 // GitHub telemetry forwarding is experimental.

public class GitHubTelemetryForwardingE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "github_telemetry", output)
{
    [Fact]
    public async Task Should_Forward_GitHub_Telemetry_For_A_Live_Session()
    {
        var notifications = new ConcurrentQueue<GitHubTelemetryNotification>();

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            OnGitHubTelemetry = notification =>
            {
                notifications.Enqueue(notification);
                return Task.CompletedTask;
            },
        });

        CopilotSession? session = null;
        try
        {
            session = await client.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
            });

            await TestHelper.WaitForConditionAsync(
                () => !notifications.IsEmpty,
                timeout: TimeSpan.FromSeconds(30),
                timeoutMessage: "Timed out waiting for GitHub telemetry notification.");

            Assert.True(notifications.TryPeek(out var notification));
            Assert.NotEmpty(notification.SessionId);
            Assert.NotNull(notification.Event);
            Assert.NotEmpty(notification.Event.Kind);
            Assert.IsType<bool>(notification.Restricted);
        }
        finally
        {
            if (session is not null)
            {
                await session.DisposeAsync();
            }

            await client.StopAsync();
        }
    }
}

#pragma warning restore GHCP001
