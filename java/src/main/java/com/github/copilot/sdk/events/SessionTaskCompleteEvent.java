/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.task_complete
 *
 * @since 1.0.11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionTaskCompleteEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionTaskCompleteData data;

    @Override
    public String getType() {
        return "session.task_complete";
    }

    public SessionTaskCompleteData getData() {
        return data;
    }

    public void setData(SessionTaskCompleteData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionTaskCompleteData(@JsonProperty("summary") String summary) {
    }
}
