/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json.Serialization;

namespace GitHub.Copilot;

public partial class PermissionRequest
{
    /// <summary>
    /// Gets or sets whether managed policy requires an explicit human decision.
    /// Automatic approval must be bypassed when this value is <see langword="true"/>.
    /// </summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("managedApprovalRequired")]
    public bool? ManagedApprovalRequired { get; set; }
}
