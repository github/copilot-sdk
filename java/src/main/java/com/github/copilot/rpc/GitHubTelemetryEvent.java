/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.github.copilot.CopilotExperimental;

/**
 * A single telemetry event in the runtime's native GitHub-shaped telemetry
 * format, forwarded verbatim to opted-in hosts.
 *
 * <p>
 * Internal/experimental: this type is part of the GitHub telemetry forwarding
 * surface and may change or be removed without notice.
 *
 * @since 1.0.0
 */
@CopilotExperimental
public class GitHubTelemetryEvent {

    @JsonProperty("client")
    private GitHubTelemetryClientInfo client;

    @JsonProperty("copilot_tracking_id")
    private String copilotTrackingId;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("exp_assignment_context")
    private String expAssignmentContext;

    @JsonProperty("features")
    private Map<String, String> features;

    @JsonProperty("kind")
    private String kind = "";

    @JsonProperty("metrics")
    private Map<String, Double> metrics = Collections.emptyMap();

    @JsonProperty("model_call_id")
    private String modelCallId;

    @JsonProperty("properties")
    private Map<String, String> properties = Collections.emptyMap();

    @JsonProperty("session_id")
    private String sessionId;

    /**
     * Gets the client environment metadata.
     *
     * @return the client info, or {@code null} if absent
     */
    public GitHubTelemetryClientInfo getClient() {
        return client;
    }

    /**
     * Gets the Copilot tracking ID for user-level attribution.
     *
     * @return the tracking ID, or {@code null} if absent
     */
    public String getCopilotTrackingId() {
        return copilotTrackingId;
    }

    /**
     * Gets the timestamp when the event was created (ISO 8601 format).
     *
     * @return the creation timestamp, or {@code null} if absent
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the experiment assignment context.
     *
     * @return the assignment context, or {@code null} if absent
     */
    public String getExpAssignmentContext() {
        return expAssignmentContext;
    }

    /**
     * Gets the feature flags enabled for this session, as a map from flag to value.
     *
     * @return the features map, or {@code null} if absent
     */
    public Map<String, String> getFeatures() {
        return features;
    }

    /**
     * Gets the event type/kind (e.g. get_completion_with_tools_turn,
     * tool_call_executed).
     *
     * @return the event kind
     */
    public String getKind() {
        return kind;
    }

    /**
     * Gets the numeric metrics as a map from key to value.
     *
     * @return the metrics map
     */
    public Map<String, Double> getMetrics() {
        return metrics;
    }

    /**
     * Gets the reference to the model call that produced this event.
     *
     * @return the model call ID, or {@code null} if absent
     */
    public String getModelCallId() {
        return modelCallId;
    }

    /**
     * Gets the string-valued properties as a map from key to value.
     *
     * @return the properties map
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Gets the session identifier the event belongs to.
     *
     * @return the session ID, or {@code null} if absent
     */
    public String getSessionId() {
        return sessionId;
    }
}
