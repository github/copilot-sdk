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
 * Parameters for shutting down the session
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionShutdownParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Why the session is being shut down. Defaults to "routine" when omitted. */
    @JsonProperty("type") ShutdownType type,
    /** Optional human-readable reason. Typically the message of the error that triggered shutdown when type is 'error'. */
    @JsonProperty("reason") String reason
) {
}
