/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json.Serialization;

namespace GitHub.Copilot.Rpc;

/// <summary>
/// SDK-only <see cref="PermissionDecision"/> value indicating the handler
/// declines to respond to this permission request. The SDK then suppresses
/// the response so another connected client can answer instead.
/// </summary>
public sealed class PermissionDecisionNoResult : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "no-result";
}

/// <summary>
/// Static factories for the common <see cref="PermissionDecision"/> variants
/// returned by <c>OnPermissionRequest</c> handlers. Use these for quick
/// discoverability via <c>PermissionDecision.&lt;dot&gt;</c>. For richer
/// decisions (per-session, per-location, permanent) that need an
/// <c>Approval</c> payload, instantiate the variant class directly.
/// </summary>
[JsonDerivedType(typeof(PermissionDecisionNoResult), "no-result")]
public partial class PermissionDecision
{
    /// <summary>Approve this single request.</summary>
    public static PermissionDecision ApproveOnce() => new PermissionDecisionApproveOnce();

    /// <summary>Reject the request, optionally forwarding feedback to the LLM.</summary>
    public static PermissionDecision Reject(string? feedback = null) =>
        new PermissionDecisionReject { Feedback = feedback };

    /// <summary>Deny the request because no user is available to confirm it.</summary>
    public static PermissionDecision UserNotAvailable() => new PermissionDecisionUserNotAvailable();

    /// <summary>
    /// Decline to respond to this permission request, allowing another
    /// connected client to answer instead.
    /// </summary>
    public static PermissionDecision NoResult() => new PermissionDecisionNoResult();
}
