/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot.Test.Harness;

public static class TestHelper
{
    // Default tolerates CLI / replay-proxy cold start on Windows GitHub Actions
    // runners, where the first test in a fixture can take ~60s before the first
    // assistant message arrives. Subsequent tests in the same fixture typically
    // complete in well under a second.
    private static readonly TimeSpan DefaultEventTimeout = TimeSpan.FromSeconds(120);
    private static readonly TimeSpan DefaultPollInterval = TimeSpan.FromMilliseconds(100);

    public static async Task<AssistantMessageEvent?> GetFinalAssistantMessageAsync(
        CopilotSession session,
        TimeSpan? timeout = null,
        bool alreadyIdle = false)
    {
        var tcs = new TaskCompletionSource<AssistantMessageEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var cts = new CancellationTokenSource(timeout ?? DefaultEventTimeout);

        // Both `finalAssistantMessage` and `sawIdle` are set from two threads — the
        // subscription callback (CLI read loop) and CheckExistingMessagesAsync (RPC reply).
        // We complete only once we've observed both, regardless of which path saw which.
        var stateLock = new object();
        AssistantMessageEvent? finalAssistantMessage = null;
        bool sawIdle = false;

        void TryComplete()
        {
            AssistantMessageEvent? snapshot;
            bool idle;
            lock (stateLock)
            {
                snapshot = finalAssistantMessage;
                idle = sawIdle;
            }
            if (snapshot != null && idle) tcs.TrySetResult(snapshot);
        }

        using var subscription = session.On<SessionEvent>(evt =>
        {
            switch (evt)
            {
                case AssistantMessageEvent msg:
                    lock (stateLock) { finalAssistantMessage = msg; }
                    TryComplete();
                    break;
                case SessionIdleEvent:
                    lock (stateLock) { sawIdle = true; }
                    TryComplete();
                    break;
                case SessionErrorEvent error:
                    tcs.TrySetException(new Exception(error.Data.Message ?? "session error"));
                    break;
            }
        });

        // Backfill from already-delivered messages so we don't lose events that arrived
        // between SendAsync returning and the subscription being installed. Run it
        // concurrently with the live subscription, but keep the Task observable so any
        // exception is propagated through tcs (not the unobserved-task handler) and so
        // we can drain it deterministically below. Pass cts.Token so the backfill is
        // bounded by the same timeout as the wait itself, and so a hung GetEventsAsync
        // can't block the drain in `finally`.
        var backfill = CheckExistingMessagesAsync(cts.Token);

        using var registration = cts.Token.Register(
            static state => ((TaskCompletionSource<AssistantMessageEvent>)state!).TrySetException(
                new TimeoutException("Timeout waiting for assistant message")),
            tcs);

        try
        {
            return await tcs.Task;
        }
        finally
        {
            // Drain the backfill before our `using` scopes (cts, subscription) dispose.
            // Any exception was already routed through tcs above, so swallow here.
            try { await backfill.ConfigureAwait(false); }
            catch (Exception) { /* intentionally ignored: already propagated via tcs */ }
        }

        async Task CheckExistingMessagesAsync(CancellationToken cancellationToken)
        {
            try
            {
                var (existingFinal, existingIdle) = await GetExistingMessagesAsync(session, alreadyIdle, cancellationToken);
                lock (stateLock)
                {
                    // Preserve a newer message captured by the subscription in the meantime.
                    if (existingFinal != null && finalAssistantMessage == null)
                    {
                        finalAssistantMessage = existingFinal;
                    }
                    if (existingIdle) sawIdle = true;
                }
                TryComplete();
            }
            catch (Exception ex)
            {
                tcs.TrySetException(ex);
            }
        }
    }

    private static async Task<(AssistantMessageEvent? Final, bool SawIdle)> GetExistingMessagesAsync(CopilotSession session, bool alreadyIdle, CancellationToken cancellationToken = default)
    {
        var messages = (await session.GetEventsAsync(cancellationToken)).ToList();

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
        => await GetNextEventOfTypeAsync<T>(session, static _ => true, timeout);

    public static async Task<T> GetNextEventOfTypeAsync<T>(
        CopilotSession session,
        Func<T, bool> predicate,
        TimeSpan? timeout = null,
        string? timeoutDescription = null) where T : SessionEvent
    {
        var tcs = new TaskCompletionSource<T>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var cts = new CancellationTokenSource(timeout ?? DefaultEventTimeout);

        using var subscription = session.On<SessionEvent>(evt =>
        {
            if (evt is T matched && predicate(matched))
            {
                tcs.TrySetResult(matched);
            }
            else if (evt is SessionErrorEvent error)
            {
                tcs.TrySetException(new Exception(error.Data.Message ?? "session error"));
            }
        });

        cts.Token.Register(() => tcs.TrySetException(
            new TimeoutException($"Timeout waiting for {timeoutDescription ?? $"event of type '{typeof(T).Name}'"}")));

        return await tcs.Task;
    }

    public static Task WaitForConditionAsync(
        Func<bool> condition,
        TimeSpan? timeout = null,
        string? timeoutMessage = null,
        TimeSpan? pollInterval = null)
        => WaitForConditionAsync(
            () => Task.FromResult(condition()),
            timeout,
            timeoutMessage,
            transientExceptionFilter: null,
            pollInterval);

    public static async Task WaitForConditionAsync(
        Func<Task<bool>> condition,
        TimeSpan? timeout = null,
        string? timeoutMessage = null,
        Func<Exception, bool>? transientExceptionFilter = null,
        TimeSpan? pollInterval = null)
    {
        using var cts = new CancellationTokenSource(timeout ?? DefaultEventTimeout);
        Exception? lastTransientException = null;

        while (true)
        {
            try
            {
                if (await condition())
                {
                    return;
                }

                lastTransientException = null;
            }
            catch (Exception ex) when (transientExceptionFilter?.Invoke(ex) == true)
            {
                lastTransientException = ex;
            }

            try
            {
                await Task.Delay(pollInterval ?? DefaultPollInterval, cts.Token);
            }
            catch (OperationCanceledException) when (cts.IsCancellationRequested)
            {
                break;
            }
        }

        try
        {
            if (await condition())
            {
                return;
            }
        }
        catch (Exception ex) when (transientExceptionFilter?.Invoke(ex) == true)
        {
            lastTransientException = ex;
        }

        throw lastTransientException is null
            ? new TimeoutException(timeoutMessage ?? "Timed out waiting for condition.")
            : new TimeoutException(timeoutMessage ?? "Timed out waiting for condition.", lastTransientException);
    }

    public static bool IsTransientFileSystemException(Exception exception)
        => exception is IOException or UnauthorizedAccessException;
}
