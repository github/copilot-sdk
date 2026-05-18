/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "pending_messages.modified". Empty payload; the event signals that the pending message queue has changed
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PendingMessagesModifiedEvent extends SessionEvent {

    @Override
    public String getType() { return "pending_messages.modified"; }

    @JsonProperty("data")
    private PendingMessagesModifiedEventData data;

    public PendingMessagesModifiedEventData getData() { return data; }
    public void setData(PendingMessagesModifiedEventData data) { this.data = data; }

    /** Data payload for {@link PendingMessagesModifiedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PendingMessagesModifiedEventData() {
    }
}
