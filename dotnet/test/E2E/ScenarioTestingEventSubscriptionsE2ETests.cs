/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;
using GitHub.Copilot.Test.Harness;
using System.ComponentModel;
using System.Reflection;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ScenarioTestingEventSubscriptionsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_event_subscriptions", output)
{
    private static readonly TimeSpan EventTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Deliver_Mixed_Scenario_Event_Stream_In_Order_After_Handler_Lag()
    {
        var handlerEntered = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var toolInvoked = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        using var releaseHandler = new ManualResetEventSlim();
        var events = new List<SessionEvent>();

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Streaming = true,
            Tools = [AIFunctionFactory.Create(ScenarioLookup, "scenario_event_lookup")],
        });

        using var subscription = session.On<SessionEvent>(evt =>
        {
            if (evt is UserMessageEvent)
            {
                handlerEntered.TrySetResult();
                releaseHandler.Wait(EventTimeout);
            }

            lock (events)
            {
                events.Add(evt);
            }
        });

        var send = session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Call scenario_event_lookup with key 'ordered', then reply with exactly its result.",
            DisplayPrompt = "Run ordered scenario lookup",
            Source = MessageSource.Agent("scenario-client"),
        }, timeout: TimeSpan.FromSeconds(120));

        await handlerEntered.Task.WaitAsync(EventTimeout);
        try
        {
            await toolInvoked.Task.WaitAsync(EventTimeout);
        }
        finally
        {
            releaseHandler.Set();
        }

        var response = await send;
        Assert.Contains("SCENARIO_EVENT_ORDERED", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        List<string> types;
        lock (events)
        {
            types = events.Select(evt => evt.Type).ToList();
        }

        var user = types.IndexOf("user.message");
        var toolStart = types.IndexOf("tool.execution_start");
        var toolComplete = types.IndexOf("tool.execution_complete");
        var assistant = types.LastIndexOf("assistant.message");
        var idle = types.LastIndexOf("session.idle");
        Assert.True(user < toolStart, string.Join(", ", types));
        Assert.True(toolStart < toolComplete, string.Join(", ", types));
        Assert.True(toolComplete < assistant, string.Join(", ", types));
        Assert.True(assistant < idle, string.Join(", ", types));

        [Description("Looks up scenario-owned event data")]
        string ScenarioLookup([Description("Lookup key")] string key)
        {
            toolInvoked.TrySetResult();
            return $"SCENARIO_EVENT_{key.ToUpperInvariant()}";
        }
    }

    [Fact]
    public async Task Should_Stop_Closed_And_Replaced_Scenario_Event_Sources()
    {
        const string connectionToken = "scenario-client-events-token";
        await using var server = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForTcp(connectionToken: connectionToken),
        });
        await server.StartAsync();
        var cliUrl = $"localhost:{server.RuntimePort}";

        var oldEventCount = 0;
        string sessionId;
        await using (var firstClient = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(cliUrl, connectionToken: connectionToken),
        }))
        {
            var firstSession = await Ctx.CreateSessionAsync(firstClient, new SessionConfig
            {
                Streaming = true,
                OnPermissionRequest = PermissionHandler.ApproveAll,
            });
            sessionId = firstSession.SessionId;
            using var oldSubscription = firstSession.On<SessionEvent>(_ => Interlocked.Increment(ref oldEventCount));

            await firstSession.SendAndWaitAsync(new MessageOptions
            {
                Prompt = "Reply with exactly SCENARIO_EVENT_SOURCE_ONE.",
                Source = MessageSource.Agent("scenario-client"),
            });
            await firstSession.Rpc.SuspendAsync();
            await firstSession.DisposeAsync();
            await firstClient.ForceStopAsync();

            await TestHelper.WaitForConditionAsync(
                () => Task.FromResult(IsEventChannelClosed(firstSession)),
                timeout: EventTimeout,
                timeoutMessage: "Timed out waiting for the old scenario event source to close.");
        }

        var countAfterClose = Volatile.Read(ref oldEventCount);
        await using var secondClient = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(cliUrl, connectionToken: connectionToken),
        });
        await using var secondSession = await Ctx.ResumeSessionAsync(secondClient, sessionId, new ResumeSessionConfig
        {
            Streaming = true,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var newEvents = new List<SessionEvent>();
        using var newSubscription = secondSession.On<SessionEvent>(evt =>
        {
            lock (newEvents)
            {
                newEvents.Add(evt);
            }
        });
        var newInfo = new TaskCompletionSource<SessionInfoEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var infoSubscription = secondSession.On<SessionInfoEvent>(evt =>
        {
            if (evt.Data.Message == "SCENARIO_EVENT_SOURCE_TWO")
            {
                newInfo.TrySetResult(evt);
            }
        });
        await secondSession.LogAsync("SCENARIO_EVENT_SOURCE_TWO");
        await newInfo.Task.WaitAsync(EventTimeout);

        Assert.Equal(countAfterClose, Volatile.Read(ref oldEventCount));
        lock (newEvents)
        {
            Assert.Contains(newEvents, evt => evt is SessionInfoEvent info && info.Data.Message == "SCENARIO_EVENT_SOURCE_TWO");
        }
    }

    private static bool IsEventChannelClosed(CopilotSession session)
    {
        var eventChannelField = typeof(CopilotSession).GetField(
            "_eventChannel",
            BindingFlags.Instance | BindingFlags.NonPublic)
            ?? throw new InvalidOperationException("CopilotSession._eventChannel was not found.");
        var channel = eventChannelField.GetValue(session)!;
        var reader = channel.GetType().GetProperty("Reader")!.GetValue(channel)!;
        return ((Task)reader.GetType().GetProperty("Completion")!.GetValue(reader)!).IsCompleted;
    }
}
