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
}
