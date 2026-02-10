/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Event: session.start
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionStartEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionStartData data;

    @Override
    public String getType() {
        return "session.start";
    }

    public SessionStartData getData() {
        return data;
    }

    public void setData(SessionStartData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionStartData(@JsonProperty("sessionId") String sessionId, @JsonProperty("version") double version,
            @JsonProperty("producer") String producer, @JsonProperty("copilotVersion") String copilotVersion,
            @JsonProperty("startTime") OffsetDateTime startTime, @JsonProperty("selectedModel") String selectedModel) {
    }
}
