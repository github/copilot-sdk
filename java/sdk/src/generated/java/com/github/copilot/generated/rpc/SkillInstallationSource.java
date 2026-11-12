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
 * Source identity retained from Agent Finder and the pinned GitHub descriptor.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillInstallationSource(
    /** Agent Finder resource identifier. */
    @JsonProperty("resourceId") String resourceId,
    /** Agent Finder materialisation revision identifier. */
    @JsonProperty("catalogRevisionId") String catalogRevisionId,
    /** GitHub repository database identifier. */
    @JsonProperty("repositoryId") String repositoryId,
    /** Repository full name, for example owner/name. */
    @JsonProperty("repository") String repository,
    /** Pinned Git commit revision. */
    @JsonProperty("revision") String revision,
    /** Root path within the pinned repository. */
    @JsonProperty("root") String root,
    /** Digest of the canonical materialisation descriptor. */
    @JsonProperty("descriptorDigest") String descriptorDigest,
    /** Digest of the descriptor's bundle manifest. */
    @JsonProperty("bundleDigest") String bundleDigest
) {
}
