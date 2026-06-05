/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "assistant.message". Assistant response containing text content, optional tool requests, and interaction metadata
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantMessageEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.message"; }

    @JsonProperty("data")
    private AssistantMessageEventData data;

    public AssistantMessageEventData getData() { return data; }
    public void setData(AssistantMessageEventData data) { this.data = data; }

    /** Data payload for {@link AssistantMessageEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantMessageEventData(
        /** Unique identifier for this assistant message */
        @JsonProperty("messageId") String messageId,
        /** Model that produced this assistant message, if known */
        @JsonProperty("model") String model,
        /** The assistant's text response content */
        @JsonProperty("content") String content,
        /** Tool invocations requested by the assistant in this message */
        @JsonProperty("toolRequests") List<AssistantMessageToolRequest> toolRequests,
        /** Opaque/encrypted extended thinking data from Anthropic models. Session-bound and stripped on resume. */
        @JsonProperty("reasoningOpaque") String reasoningOpaque,
        /** Readable reasoning text from the model's extended thinking */
        @JsonProperty("reasoningText") String reasoningText,
        /** Encrypted reasoning content from OpenAI models. Session-bound and stripped on resume. */
        @JsonProperty("encryptedContent") String encryptedContent,
        /** Generation phase for phased-output models (e.g., thinking vs. response phases) */
        @JsonProperty("phase") String phase,
        /** Actual output token count from the API response (completion_tokens), used for accurate token accounting */
        @JsonProperty("outputTokens") Long outputTokens,
        /** CAPI interaction ID for correlating this message with upstream telemetry */
        @JsonProperty("interactionId") String interactionId,
        /** GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs */
        @JsonProperty("requestId") String requestId,
        /** Copilot service request ID (x-copilot-service-request-id header) for CAPI log correlation */
        @JsonProperty("serviceRequestId") String serviceRequestId,
        /** Raw Anthropic content array with advisor blocks (server_tool_use, advisor_tool_result) for verbatim round-tripping */
        @JsonProperty("anthropicAdvisorBlocks") List<Object> anthropicAdvisorBlocks,
        /** Anthropic advisor model ID used for this response, for timeline display on replay */
        @JsonProperty("anthropicAdvisorModel") String anthropicAdvisorModel,
        /** Identifier for the agent loop turn that produced this message, matching the corresponding assistant.turn_start event */
        @JsonProperty("turnId") String turnId,
        /** Tool call ID of the parent tool invocation when this event originates from a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId
    ) {
    }
}
