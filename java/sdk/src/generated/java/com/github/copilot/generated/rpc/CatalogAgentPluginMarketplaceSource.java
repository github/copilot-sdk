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
 * A plugin marketplace named by verified catalogue install data, installed as `pluginName@name`.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogAgentPluginMarketplaceSource extends CatalogAgentPluginInstallSource {

    @JsonProperty("kind")
    private final String kind = "marketplace";

    @Override
    public String getKind() { return kind; }

    /** Marketplace name. */
    @JsonProperty("name")
    private String name;

    /** Canonical lowercase GitHub owner/repository hosting the marketplace. */
    @JsonProperty("repository")
    private String repository;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRepository() { return repository; }
    public void setRepository(String repository) { this.repository = repository; }


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
        private String repository;

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
         * Sets the {@code repository} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repository(String value) {
            this.repository = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogAgentPluginMarketplaceSource build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (repository == null) throw new IllegalStateException("Missing required property: repository");
            CatalogAgentPluginMarketplaceSource instance = new CatalogAgentPluginMarketplaceSource();
            instance.setName(name);
            instance.setRepository(repository);
            return instance;
        }
    }
}
