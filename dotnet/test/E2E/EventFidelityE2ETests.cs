/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// Verifies the shape and ordering of <see cref="SessionEvent"/>s emitted from the
/// runtime: every event has an id and timestamp, user/assistant messages carry
/// content, tool execution events carry a <c>toolCallId</c>, and
/// <c>session.idle</c> is the last event of a turn. Mirrors
/// <c>nodejs/test/e2e/event_fidelity.e2e.test.ts</c>.
/// </summary>
public class EventFidelityE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "event_fidelity", output)
{
    [Fact]
    public async Task Should_Emit_Events_In_Correct_Order_For_Tool_Using_Conversation()
    {
        await File.WriteAllTextAsync(Path.Join(Ctx.WorkDir, "hello.txt"), "Hello World");

        var session = await CreateSessionAsync();
        var events = new List<SessionEvent>();
        session.On<SessionEvent>(evt => { lock (events) { events.Add(evt); } });

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Read the file 'hello.txt' and tell me its contents.",
        });

        List<string> types;
        lock (events) { types = events.Select(e => e.Type).ToList(); }

        Assert.Contains("user.message", types);
        Assert.Contains("assistant.message", types);

        // user.message should come before the last assistant.message
        var userIdx = types.IndexOf("user.message");
        var assistantIdx = types.LastIndexOf("assistant.message");
        Assert.True(userIdx < assistantIdx, $"Expected user.message ({userIdx}) before last assistant.message ({assistantIdx})");

        // session.idle should be the last event we observed
        var idleIdx = types.LastIndexOf("session.idle");
        Assert.Equal(types.Count - 1, idleIdx);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Include_Valid_Fields_On_All_Events()
    {
        var session = await CreateSessionAsync();
        var events = new List<SessionEvent>();
        session.On<SessionEvent>(evt => { lock (events) { events.Add(evt); } });

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "What is 5+5? Reply with just the number.",
        });

        List<SessionEvent> snapshot;
        lock (events) { snapshot = [.. events]; }

        // All events must have an id and a timestamp
        foreach (var evt in snapshot)
        {
            Assert.NotEqual(Guid.Empty, evt.Id);
            Assert.NotEqual(default, evt.Timestamp);
        }

        // user.message should have content
        var userEvent = snapshot.OfType<UserMessageEvent>().FirstOrDefault();
        Assert.NotNull(userEvent);
        Assert.NotNull(userEvent!.Data.Content);

        // assistant.message should have messageId and content
        var assistantEvent = snapshot.OfType<AssistantMessageEvent>().FirstOrDefault();
        Assert.NotNull(assistantEvent);
        Assert.False(string.IsNullOrEmpty(assistantEvent!.Data.MessageId));
        Assert.NotNull(assistantEvent.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Emit_Assistant_Usage_Event_After_Model_Call()
    {
        var session = await CreateSessionAsync();
        var events = new List<SessionEvent>();
        session.On<SessionEvent>(evt => { lock (events) { events.Add(evt); } });

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "What is 5+5? Reply with just the number.",
        });

        AssistantUsageEvent? usageEvent;
        lock (events) { usageEvent = events.OfType<AssistantUsageEvent>().LastOrDefault(); }

        Assert.NotNull(usageEvent);
        Assert.False(string.IsNullOrWhiteSpace(usageEvent!.Data.Model));
        Assert.NotEqual(Guid.Empty, usageEvent.Id);
        Assert.NotEqual(default, usageEvent.Timestamp);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Emit_Session_Usage_Info_Event_After_Model_Call()
    {
        var session = await CreateSessionAsync();
        var events = new List<SessionEvent>();
        session.On<SessionEvent>(evt => { lock (events) { events.Add(evt); } });

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "What is 5+5? Reply with just the number.",
        });

        SessionUsageInfoEvent? usageInfoEvent;
        lock (events) { usageInfoEvent = events.OfType<SessionUsageInfoEvent>().LastOrDefault(); }

        Assert.NotNull(usageInfoEvent);
        Assert.True(usageInfoEvent!.Data.CurrentTokens > 0);
        Assert.True(usageInfoEvent.Data.MessagesLength > 0);
        Assert.True(usageInfoEvent.Data.TokenLimit > 0);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Emit_Pending_Messages_Modified_Event_When_Message_Queue_Changes()
    {
        var session = await CreateSessionAsync();
        var events = new List<SessionEvent>();
        session.On<SessionEvent>(evt => { lock (events) { events.Add(evt); } });

        // Use SendAndWaitAsync + a single event collector to match the pattern
        // of every other test in this fixture (and the Rust E2E equivalent).
        // The earlier SendAsync + GetFinalAssistantMessageAsync split relied
        // on a custom helper with an async-void backfill and required juggling
        // two independently-timed awaits, which has been observed to flake in
        // CI.
        var answer = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "What is 9+9? Reply with just the number.",
        }, timeout: TimeSpan.FromSeconds(120));

        PendingMessagesModifiedEvent? pendingEvent;
        lock (events) { pendingEvent = events.OfType<PendingMessagesModifiedEvent>().FirstOrDefault(); }

        Assert.NotNull(pendingEvent);
        Assert.NotNull(answer);
        Assert.Contains("18", answer!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Emit_Tool_Execution_Events_With_Correct_Fields()
    {
        await File.WriteAllTextAsync(Path.Join(Ctx.WorkDir, "data.txt"), "test data");

        var session = await CreateSessionAsync();
        var events = new List<SessionEvent>();
        session.On<SessionEvent>(evt => { lock (events) { events.Add(evt); } });

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Read the file 'data.txt'.",
        });

        List<SessionEvent> snapshot;
        lock (events) { snapshot = [.. events]; }

        var toolStarts = snapshot.OfType<ToolExecutionStartEvent>().ToList();
        var toolCompletes = snapshot.OfType<ToolExecutionCompleteEvent>().ToList();

        Assert.NotEmpty(toolStarts);
        Assert.NotEmpty(toolCompletes);

        var firstStart = toolStarts[0];
        Assert.False(string.IsNullOrEmpty(firstStart.Data.ToolCallId));
        Assert.False(string.IsNullOrEmpty(firstStart.Data.ToolName));

        var firstComplete = toolCompletes[0];
        Assert.False(string.IsNullOrEmpty(firstComplete.Data.ToolCallId));

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Emit_Assistant_Message_With_MessageId()
    {
        var session = await CreateSessionAsync();
        var events = new List<SessionEvent>();
        session.On<SessionEvent>(evt => { lock (events) { events.Add(evt); } });

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Say 'pong'.",
        });

        List<AssistantMessageEvent> assistantEvents;
        lock (events) { assistantEvents = events.OfType<AssistantMessageEvent>().ToList(); }

        Assert.NotEmpty(assistantEvents);

        var msg = assistantEvents[0];
        Assert.False(string.IsNullOrEmpty(msg.Data.MessageId));
        Assert.Contains("pong", msg.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Preserve_Message_Order_In_GetMessages_After_Tool_Use()
    {
        await File.WriteAllTextAsync(Path.Join(Ctx.WorkDir, "order.txt"), "ORDER_CONTENT_42");

        var session = await CreateSessionAsync();

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Read the file 'order.txt' and tell me what the number is.",
        });

        var messages = await session.GetEventsAsync();
        var types = messages.Select(m => m.Type).ToList();

        // Verify complete event ordering contract:
        // session.start → user.message → tool.execution_start → tool.execution_complete → assistant.message
        var sessionStartIdx = types.IndexOf("session.start");
        var userMsgIdx = types.IndexOf("user.message");
        var toolStartIdx = types.IndexOf("tool.execution_start");
        var toolCompleteIdx = types.IndexOf("tool.execution_complete");
        var assistantMsgIdx = types.LastIndexOf("assistant.message");

        Assert.True(sessionStartIdx >= 0, "Expected session.start event");
        Assert.True(userMsgIdx >= 0, "Expected user.message event");
        Assert.True(toolStartIdx >= 0, "Expected tool.execution_start event");
        Assert.True(toolCompleteIdx >= 0, "Expected tool.execution_complete event");
        Assert.True(assistantMsgIdx >= 0, "Expected assistant.message event");

        Assert.True(sessionStartIdx < userMsgIdx, "session.start should precede user.message");
        Assert.True(userMsgIdx < toolStartIdx, "user.message should precede tool.execution_start");
        Assert.True(toolStartIdx < toolCompleteIdx, "tool.execution_start should precede tool.execution_complete");
        Assert.True(toolCompleteIdx < assistantMsgIdx, "tool.execution_complete should precede final assistant.message");

        // Verify user.message has our content
        var userEvent = messages.OfType<UserMessageEvent>().First();
        Assert.Contains("order.txt", userEvent.Data.Content ?? string.Empty);

        // Verify assistant.message references the file content
        var assistantEvent = messages.OfType<AssistantMessageEvent>().Last();
        Assert.Contains("42", assistantEvent.Data.Content ?? string.Empty);

        await session.DisposeAsync();
    }
}
