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
 * A GitHub repository holding the plugin, installed directly as `owner/repo[:path]` at the plan's commit, without a marketplace.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogAgentPluginRepositorySource extends CatalogAgentPluginInstallSource {

    @JsonProperty("kind")
    private final String kind = "repository";

    @Override
    public String getKind() { return kind; }

    /** Canonical lowercase GitHub owner/repository holding the plugin. */
    @JsonProperty("repository")
    private String repository;

    /** Relative POSIX path of the plugin directory in the repository. Omitted when the plugin is at the repository root. */
    @JsonProperty("path")
    private String path;

    public String getRepository() { return repository; }
    public void setRepository(String repository) { this.repository = repository; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
