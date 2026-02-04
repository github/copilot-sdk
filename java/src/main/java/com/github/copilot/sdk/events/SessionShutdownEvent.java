/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Event: session.shutdown
 * <p>
 * This event is emitted when a session is shutting down, either routinely or
 * due to an error. It contains metrics about the session's usage.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionShutdownEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionShutdownData data;

    @Override
    public String getType() {
        return "session.shutdown";
    }

    public SessionShutdownData getData() {
        return data;
    }

    public void setData(SessionShutdownData data) {
        this.data = data;
    }

    /**
     * Data for the session shutdown event.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionShutdownData {

        @JsonProperty("shutdownType")
        private ShutdownType shutdownType;

        @JsonProperty("errorReason")
        private String errorReason;

        @JsonProperty("totalPremiumRequests")
        private double totalPremiumRequests;

        @JsonProperty("totalApiDurationMs")
        private double totalApiDurationMs;

        @JsonProperty("sessionStartTime")
        private double sessionStartTime;

        @JsonProperty("codeChanges")
        private CodeChanges codeChanges;

        @JsonProperty("modelMetrics")
        private Map<String, Object> modelMetrics;

        @JsonProperty("currentModel")
        private String currentModel;

        public ShutdownType getShutdownType() {
            return shutdownType;
        }

        public void setShutdownType(ShutdownType shutdownType) {
            this.shutdownType = shutdownType;
        }

        public String getErrorReason() {
            return errorReason;
        }

        public void setErrorReason(String errorReason) {
            this.errorReason = errorReason;
        }

        public double getTotalPremiumRequests() {
            return totalPremiumRequests;
        }

        public void setTotalPremiumRequests(double totalPremiumRequests) {
            this.totalPremiumRequests = totalPremiumRequests;
        }

        public double getTotalApiDurationMs() {
            return totalApiDurationMs;
        }

        public void setTotalApiDurationMs(double totalApiDurationMs) {
            this.totalApiDurationMs = totalApiDurationMs;
        }

        public double getSessionStartTime() {
            return sessionStartTime;
        }

        public void setSessionStartTime(double sessionStartTime) {
            this.sessionStartTime = sessionStartTime;
        }

        public CodeChanges getCodeChanges() {
            return codeChanges;
        }

        public void setCodeChanges(CodeChanges codeChanges) {
            this.codeChanges = codeChanges;
        }

        public Map<String, Object> getModelMetrics() {
            return modelMetrics;
        }

        public void setModelMetrics(Map<String, Object> modelMetrics) {
            this.modelMetrics = modelMetrics;
        }

        public String getCurrentModel() {
            return currentModel;
        }

        public void setCurrentModel(String currentModel) {
            this.currentModel = currentModel;
        }
    }

    /**
     * Code changes made during the session.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CodeChanges {

        @JsonProperty("linesAdded")
        private double linesAdded;

        @JsonProperty("linesRemoved")
        private double linesRemoved;

        @JsonProperty("filesModified")
        private List<String> filesModified;

        public double getLinesAdded() {
            return linesAdded;
        }

        public void setLinesAdded(double linesAdded) {
            this.linesAdded = linesAdded;
        }

        public double getLinesRemoved() {
            return linesRemoved;
        }

        public void setLinesRemoved(double linesRemoved) {
            this.linesRemoved = linesRemoved;
        }

        public List<String> getFilesModified() {
            return filesModified;
        }

        public void setFilesModified(List<String> filesModified) {
            this.filesModified = filesModified;
        }
    }

    /**
     * Type of session shutdown.
     */
    public enum ShutdownType {
        @JsonProperty("routine")
        ROUTINE, @JsonProperty("error")
        ERROR
    }
}
