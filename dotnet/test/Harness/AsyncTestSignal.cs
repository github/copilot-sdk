/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot.Test.Harness;

internal sealed class AsyncTestSignal
{
    private readonly object _gate = new();
    private TaskCompletionSource _changed = new(TaskCreationOptions.RunContinuationsAsynchronously);

    public void Pulse()
    {
        lock (_gate)
        {
            var changed = _changed;
            _changed = new(TaskCreationOptions.RunContinuationsAsynchronously);
            changed.TrySetResult();
        }
    }

    public async Task WaitForAsync(
        Func<bool> condition,
        TimeSpan timeout,
        string? timeoutMessage = null,
        CancellationToken cancellationToken = default)
    {
        using var deadline = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        deadline.CancelAfter(timeout);
        while (true)
        {
            Task changed;
            lock (_gate)
            {
                changed = _changed.Task;
            }

            // Capture the notification before inspecting state so a concurrent update cannot be lost.
            if (condition())
            {
                return;
            }

            try
            {
                deadline.Token.ThrowIfCancellationRequested();
                await changed.WaitAsync(Timeout.InfiniteTimeSpan, deadline.Token);
            }
            catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
            {
                throw new TimeoutException(timeoutMessage ?? "Timed out waiting for the signalled test condition.");
            }
        }
    }
}
