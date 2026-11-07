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
 * An entry that cannot be presented, kept in its position so clients decide how to render it.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogFeedUnavailableEntry extends CatalogFeedEntry {

    @JsonProperty("status")
    private final String status = "unavailable";

    @Override
    public String getStatus() { return status; }

    /** The entry's resource identifier as saved in the feed. */
    @JsonProperty("identifier")
    private String identifier;

    /** Why the entry cannot be presented. */
    @JsonProperty("reason")
    private CatalogFeedUnavailableReason reason;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public CatalogFeedUnavailableReason getReason() { return reason; }
    public void setReason(CatalogFeedUnavailableReason reason) { this.reason = reason; }


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
        private CatalogFeedUnavailableReason reason;

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
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(CatalogFeedUnavailableReason value) {
            this.reason = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogFeedUnavailableEntry build() {
            if (identifier == null) throw new IllegalStateException("Missing required property: identifier");
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            CatalogFeedUnavailableEntry instance = new CatalogFeedUnavailableEntry();
            instance.setIdentifier(identifier);
            instance.setReason(reason);
            return instance;
        }
    }
}
