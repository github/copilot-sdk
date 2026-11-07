/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "subagent.configured". Resolved runtime configuration for a configured sub-agent
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SubagentConfiguredEvent extends SessionEvent {

    @Override
    public String getType() { return "subagent.configured"; }

    @JsonProperty("data")
    private SubagentConfiguredEventData data;

    public SubagentConfiguredEventData getData() { return data; }
    public void setData(SubagentConfiguredEventData data) { this.data = data; }

    /** Data payload for {@link SubagentConfiguredEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubagentConfiguredEventData(
        /** Resolved model the sub-agent will run with */
        @JsonProperty("model") String model,
        /** Resolved reasoning effort, when configured for the model */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Resolved context tier, when configured for the model */
        @JsonProperty("contextTier") String contextTier,
        /** Whether the sub-agent accepts follow-up turns */
        @JsonProperty("multiTurn") Boolean multiTurn
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SubagentConfiguredEventData {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        public static final class Builder {
            private Builder() {}
            private String model;
            private String reasoningEffort;
            private String contextTier;
            private Boolean multiTurn;

            /**
             * Sets the {@code model} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder model(String value) {
                this.model = value;
                return this;
            }

            /**
             * Sets the {@code reasoningEffort} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningEffort(String value) {
                this.reasoningEffort = value;
                return this;
            }

            /**
             * Sets the {@code contextTier} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder contextTier(String value) {
                this.contextTier = value;
                return this;
            }

            /**
             * Sets the {@code multiTurn} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder multiTurn(Boolean value) {
                this.multiTurn = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SubagentConfiguredEventData build() {
                if (model == null) throw new IllegalStateException("Missing required property: model");
                if (multiTurn == null) throw new IllegalStateException("Missing required property: multiTurn");
                return new SubagentConfiguredEventData(model, reasoningEffort, contextTier, multiTurn);
            }
        }
    }
}
