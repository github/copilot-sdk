/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot.SDK;

/// <summary>
/// A disposable that invokes an action when disposed.
/// </summary>
internal sealed class ActionDisposable(Action action) : IDisposable
{
    private Action? _action = action;

    public void Dispose()
    {
        var action = Interlocked.Exchange(ref _action, null);
        action?.Invoke();
    }
}
