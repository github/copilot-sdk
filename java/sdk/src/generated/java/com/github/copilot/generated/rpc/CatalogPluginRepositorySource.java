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
 * Syntactically validated GitHub repository provenance declared by catalog metadata. This is a source claim rather than proof that the descriptor URL resolves to the repository.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogPluginRepositorySource(
    /** Canonical lowercase owner/repository name derived from metadata.sourceSet. */
    @JsonProperty("repository") String repository,
    /** Case-preserving safe relative POSIX path derived from metadata.repoPath. Absent when the catalog declared sourceSet without repoPath, meaning the plugin is at the repository root; only callers that require agent-plugin-confirmed-installation receive such a source. */
    @JsonProperty("path") String path
) {
}
