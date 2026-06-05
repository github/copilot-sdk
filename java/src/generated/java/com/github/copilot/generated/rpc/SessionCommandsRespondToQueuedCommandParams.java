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
 * Queued-command request ID and the result indicating whether the host executed it (and whether to stop processing further queued commands).
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionCommandsRespondToQueuedCommandParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Request ID from the `command.queued` event the host is responding to. */
    @JsonProperty("requestId") String requestId,
    /** Result of the queued command execution. */
    @JsonProperty("result") Object result
) {
}
