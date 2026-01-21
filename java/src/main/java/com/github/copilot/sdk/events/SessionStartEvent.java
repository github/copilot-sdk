/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Event: session.start
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
    public static class SessionStartData {

        @JsonProperty("sessionId")
        private String sessionId;

        @JsonProperty("version")
        private double version;

        @JsonProperty("producer")
        private String producer;

        @JsonProperty("copilotVersion")
        private String copilotVersion;

        @JsonProperty("startTime")
        private OffsetDateTime startTime;

        @JsonProperty("selectedModel")
        private String selectedModel;

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public double getVersion() {
            return version;
        }

        public void setVersion(double version) {
            this.version = version;
        }

        public String getProducer() {
            return producer;
        }

        public void setProducer(String producer) {
            this.producer = producer;
        }

        public String getCopilotVersion() {
            return copilotVersion;
        }

        public void setCopilotVersion(String copilotVersion) {
            this.copilotVersion = copilotVersion;
        }

        public OffsetDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(OffsetDateTime startTime) {
            this.startTime = startTime;
        }

        public String getSelectedModel() {
            return selectedModel;
        }

        public void setSelectedModel(String selectedModel) {
            this.selectedModel = selectedModel;
        }
    }
}
