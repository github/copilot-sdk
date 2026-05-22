/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot;

/// <summary>Provides pre-built permission request handlers.</summary>
public static class PermissionHandler
{
    /// <summary>A permission handler that approves all permission requests.</summary>
    public static Func<PermissionRequest, PermissionInvocation, Task<PermissionRequestResult>> ApproveAll { get; } =
        (_, _) => Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved });
}
