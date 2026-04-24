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
 * Request parameters for the {@code session.log} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionLogParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Human-readable message */
    @JsonProperty("message") String message,
    /** Log severity level. Determines how the message is displayed in the timeline. Defaults to "info". */
    @JsonProperty("level") SessionLogLevel level,
    /** When true, the message is transient and not persisted to the session event log on disk */
    @JsonProperty("ephemeral") Boolean ephemeral,
    /** Optional URL the user can open in their browser for more details */
    @JsonProperty("url") String url
) {
}
