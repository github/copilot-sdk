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
 * Review for installing a verified Skill.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SkillInstallationReviewInstall extends SkillInstallationReview {

    @JsonProperty("action")
    private final String action = "install";

    @Override
    public String getAction() { return action; }

    /** Skill invocation name from SKILL.md. */
    @JsonProperty("name")
    private String name;

    /** Skill description from SKILL.md when present. */
    @JsonProperty("description")
    private String description;

    /** Catalogue identity retained from the bound candidate before consent. */
    @JsonProperty("catalogue")
    private InstallationCatalogueIdentity catalogue;

    /** Exact verified source identity. */
    @JsonProperty("source")
    private SkillInstallationSource source;

    /** Exact user-scope target location without an absolute host path. */
    @JsonProperty("target")
    private SkillInstallationLocation target;

    /** Installing never grants immediate use; the Skill is written disabled. */
    @JsonProperty("installsDisabled")
    private Boolean installsDisabled;

    /** Reviewed files and digests. */
    @JsonProperty("files")
    private List<SkillInstallationFileReview> files;

    /** Total reviewed payload size in bytes. */
    @JsonProperty("totalBytes")
    private Long totalBytes;

    /** Relative path of the verified Skill entrypoint. */
    @JsonProperty("entrypointPath")
    private String entrypointPath;

    /** Complete verified SKILL.md content. Planning refuses with review-too-large
when this exceeds 262144 UTF-8 bytes; it is never truncated. */
    @JsonProperty("entrypointContent")
    private String entrypointContent;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public InstallationCatalogueIdentity getCatalogue() { return catalogue; }
    public void setCatalogue(InstallationCatalogueIdentity catalogue) { this.catalogue = catalogue; }

    public SkillInstallationSource getSource() { return source; }
    public void setSource(SkillInstallationSource source) { this.source = source; }

    public SkillInstallationLocation getTarget() { return target; }
    public void setTarget(SkillInstallationLocation target) { this.target = target; }

    public Boolean getInstallsDisabled() { return installsDisabled; }
    public void setInstallsDisabled(Boolean installsDisabled) { this.installsDisabled = installsDisabled; }

    public List<SkillInstallationFileReview> getFiles() { return files; }
    public void setFiles(List<SkillInstallationFileReview> files) { this.files = files; }

    public Long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(Long totalBytes) { this.totalBytes = totalBytes; }

    public String getEntrypointPath() { return entrypointPath; }
    public void setEntrypointPath(String entrypointPath) { this.entrypointPath = entrypointPath; }

    public String getEntrypointContent() { return entrypointContent; }
    public void setEntrypointContent(String entrypointContent) { this.entrypointContent = entrypointContent; }


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
        private InstallationCatalogueIdentity catalogue;
        private SkillInstallationSource source;
        private SkillInstallationLocation target;
        private Boolean installsDisabled;
        private List<SkillInstallationFileReview> files;
        private Long totalBytes;
        private String entrypointPath;
        private String entrypointContent;

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
        public Builder source(SkillInstallationSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code target} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder target(SkillInstallationLocation value) {
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
        public Builder files(List<SkillInstallationFileReview> value) {
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
         * Sets the {@code entrypointPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder entrypointPath(String value) {
            this.entrypointPath = value;
            return this;
        }

        /**
         * Sets the {@code entrypointContent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder entrypointContent(String value) {
            this.entrypointContent = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillInstallationReviewInstall build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (catalogue == null) throw new IllegalStateException("Missing required property: catalogue");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (target == null) throw new IllegalStateException("Missing required property: target");
            if (installsDisabled == null) throw new IllegalStateException("Missing required property: installsDisabled");
            if (files == null) throw new IllegalStateException("Missing required property: files");
            if (totalBytes == null) throw new IllegalStateException("Missing required property: totalBytes");
            if (entrypointPath == null) throw new IllegalStateException("Missing required property: entrypointPath");
            if (entrypointContent == null) throw new IllegalStateException("Missing required property: entrypointContent");
            SkillInstallationReviewInstall instance = new SkillInstallationReviewInstall();
            instance.setName(name);
            instance.setDescription(description);
            instance.setCatalogue(catalogue);
            instance.setSource(source);
            instance.setTarget(target);
            instance.setInstallsDisabled(installsDisabled);
            instance.setFiles(files);
            instance.setTotalBytes(totalBytes);
            instance.setEntrypointPath(entrypointPath);
            instance.setEntrypointContent(entrypointContent);
            return instance;
        }
    }
}
