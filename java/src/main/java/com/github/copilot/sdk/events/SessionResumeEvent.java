/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Event: session.resume
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionResumeEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionResumeData data;

    @Override
    public String getType() {
        return "session.resume";
    }

    public SessionResumeData getData() {
        return data;
    }

    public void setData(SessionResumeData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionResumeData(@JsonProperty("resumeTime") OffsetDateTime resumeTime,
            @JsonProperty("eventCount") double eventCount) {
    }
}
