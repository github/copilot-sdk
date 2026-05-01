/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

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
        session.On(evt => { lock (events) { events.Add(evt); } });

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
        session.On(evt => { lock (events) { events.Add(evt); } });

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
    public async Task Should_Emit_Tool_Execution_Events_With_Correct_Fields()
    {
        await File.WriteAllTextAsync(Path.Join(Ctx.WorkDir, "data.txt"), "test data");

        var session = await CreateSessionAsync();
        var events = new List<SessionEvent>();
        session.On(evt => { lock (events) { events.Add(evt); } });

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
        session.On(evt => { lock (events) { events.Add(evt); } });

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
}
