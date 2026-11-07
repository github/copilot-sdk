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
 * An Agent Finder materialisation reference pinning one exact resource revision.

It carries no origin or URL: the runtime always builds the descriptor URL on its fixed Agent
Finder authority. It carries no display data either; every reviewed field comes from the
verified descriptor and Skill files.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogMaterializationReference(
    /** Agent Finder resource identifier, a positive decimal integer. */
    @JsonProperty("resourceId") String resourceId,
    /** Agent Finder materialisation revision identifier, a positive decimal integer. This is not a Git commit. */
    @JsonProperty("revisionId") String revisionId,
    /** Materialisation descriptor schema version, currently `1.0.0`. */
    @JsonProperty("schemaVersion") String schemaVersion,
    /** Expected `sha256:<lowercase hex>` digest of the canonical descriptor. */
    @JsonProperty("descriptorDigest") String descriptorDigest,
    /** Expected `sha256:<lowercase hex>` digest of the descriptor's bundle manifest. */
    @JsonProperty("bundleDigest") String bundleDigest
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CatalogMaterializationReference {}

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
        private String revisionId;
        private String schemaVersion;
        private String descriptorDigest;
        private String bundleDigest;

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
         * Sets the {@code revisionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder revisionId(String value) {
            this.revisionId = value;
            return this;
        }

        /**
         * Sets the {@code schemaVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder schemaVersion(String value) {
            this.schemaVersion = value;
            return this;
        }

        /**
         * Sets the {@code descriptorDigest} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder descriptorDigest(String value) {
            this.descriptorDigest = value;
            return this;
        }

        /**
         * Sets the {@code bundleDigest} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder bundleDigest(String value) {
            this.bundleDigest = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogMaterializationReference build() {
            if (resourceId == null) throw new IllegalStateException("Missing required property: resourceId");
            if (revisionId == null) throw new IllegalStateException("Missing required property: revisionId");
            if (schemaVersion == null) throw new IllegalStateException("Missing required property: schemaVersion");
            if (descriptorDigest == null) throw new IllegalStateException("Missing required property: descriptorDigest");
            if (bundleDigest == null) throw new IllegalStateException("Missing required property: bundleDigest");
            return new CatalogMaterializationReference(resourceId, revisionId, schemaVersion, descriptorDigest, bundleDigest);
        }
    }
}
