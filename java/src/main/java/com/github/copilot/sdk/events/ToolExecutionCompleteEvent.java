/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/**
 * Event: tool.execution_complete
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ToolExecutionCompleteEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private ToolExecutionCompleteData data;

    @Override
    public String getType() {
        return "tool.execution_complete";
    }

    public ToolExecutionCompleteData getData() {
        return data;
    }

    public void setData(ToolExecutionCompleteData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolExecutionCompleteData(@JsonProperty("toolCallId") String toolCallId,
            @JsonProperty("success") boolean success, @JsonProperty("isUserRequested") Boolean isUserRequested,
            @JsonProperty("result") Result result, @JsonProperty("error") Error error,
            @JsonProperty("toolTelemetry") Map<String, Object> toolTelemetry,
            @JsonProperty("parentToolCallId") String parentToolCallId) {

        /** Returns a defensive copy of the tool telemetry map. */
        @Override
        public Map<String, Object> toolTelemetry() {
            return toolTelemetry == null ? null : Collections.unmodifiableMap(toolTelemetry);
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Result(@JsonProperty("content") String content,
                @JsonProperty("detailedContent") String detailedContent) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Error(@JsonProperty("message") String message, @JsonProperty("code") String code) {
        }
    }
}
