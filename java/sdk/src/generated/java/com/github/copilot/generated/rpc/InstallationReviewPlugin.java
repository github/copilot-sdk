/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Variant {@code plugin} of {@link InstallationReview}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class InstallationReviewPlugin extends InstallationReview {

    @JsonProperty("resource")
    private final String resource = "plugin";

    @Override
    public String getResource() { return resource; }

    /** The exact verified Agent Plugin action and its reviewed files. */
    @JsonProperty("review")
    private InstallationReviewPluginReview review;

    public InstallationReviewPluginReview getReview() { return review; }
    public void setReview(InstallationReviewPluginReview review) { this.review = review; }


    /** Review for installing a verified Agent Plugin. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InstallationReviewPluginReview(
        /** Installed plugin name, derived from the verified manifest exactly as a direct install derives it. */
        @JsonProperty("name") String name,
        /** Manifest description when present. */
        @JsonProperty("description") String description,
        /** Manifest version when present. */
        @JsonProperty("version") String version,
        /** Catalogue identity retained from the bound candidate, or verified data for a reference. */
        @JsonProperty("catalogue") InstallationCatalogueIdentity catalogue,
        /** Exact verified source identity. */
        @JsonProperty("source") PluginInstallationSource source,
        /** Exact install location without an absolute host path. */
        @JsonProperty("target") PluginInstallationLocation target,
        /** Installing never grants immediate use; the plugin is registered disabled. */
        @JsonProperty("installsDisabled") Boolean installsDisabled,
        /** Reviewed files and digests. */
        @JsonProperty("files") List<PluginInstallationFileReview> files,
        /** Total reviewed payload size in bytes. */
        @JsonProperty("totalBytes") Long totalBytes,
        /** Relative path of the verified plugin manifest. */
        @JsonProperty("manifestPath") String manifestPath,
        /** Complete verified manifest content. Planning refuses with review-too-large
when this exceeds 262144 UTF-8 bytes; it is never truncated. */
        @JsonProperty("manifestContent") String manifestContent,
        /** MCP servers the verified files declare, exactly as an install of these files loads
them. Omitted when the plugin declares none. */
        @JsonProperty("mcpServers") List<PluginInstallationMcpServerReview> mcpServers,
        /** Exact reviewed Agent Plugin installation action. */
        @JsonProperty("action") String action
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public InstallationReviewPluginReview {}

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
            private String version;
            private InstallationCatalogueIdentity catalogue;
            private PluginInstallationSource source;
            private PluginInstallationLocation target;
            private Boolean installsDisabled;
            private List<PluginInstallationFileReview> files;
            private Long totalBytes;
            private String manifestPath;
            private String manifestContent;
            private List<PluginInstallationMcpServerReview> mcpServers;
            private String action;

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
             * Sets the {@code catalogue} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder catalogue(InstallationCatalogueIdentity value) {
                this.catalogue = value;
                return this;
            }

            /**
             * Sets the {@code source} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder source(PluginInstallationSource value) {
                this.source = value;
                return this;
            }

            /**
             * Sets the {@code target} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder target(PluginInstallationLocation value) {
                this.target = value;
                return this;
            }

            /**
             * Sets the {@code installsDisabled} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder installsDisabled(Boolean value) {
                this.installsDisabled = value;
                return this;
            }

            /**
             * Sets the {@code files} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder files(List<PluginInstallationFileReview> value) {
                this.files = value;
                return this;
            }

            /**
             * Sets the {@code totalBytes} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder totalBytes(Long value) {
                this.totalBytes = value;
                return this;
            }

            /**
             * Sets the {@code manifestPath} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder manifestPath(String value) {
                this.manifestPath = value;
                return this;
            }

            /**
             * Sets the {@code manifestContent} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder manifestContent(String value) {
                this.manifestContent = value;
                return this;
            }

            /**
             * Sets the {@code mcpServers} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcpServers(List<PluginInstallationMcpServerReview> value) {
                this.mcpServers = value;
                return this;
            }

            /**
             * Sets the {@code action} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder action(String value) {
                this.action = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public InstallationReviewPluginReview build() {
                if (name == null) throw new IllegalStateException("Missing required property: name");
                if (catalogue == null) throw new IllegalStateException("Missing required property: catalogue");
                if (source == null) throw new IllegalStateException("Missing required property: source");
                if (target == null) throw new IllegalStateException("Missing required property: target");
                if (installsDisabled == null) throw new IllegalStateException("Missing required property: installsDisabled");
                if (files == null) throw new IllegalStateException("Missing required property: files");
                if (totalBytes == null) throw new IllegalStateException("Missing required property: totalBytes");
                if (manifestPath == null) throw new IllegalStateException("Missing required property: manifestPath");
                if (manifestContent == null) throw new IllegalStateException("Missing required property: manifestContent");
                if (action == null) throw new IllegalStateException("Missing required property: action");
                return new InstallationReviewPluginReview(name, description, version, catalogue, source, target, installsDisabled, files, totalBytes, manifestPath, manifestContent, mcpServers, action);
            }
        }
    }

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
        private InstallationReviewPluginReview review;

        /**
         * Sets the {@code review} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder review(InstallationReviewPluginReview value) {
            this.review = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public InstallationReviewPlugin build() {
            if (review == null) throw new IllegalStateException("Missing required property: review");
            InstallationReviewPlugin instance = new InstallationReviewPlugin();
            instance.setReview(review);
            return instance;
        }
    }
}
