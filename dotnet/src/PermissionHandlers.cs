/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot.SDK;

/// <summary>Provides pre-built <see cref="PermissionHandler"/> implementations.</summary>
public static class PermissionHandlers
{
    /// <summary>A <see cref="PermissionHandler"/> that approves all permission requests.</summary>
    public static PermissionHandler ApproveAll { get; } =
        (_, _) => Task.FromResult(new PermissionRequestResult { Kind = "approved" });
}
