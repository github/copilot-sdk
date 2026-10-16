/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Session event "permission.messageAuthorizationDegraded". Historical decode-only degradation marker from the retired extractor. Current runtimes ignore it for permission decisions.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
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
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionMessageAuthorizationDegradedEventData(
        /** The human turn that could not be represented safely. */
        @CopilotExperimental
        @JsonProperty("turnIndex") Long turnIndex
    ) {
    }
}
