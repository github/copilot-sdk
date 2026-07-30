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
        (request, invocation) => invocation.ManagedSettingsEnabled
            ? Task.FromException<PermissionDecision>(
                new InvalidOperationException("ApproveAll cannot be used when managed settings are enabled"))
            : RequiresManagedApproval(request)
                ? Task.FromResult(PermissionDecision.NoResult())
                : Task.FromResult(PermissionDecision.ApproveOnce());

    private static bool RequiresManagedApproval(PermissionRequest request) => request switch
    {
        PermissionRequestShell shell => shell.ManagedApprovalRequired is true,
        PermissionRequestWrite write => write.ManagedApprovalRequired is true,
        PermissionRequestRead read => read.ManagedApprovalRequired is true,
        PermissionRequestUrl url => url.ManagedApprovalRequired is true,
        _ => false,
    };
}
