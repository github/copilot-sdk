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
 * Remote session metadata for the session to hand off (typically obtained from `sessions.list` with `source: "remote"`).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteSessionMetadataValue(
    /** Stable session identifier. */
    @JsonProperty("sessionId") String sessionId,
    /** Session creation time as an ISO 8601 timestamp. */
    @JsonProperty("startTime") String startTime,
    /** Last-modified time as an ISO 8601 timestamp. */
    @JsonProperty("modifiedTime") String modifiedTime,
    /** Short summary of the session, when one has been derived. */
    @JsonProperty("summary") String summary,
    /** Optional human-friendly name set via /rename. */
    @JsonProperty("name") String name,
    /** Always true for remote sessions. */
    @JsonProperty("isRemote") Boolean isRemote,
    /** Most recent working directory context. */
    @JsonProperty("context") SessionContext context,
    /** GitHub repository the remote session belongs to. */
    @JsonProperty("repository") RemoteSessionMetadataRepository repository,
    /** Backing remote session IDs (most recent first). */
    @JsonProperty("remoteSessionIds") List<String> remoteSessionIds,
    /** Pull request number associated with the session. */
    @JsonProperty("pullRequestNumber") Long pullRequestNumber,
    /** Original remote resource identifier (task ID or PR node ID). */
    @JsonProperty("resourceId") String resourceId,
    /** Whether the remote task originated from CCA or CLI `--remote`. */
    @JsonProperty("taskType") RemoteSessionMetadataTaskType taskType,
    /** Deadline (ISO 8601) at which a CLI remote session becomes stale without further heartbeats. */
    @JsonProperty("staleAt") String staleAt,
    /** Server-side task state returned by GitHub. */
    @JsonProperty("state") String state
) {
}
