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
 * Session event "assistant.tool_call_delta". Streaming tool-call input delta for incremental tool-call updates
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantToolCallDeltaEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.tool_call_delta"; }

    @JsonProperty("data")
    private AssistantToolCallDeltaEventData data;

    public AssistantToolCallDeltaEventData getData() { return data; }
    public void setData(AssistantToolCallDeltaEventData data) { this.data = data; }

    /** Data payload for {@link AssistantToolCallDeltaEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantToolCallDeltaEventData(
        /** Tool call ID this delta belongs to, matching the corresponding assistant.message tool request */
        @JsonProperty("toolCallId") String toolCallId,
        /** Name of the tool being invoked, when known from the stream */
        @JsonProperty("toolName") String toolName,
        /** Tool call type, when known from the stream */
        @JsonProperty("toolType") AssistantMessageToolRequestType toolType,
        /** Raw provider tool input fragment to append for this tool call. Function/tool-use providers stream serialized JSON argument text (so newlines inside JSON string values may appear as escaped `\n` until the accumulated JSON is parsed); custom tool calls stream raw custom input. */
        @JsonProperty("inputDelta") String inputDelta
    ) {
    }
}
