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

    private static bool RequiresManagedApproval(PermissionRequest request)
    {
        if (request.ManagedApprovalRequired is true)
        {
            return true;
        }

        return request.GetType() == typeof(PermissionRequest)
            && request.Kind is not ("shell"
                or "write"
                or "read"
                or "mcp"
                or "url"
                or "memory"
                or "custom-tool"
                or "hook"
                or "extension-management"
                or "extension-permission-access");
    }
}
