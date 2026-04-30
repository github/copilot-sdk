/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot.SDK.Test.Harness;

public static class TestHelper
{
    public static async Task<AssistantMessageEvent?> GetFinalAssistantMessageAsync(
        CopilotSession session,
        TimeSpan? timeout = null,
        bool alreadyIdle = false)
    {
        var tcs = new TaskCompletionSource<AssistantMessageEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var cts = new CancellationTokenSource(timeout ?? TimeSpan.FromSeconds(60));

        // Synchronize access to finalAssistantMessage between the subscription callback
        // (events from the CLI read loop) and CheckExistingMessages (RPC reply thread).
        var stateLock = new object();
        AssistantMessageEvent? finalAssistantMessage = null;

        using var subscription = session.On(evt =>
        {
            switch (evt)
            {
                case AssistantMessageEvent msg:
                    lock (stateLock) { finalAssistantMessage = msg; }
                    break;
                case SessionIdleEvent:
                    AssistantMessageEvent? snapshot;
                    lock (stateLock) { snapshot = finalAssistantMessage; }
                    if (snapshot != null) tcs.TrySetResult(snapshot);
                    break;
                case SessionErrorEvent error:
                    tcs.TrySetException(new Exception(error.Data.Message ?? "session error"));
                    break;
            }
        });

        // Backfill from already-delivered messages so we don't lose events that arrived
        // between SendAsync returning and the subscription being installed.
        CheckExistingMessages();

        cts.Token.Register(() => tcs.TrySetException(new TimeoutException("Timeout waiting for assistant message")));

        return await tcs.Task;

        async void CheckExistingMessages()
        {
            try
            {
                var (existingFinal, existingIdle) = await GetExistingMessagesAsync(session, alreadyIdle);
                if (existingFinal != null)
                {
                    lock (stateLock)
                    {
                        // Preserve a newer message captured by the subscription in the meantime.
                        if (finalAssistantMessage == null) finalAssistantMessage = existingFinal;
                    }
                }

                // If the turn already finished before we subscribed, complete now.
                if (existingIdle && existingFinal != null)
                {
                    tcs.TrySetResult(existingFinal);
                }
            }
            catch (Exception ex)
            {
                tcs.TrySetException(ex);
            }
        }
    }

    private static async Task<(AssistantMessageEvent? Final, bool SawIdle)> GetExistingMessagesAsync(CopilotSession session, bool alreadyIdle)
    {
        var messages = (await session.GetMessagesAsync()).ToList();

        var lastUserIdx = messages.FindLastIndex(m => m is UserMessageEvent);
        var currentTurn = lastUserIdx < 0 ? messages : messages.Skip(lastUserIdx).ToList();

        var error = currentTurn.OfType<SessionErrorEvent>().FirstOrDefault();
        if (error != null) throw new Exception(error.Data.Message ?? "session error");

        var idleIdx = alreadyIdle ? currentTurn.Count : currentTurn.FindIndex(m => m is SessionIdleEvent);
        var sawIdle = alreadyIdle || idleIdx >= 0;

        // Find the most recent assistant message in the turn (whether idle has arrived or not).
        var searchEnd = idleIdx >= 0 ? idleIdx : currentTurn.Count;
        for (var i = searchEnd - 1; i >= 0; i--)
        {
            if (currentTurn[i] is AssistantMessageEvent msg)
                return (msg, sawIdle);
        }

        return (null, sawIdle);
    }

    public static async Task<T> GetNextEventOfTypeAsync<T>(
        CopilotSession session,
        TimeSpan? timeout = null) where T : SessionEvent
    {
        var tcs = new TaskCompletionSource<T>(TaskCreationOptions.RunContinuationsAsynchronously);
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
