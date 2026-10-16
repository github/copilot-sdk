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
    }
}
