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
 * Session event "model.call_final_result". Internal telemetry result for one logical model operation after all orchestrator-owned retries settle
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ModelCallFinalResultEvent extends SessionEvent {

    @Override
    public String getType() { return "model.call_final_result"; }

    @JsonProperty("data")
    private ModelCallFinalResultEventData data;

    public ModelCallFinalResultEventData getData() { return data; }
    public void setData(ModelCallFinalResultEventData data) { this.data = data; }

    /** Data payload for {@link ModelCallFinalResultEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ModelCallFinalResultEventData(
        /** Model identifier used by the final attempt */
        @JsonProperty("model") String model,
        /** Whether the final attempt used a bring-your-own-key provider */
        @JsonProperty("isByok") Boolean isByok,
        /** Bounded result of the final attempt */
        @JsonProperty("result") ModelCallFinalResult result
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ModelCallFinalResultEventData {}

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
            private Boolean isByok;
            private ModelCallFinalResult result;

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
             * Sets the {@code isByok} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder isByok(Boolean value) {
                this.isByok = value;
                return this;
            }

            /**
             * Sets the {@code result} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder result(ModelCallFinalResult value) {
                this.result = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public ModelCallFinalResultEventData build() {
                if (model == null) throw new IllegalStateException("Missing required property: model");
                if (result == null) throw new IllegalStateException("Missing required property: result");
                return new ModelCallFinalResultEventData(model, isByok, result);
            }
        }
    }
}
