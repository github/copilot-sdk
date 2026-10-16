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
}
