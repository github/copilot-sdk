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
 * Session event "permission.messageAuthorizationRead". Records that one human turn has been read by the blinded authorization proposer, whether or not it minted anything, so a resumed session does not re-run the extraction model on a turn the live session already read. Persisted purely to avoid wasted model calls across resume; it is never a correctness mechanism.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionMessageAuthorizationReadEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.messageAuthorizationRead"; }

    @JsonProperty("data")
    private PermissionMessageAuthorizationReadEventData data;

    public PermissionMessageAuthorizationReadEventData getData() { return data; }
    public void setData(PermissionMessageAuthorizationReadEventData data) { this.data = data; }

    /** Data payload for {@link PermissionMessageAuthorizationReadEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionMessageAuthorizationReadEventData(
        /** The human turn that was read by the proposer. */
        @JsonProperty("turnIndex") Long turnIndex
    ) {
    }
}
