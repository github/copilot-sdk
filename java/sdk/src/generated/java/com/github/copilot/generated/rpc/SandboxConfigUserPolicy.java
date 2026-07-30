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
 * User-managed sandbox policy fragment merged into the auto-discovered base policy.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxConfigUserPolicy(
    /** Filesystem rules to merge into the base policy. */
    @JsonProperty("filesystem") SandboxConfigUserPolicyFilesystem filesystem,
    /** Network rules to merge into the base policy. */
    @JsonProperty("network") SandboxConfigUserPolicyNetwork network,
    /** macOS seatbelt options to merge into the base policy. */
    @JsonProperty("seatbelt") SandboxConfigUserPolicySeatbelt seatbelt,
    /** Deprecated legacy location for `seatbelt`; read only when the top-level `seatbelt` is absent. */
    @JsonProperty("experimental") SandboxConfigUserPolicyExperimental experimental
) {
}
