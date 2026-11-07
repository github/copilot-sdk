/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;

namespace GitHub.Copilot.Test.Unit;

// Exercises .NET 8's ConfigureAwaitOptions on net8.0 and the SDK's downlevel polyfill, which the test project compiles
// in, on net472.
public class ConfigureAwaitOptionsTests
{
    private static readonly TimeSpan WaitTimeout = TimeSpan.FromSeconds(5);

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task ForceYielding_Queues_Completed_Await_To_Thread_Pool_Unless_Continuing_On_Captured_Context(
        bool continueOnCapturedContext)
    {
        var options = ConfigureAwaitOptions.ForceYielding |
            (continueOnCapturedContext ? ConfigureAwaitOptions.ContinueOnCapturedContext : ConfigureAwaitOptions.None);
        var context = new RecordingSynchronizationContext();
        var scheduler = new RecordingTaskScheduler();
        using var callerReturned = new ManualResetEventSlim();

        // Await under both a custom TaskScheduler and a custom SynchronizationContext, which Task.Yield would honor.
        var resumption = await Task.Factory.StartNew(
            () =>
            {
                var previous = SynchronizationContext.Current;
                SynchronizationContext.SetSynchronizationContext(context);
                try
                {
                    return ResumeAfterForcedYieldAsync(options, callerReturned);
                }
                finally
                {
                    SynchronizationContext.SetSynchronizationContext(previous);
                    callerReturned.Set();
                }
            },
            CancellationToken.None,
            TaskCreationOptions.None,
            scheduler).Unwrap().WaitAsync(WaitTimeout);

        Assert.True(resumption.AfterCallerReturned, "The await of a completed task continued synchronously.");
        Assert.Same(continueOnCapturedContext ? context : null, resumption.Context);
        Assert.Equal(continueOnCapturedContext ? 1 : 0, context.Posts);
        // Only the task that ran the caller.
        Assert.Equal(1, scheduler.QueuedTasks);
        Assert.Same(TaskScheduler.Default, resumption.Scheduler);
        Assert.True(resumption.OnThreadPoolThread);
    }

    private static async Task<(bool AfterCallerReturned, SynchronizationContext? Context, TaskScheduler Scheduler, bool OnThreadPoolThread)>
        ResumeAfterForcedYieldAsync(ConfigureAwaitOptions options, ManualResetEventSlim callerReturned)
    {
        await Task.CompletedTask.ConfigureAwait(options);
        return (
            callerReturned.Wait(WaitTimeout),
            SynchronizationContext.Current,
            TaskScheduler.Current,
            Thread.CurrentThread.IsThreadPoolThread);
    }

    private sealed class RecordingSynchronizationContext : SynchronizationContext
    {
        private int _posts;

        public int Posts => Volatile.Read(ref _posts);

        public override void Post(SendOrPostCallback d, object? state)
        {
            Interlocked.Increment(ref _posts);
            ThreadPool.QueueUserWorkItem(_ =>
            {
                SetSynchronizationContext(this);
                try
                {
                    d(state);
                }
                finally
                {
                    SetSynchronizationContext(null);
                }
            });
        }
    }

    private sealed class RecordingTaskScheduler : TaskScheduler
    {
        private int _queuedTasks;

        public int QueuedTasks => Volatile.Read(ref _queuedTasks);

        protected override void QueueTask(Task task)
        {
            Interlocked.Increment(ref _queuedTasks);
            ThreadPool.QueueUserWorkItem(_ => TryExecuteTask(task));
        }

        protected override bool TryExecuteTaskInline(Task task, bool taskWasPreviouslyQueued) => false;

        protected override IEnumerable<Task> GetScheduledTasks() => Array.Empty<Task>();
    }
}
