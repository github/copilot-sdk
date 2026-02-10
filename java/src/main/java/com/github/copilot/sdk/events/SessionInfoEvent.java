/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.info
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionInfoEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionInfoData data;

    @Override
    public String getType() {
        return "session.info";
    }

    public SessionInfoData getData() {
        return data;
    }

    public void setData(SessionInfoData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionInfoData(@JsonProperty("infoType") String infoType, @JsonProperty("message") String message) {
    }
}
