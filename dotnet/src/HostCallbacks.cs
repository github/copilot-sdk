/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot;

/// <summary>
/// Runs SDK host callbacks that the runtime requests over JSON-RPC.
/// </summary>
internal static class HostCallbacks
{
    /// <summary>
    /// Invokes <paramref name="callback"/> with the request's cancellation token. Callers must already have left the
    /// JSON-RPC reader, because host callbacks may block (e.g. on a prompt) before returning their task and the reader
    /// must still be able to deliver the runtime's cancellation of the request.
    /// </summary>
    /// <param name="callback">The SDK host callback.</param>
    /// <param name="request">The runtime's request.</param>
    /// <param name="callbackName">Names the callback in the error reported for callback-local cancellation.</param>
    /// <param name="cancellationToken">Cancelled when the runtime cancels the request or the connection closes.</param>
    /// <returns>The callback's result.</returns>
    internal static async Task<TResult> RunCancellableAsync<TRequest, TResult>(
        Func<TRequest, CancellationToken, Task<TResult>> callback,
        TRequest request,
        string callbackName,
        CancellationToken cancellationToken)
    {
        try
        {
            // A request the runtime cancelled before the callback started is not started.
            cancellationToken.ThrowIfCancellationRequested();
            var lookup = callback(request, cancellationToken);
            // Observe a failure of a callback that is no longer awaited because the request was cancelled
            // or the connection closed.
            _ = lookup.ContinueWith(
                static task => _ = task.Exception,
                CancellationToken.None,
                TaskContinuationOptions.OnlyOnFaulted | TaskContinuationOptions.ExecuteSynchronously,
                TaskScheduler.Default);
            // Stop waiting when the request is cancelled or the connection closes, even if the callback
            // ignores the token, so the request is retired like in the other SDKs.
            var result = await lookup.WaitAsync(Timeout.InfiniteTimeSpan, cancellationToken).ConfigureAwait(false);
            // A callback that finished before the wait began returns its result even if the request
            // ended meanwhile; the dispatcher would then send it, even on a closed connection.
            cancellationToken.ThrowIfCancellationRequested();
            return result;
        }
        catch (OperationCanceledException ex) when (!cancellationToken.IsCancellationRequested)
        {
            // The dispatcher only replies to cancellation of the request itself; surface
            // callback-local cancellation as an ordinary failure so the runtime is not left waiting.
            throw new InvalidOperationException($"{callbackName} was cancelled.", ex);
        }
    }
}
