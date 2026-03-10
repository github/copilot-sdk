/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: external_tool.completed
 * <p>
 * Broadcast when a pending tool call has been resolved by a client (protocol
 * v3).
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ExternalToolCompletedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private ExternalToolCompletedData data;

    @Override
    public String getType() {
        return "external_tool.completed";
    }

    public ExternalToolCompletedData getData() {
        return data;
    }

    public void setData(ExternalToolCompletedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExternalToolCompletedData(@JsonProperty("requestId") String requestId) {
    }
}
