/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.idle
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionIdleEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionIdleData data;

    @Override
    public String getType() {
        return "session.idle";
    }

    public SessionIdleData getData() {
        return data;
    }

    public void setData(SessionIdleData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionIdleData {
        // Empty data
    }
}
