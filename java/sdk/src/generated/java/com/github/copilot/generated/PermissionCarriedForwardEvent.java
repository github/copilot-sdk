/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "permission.carriedForward". Historical decode-only receipt from the retired Assisted Permissions authorization extractor. Current runtimes ignore it for permission decisions.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionCarriedForwardEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.carriedForward"; }

    @JsonProperty("data")
    private PermissionCarriedForwardEventData data;

    public PermissionCarriedForwardEventData getData() { return data; }
    public void setData(PermissionCarriedForwardEventData data) { this.data = data; }

    /** Data payload for {@link PermissionCarriedForwardEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionCarriedForwardEventData(
        /** Authorization edge minted for this admission. Not a prompt id: no prompt was raised, so no client should expect a request with this id. */
        @JsonProperty("requestId") String requestId,
        /** Tool call this admission authorizes. Its execution receipts the prior grant, which is how a single-effect approval is spent rather than carried forward again. */
        @JsonProperty("toolCallId") String toolCallId,
        /** Identity of the prior authorization record that contained the proposal. */
        @JsonProperty("recordId") String recordId,
        /** Always `authorization_carry_forward`. Stated explicitly so a consumer reading this event cannot mistake it for a human, host-policy, or assisted-approval decision. */
        @JsonProperty("decisionSource") PermissionDecisionSource decisionSource
    ) {
    }
}
