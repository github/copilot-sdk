/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: elicitation.requested
 * <p>
 * Broadcast when the server or an MCP tool requests structured input from the
 * user. Clients that have an elicitation handler should respond via
 * {@code session.ui.handlePendingElicitation}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ElicitationRequestedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private ElicitationRequestedData data;

    @Override
    public String getType() {
        return "elicitation.requested";
    }

    public ElicitationRequestedData getData() {
        return data;
    }

    public void setData(ElicitationRequestedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ElicitationRequestedData(@JsonProperty("requestId") String requestId,
            @JsonProperty("toolCallId") String toolCallId, @JsonProperty("elicitationSource") String elicitationSource,
            @JsonProperty("message") String message, @JsonProperty("mode") String mode,
            @JsonProperty("requestedSchema") ElicitationRequestedSchema requestedSchema,
            @JsonProperty("url") String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ElicitationRequestedSchema(@JsonProperty("type") String type,
            @JsonProperty("properties") Map<String, Object> properties,
            @JsonProperty("required") List<String> required) {
    }
}
