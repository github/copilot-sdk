/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Tool execution result on success
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteResult(
    /** Concise tool result text sent to the LLM for chat completion, potentially truncated for token efficiency */
    @JsonProperty("content") String content,
    /** Full detailed tool result for UI/timeline display, preserving complete content such as diffs. Falls back to content when absent. */
    @JsonProperty("detailedContent") String detailedContent,
    /** Structured content blocks (text, images, audio, resources) returned by the tool in their native format */
    @JsonProperty("contents") List<Object> contents
) {
}
