/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The {@code tool.execution_progress} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolExecutionProgressEvent extends SessionEvent {

    @Override
    public String getType() { return "tool.execution_progress"; }

    @JsonProperty("data")
    private ToolExecutionProgressEventData data;

    public ToolExecutionProgressEventData getData() { return data; }
    public void setData(ToolExecutionProgressEventData data) { this.data = data; }

    /** Data payload for {@link ToolExecutionProgressEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolExecutionProgressEventData(
        /** Tool call ID this progress notification belongs to */
        @JsonProperty("toolCallId") String toolCallId,
        /** Human-readable progress status message (e.g., from an MCP server) */
        @JsonProperty("progressMessage") String progressMessage
    ) {
    }
}
