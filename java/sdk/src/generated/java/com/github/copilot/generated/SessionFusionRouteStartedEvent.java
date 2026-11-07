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
 * Session event "session.fusion_route_started". Experimental transient signal that HydraFusion routing has started for an eligible turn.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionFusionRouteStartedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.fusion_route_started"; }

    @JsonProperty("data")
    private SessionFusionRouteStartedEventData data;

    public SessionFusionRouteStartedEventData getData() { return data; }
    public void setData(SessionFusionRouteStartedEventData data) { this.data = data; }

    /** Data payload for {@link SessionFusionRouteStartedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionFusionRouteStartedEventData(
        /** Identifier for this routing attempt before a durable Fusion turn exists. */
        @JsonProperty("attemptId") String attemptId,
        /** Kind of turn being routed. */
        @JsonProperty("turnKind") FusionTurnKind turnKind,
        /** Synthetic HydraFusion model selected for the session. */
        @JsonProperty("syntheticModel") String syntheticModel,
        /** HydraFusion routing policy requested for the turn. */
        @JsonProperty("policy") String policy
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionFusionRouteStartedEventData {}

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
            private FusionTurnKind turnKind;
            private String syntheticModel;
            private String policy;

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
             * Sets the {@code turnKind} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder turnKind(FusionTurnKind value) {
                this.turnKind = value;
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
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionFusionRouteStartedEventData build() {
                if (attemptId == null) throw new IllegalStateException("Missing required property: attemptId");
                if (turnKind == null) throw new IllegalStateException("Missing required property: turnKind");
                return new SessionFusionRouteStartedEventData(attemptId, turnKind, syntheticModel, policy);
            }
        }
    }
}
