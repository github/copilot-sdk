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
 * Request ID of a pending `sampling.requested` event and an optional sampling result payload (omit to reject).
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUiHandlePendingSamplingParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The unique request ID from the sampling.requested event */
    @JsonProperty("requestId") String requestId,
    /** Optional sampling result payload. Omit to reject/cancel the sampling request without providing a result. */
    @JsonProperty("response") UIHandlePendingSamplingResponse response
) {
}
