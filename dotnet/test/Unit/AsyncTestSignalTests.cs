/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class AsyncTestSignalTests
{
    [Fact]
    public async Task Accepts_State_That_Arrived_Before_Waiting()
    {
        var signal = new AsyncTestSignal();
        signal.Pulse();
        await signal.WaitForAsync(() => true, TimeSpan.FromSeconds(1));
    }

    [Fact]
    public async Task Does_Not_Lose_Update_During_Condition_Check()
    {
        var signal = new AsyncTestSignal();
        var checks = 0;
        await signal.WaitForAsync(() =>
        {
            if (++checks == 1)
            {
                signal.Pulse();
                return false;
            }
            return true;
        }, TimeSpan.FromSeconds(1));
        Assert.Equal(2, checks);
    }

    [Fact]
    public async Task Wakes_All_Waiters_And_Rechecks_After_Unrelated_Update()
    {
        var signal = new AsyncTestSignal();
        var ready = false;
        var checkedAgain = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var checks = 0;
        var gate = new object();
        bool Condition()
        {
            lock (gate)
            {
                if (++checks >= 3)
                {
                    checkedAgain.TrySetResult();
                }
                return ready;
            }
        }

        var first = signal.WaitForAsync(Condition, TimeSpan.FromSeconds(1));
        var second = signal.WaitForAsync(Condition, TimeSpan.FromSeconds(1));
        signal.Pulse();
        await checkedAgain.Task.WaitAsync(TimeSpan.FromSeconds(1));
        Assert.False(first.IsCompleted);
        Assert.False(second.IsCompleted);
        lock (gate)
        {
            ready = true;
        }
        signal.Pulse();
        await Task.WhenAll(first, second);
    }

    [Fact]
    public async Task Propagates_Predicate_Error_And_Cancellation_And_Bounds_Wait()
    {
        var signal = new AsyncTestSignal();
        var error = new InvalidOperationException("predicate failed");
        Assert.Same(error, await Assert.ThrowsAsync<InvalidOperationException>(
            () => signal.WaitForAsync(() => throw error, TimeSpan.FromSeconds(1))));
        using var cancellation = new CancellationTokenSource();
        var pending = signal.WaitForAsync(() => false, TimeSpan.FromSeconds(1), cancellationToken: cancellation.Token);
        cancellation.Cancel();
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() => pending);
        await Assert.ThrowsAsync<TimeoutException>(
            () => signal.WaitForAsync(() => false, TimeSpan.Zero));
    }
}
