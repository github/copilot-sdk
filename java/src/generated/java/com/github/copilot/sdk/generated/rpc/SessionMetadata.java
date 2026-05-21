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
 * Schema for the `SessionMetadata` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadata(
    /** Stable session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Session creation time as an ISO 8601 timestamp */
    @JsonProperty("startTime") String startTime,
    /** Last-modified time of the session's persisted state, as ISO 8601 */
    @JsonProperty("modifiedTime") String modifiedTime,
    /** Short summary of the session, when one has been derived */
    @JsonProperty("summary") String summary,
    /** Optional human-friendly name set via /rename */
    @JsonProperty("name") String name,
    /** True for remote (GitHub) sessions; false for local */
    @JsonProperty("isRemote") Boolean isRemote,
    /** Schema for the `SessionContext` type. */
    @JsonProperty("context") SessionContext context,
    /** GitHub task ID, when this local session is bound to one. Only present for local sessions exported to remote control. */
    @JsonProperty("mcTaskId") String mcTaskId
) {
}
