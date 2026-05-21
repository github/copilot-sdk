/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// Polyfills for Task APIs not available on .NET Framework.
// These are test-only and not optimized for production use.

#if !NET8_0_OR_GREATER

using System.Threading;

namespace System.Threading.Tasks;

internal static class TestDownlevelTaskExtensions
{
    extension(Task task)
    {
        public Task WaitAsync(TimeSpan timeout)
        {
            if (task.IsCompleted)
            {
                return task;
            }

            return WaitAsyncCore(task, timeout);
        }
    }

    extension<T>(Task<T> task)
    {
        public Task<T> WaitAsync(TimeSpan timeout)
        {
            if (task.IsCompleted)
            {
                return task;
            }

            return WaitAsyncCoreGeneric(task, timeout);
        }
    }

    private static async Task WaitAsyncCore(Task task, TimeSpan timeout)
    {
        using var cts = new CancellationTokenSource();
        var delayTask = Task.Delay(timeout, cts.Token);
        var completedTask = await Task.WhenAny(task, delayTask).ConfigureAwait(false);
        if (completedTask == task)
        {
            cts.Cancel();
            await task.ConfigureAwait(false);
        }
        else
        {
            throw new TimeoutException();
        }
    }

    private static async Task<T> WaitAsyncCoreGeneric<T>(Task<T> task, TimeSpan timeout)
    {
        using var cts = new CancellationTokenSource();
        var delayTask = Task.Delay(timeout, cts.Token);
        var completedTask = await Task.WhenAny(task, delayTask).ConfigureAwait(false);
        if (completedTask == task)
        {
            cts.Cancel();
            return await task.ConfigureAwait(false);
        }

        throw new TimeoutException();
    }
}

#endif
