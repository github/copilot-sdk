/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * A single telemetry event in the runtime's native GitHub-shaped telemetry format, forwarded verbatim to opted-in hosts. The `restricted` flag on the enclosing GitHubTelemetryNotification distinguishes standard from restricted events; the payload shape is identical for both.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubTelemetryEvent(
    /** Event type/kind (e.g. get_completion_with_tools_turn, tool_call_executed). */
    @JsonProperty("kind") String kind,
    /** Timestamp when the event was created (ISO 8601 format). */
    @JsonProperty("created_at") String createdAt,
    /** Reference to the model call that produced this event. */
    @JsonProperty("model_call_id") String modelCallId,
    /** String-valued properties as a map from key to value. */
    @JsonProperty("properties") Map<String, String> properties,
    /** Numeric metrics as a map from key to value. */
    @JsonProperty("metrics") Map<String, Double> metrics,
    /** Experiment assignment context. */
    @JsonProperty("exp_assignment_context") String expAssignmentContext,
    /** Feature flags enabled for this session, as a map from flag to value. */
    @JsonProperty("features") Map<String, String> features,
    /** Session identifier the event belongs to. */
    @JsonProperty("session_id") String sessionId,
    /** Copilot tracking ID for user-level attribution. */
    @JsonProperty("copilot_tracking_id") String copilotTrackingId,
    /** Client environment metadata. */
    @JsonProperty("client") GitHubTelemetryClientInfo client
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public GitHubTelemetryEvent {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String kind;
        private String createdAt;
        private String modelCallId;
        private Map<String, String> properties;
        private Map<String, Double> metrics;
        private String expAssignmentContext;
        private Map<String, String> features;
        private String sessionId;
        private String copilotTrackingId;
        private GitHubTelemetryClientInfo client;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder kind(String value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code created_at} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder createdAt(String value) {
            this.createdAt = value;
            return this;
        }

        /**
         * Sets the {@code model_call_id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modelCallId(String value) {
            this.modelCallId = value;
            return this;
        }

        /**
         * Sets the {@code properties} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder properties(Map<String, String> value) {
            this.properties = value;
            return this;
        }

        /**
         * Sets the {@code metrics} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder metrics(Map<String, Double> value) {
            this.metrics = value;
            return this;
        }

        /**
         * Sets the {@code exp_assignment_context} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder expAssignmentContext(String value) {
            this.expAssignmentContext = value;
            return this;
        }

        /**
         * Sets the {@code features} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder features(Map<String, String> value) {
            this.features = value;
            return this;
        }

        /**
         * Sets the {@code session_id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code copilot_tracking_id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder copilotTrackingId(String value) {
            this.copilotTrackingId = value;
            return this;
        }

        /**
         * Sets the {@code client} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder client(GitHubTelemetryClientInfo value) {
            this.client = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubTelemetryEvent build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (properties == null) throw new IllegalStateException("Missing required property: properties");
            if (metrics == null) throw new IllegalStateException("Missing required property: metrics");
            return new GitHubTelemetryEvent(kind, createdAt, modelCallId, properties, metrics, expAssignmentContext, features, sessionId, copilotTrackingId, client);
        }
    }
}
