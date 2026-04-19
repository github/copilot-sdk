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
 * Request parameters for the {@code session.tools.handlePendingToolCall} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionToolsHandlePendingToolCallParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Request ID of the pending tool call */
    @JsonProperty("requestId") String requestId,
    /** Tool call result (string or expanded result object) */
    @JsonProperty("result") Object result,
    /** Error message if the tool call failed */
    @JsonProperty("error") String error
) {
}
