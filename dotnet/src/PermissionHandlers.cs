/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;

namespace GitHub.Copilot;

/// <summary>Provides pre-built permission request handlers.</summary>
public static class PermissionHandler
{
    /// <summary>A permission handler that approves all permission requests.</summary>
    public static Func<PermissionRequest, PermissionInvocation, Task<PermissionDecision>> ApproveAll { get; } =
        (_, _) => Task.FromResult<PermissionDecision>(PermissionDecision.ApproveOnce());
}
