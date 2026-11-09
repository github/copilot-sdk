/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Published token limits of a local model.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalModelLimits(
    /** Maximum prompt tokens. */
    @JsonProperty("maxPromptTokens") Double maxPromptTokens,
    /** Maximum context tokens. */
    @JsonProperty("maxContextWindowTokens") Double maxContextWindowTokens,
    /** Maximum output tokens. */
    @JsonProperty("maxOutputTokens") Double maxOutputTokens
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelLimits {}

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
        private Double maxPromptTokens;
        private Double maxContextWindowTokens;
        private Double maxOutputTokens;

        /**
         * Sets the {@code maxPromptTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxPromptTokens(Double value) {
            this.maxPromptTokens = value;
            return this;
        }

        /**
         * Sets the {@code maxContextWindowTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxContextWindowTokens(Double value) {
            this.maxContextWindowTokens = value;
            return this;
        }

        /**
         * Sets the {@code maxOutputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxOutputTokens(Double value) {
            this.maxOutputTokens = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelLimits build() {
            if (maxPromptTokens == null) throw new IllegalStateException("Missing required property: maxPromptTokens");
            if (maxContextWindowTokens == null) throw new IllegalStateException("Missing required property: maxContextWindowTokens");
            if (maxOutputTokens == null) throw new IllegalStateException("Missing required property: maxOutputTokens");
            return new LocalModelLimits(maxPromptTokens, maxContextWindowTokens, maxOutputTokens);
        }
    }
}
