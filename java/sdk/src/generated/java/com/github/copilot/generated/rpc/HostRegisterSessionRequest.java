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
 * Listener-scoped registration, not a copy or durable adoption of a session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record HostRegisterSessionRequest(
    /** Canonical ID of the existing resident runtime session. */
    @JsonProperty("sessionId") String sessionId,
    /** Absolute working directory of the resident session. */
    @JsonProperty("workingDirectory") String workingDirectory,
    /** Additional directories already granted to the resident session. */
    @JsonProperty("additionalDirectories") List<String> additionalDirectories,
    /** Current display title of the resident session, when available. */
    @JsonProperty("title") String title,
    /** Session creation time in milliseconds since the Unix epoch, when available. */
    @JsonProperty("createdAtUnixMs") Long createdAtUnixMs,
    /** Last session modification time in milliseconds since the Unix epoch, when available. */
    @JsonProperty("modifiedAtUnixMs") Long modifiedAtUnixMs
) {
}
