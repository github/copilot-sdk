/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Where and when the runtime observed the trust metadata. Observation time is not the authority's evaluation time and must not be used to infer staleness.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogTrustProvenance(
    /** Bounded authority that supplied the trust field. */
    @JsonProperty("source") CatalogTrustSource source,
    /** ISO 8601 timestamp with a timezone offset at which the runtime observed the search result carrying this trust field. */
    @JsonProperty("observedAt") OffsetDateTime observedAt
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CatalogTrustProvenance {}

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
        private CatalogTrustSource source;
        private OffsetDateTime observedAt;

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(CatalogTrustSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code observedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder observedAt(OffsetDateTime value) {
            this.observedAt = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogTrustProvenance build() {
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (observedAt == null) throw new IllegalStateException("Missing required property: observedAt");
            return new CatalogTrustProvenance(source, observedAt);
        }
    }
}
