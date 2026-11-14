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
 * An Agent Finder materialisation reference pinning one exact resource revision.

It carries no origin or URL: the runtime always builds the descriptor URL on its fixed Agent
Finder authority. It carries no display data either; every reviewed field comes from the
verified descriptor and Skill files.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogMaterializationReference(
    /** Agent Finder resource identifier, a positive decimal integer. */
    @JsonProperty("resourceId") String resourceId,
    /** Agent Finder materialisation revision identifier, a positive decimal integer. This is not a Git commit. */
    @JsonProperty("revisionId") String revisionId,
    /** Materialisation descriptor schema version, currently `1.0.0`. */
    @JsonProperty("schemaVersion") String schemaVersion,
    /** Expected `sha256:<lowercase hex>` digest of the canonical descriptor. */
    @JsonProperty("descriptorDigest") String descriptorDigest,
    /** Expected `sha256:<lowercase hex>` digest of the descriptor's bundle manifest. */
    @JsonProperty("bundleDigest") String bundleDigest
) {
}
