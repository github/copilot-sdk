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
 * Pending command request ID and an optional error if the client handler failed.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionCommandsHandlePendingCommandParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Request ID from the command invocation event */
    @JsonProperty("requestId") String requestId,
    /** Error message if the command handler failed */
    @JsonProperty("error") String error
) {
}
