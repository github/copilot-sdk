/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Server liveness response, including the echoed message, current server timestamp, and protocol version.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PingResult(
    /** Echoed message (or default greeting) */
    @JsonProperty("message") String message,
    /** ISO 8601 timestamp when the server handled the ping */
    @JsonProperty("timestamp") OffsetDateTime timestamp,
    /** Server protocol version number */
    @JsonProperty("protocolVersion") Long protocolVersion
) {
}
