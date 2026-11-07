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
 * Plugin entry advertised by a marketplace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketplacePluginInfo(
    /** Plugin name as listed in the marketplace catalog */
    @JsonProperty("name") String name,
    /** Short description from the marketplace catalog, when present */
    @JsonProperty("description") String description,
    /** Soft assignment from effective managed settings for this marketplace entry. */
    @JsonProperty("assignment") ManagedPluginAssignment assignment
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public MarketplacePluginInfo {}

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
        private String name;
        private String description;
        private ManagedPluginAssignment assignment;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code assignment} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder assignment(ManagedPluginAssignment value) {
            this.assignment = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public MarketplacePluginInfo build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            return new MarketplacePluginInfo(name, description, assignment);
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Plugin name as listed in the marketplace catalog
     * @param description Short description from the marketplace catalog, when present
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public MarketplacePluginInfo(
        String name,
        String description
    ) {
        this(name, description, null);
    }
}
