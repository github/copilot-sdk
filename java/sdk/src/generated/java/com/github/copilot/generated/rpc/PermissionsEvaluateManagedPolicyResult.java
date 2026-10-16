/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The policy questions a host asks before granting elevated permissions.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record PermissionsEvaluateManagedPolicyResult(
    /** True when managed policy forbids bypass-permissions mode, so allow-all style flags must not take effect. */
    @JsonProperty("bypassPermissionsDisabled") Boolean bypassPermissionsDisabled,
    /** True when a managed policy exists but this build could not determine its meaning. Hosts fail closed on it rather than assuming permission was granted. */
    @JsonProperty("policyUndetermined") Boolean policyUndetermined,
    /** True when the managed layer supplies permission rules that the session must enforce. */
    @JsonProperty("permissionRulesActive") Boolean permissionRulesActive
) {
}
