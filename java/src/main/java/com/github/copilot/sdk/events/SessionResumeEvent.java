/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Event: session.resume
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
    public static class SessionResumeData {

        @JsonProperty("resumeTime")
        private OffsetDateTime resumeTime;

        @JsonProperty("eventCount")
        private double eventCount;

        public OffsetDateTime getResumeTime() {
            return resumeTime;
        }

        public void setResumeTime(OffsetDateTime resumeTime) {
            this.resumeTime = resumeTime;
        }

        public double getEventCount() {
            return eventCount;
        }

        public void setEventCount(double eventCount) {
            this.eventCount = eventCount;
        }
    }
}
