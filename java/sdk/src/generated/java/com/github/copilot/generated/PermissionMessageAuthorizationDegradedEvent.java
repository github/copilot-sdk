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
 * Session event "permission.messageAuthorizationDegraded". Historical decode-only degradation marker from the retired extractor. Current runtimes ignore it for permission decisions.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionMessageAuthorizationDegradedEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.messageAuthorizationDegraded"; }

    @JsonProperty("data")
    private PermissionMessageAuthorizationDegradedEventData data;

    public PermissionMessageAuthorizationDegradedEventData getData() { return data; }
    public void setData(PermissionMessageAuthorizationDegradedEventData data) { this.data = data; }

    /** Data payload for {@link PermissionMessageAuthorizationDegradedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionMessageAuthorizationDegradedEventData(
        /** The human turn that could not be represented safely. */
        @JsonProperty("turnIndex") Long turnIndex
    ) {
    }
}
