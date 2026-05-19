/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace System.Threading.Tasks;

internal sealed class TaskCompletionSource : TaskCompletionSource<object?>
{
    public TaskCompletionSource()
    {
    }

    public TaskCompletionSource(TaskCreationOptions creationOptions)
        : base(creationOptions)
    {
    }

    public new Task Task => base.Task;

    public void SetResult() => base.SetResult(null);

    public bool TrySetResult() => base.TrySetResult(null);
}
