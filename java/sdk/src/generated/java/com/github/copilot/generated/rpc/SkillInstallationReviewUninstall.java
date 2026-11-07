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
 * Review for uninstalling an owned verified Skill.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SkillInstallationReviewUninstall extends SkillInstallationReview {

    @JsonProperty("action")
    private final String action = "uninstall";

    @Override
    public String getAction() { return action; }

    /** Owned installation being removed. */
    @JsonProperty("installation")
    private SkillInstallationSummary installation;

    /** Whether current files differ from the receipt. Apply refuses drift. */
    @JsonProperty("filesModified")
    private Boolean filesModified;

    /** Files recorded by the installation receipt. */
    @JsonProperty("files")
    private List<SkillInstallationFileReview> files;

    /** Total receipt-owned payload size in bytes. */
    @JsonProperty("totalBytes")
    private Long totalBytes;

    /** Catalogue identity retained at install time. */
    @JsonProperty("catalogue")
    private InstallationCatalogueIdentity catalogue;

    public SkillInstallationSummary getInstallation() { return installation; }
    public void setInstallation(SkillInstallationSummary installation) { this.installation = installation; }

    public Boolean getFilesModified() { return filesModified; }
    public void setFilesModified(Boolean filesModified) { this.filesModified = filesModified; }

    public List<SkillInstallationFileReview> getFiles() { return files; }
    public void setFiles(List<SkillInstallationFileReview> files) { this.files = files; }

    public Long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(Long totalBytes) { this.totalBytes = totalBytes; }

    public InstallationCatalogueIdentity getCatalogue() { return catalogue; }
    public void setCatalogue(InstallationCatalogueIdentity catalogue) { this.catalogue = catalogue; }


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
        private SkillInstallationSummary installation;
        private Boolean filesModified;
        private List<SkillInstallationFileReview> files;
        private Long totalBytes;
        private InstallationCatalogueIdentity catalogue;

        /**
         * Sets the {@code installation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installation(SkillInstallationSummary value) {
            this.installation = value;
            return this;
        }

        /**
         * Sets the {@code filesModified} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder filesModified(Boolean value) {
            this.filesModified = value;
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
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillInstallationReviewUninstall build() {
            if (installation == null) throw new IllegalStateException("Missing required property: installation");
            if (filesModified == null) throw new IllegalStateException("Missing required property: filesModified");
            if (files == null) throw new IllegalStateException("Missing required property: files");
            if (totalBytes == null) throw new IllegalStateException("Missing required property: totalBytes");
            if (catalogue == null) throw new IllegalStateException("Missing required property: catalogue");
            SkillInstallationReviewUninstall instance = new SkillInstallationReviewUninstall();
            instance.setInstallation(installation);
            instance.setFilesModified(filesModified);
            instance.setFiles(files);
            instance.setTotalBytes(totalBytes);
            instance.setCatalogue(catalogue);
            return instance;
        }
    }
}
