/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot.SDK;

/// <summary>Provides pre-built <see cref="PermissionRequestHandler"/> implementations.</summary>
public static class PermissionHandler
{
    /// <summary>A <see cref="PermissionRequestHandler"/> that approves all permission requests.</summary>
    public static PermissionRequestHandler ApproveAll { get; } =
        (_, _) => Task.FromResult(new PermissionRequestResult { Kind = "approved" });
}
