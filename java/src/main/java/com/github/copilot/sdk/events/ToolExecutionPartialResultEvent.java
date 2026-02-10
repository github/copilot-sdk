/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: tool.execution_partial_result
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ToolExecutionPartialResultEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private ToolExecutionPartialResultData data;

    @Override
    public String getType() {
        return "tool.execution_partial_result";
    }

    public ToolExecutionPartialResultData getData() {
        return data;
    }

    public void setData(ToolExecutionPartialResultData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolExecutionPartialResultData(@JsonProperty("toolCallId") String toolCallId,
            @JsonProperty("partialOutput") String partialOutput) {
    }
}
