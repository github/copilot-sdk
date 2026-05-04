/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

/// <summary>
/// Lifecycle coverage at the <see cref="CopilotClient"/> level: listing
/// persisted sessions, deleting a session, retrieving a session's stored
/// events, and running multiple sessions concurrently. Mirrors
/// <c>nodejs/test/e2e/session_lifecycle.e2e.test.ts</c>.
/// </summary>
public class SessionLifecycleE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "session_lifecycle", output)
{
    [Fact]
    public async Task Should_List_Created_Sessions_After_Sending_A_Message()
    {
        var session1 = await CreateSessionAsync();
        var session2 = await CreateSessionAsync();

        // Sessions must have activity to be persisted to disk
        await session1.SendAndWaitAsync(new MessageOptions { Prompt = "Say hello" });
        await session2.SendAndWaitAsync(new MessageOptions { Prompt = "Say world" });

        IList<SessionMetadata>? sessions = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                sessions = await Client.ListSessionsAsync();
                var ids = sessions.Select(s => s.SessionId).ToHashSet();
                return ids.Contains(session1.SessionId) && ids.Contains(session2.SessionId);
            },
            timeout: TimeSpan.FromSeconds(10),
            timeoutMessage: "Timed out waiting for both created sessions to appear in ListSessionsAsync().");

        Assert.NotNull(sessions);
        var sessionIds = sessions!.Select(s => s.SessionId).ToList();
        Assert.Contains(session1.SessionId, sessionIds);
        Assert.Contains(session2.SessionId, sessionIds);

        await session1.DisposeAsync();
        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Should_Delete_Session_Permanently()
    {
        var session = await CreateSessionAsync();
        var sessionId = session.SessionId;

        // Send a message so the session is persisted
        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi" });

        // Wait for the session to appear in the list
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var before = await Client.ListSessionsAsync();
                return before.Any(s => s.SessionId == sessionId);
            },
            timeout: TimeSpan.FromSeconds(10),
            timeoutMessage: "Timed out waiting for the persisted session to appear in ListSessionsAsync().");

        await session.DisposeAsync();
        await Client.DeleteSessionAsync(sessionId);

        // After delete, the session should not be in the list
        var after = await Client.ListSessionsAsync();
        Assert.DoesNotContain(after, s => s.SessionId == sessionId);
    }

    [Fact]
    public async Task Should_Return_Events_Via_GetMessages_After_Conversation()
    {
        var session = await CreateSessionAsync();

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "What is 2+2? Reply with just the number.",
        });

        var messages = await session.GetMessagesAsync();
        Assert.NotEmpty(messages);

        // Should have at least session.start, user.message, assistant.message
        var types = messages.Select(m => m.Type).ToList();
        Assert.Contains("session.start", types);
        Assert.Contains("user.message", types);
        Assert.Contains("assistant.message", types);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Support_Multiple_Concurrent_Sessions()
    {
        var session1 = await CreateSessionAsync();
        var session2 = await CreateSessionAsync();

        // Send to both sessions in parallel
        var task1 = session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "What is 1+1? Reply with just the number.",
        });
        var task2 = session2.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "What is 3+3? Reply with just the number.",
        });

        var results = await Task.WhenAll(task1, task2);

        Assert.Contains("2", results[0]?.Data.Content ?? string.Empty);
        Assert.Contains("6", results[1]?.Data.Content ?? string.Empty);

        await session1.DisposeAsync();
        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Should_Isolate_Events_Between_Concurrent_Sessions()
    {
        var session1 = await CreateSessionAsync();
        var session2 = await CreateSessionAsync();

        var session1Events = new List<SessionEvent>();
        var session2Events = new List<SessionEvent>();

        session1.On(evt => { lock (session1Events) { session1Events.Add(evt); } });
        session2.On(evt => { lock (session2Events) { session2Events.Add(evt); } });

        // Send to both sessions
        await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Say 'session_one_response'.",
        });
        await session2.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Say 'session_two_response'.",
        });

        List<SessionEvent> s1Snapshot, s2Snapshot;
        lock (session1Events) { s1Snapshot = [.. session1Events]; }
        lock (session2Events) { s2Snapshot = [.. session2Events]; }

        // Session 1's events should contain "session_one_response" but NOT "session_two_response"
        var s1Messages = s1Snapshot.OfType<AssistantMessageEvent>().Select(e => e.Data.Content ?? "").ToList();
        Assert.Contains(s1Messages, m => m.Contains("session_one_response"));
        Assert.DoesNotContain(s1Messages, m => m.Contains("session_two_response"));

        // Session 2's events should contain "session_two_response" but NOT "session_one_response"
        var s2Messages = s2Snapshot.OfType<AssistantMessageEvent>().Select(e => e.Data.Content ?? "").ToList();
        Assert.Contains(s2Messages, m => m.Contains("session_two_response"));
        Assert.DoesNotContain(s2Messages, m => m.Contains("session_one_response"));

        await session1.DisposeAsync();
        await session2.DisposeAsync();
    }
}
