/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.mode_changed
 *
 * @since 1.0.10
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionModeChangedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionModeChangedData data;

    @Override
    public String getType() {
        return "session.mode_changed";
    }

    public SessionModeChangedData getData() {
        return data;
    }

    public void setData(SessionModeChangedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionModeChangedData(@JsonProperty("previousMode") String previousMode,
            @JsonProperty("newMode") String newMode) {
    }
}
