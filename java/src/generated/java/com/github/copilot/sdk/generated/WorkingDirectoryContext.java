/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Working directory and git context at session start
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkingDirectoryContext(
    /** Current working directory path */
    @JsonProperty("cwd") String cwd,
    /** Root directory of the git repository, resolved via git rev-parse */
    @JsonProperty("gitRoot") String gitRoot,
    /** Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps) */
    @JsonProperty("repository") String repository,
    /** Hosting platform type of the repository (github or ado) */
    @JsonProperty("hostType") WorkingDirectoryContextHostType hostType,
    /** Raw host string from the git remote URL (e.g. "github.com", "mycompany.ghe.com", "dev.azure.com") */
    @JsonProperty("repositoryHost") String repositoryHost,
    /** Current git branch name */
    @JsonProperty("branch") String branch,
    /** Head commit of current git branch at session start time */
    @JsonProperty("headCommit") String headCommit,
    /** Base commit of current git branch at session start time */
    @JsonProperty("baseCommit") String baseCommit
) {
}
