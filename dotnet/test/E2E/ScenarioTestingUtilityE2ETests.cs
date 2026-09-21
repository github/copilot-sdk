/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ScenarioTestingUtilityE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_utility", output)
{
    [Fact]
    public async Task Should_Send_Wait_Observe_Idle_Events_And_Delete_Suggestion_Session()
    {
        var sessionId = Guid.NewGuid().ToString();
        var session = await CreateSessionAsync(new SessionConfig { SessionId = sessionId });
        var idle = TestHelper.GetNextEventOfTypeAsync<SessionIdleEvent>(
            session,
            TimeSpan.FromSeconds(60));

        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with exactly SCENARIO_SUGGESTION_ACCEPTED.",
            DisplayPrompt = "Apply suggested response",
            Mode = "enqueue",
            Source = MessageSource.Agent("suggestions"),
        });
        await idle;

        Assert.Contains("SCENARIO_SUGGESTION_ACCEPTED", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);
        var events = await session.GetEventsAsync();
        var userMessage = Assert.Single(
            events.OfType<UserMessageEvent>(),
            evt => evt.Data.Content == "Apply suggested response");
        Assert.Equal("agent-suggestions", userMessage.Data.Source);
        Assert.Equal(UserMessageDelivery.Idle, userMessage.Data.Delivery);
        Assert.Contains(
            events.OfType<AssistantMessageEvent>(),
            evt => (evt.Data.Content ?? string.Empty).Contains("SCENARIO_SUGGESTION_ACCEPTED", StringComparison.Ordinal));

        await session.DisposeAsync();
        await Client.DeleteSessionAsync(sessionId);
        Assert.Null(await Client.GetSessionMetadataAsync(sessionId));
    }
}
