/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Metadata for a connected remote session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectedRemoteSessionMetadata(
    /** SDK session ID for the connected remote session. */
    @JsonProperty("sessionId") String sessionId,
    /** Optional friendly session name. */
    @JsonProperty("name") String name,
    /** Optional session summary. */
    @JsonProperty("summary") String summary,
    /** Session start time as an ISO 8601 string. */
    @JsonProperty("startTime") OffsetDateTime startTime,
    /** Last session update time as an ISO 8601 string. */
    @JsonProperty("modifiedTime") OffsetDateTime modifiedTime,
    /** Repository associated with the connected remote session. */
    @JsonProperty("repository") ConnectedRemoteSessionMetadataRepository repository,
    /** Pull request number associated with the session. */
    @JsonProperty("pullRequestNumber") Long pullRequestNumber,
    /** Original remote resource identifier. */
    @JsonProperty("resourceId") String resourceId,
    /** Neutral SDK discriminator for the connected remote session kind. */
    @JsonProperty("kind") ConnectedRemoteSessionMetadataKind kind,
    /** Remote session staleness deadline as an ISO 8601 string. */
    @JsonProperty("staleAt") OffsetDateTime staleAt,
    /** Remote session state returned by the backing service. */
    @JsonProperty("state") String state
) {
}
