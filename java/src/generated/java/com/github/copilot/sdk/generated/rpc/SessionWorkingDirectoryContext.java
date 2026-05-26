/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Updated working directory and git context. Emitted as the new payload of `session.context_changed`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkingDirectoryContext(
    /** Current working directory path */
    @JsonProperty("cwd") String cwd,
    /** Root directory of the git repository, resolved via git rev-parse */
    @JsonProperty("gitRoot") String gitRoot,
    /** Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps) */
    @JsonProperty("repository") String repository,
    /** Hosting platform type of the repository */
    @JsonProperty("hostType") SessionWorkingDirectoryContextHostType hostType,
    /** Raw host string from the git remote URL (e.g. "github.com", "dev.azure.com") */
    @JsonProperty("repositoryHost") String repositoryHost,
    /** Current git branch name */
    @JsonProperty("branch") String branch,
    /** Head commit of the current git branch */
    @JsonProperty("headCommit") String headCommit,
    /** Merge-base commit SHA (fork point from the remote default branch) */
    @JsonProperty("baseCommit") String baseCommit
) {
}
