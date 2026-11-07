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
 * A generated feed's identity. It has no version; generatedAt is its only freshness signal.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogGeneratedFeedDescription(
    /** Generation strategy that was requested. */
    @JsonProperty("strategy") String strategy,
    /** Feed key that was requested. */
    @JsonProperty("key") String key,
    /** When the feed was computed, as reported by the authority. Omitted when the authority reports no valid timestamp. */
    @JsonProperty("generatedAt") String generatedAt
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CatalogGeneratedFeedDescription {}

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
        private String strategy;
        private String key;
        private String generatedAt;

        /**
         * Sets the {@code strategy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder strategy(String value) {
            this.strategy = value;
            return this;
        }

        /**
         * Sets the {@code key} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder key(String value) {
            this.key = value;
            return this;
        }

        /**
         * Sets the {@code generatedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder generatedAt(String value) {
            this.generatedAt = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogGeneratedFeedDescription build() {
            if (strategy == null) throw new IllegalStateException("Missing required property: strategy");
            if (key == null) throw new IllegalStateException("Missing required property: key");
            return new CatalogGeneratedFeedDescription(strategy, key, generatedAt);
        }
    }
}
