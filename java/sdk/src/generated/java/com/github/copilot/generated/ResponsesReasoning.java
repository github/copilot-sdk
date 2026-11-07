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
 * Original request-level and effective conversation reasoning effort for a provider history boundary; the historical type name is retained for compatibility
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponsesReasoning(
    /** Provider model whose reasoning settings this boundary records */
    @JsonProperty("model") String model,
    /** Original request-level effort, retained while replaying this conversation prefix */
    @JsonProperty("initialEffort") String initialEffort,
    /** Effective effort selected before this message, independent of the response-level reasoning field */
    @JsonProperty("effort") String effort
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ResponsesReasoning {}

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
        private String initialEffort;
        private String effort;

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
         * Sets the {@code initialEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder initialEffort(String value) {
            this.initialEffort = value;
            return this;
        }

        /**
         * Sets the {@code effort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder effort(String value) {
            this.effort = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ResponsesReasoning build() {
            if (model == null) throw new IllegalStateException("Missing required property: model");
            if (initialEffort == null) throw new IllegalStateException("Missing required property: initialEffort");
            if (effort == null) throw new IllegalStateException("Missing required property: effort");
            return new ResponsesReasoning(model, initialEffort, effort);
        }
    }
}
