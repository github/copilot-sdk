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
 * A working-directory context together with the client that produced it.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionWorkingDirectoryContextWithClient(
    /** Current working directory path */
    @JsonProperty("cwd") String cwd,
    /** Root directory of the git repository */
    @JsonProperty("gitRoot") String gitRoot,
    /** Repository identifier derived from the git remote URL */
    @JsonProperty("repository") String repository,
    /** Hosting platform type of the repository */
    @JsonProperty("hostType") String hostType,
    /** Current git branch name */
    @JsonProperty("branch") String branch,
    /** Raw host string from the git remote URL */
    @JsonProperty("repositoryHost") String repositoryHost,
    /** Head commit of the current git branch */
    @JsonProperty("headCommit") String headCommit,
    /** Merge-base commit SHA */
    @JsonProperty("baseCommit") String baseCommit,
    /** Name of the client that created the session */
    @JsonProperty("clientName") String clientName
) {
}
