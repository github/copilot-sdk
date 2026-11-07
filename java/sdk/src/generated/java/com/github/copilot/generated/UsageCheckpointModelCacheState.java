/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Internal prompt-cache expiration state for one model
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageCheckpointModelCacheState(
    /** Model identifier associated with this cache state */
    @JsonProperty("modelId") String modelId,
    /** Latest known prompt-cache expiration */
    @JsonProperty("cacheExpiresAt") OffsetDateTime cacheExpiresAt,
    /** Retained cache lifetime in seconds, used to refresh expiration after a cache read */
    @JsonProperty("cacheTtlSeconds") Long cacheTtlSeconds
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public UsageCheckpointModelCacheState {}

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
        private String modelId;
        private OffsetDateTime cacheExpiresAt;
        private Long cacheTtlSeconds;

        /**
         * Sets the {@code modelId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelId(String value) {
            this.modelId = value;
            return this;
        }

        /**
         * Sets the {@code cacheExpiresAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cacheExpiresAt(OffsetDateTime value) {
            this.cacheExpiresAt = value;
            return this;
        }

        /**
         * Sets the {@code cacheTtlSeconds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cacheTtlSeconds(Long value) {
            this.cacheTtlSeconds = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UsageCheckpointModelCacheState build() {
            if (modelId == null) throw new IllegalStateException("Missing required property: modelId");
            if (cacheExpiresAt == null) throw new IllegalStateException("Missing required property: cacheExpiresAt");
            if (cacheTtlSeconds == null) throw new IllegalStateException("Missing required property: cacheTtlSeconds");
            return new UsageCheckpointModelCacheState(modelId, cacheExpiresAt, cacheTtlSeconds);
        }
    }
}
