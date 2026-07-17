/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections;
using System.Reflection;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// Verifies that .NET completion remains reliable when the runtime's ephemeral
/// <c>session.idle</c> notification does not reach the SendAndWait handler.
/// </summary>
public class SendAndWaitReliabilityE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "send_and_wait_reliability", output)
{
    [Fact]
    public async Task Should_Complete_When_Live_SessionIdle_Is_Dropped()
    {
        await using var session = await CreateSessionAsync();
        var userMessageDispatched = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseUserMessage = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var idleObserved = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);

        using var userSubscription = session.On<UserMessageEvent>(_ =>
        {
            userMessageDispatched.TrySetResult();
            releaseUserMessage.Task.GetAwaiter().GetResult();
        });
        using var idleSubscription = session.On<SessionIdleEvent>(_ => idleObserved.TrySetResult());

        var completionTask = session.SendAndWaitAsync(
            new MessageOptions { Prompt = "What is 5+5? Reply with just the number." },
            timeout: TimeSpan.FromSeconds(30));

        try
        {
            await userMessageDispatched.Task.WaitAsync(TimeSpan.FromSeconds(10));
            SuppressIdleForSendAndWaitHandler(session);
        }
        finally
        {
            releaseUserMessage.TrySetResult();
        }

        var response = await completionTask;
        await idleObserved.Task.WaitAsync(TimeSpan.FromSeconds(10));

        Assert.NotNull(response);
        Assert.Contains("10", response.Data.Content);
    }

    private static void SuppressIdleForSendAndWaitHandler(CopilotSession session)
    {
        var handlersField = typeof(CopilotSession).GetField(
            "_eventHandlers",
            BindingFlags.Instance | BindingFlags.NonPublic)
            ?? throw new InvalidOperationException("CopilotSession._eventHandlers was not found.");
        var handlers = (IEnumerable)(handlersField.GetValue(session)
            ?? throw new InvalidOperationException("CopilotSession._eventHandlers was null."));

        var sendAndWaitSubscription = handlers.Cast<object>().Last(subscription =>
        {
            var eventType = subscription.GetType().GetProperty("EventType")?.GetValue(subscription);
            return Equals(eventType, typeof(SessionEvent));
        });
        var handlerField = sendAndWaitSubscription.GetType().GetField(
            "<Handler>k__BackingField",
            BindingFlags.Instance | BindingFlags.NonPublic)
            ?? throw new InvalidOperationException("EventSubscription.Handler backing field was not found.");
        var original = (Action<SessionEvent>)(handlerField.GetValue(sendAndWaitSubscription)
            ?? throw new InvalidOperationException("SendAndWait event handler was null."));

        handlerField.SetValue(sendAndWaitSubscription, (Action<SessionEvent>)(evt =>
        {
            if (evt is not SessionIdleEvent)
            {
                original(evt);
            }
        }));
    }
}
