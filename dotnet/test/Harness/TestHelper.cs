/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot.SDK.Test.Harness;

public static class TestHelper
{
    public static async Task<SessionOutcome> GetFinalSessionOutcomeAsync(
        CopilotSession session,
        TimeSpan? timeout = null)
    {
        var tcs = new TaskCompletionSource<SessionOutcome>();
        using var cts = new CancellationTokenSource(timeout ?? TimeSpan.FromSeconds(60));

        AssistantMessageEvent? finalAssistantMessage = null;

        using var subscription = session.On(evt =>
        {
            switch (evt)
            {
                case AssistantMessageEvent msg:
                    finalAssistantMessage = msg;
                    break;
                case SessionIdleEvent:
                    tcs.TrySetResult(finalAssistantMessage != null
                        ? SessionOutcome.Message(finalAssistantMessage)
                        : SessionOutcome.Abstention());
                    break;
                case SessionErrorEvent error:
                    tcs.TrySetException(new Exception(error.Data.Message ?? "session error"));
                    break;
            }
        });

        // Check existing messages
        CheckExistingMessages();

        cts.Token.Register(() => tcs.TrySetException(new TimeoutException("Timeout waiting for session outcome")));

        return await tcs.Task;

        async void CheckExistingMessages()
        {
            try
            {
                var existing = await GetExistingFinalOutcomeAsync(session);
                if (existing != null) tcs.TrySetResult(existing);
            }
            catch (Exception ex)
            {
                tcs.TrySetException(ex);
            }
        }
    }

    public static async Task<AssistantMessageEvent?> GetFinalAssistantMessageAsync(
        CopilotSession session,
        TimeSpan? timeout = null)
    {
        var outcome = await GetFinalSessionOutcomeAsync(session, timeout);
        return outcome.AssistantMessage;
    }

    private static async Task<SessionOutcome?> GetExistingFinalOutcomeAsync(CopilotSession session)
    {
        var messages = (await session.GetMessagesAsync()).ToList();

        var lastUserIdx = messages.FindLastIndex(m => m is UserMessageEvent);
        var currentTurn = lastUserIdx < 0 ? messages : messages.Skip(lastUserIdx).ToList();

        var error = currentTurn.OfType<SessionErrorEvent>().FirstOrDefault();
        if (error != null) throw new Exception(error.Data.Message ?? "session error");

        var idleIdx = currentTurn.FindIndex(m => m is SessionIdleEvent);
        if (idleIdx == -1) return null;

        for (var i = idleIdx - 1; i >= 0; i--)
        {
            if (currentTurn[i] is AssistantMessageEvent msg)
                return SessionOutcome.Message(msg);
        }

        return SessionOutcome.Abstention();
    }

    public static async Task<T> GetNextEventOfTypeAsync<T>(
        CopilotSession session,
        TimeSpan? timeout = null) where T : SessionEvent
    {
        var tcs = new TaskCompletionSource<T>();
        using var cts = new CancellationTokenSource(timeout ?? TimeSpan.FromSeconds(60));

        using var subscription = session.On(evt =>
        {
            if (evt is T matched)
            {
                tcs.TrySetResult(matched);
            }
            else if (evt is SessionErrorEvent error)
            {
                tcs.TrySetException(new Exception(error.Data.Message ?? "session error"));
            }
        });

        cts.Token.Register(() => tcs.TrySetException(
            new TimeoutException($"Timeout waiting for event of type '{typeof(T).Name}'")));

        return await tcs.Task;
    }
}
