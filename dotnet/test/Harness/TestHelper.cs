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

    public static async Task<AssistantMessageEvent> SendAndGetFinalAssistantMessageAsync(
        CopilotSession session,
        MessageOptions options,
        TimeSpan? timeout = null)
    {
        // Subscribe before sending: session.idle is ephemeral and cannot be backfilled.
        return await session.SendAndWaitAsync(options, timeout ?? DefaultEventTimeout)
            ?? throw new InvalidOperationException("Session became idle without an assistant message.");
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
