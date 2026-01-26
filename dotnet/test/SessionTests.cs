/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using System.ComponentModel;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class SessionTests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "session", output)
{
    [Fact]
    public async Task ShouldCreateAndDestroySessions()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig { Model = "fake-test-model" });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        var messages = await session.GetMessagesAsync();
        Assert.NotEmpty(messages);
        var startEvent = Assert.IsType<SessionStartEvent>(messages[0]);
        Assert.Equal(session.SessionId, startEvent.Data.SessionId);

        await session.DisposeAsync();

        var ex = await Assert.ThrowsAsync<IOException>(() => session.GetMessagesAsync());
        Assert.Contains("not found", ex.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_Have_Stateful_Conversation()
    {
        var session = await Client.CreateSessionAsync();

        var assistantMessage = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });
        Assert.NotNull(assistantMessage);
        Assert.Contains("2", assistantMessage!.Data.Content);

        var secondMessage = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Now if you double that, what do you get?" });
        Assert.NotNull(secondMessage);
        Assert.Contains("4", secondMessage!.Data.Content);
    }

    [Fact]
    public async Task Should_Create_A_Session_With_Appended_SystemMessage_Config()
    {
        var systemMessageSuffix = "End each response with the phrase 'Have a nice day!'";
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            SystemMessage = new SystemMessageConfig { Mode = SystemMessageMode.Append, Content = systemMessageSuffix }
        });

        await session.SendAsync(new MessageOptions { Prompt = "What is your full name?" });
        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);

        var content = assistantMessage!.Data.Content ?? string.Empty;
        Assert.Contains("GitHub", content);
        Assert.Contains("Have a nice day!", content);

        var traffic = await Ctx.GetExchangesAsync();
        Assert.NotEmpty(traffic);
        var systemMessage = GetSystemMessage(traffic[0]);
        Assert.Contains("GitHub", systemMessage);
        Assert.Contains(systemMessageSuffix, systemMessage);
    }

    [Fact]
    public async Task Should_Create_A_Session_With_Replaced_SystemMessage_Config()
    {
        var testSystemMessage = "You are an assistant called Testy McTestface. Reply succinctly.";
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            SystemMessage = new SystemMessageConfig { Mode = SystemMessageMode.Replace, Content = testSystemMessage }
        });

        await session.SendAsync(new MessageOptions { Prompt = "What is your full name?" });
        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);

        var content = assistantMessage!.Data.Content ?? string.Empty;
        Assert.DoesNotContain("GitHub", content);
        Assert.Contains("Testy", content);

        var traffic = await Ctx.GetExchangesAsync();
        Assert.NotEmpty(traffic);
        Assert.Equal(testSystemMessage, GetSystemMessage(traffic[0]));
    }

    [Fact]
    public async Task Should_Create_A_Session_With_AvailableTools()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            AvailableTools = new List<string> { "view", "edit" }
        });

        await session.SendAsync(new MessageOptions { Prompt = "What is 1+1?" });
        await TestHelper.GetFinalAssistantMessageAsync(session);

        var traffic = await Ctx.GetExchangesAsync();
        Assert.NotEmpty(traffic);

        var toolNames = GetToolNames(traffic[0]);
        Assert.Equal(2, toolNames.Count);
        Assert.Contains("view", toolNames);
        Assert.Contains("edit", toolNames);
    }

    [Fact]
    public async Task Should_Create_A_Session_With_ExcludedTools()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            ExcludedTools = new List<string> { "view" }
        });

        await session.SendAsync(new MessageOptions { Prompt = "What is 1+1?" });
        await TestHelper.GetFinalAssistantMessageAsync(session);

        var traffic = await Ctx.GetExchangesAsync();
        Assert.NotEmpty(traffic);

        var toolNames = GetToolNames(traffic[0]);
        Assert.DoesNotContain("view", toolNames);
        Assert.Contains("edit", toolNames);
        Assert.Contains("grep", toolNames);
    }

    [Fact]
    public async Task Should_Create_Session_With_Custom_Tool()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            Tools =
            [
                AIFunctionFactory.Create(async ([Description("Key")] string key) => {
                    await Task.Delay(100); // Just to verify tools can be async
                    return key == "ALPHA" ? 54321 : 0;
                }, "get_secret_number", "Gets the secret number"),
            ]
        });

        await session.SendAsync(new MessageOptions { Prompt = "What is the secret number for key ALPHA?" });
        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Contains("54321", assistantMessage!.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Should_Resume_A_Session_Using_The_Same_Client()
    {
        var session1 = await Client.CreateSessionAsync();
        var sessionId = session1.SessionId;

        await session1.SendAsync(new MessageOptions { Prompt = "What is 1+1?" });
        var answer = await TestHelper.GetFinalAssistantMessageAsync(session1);
        Assert.NotNull(answer);
        Assert.Contains("2", answer!.Data.Content ?? string.Empty);

        var session2 = await Client.ResumeSessionAsync(sessionId);
        Assert.Equal(sessionId, session2.SessionId);

        var answer2 = await TestHelper.GetFinalAssistantMessageAsync(session2);
        Assert.NotNull(answer2);
        Assert.Contains("2", answer2!.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Should_Resume_A_Session_Using_A_New_Client()
    {
        var session1 = await Client.CreateSessionAsync();
        var sessionId = session1.SessionId;

        await session1.SendAsync(new MessageOptions { Prompt = "What is 1+1?" });
        var answer = await TestHelper.GetFinalAssistantMessageAsync(session1);
        Assert.NotNull(answer);
        Assert.Contains("2", answer!.Data.Content ?? string.Empty);

        using var newClient = Ctx.CreateClient();
        var session2 = await newClient.ResumeSessionAsync(sessionId);
        Assert.Equal(sessionId, session2.SessionId);

        var messages = await session2.GetMessagesAsync();
        Assert.Contains(messages, m => m is UserMessageEvent);
        Assert.Contains(messages, m => m is SessionResumeEvent);
    }

    [Fact]
    public async Task Should_Throw_Error_When_Resuming_Non_Existent_Session()
    {
        await Assert.ThrowsAsync<IOException>(() =>
            Client.ResumeSessionAsync("non-existent-session-id"));
    }

    [Fact]
    public async Task Should_Abort_A_Session()
    {
        var session = await Client.CreateSessionAsync();

        // Set up wait for tool execution to start BEFORE sending
        var toolStartTask = TestHelper.GetNextEventOfTypeAsync<ToolExecutionStartEvent>(session);
        var sessionIdleTask = TestHelper.GetNextEventOfTypeAsync<SessionIdleEvent>(session);

        // Send a message that will take some time to process
        await session.SendAsync(new MessageOptions
        {
            Prompt = "run the shell command 'sleep 100' (note this works on both bash and PowerShell)"
        });

        // Wait for tool execution to start
        await toolStartTask;

        // Abort the session
        await session.AbortAsync();
        await sessionIdleTask;

        // The session should still be alive and usable after abort
        var messages = await session.GetMessagesAsync();
        Assert.NotEmpty(messages);

        // Verify an abort event exists in messages
        Assert.Contains(messages, m => m is AbortEvent);

        // We should be able to send another message
        var answer = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });
        Assert.NotNull(answer);
        Assert.Contains("4", answer!.Data.Content ?? string.Empty);
    }

    // TODO: This test requires the session-events.schema.json to include assistant.message_delta.
    // The CLI v0.0.376 emits delta events at runtime, but the schema hasn't been updated yet.
    // Once the schema is updated and types are regenerated, this test can be enabled.
    [Fact(Skip = "Requires schema update for AssistantMessageDeltaEvent type")]
    public async Task Should_Receive_Streaming_Delta_Events_When_Streaming_Is_Enabled()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig { Streaming = true });

        var deltaContents = new List<string>();
        var doneEvent = new TaskCompletionSource<bool>();

        session.On(evt =>
        {
            switch (evt)
            {
                // TODO: Uncomment once AssistantMessageDeltaEvent is generated
                // case AssistantMessageDeltaEvent delta:
                //     if (!string.IsNullOrEmpty(delta.Data.DeltaContent))
                //         deltaContents.Add(delta.Data.DeltaContent);
                //     break;
                case SessionIdleEvent:
                    doneEvent.TrySetResult(true);
                    break;
            }
        });

        await session.SendAsync(new MessageOptions { Prompt = "What is 2+2?" });

        // Wait for completion
        var completed = await Task.WhenAny(doneEvent.Task, Task.Delay(TimeSpan.FromSeconds(60)));
        Assert.Equal(doneEvent.Task, completed);

        // Should have received delta events
        Assert.NotEmpty(deltaContents);

        // Get the final message to compare
        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);

        // Accumulated deltas should equal the final message
        var accumulated = string.Join("", deltaContents);
        Assert.Equal(assistantMessage!.Data.Content, accumulated);

        // Final message should contain the answer
        Assert.Contains("4", assistantMessage.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Should_Pass_Streaming_Option_To_Session_Creation()
    {
        // Verify that the streaming option is accepted without errors
        var session = await Client.CreateSessionAsync(new SessionConfig { Streaming = true });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        // Session should still work normally
        await session.SendAsync(new MessageOptions { Prompt = "What is 1+1?" });
        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Contains("2", assistantMessage!.Data.Content);
    }

    [Fact]
    public async Task Should_Receive_Session_Events()
    {
        var session = await Client.CreateSessionAsync();
        var receivedEvents = new List<SessionEvent>();
        var idleReceived = new TaskCompletionSource<bool>();

        session.On(evt =>
        {
            receivedEvents.Add(evt);
            if (evt is SessionIdleEvent)
            {
                idleReceived.TrySetResult(true);
            }
        });

        // Send a message to trigger events
        await session.SendAsync(new MessageOptions { Prompt = "What is 100+200?" });

        // Wait for session to become idle (indicating message processing is complete)
        var completed = await Task.WhenAny(idleReceived.Task, Task.Delay(TimeSpan.FromSeconds(60)));
        Assert.Equal(idleReceived.Task, completed);

        // Should have received multiple events (user message, assistant message, idle, etc.)
        Assert.NotEmpty(receivedEvents);
        Assert.Contains(receivedEvents, evt => evt is UserMessageEvent);
        Assert.Contains(receivedEvents, evt => evt is AssistantMessageEvent);
        Assert.Contains(receivedEvents, evt => evt is SessionIdleEvent);

        // Verify the assistant response contains the expected answer
        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Contains("300", assistantMessage!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Send_Returns_Immediately_While_Events_Stream_In_Background()
    {
        var session = await Client.CreateSessionAsync();
        var events = new List<string>();

        session.On(evt => events.Add(evt.Type));

        // Use a slow command so we can verify SendAsync() returns before completion
        await session.SendAsync(new MessageOptions { Prompt = "Run 'sleep 2 && echo done'" });

        // SendAsync() should return before turn completes (no session.idle yet)
        Assert.DoesNotContain("session.idle", events);

        // Wait for turn to complete
        var message = await TestHelper.GetFinalAssistantMessageAsync(session);

        Assert.Contains("done", message?.Data.Content ?? string.Empty);
        Assert.Contains("session.idle", events);
        Assert.Contains("assistant.message", events);
    }

    [Fact]
    public async Task SendAndWait_Blocks_Until_Session_Idle_And_Returns_Final_Assistant_Message()
    {
        var session = await Client.CreateSessionAsync();
        var events = new List<string>();

        session.On(evt => events.Add(evt.Type));

        var response = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });

        Assert.NotNull(response);
        Assert.Equal("assistant.message", response!.Type);
        Assert.Contains("4", response.Data.Content ?? string.Empty);
        Assert.Contains("session.idle", events);
        Assert.Contains("assistant.message", events);
    }

    [Fact]
    public async Task SendAndWait_Throws_On_Timeout()
    {
        var session = await Client.CreateSessionAsync();

        // Use a slow command to ensure timeout triggers before completion
        var ex = await Assert.ThrowsAsync<TimeoutException>(() =>
            session.SendAndWaitAsync(new MessageOptions { Prompt = "Run 'sleep 2 && echo done'" }, TimeSpan.FromMilliseconds(100)));

        Assert.Contains("timed out", ex.Message);
    }

    [Fact]
    public async Task Should_Create_Session_With_Custom_Config_Dir()
    {
        var customConfigDir = Path.Join(Ctx.HomeDir, "custom-config");
        var session = await Client.CreateSessionAsync(new SessionConfig { ConfigDir = customConfigDir });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        // Session should work normally with custom config dir
        await session.SendAsync(new MessageOptions { Prompt = "What is 1+1?" });
        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Contains("2", assistantMessage!.Data.Content);
    }
}
