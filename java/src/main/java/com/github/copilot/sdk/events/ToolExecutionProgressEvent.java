/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event fired when a tool execution reports progress.
 * <p>
 * This event provides progress updates during tool execution.
 *
 * @since 1.0.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ToolExecutionProgressEvent extends AbstractSessionEvent {

    public static final String TYPE = "tool.execution_progress";

    @JsonProperty("data")
    private ToolExecutionProgressData data;

    @Override
    public String getType() {
        return TYPE;
    }

    public ToolExecutionProgressData getData() {
        return data;
    }

    public void setData(ToolExecutionProgressData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolExecutionProgressData(@JsonProperty("toolCallId") String toolCallId,
            @JsonProperty("progressMessage") String progressMessage) {
    }
}
