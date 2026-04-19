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
 * The {@code tool.execution_partial_result} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolExecutionPartialResultEvent extends SessionEvent {

    @Override
    public String getType() { return "tool.execution_partial_result"; }

    @JsonProperty("data")
    private ToolExecutionPartialResultEventData data;

    public ToolExecutionPartialResultEventData getData() { return data; }
    public void setData(ToolExecutionPartialResultEventData data) { this.data = data; }

    /** Data payload for {@link ToolExecutionPartialResultEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolExecutionPartialResultEventData(
        /** Tool call ID this partial result belongs to */
        @JsonProperty("toolCallId") String toolCallId,
        /** Incremental output chunk from the running tool */
        @JsonProperty("partialOutput") String partialOutput
    ) {
    }
}
