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
 * Session event "assistant.server_tool_progress". Live progress signal for a provider-hosted server tool (e.g. hosted web search) while it runs, before the finalized serverTools envelope lands on the terminal assistant.message
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantServerToolProgressEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.server_tool_progress"; }

    @JsonProperty("data")
    private AssistantServerToolProgressEventData data;

    public AssistantServerToolProgressEventData getData() { return data; }
    public void setData(AssistantServerToolProgressEventData data) { this.data = data; }

    /** Data payload for {@link AssistantServerToolProgressEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantServerToolProgressEventData(
        /** Position of the hosted tool call in the response output. Stable across the call's lifecycle events (unlike the provider's per-event item id, which CAPI rotates), so the host keys the live in-progress row on it. */
        @JsonProperty("outputIndex") Long outputIndex,
        /** Kind of hosted server tool that is running. Only `web_search` is emitted today. */
        @JsonProperty("kind") String kind,
        /** Lifecycle status of the hosted call: `in_progress`, `searching`, or `completed`. */
        @JsonProperty("status") String status
    ) {
    }
}
