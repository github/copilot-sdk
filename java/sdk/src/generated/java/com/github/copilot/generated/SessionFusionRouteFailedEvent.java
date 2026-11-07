/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Session event "session.fusion_route_failed". Experimental durable HydraFusion routing failure and the deterministic concrete fallback selected for the turn.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionFusionRouteFailedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.fusion_route_failed"; }

    @JsonProperty("data")
    private SessionFusionRouteFailedEventData data;

    public SessionFusionRouteFailedEventData getData() { return data; }
    public void setData(SessionFusionRouteFailedEventData data) { this.data = data; }

    /** Data payload for {@link SessionFusionRouteFailedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionFusionRouteFailedEventData(
        /** Identifier of the routing attempt that failed. */
        @JsonProperty("attemptId") String attemptId,
        /** Identifier of the HydraFusion turn whose routing failed; its fallback phase runs under the same id. Absent on events written before it was recorded. */
        @JsonProperty("fusionId") String fusionId,
        /** Synthetic HydraFusion model selected for the session. */
        @JsonProperty("syntheticModel") String syntheticModel,
        /** HydraFusion routing policy requested for the turn. */
        @JsonProperty("policy") String policy,
        /** Stable machine-readable reason for the routing failure. */
        @JsonProperty("reason") String reason,
        /** Provider or validation error detail, when available. */
        @JsonProperty("errorMessage") String errorMessage,
        /** Concrete model selected as the deterministic fallback. */
        @JsonProperty("fallbackModel") String fallbackModel,
        /** Elapsed routing time in milliseconds before the failure. */
        @JsonProperty("routingLatencyMs") Double routingLatencyMs
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionFusionRouteFailedEventData {}

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
            private String attemptId;
            private String fusionId;
            private String syntheticModel;
            private String policy;
            private String reason;
            private String errorMessage;
            private String fallbackModel;
            private Double routingLatencyMs;

            /**
             * Sets the {@code attemptId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder attemptId(String value) {
                this.attemptId = value;
                return this;
            }

            /**
             * Sets the {@code fusionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fusionId(String value) {
                this.fusionId = value;
                return this;
            }

            /**
             * Sets the {@code syntheticModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder syntheticModel(String value) {
                this.syntheticModel = value;
                return this;
            }

            /**
             * Sets the {@code policy} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder policy(String value) {
                this.policy = value;
                return this;
            }

            /**
             * Sets the {@code reason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder reason(String value) {
                this.reason = value;
                return this;
            }

            /**
             * Sets the {@code errorMessage} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder errorMessage(String value) {
                this.errorMessage = value;
                return this;
            }

            /**
             * Sets the {@code fallbackModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fallbackModel(String value) {
                this.fallbackModel = value;
                return this;
            }

            /**
             * Sets the {@code routingLatencyMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder routingLatencyMs(Double value) {
                this.routingLatencyMs = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionFusionRouteFailedEventData build() {
                if (attemptId == null) throw new IllegalStateException("Missing required property: attemptId");
                if (syntheticModel == null) throw new IllegalStateException("Missing required property: syntheticModel");
                if (policy == null) throw new IllegalStateException("Missing required property: policy");
                if (reason == null) throw new IllegalStateException("Missing required property: reason");
                if (fallbackModel == null) throw new IllegalStateException("Missing required property: fallbackModel");
                return new SessionFusionRouteFailedEventData(attemptId, fusionId, syntheticModel, policy, reason, errorMessage, fallbackModel, routingLatencyMs);
            }
        }
    }
}
