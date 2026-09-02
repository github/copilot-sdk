/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Final structured tool completion in the covered event range.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompletionReceiptFinalTool(
    /** Unique identifier of the completed tool call. */
    @JsonProperty("toolCallId") String toolCallId,
    /** Tool name from the matching tool execution start event, when available. */
    @JsonProperty("toolName") String toolName,
    /** Structured success or failure status from the tool completion event. */
    @JsonProperty("status") CompletionReceiptToolStatus status,
    /** Process exit code from a structured shell result, when available. */
    @JsonProperty("exitCode") Long exitCode
) {
}
