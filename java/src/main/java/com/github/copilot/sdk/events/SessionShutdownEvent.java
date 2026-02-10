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
    public record SessionShutdownData(@JsonProperty("shutdownType") ShutdownType shutdownType,
            @JsonProperty("errorReason") String errorReason,
            @JsonProperty("totalPremiumRequests") double totalPremiumRequests,
            @JsonProperty("totalApiDurationMs") double totalApiDurationMs,
            @JsonProperty("sessionStartTime") double sessionStartTime,
            @JsonProperty("codeChanges") CodeChanges codeChanges,
            @JsonProperty("modelMetrics") Map<String, Object> modelMetrics,
            @JsonProperty("currentModel") String currentModel) {
    }

    /**
     * Code changes made during the session.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CodeChanges(@JsonProperty("linesAdded") double linesAdded,
            @JsonProperty("linesRemoved") double linesRemoved,
            @JsonProperty("filesModified") List<String> filesModified) {
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
