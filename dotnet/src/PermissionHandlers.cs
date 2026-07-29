/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;

namespace GitHub.Copilot;

/// <summary>Provides pre-built permission request handlers.</summary>
public static class PermissionHandler
{
    /// <summary>
    /// A permission handler that approves ordinary requests and leaves managed
    /// requests pending for an explicit human decision.
    /// </summary>
    public static Func<PermissionRequest, PermissionInvocation, Task<PermissionDecision>> ApproveAll { get; } =
        (request, _) => Task.FromResult(
            request.ManagedApprovalRequired == true
                ? PermissionDecision.NoResult()
                : PermissionDecision.ApproveOnce());
}
