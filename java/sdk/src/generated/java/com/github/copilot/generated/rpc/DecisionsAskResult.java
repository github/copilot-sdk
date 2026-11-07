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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The decision model's answers.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionsAskResult(
    /** Decision model id that answered; valid as `model` in a later request. */
    @JsonProperty("model") String model,
    /** Model name the backend reported, when it differs from `model`. Informational only. */
    @JsonProperty("upstreamModel") String upstreamModel,
    /** One answer per question, in request order. */
    @JsonProperty("answers") List<Object> answers,
    /** Token usage, when the backend reports it. */
    @JsonProperty("usage") DecisionUsage usage,
    /** Milliseconds from receiving the request to producing the result. */
    @JsonProperty("latencyMs") Long latencyMs
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DecisionsAskResult {}

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
        private String model;
        private String upstreamModel;
        private List<Object> answers;
        private DecisionUsage usage;
        private Long latencyMs;

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code upstreamModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder upstreamModel(String value) {
            this.upstreamModel = value;
            return this;
        }

        /**
         * Sets the {@code answers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder answers(List<Object> value) {
            this.answers = value;
            return this;
        }

        /**
         * Sets the {@code usage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder usage(DecisionUsage value) {
            this.usage = value;
            return this;
        }

        /**
         * Sets the {@code latencyMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder latencyMs(Long value) {
            this.latencyMs = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DecisionsAskResult build() {
            if (model == null) throw new IllegalStateException("Missing required property: model");
            if (answers == null) throw new IllegalStateException("Missing required property: answers");
            if (latencyMs == null) throw new IllegalStateException("Missing required property: latencyMs");
            return new DecisionsAskResult(model, upstreamModel, answers, usage, latencyMs);
        }
    }
}
