/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Runtime.CompilerServices;

namespace System.Threading.Tasks;

/// <summary>Options to control behavior when awaiting, matching .NET 8's <c>ConfigureAwaitOptions</c>.</summary>
[Flags]
internal enum ConfigureAwaitOptions
{
    None = 0x0,
    ContinueOnCapturedContext = 0x1,
    SuppressThrowing = 0x2,
    ForceYielding = 0x4,
}

/// <summary>
/// The awaitable returned by the downlevel <c>Task.ConfigureAwait(ConfigureAwaitOptions)</c>, standing in for .NET 8's
/// <see cref="ConfiguredTaskAwaitable"/>, which has no public constructor.
/// </summary>
internal readonly struct ConfiguredTaskAwaitableWithOptions
{
    private readonly Awaiter _awaiter;

    internal ConfiguredTaskAwaitableWithOptions(Task task, ConfigureAwaitOptions options) => _awaiter = new(task, options);

    public Awaiter GetAwaiter() => _awaiter;

    internal readonly struct Awaiter : ICriticalNotifyCompletion
    {
        private readonly Task _task;
        private readonly ConfigureAwaitOptions _options;

        internal Awaiter(Task task, ConfigureAwaitOptions options)
        {
            _task = task;
            _options = options;
        }

        // As in .NET 8, ForceYielding just reports a completed task as incomplete so that the await registers a
        // continuation. The BCL awaiter queues a continuation registered on a completed task rather than running it
        // inline: to the captured SynchronizationContext or TaskScheduler with ContinueOnCapturedContext, otherwise to
        // the thread pool.
        public bool IsCompleted => (_options & ConfigureAwaitOptions.ForceYielding) == 0 && _task.IsCompleted;

        public void OnCompleted(Action continuation) =>
            _task.ConfigureAwait(ContinueOnCapturedContext).GetAwaiter().OnCompleted(continuation);

        public void UnsafeOnCompleted(Action continuation) =>
            _task.ConfigureAwait(ContinueOnCapturedContext).GetAwaiter().UnsafeOnCompleted(continuation);

        public void GetResult()
        {
            if ((_options & ConfigureAwaitOptions.SuppressThrowing) == 0)
            {
                _task.GetAwaiter().GetResult();
            }
        }

        private bool ContinueOnCapturedContext => (_options & ConfigureAwaitOptions.ContinueOnCapturedContext) != 0;
    }
}
