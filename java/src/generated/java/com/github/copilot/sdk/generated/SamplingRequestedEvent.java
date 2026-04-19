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
 * The {@code sampling.requested} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SamplingRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "sampling.requested"; }

    @JsonProperty("data")
    private SamplingRequestedEventData data;

    public SamplingRequestedEventData getData() { return data; }
    public void setData(SamplingRequestedEventData data) { this.data = data; }

    /** Data payload for {@link SamplingRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SamplingRequestedEventData(
        /** Unique identifier for this sampling request; used to respond via session.respondToSampling() */
        @JsonProperty("requestId") String requestId,
        /** Name of the MCP server that initiated the sampling request */
        @JsonProperty("serverName") String serverName,
        /** The JSON-RPC request ID from the MCP protocol */
        @JsonProperty("mcpRequestId") Object mcpRequestId
    ) {
    }
}
