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
 * Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetadataSnapshotRemoteMetadata(
    /** The original resource identifier (task ID or PR node ID), preserved across event-replay reconstructions. Falls back to `sessionId` when absent. */
    @JsonProperty("resourceId") String resourceId,
    /** The repository the remote session targets. */
    @JsonProperty("repository") MetadataSnapshotRemoteMetadataRepository repository,
    /** The pull request number the remote session is associated with, if any. */
    @JsonProperty("pullRequestNumber") Long pullRequestNumber,
    /** Whether the remote task originated from Copilot Coding Agent (cca) or a CLI `--remote` invocation. */
    @JsonProperty("taskType") MetadataSnapshotRemoteMetadataTaskType taskType
) {
}
