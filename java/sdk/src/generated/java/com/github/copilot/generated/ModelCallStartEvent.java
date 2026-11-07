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
 * Session event "model.call_start". Model API dispatch metadata for internal telemetry
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ModelCallStartEvent extends SessionEvent {

    @Override
    public String getType() { return "model.call_start"; }

    @JsonProperty("data")
    private ModelCallStartEventData data;

    public ModelCallStartEventData getData() { return data; }
    public void setData(ModelCallStartEventData data) { this.data = data; }

    /** Data payload for {@link ModelCallStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ModelCallStartEventData(
        /** Identifier of the assistant turn that initiated the model call */
        @JsonProperty("turnId") String turnId,
        /** Model identifier used for this API call, when known */
        @JsonProperty("model") String model,
        /** Previous response or interaction identifier included in the model request, when present */
        @JsonProperty("previousResponseId") String previousResponseId,
        /** Experimental HydraFusion attribution for this concrete model call. */
        @CopilotExperimental
        @JsonProperty("fusion") FusionAttribution fusion,
        /** Parent task tool call ID when this model call belongs to a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ModelCallStartEventData {}

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
            private String turnId;
            private String model;
            private String previousResponseId;
            private FusionAttribution fusion;
            private String parentToolCallId;

            /**
             * Sets the {@code turnId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder turnId(String value) {
                this.turnId = value;
                return this;
            }

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
             * Sets the {@code previousResponseId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder previousResponseId(String value) {
                this.previousResponseId = value;
                return this;
            }

            /**
             * Sets the {@code fusion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fusion(FusionAttribution value) {
                this.fusion = value;
                return this;
            }

            /**
             * Sets the {@code parentToolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder parentToolCallId(String value) {
                this.parentToolCallId = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public ModelCallStartEventData build() {
                if (turnId == null) throw new IllegalStateException("Missing required property: turnId");
                return new ModelCallStartEventData(turnId, model, previousResponseId, fusion, parentToolCallId);
            }
        }
    }
}
