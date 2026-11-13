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
 * The repository the remote session targets.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetadataSnapshotRemoteMetadataRepository(
    /** The GitHub owner (user or organization) of the target repository. */
    @JsonProperty("owner") String owner,
    /** The GitHub repository name (without owner). */
    @JsonProperty("name") String name,
    /** The branch the remote session is operating on. */
    @JsonProperty("branch") String branch
) {
}
