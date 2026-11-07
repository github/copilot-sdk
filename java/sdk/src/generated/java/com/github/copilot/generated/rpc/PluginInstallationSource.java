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
 * Source identity retained from Agent Finder and the pinned GitHub descriptor.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginInstallationSource(
    /** Agent Finder resource identifier. */
    @JsonProperty("resourceId") String resourceId,
    /** Agent Finder materialisation revision identifier. */
    @JsonProperty("catalogRevisionId") String catalogRevisionId,
    /** GitHub repository database identifier. */
    @JsonProperty("repositoryId") String repositoryId,
    /** Repository full name, for example owner/name. */
    @JsonProperty("repository") String repository,
    /** Pinned Git commit revision. */
    @JsonProperty("revision") String revision,
    /** Plugin root within the pinned repository. Empty for the repository root. */
    @JsonProperty("root") String root,
    /** Digest of the canonical materialisation descriptor. */
    @JsonProperty("descriptorDigest") String descriptorDigest,
    /** Digest of the descriptor's bundle manifest. */
    @JsonProperty("bundleDigest") String bundleDigest
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PluginInstallationSource {}

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
        private String catalogRevisionId;
        private String repositoryId;
        private String repository;
        private String revision;
        private String root;
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
         * Sets the {@code catalogRevisionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder catalogRevisionId(String value) {
            this.catalogRevisionId = value;
            return this;
        }

        /**
         * Sets the {@code repositoryId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repositoryId(String value) {
            this.repositoryId = value;
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
         * Sets the {@code revision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder revision(String value) {
            this.revision = value;
            return this;
        }

        /**
         * Sets the {@code root} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder root(String value) {
            this.root = value;
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
        public PluginInstallationSource build() {
            if (resourceId == null) throw new IllegalStateException("Missing required property: resourceId");
            if (catalogRevisionId == null) throw new IllegalStateException("Missing required property: catalogRevisionId");
            if (repositoryId == null) throw new IllegalStateException("Missing required property: repositoryId");
            if (repository == null) throw new IllegalStateException("Missing required property: repository");
            if (revision == null) throw new IllegalStateException("Missing required property: revision");
            if (root == null) throw new IllegalStateException("Missing required property: root");
            if (descriptorDigest == null) throw new IllegalStateException("Missing required property: descriptorDigest");
            if (bundleDigest == null) throw new IllegalStateException("Missing required property: bundleDigest");
            return new PluginInstallationSource(resourceId, catalogRevisionId, repositoryId, repository, revision, root, descriptorDigest, bundleDigest);
        }
    }
}
