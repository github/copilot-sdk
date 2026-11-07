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
 * An entry whose resource resolves and is presented as the same candidate catalog.search would return.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogFeedAvailableEntry extends CatalogFeedEntry {

    @JsonProperty("status")
    private final String status = "available";

    @Override
    public String getStatus() { return status; }

    /** The entry's resource identifier as saved in the feed. */
    @JsonProperty("identifier")
    private String identifier;

    /** The entry's resource, with the same projection, installability, handle and display metadata as a catalog.search candidate. */
    @JsonProperty("candidate")
    private CatalogCandidate candidate;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public CatalogCandidate getCandidate() { return candidate; }
    public void setCandidate(CatalogCandidate candidate) { this.candidate = candidate; }


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
        private String identifier;
        private CatalogCandidate candidate;

        /**
         * Sets the {@code identifier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder identifier(String value) {
            this.identifier = value;
            return this;
        }

        /**
         * Sets the {@code candidate} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder candidate(CatalogCandidate value) {
            this.candidate = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogFeedAvailableEntry build() {
            if (identifier == null) throw new IllegalStateException("Missing required property: identifier");
            if (candidate == null) throw new IllegalStateException("Missing required property: candidate");
            CatalogFeedAvailableEntry instance = new CatalogFeedAvailableEntry();
            instance.setIdentifier(identifier);
            instance.setCandidate(candidate);
            return instance;
        }
    }
}
