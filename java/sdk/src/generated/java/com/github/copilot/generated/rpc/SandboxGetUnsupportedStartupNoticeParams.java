/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * The managed sandbox policy in force, which selects which remedy the notice offers.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SandboxGetUnsupportedStartupNoticeParams(
    /** The managed `sandbox` slice, when a policy supplies one. The notice treats the sandbox as enforced only when this both enables it and sets `failIfUnavailable`, because a managed sandbox that tolerates being unavailable still leaves the user able to turn it off. */
    @JsonProperty("managedSandbox") Object managedSandbox,
    /** True when the sandbox stayed on only because the managed policy could not be determined. Distinguishes a policy that positively enforces the sandbox from one that merely failed to resolve, which is the difference between telling the user to contact an administrator and telling them to wait. Defaults to false. */
    @JsonProperty("enabledByUndeterminedPolicy") Boolean enabledByUndeterminedPolicy
) {
}
