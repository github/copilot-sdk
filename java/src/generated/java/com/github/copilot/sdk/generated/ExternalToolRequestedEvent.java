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
 * The {@code external_tool.requested} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ExternalToolRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "external_tool.requested"; }

    @JsonProperty("data")
    private ExternalToolRequestedEventData data;

    public ExternalToolRequestedEventData getData() { return data; }
    public void setData(ExternalToolRequestedEventData data) { this.data = data; }

    /** Data payload for {@link ExternalToolRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExternalToolRequestedEventData(
        /** Unique identifier for this request; used to respond via session.respondToExternalTool() */
        @JsonProperty("requestId") String requestId,
        /** Session ID that this external tool request belongs to */
        @JsonProperty("sessionId") String sessionId,
        /** Tool call ID assigned to this external tool invocation */
        @JsonProperty("toolCallId") String toolCallId,
        /** Name of the external tool to invoke */
        @JsonProperty("toolName") String toolName,
        /** Arguments to pass to the external tool */
        @JsonProperty("arguments") Object arguments,
        /** W3C Trace Context traceparent header for the execute_tool span */
        @JsonProperty("traceparent") String traceparent,
        /** W3C Trace Context tracestate header for the execute_tool span */
        @JsonProperty("tracestate") String tracestate
    ) {
    }
}
