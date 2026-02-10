/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: pending_messages.modified
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PendingMessagesModifiedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private PendingMessagesModifiedData data;

    @Override
    public String getType() {
        return "pending_messages.modified";
    }

    public PendingMessagesModifiedData getData() {
        return data;
    }

    public void setData(PendingMessagesModifiedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PendingMessagesModifiedData() {
    }
}
