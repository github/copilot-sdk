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
 * Catalogue identity retained from a bound candidate or plan at installation time.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstallationCatalogueIdentity(
    /** Authority resource identifier when supplied by the catalogue. */
    @JsonProperty("resourceId") String resourceId,
    /** Catalogue item URL when supplied by the authority. */
    @JsonProperty("itemUrl") String itemUrl,
    /** Human display name retained from the catalogue candidate. */
    @JsonProperty("displayName") String displayName,
    /** Catalogue description retained at install planning time. */
    @JsonProperty("description") String description,
    /** Catalogue publisher retained at install planning time. */
    @JsonProperty("publisher") String publisher,
    /** Catalogue version retained at install planning time. */
    @JsonProperty("version") String version,
    /** Catalogue authority/source string that supplied the candidate. */
    @JsonProperty("source") String source,
    /** Catalogue trust observation retained at install planning time. */
    @JsonProperty("trustAtInstall") CatalogTrustSnapshot trustAtInstall,
    /** HTTPS URL of the resource's icon on raw.githubusercontent.com, retained from the catalogue
candidate. Untrusted external content: load it without credentials. Omitted when the
candidate had none. */
    @JsonProperty("iconUrl") String iconUrl
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public InstallationCatalogueIdentity {}

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
        private String resourceId;
        private String itemUrl;
        private String displayName;
        private String description;
        private String publisher;
        private String version;
        private String source;
        private CatalogTrustSnapshot trustAtInstall;
        private String iconUrl;

        /**
         * Sets the {@code resourceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resourceId(String value) {
            this.resourceId = value;
            return this;
        }

        /**
         * Sets the {@code itemUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder itemUrl(String value) {
            this.itemUrl = value;
            return this;
        }

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayName(String value) {
            this.displayName = value;
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
         * Sets the {@code publisher} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder publisher(String value) {
            this.publisher = value;
            return this;
        }

        /**
         * Sets the {@code version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder version(String value) {
            this.version = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code trustAtInstall} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder trustAtInstall(CatalogTrustSnapshot value) {
            this.trustAtInstall = value;
            return this;
        }

        /**
         * Sets the {@code iconUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder iconUrl(String value) {
            this.iconUrl = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public InstallationCatalogueIdentity build() {
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            return new InstallationCatalogueIdentity(resourceId, itemUrl, displayName, description, publisher, version, source, trustAtInstall, iconUrl);
        }
    }
}
