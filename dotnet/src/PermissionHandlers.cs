/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;

namespace GitHub.Copilot;

/// <summary>Provides pre-built permission request handlers.</summary>
public static class PermissionHandler
{
    /// <summary>
    /// A permission handler that approves requests when managed settings are disabled.
    /// </summary>
    public static Func<PermissionRequest, PermissionInvocation, Task<PermissionDecision>> ApproveAll { get; } =
        (_, invocation) => invocation.ManagedSettingsEnabled
            ? Task.FromException<PermissionDecision>(
                new InvalidOperationException("ApproveAll cannot be used when managed settings are enabled"))
            : Task.FromResult(PermissionDecision.ApproveOnce());
}
