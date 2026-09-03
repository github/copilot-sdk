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
 * Immediate acknowledgement and Auto preference snapshot after a switch request. This result never implies that a pending preference committed.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelSwitchAutoTierResult(
    /** Immediate request status. `pending` means accepted but not committed. */
    @JsonProperty("status") ModelSwitchAutoTierStatus status,
    /** Auto preference currently committed for the session. */
    @JsonProperty("effectiveAutoTier") AutoTier effectiveAutoTier,
    /** Latest unclaimed Auto preference waiting for a future user turn. */
    @JsonProperty("pendingAutoTier") AutoTier pendingAutoTier,
    /** Auto preference currently claimed by an in-progress activation. Null means the activation is returning to provider-default routing. */
    @JsonProperty("activatingAutoTier") AutoTier activatingAutoTier,
    /** Earlier unclaimed preference replaced by this request. This can be present with either status, including when selecting the effective preference cancels pending work. */
    @JsonProperty("supersededAutoTier") AutoTier supersededAutoTier
) {
}
