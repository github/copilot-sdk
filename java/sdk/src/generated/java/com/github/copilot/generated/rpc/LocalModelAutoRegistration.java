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
 * Automatic registration identity and concurrency.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalModelAutoRegistration(
    /** Corresponding Copilot model identifier. */
    @JsonProperty("capiId") String capiId,
    /** Maximum simultaneous inference requests. */
    @JsonProperty("maxConcurrent") Double maxConcurrent
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelAutoRegistration {}

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
        private String capiId;
        private Double maxConcurrent;

        /**
         * Sets the {@code capiId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder capiId(String value) {
            this.capiId = value;
            return this;
        }

        /**
         * Sets the {@code maxConcurrent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxConcurrent(Double value) {
            this.maxConcurrent = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelAutoRegistration build() {
            if (capiId == null) throw new IllegalStateException("Missing required property: capiId");
            if (maxConcurrent == null) throw new IllegalStateException("Missing required property: maxConcurrent");
            return new LocalModelAutoRegistration(capiId, maxConcurrent);
        }
    }
}
