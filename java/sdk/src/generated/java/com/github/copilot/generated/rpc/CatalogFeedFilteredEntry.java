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
 * An entry whose resource resolves but is not in one of the requested trust tiers.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogFeedFilteredEntry extends CatalogFeedEntry {

    @JsonProperty("status")
    private final String status = "filtered";

    @Override
    public String getStatus() { return status; }

    /** The entry's resource identifier as saved in the feed. */
    @JsonProperty("identifier")
    private String identifier;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }


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
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogFeedFilteredEntry build() {
            if (identifier == null) throw new IllegalStateException("Missing required property: identifier");
            CatalogFeedFilteredEntry instance = new CatalogFeedFilteredEntry();
            instance.setIdentifier(identifier);
            return instance;
        }
    }
}
