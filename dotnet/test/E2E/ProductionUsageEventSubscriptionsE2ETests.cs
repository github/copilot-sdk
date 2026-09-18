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

public class ProductionUsageEventSubscriptionsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ProductionUsageE2ETestBase(fixture, "production_usage_event_subscriptions", output)
{
    private static readonly TimeSpan EventTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Deliver_Mixed_App_Event_Stream_In_Order_After_Handler_Lag()
    {
        var handlerEntered = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseHandler = new ManualResetEventSlim();
        var events = new List<SessionEvent>();

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Streaming = true,
            Tools = [AIFunctionFactory.Create(AppLookup, "app_event_lookup")],
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
            Prompt = "Call app_event_lookup with key 'ordered', then reply with exactly its result.",
            DisplayPrompt = "Run ordered app lookup",
            Source = MessageSource.Agent("production-client"),
        }, timeout: TimeSpan.FromSeconds(120));

        await handlerEntered.Task.WaitAsync(EventTimeout);
        await Task.Delay(100);
        releaseHandler.Set();
        var response = await send;
        Assert.Contains("APP_EVENT_ORDERED", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);

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

        [Description("Looks up app-owned event data")]
        static string AppLookup([Description("Lookup key")] string key) => $"APP_EVENT_{key.ToUpperInvariant()}";
    }

    [Fact]
    public async Task Should_Stop_Closed_And_Replaced_App_Event_Sources()
    {
        const string connectionToken = "production-client-events-token";
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
                Prompt = "Reply with exactly APP_EVENT_SOURCE_ONE.",
                Source = MessageSource.Agent("production-client"),
            });
            await firstSession.Rpc.SuspendAsync();
            await firstSession.DisposeAsync();
            await firstClient.ForceStopAsync();

            await TestHelper.WaitForConditionAsync(
                () => Task.FromResult(IsEventChannelClosed(firstSession)),
                timeout: EventTimeout,
                timeoutMessage: "Timed out waiting for the old app event source to close.");
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
        using var newSubscription = secondSession.On<SessionEvent>(newEvents.Add);
        var newInfo = new TaskCompletionSource<SessionInfoEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var infoSubscription = secondSession.On<SessionInfoEvent>(evt =>
        {
            if (evt.Data.Message == "APP_EVENT_SOURCE_TWO")
            {
                newInfo.TrySetResult(evt);
            }
        });
        await secondSession.LogAsync("APP_EVENT_SOURCE_TWO");
        await newInfo.Task.WaitAsync(EventTimeout);

        Assert.Equal(countAfterClose, Volatile.Read(ref oldEventCount));
        Assert.Contains(newEvents, evt => evt is SessionInfoEvent info && info.Data.Message == "APP_EVENT_SOURCE_TWO");
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
