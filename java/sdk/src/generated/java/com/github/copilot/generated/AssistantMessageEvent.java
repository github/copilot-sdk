/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
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
        /** Logical ID of the primary user message that initiated this run, matching the messageId returned by session.send (or the last messageId of session.sendMessages). Stable across model/tool iterations, steering messages, and stop-hook corrections. Subagent runs use their own initiating message ID, not the parent's. Absent for runs without an associated initiating message, such as empty batches. */
        @JsonProperty("originatingMessageId") String originatingMessageId,
        /** Model that produced this assistant message, if known */
        @JsonProperty("model") String model,
        /** Opaque origin provider identity captured at dispatch. Only an exact match with the target provider permits replay of reasoningOpaque, encryptedContent, reasoningBlocks, serverTools, and provider response/item identifiers. Matching model ids or wire protocols is insufficient. Omitted for legacy or unattributed messages; the runtime excludes unproven opaque state when projecting to a known provider. Text content and client tool requests/results remain portable semantic history. */
        @JsonProperty("providerId") String providerId,
        /** The assistant's text response content */
        @JsonProperty("content") String content,
        /** Tool invocations requested by the assistant in this message */
        @JsonProperty("toolRequests") List<AssistantMessageToolRequest> toolRequests,
        /** Opaque/encrypted extended thinking data from Anthropic models. Session-bound and stripped on resume. */
        @JsonProperty("reasoningOpaque") String reasoningOpaque,
        /** Readable reasoning text from the model's extended thinking */
        @JsonProperty("reasoningText") String reasoningText,
        /** OpenAI-compatible wire field the provider used for reasoning (e.g. reasoning_content/reasoning). Populated only when non-canonical, so the dialect round-trips across turns. */
        @JsonProperty("reasoningWireField") String reasoningWireField,
        /** Encrypted reasoning content from OpenAI models. Session-bound and stripped on resume. */
        @JsonProperty("encryptedContent") String encryptedContent,
        /** Generation phase for phased-output models (e.g., thinking vs. response phases) */
        @JsonProperty("phase") String phase,
        /** Zero-based position of this message within its model call's response. Absent when the response was not split into chunks. */
        @JsonProperty("chunkIndex") Long chunkIndex,
        /** Total messages the model call's response was split into, one per reasoning boundary. Absent for a single-message response; the last chunk is the one where chunkIndex is chunkCount - 1. */
        @JsonProperty("chunkCount") Long chunkCount,
        /** Actual output token count from the API response (completion_tokens), used for accurate token accounting */
        @JsonProperty("outputTokens") Long outputTokens,
        /** CAPI interaction ID for correlating this message with upstream telemetry */
        @JsonProperty("interactionId") String interactionId,
        /** GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs */
        @JsonProperty("requestId") String requestId,
        /** Client-minted request id (x-request-id header) echoed by the server. Distinct from requestId (x-github-request-id) and serviceRequestId (x-copilot-service-request-id). */
        @JsonProperty("clientRequestId") String clientRequestId,
        /** Copilot service request ID (x-copilot-service-request-id header) for CAPI log correlation */
        @JsonProperty("serviceRequestId") String serviceRequestId,
        /** Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable. */
        @JsonProperty("rte") Boolean rte,
        /** Provider's completion / response identifier; shared across all chunks of a single API call. Used to group multi-chunk assistant utterances. */
        @JsonProperty("apiCallId") String apiCallId,
        /** Neutral provider-tagged server-side tool-use payload (tool search, advisor) for verbatim round-tripping */
        @JsonProperty("serverTools") AssistantMessageServerTools serverTools,
        /** Neutral provider-tagged reasoning content blocks preserved verbatim for round-tripping. `reasoningText` and `reasoningOpaque` are a lossy derived view of these blocks, retained for display. */
        @JsonProperty("reasoningBlocks") AssistantMessageReasoningBlocks reasoningBlocks,
        /** Identifier for the agent loop turn that produced this message, matching the corresponding assistant.turn_start event */
        @JsonProperty("turnId") String turnId,
        /** Tool call ID of the parent tool invocation when this event originates from a sub-agent */
        @Deprecated
        @JsonProperty("parentToolCallId") String parentToolCallId,
        /** Provider-agnostic citations linking spans of this message's content to the sources that support them. Experimental; only populated when citation emission is enabled. */
        @CopilotExperimental
        @JsonProperty("citations") Citations citations,
        /** Experimental HydraFusion source attribution for this ordinary authoritative assistant message. */
        @CopilotExperimental
        @JsonProperty("fusion") FusionAttribution fusion
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantMessageEventData {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        public static final class Builder {
            private Builder() {}
            private String messageId;
            private String originatingMessageId;
            private String model;
            private String providerId;
            private String content;
            private List<AssistantMessageToolRequest> toolRequests;
            private String reasoningOpaque;
            private String reasoningText;
            private String reasoningWireField;
            private String encryptedContent;
            private String phase;
            private Long chunkIndex;
            private Long chunkCount;
            private Long outputTokens;
            private String interactionId;
            private String requestId;
            private String clientRequestId;
            private String serviceRequestId;
            private Boolean rte;
            private String apiCallId;
            private AssistantMessageServerTools serverTools;
            private AssistantMessageReasoningBlocks reasoningBlocks;
            private String turnId;
            private String parentToolCallId;
            private Citations citations;
            private FusionAttribution fusion;

            /**
             * Sets the {@code messageId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder messageId(String value) {
                this.messageId = value;
                return this;
            }

            /**
             * Sets the {@code originatingMessageId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder originatingMessageId(String value) {
                this.originatingMessageId = value;
                return this;
            }

            /**
             * Sets the {@code model} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder model(String value) {
                this.model = value;
                return this;
            }

            /**
             * Sets the {@code providerId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder providerId(String value) {
                this.providerId = value;
                return this;
            }

            /**
             * Sets the {@code content} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder content(String value) {
                this.content = value;
                return this;
            }

            /**
             * Sets the {@code toolRequests} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolRequests(List<AssistantMessageToolRequest> value) {
                this.toolRequests = value;
                return this;
            }

            /**
             * Sets the {@code reasoningOpaque} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningOpaque(String value) {
                this.reasoningOpaque = value;
                return this;
            }

            /**
             * Sets the {@code reasoningText} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningText(String value) {
                this.reasoningText = value;
                return this;
            }

            /**
             * Sets the {@code reasoningWireField} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningWireField(String value) {
                this.reasoningWireField = value;
                return this;
            }

            /**
             * Sets the {@code encryptedContent} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder encryptedContent(String value) {
                this.encryptedContent = value;
                return this;
            }

            /**
             * Sets the {@code phase} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder phase(String value) {
                this.phase = value;
                return this;
            }

            /**
             * Sets the {@code chunkIndex} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder chunkIndex(Long value) {
                this.chunkIndex = value;
                return this;
            }

            /**
             * Sets the {@code chunkCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder chunkCount(Long value) {
                this.chunkCount = value;
                return this;
            }

            /**
             * Sets the {@code outputTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder outputTokens(Long value) {
                this.outputTokens = value;
                return this;
            }

            /**
             * Sets the {@code interactionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder interactionId(String value) {
                this.interactionId = value;
                return this;
            }

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestId(String value) {
                this.requestId = value;
                return this;
            }

            /**
             * Sets the {@code clientRequestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder clientRequestId(String value) {
                this.clientRequestId = value;
                return this;
            }

            /**
             * Sets the {@code serviceRequestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder serviceRequestId(String value) {
                this.serviceRequestId = value;
                return this;
            }

            /**
             * Sets the {@code rte} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder rte(Boolean value) {
                this.rte = value;
                return this;
            }

            /**
             * Sets the {@code apiCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder apiCallId(String value) {
                this.apiCallId = value;
                return this;
            }

            /**
             * Sets the {@code serverTools} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder serverTools(AssistantMessageServerTools value) {
                this.serverTools = value;
                return this;
            }

            /**
             * Sets the {@code reasoningBlocks} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningBlocks(AssistantMessageReasoningBlocks value) {
                this.reasoningBlocks = value;
                return this;
            }

            /**
             * Sets the {@code turnId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder turnId(String value) {
                this.turnId = value;
                return this;
            }

            /**
             * Sets the {@code parentToolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder parentToolCallId(String value) {
                this.parentToolCallId = value;
                return this;
            }

            /**
             * Sets the {@code citations} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder citations(Citations value) {
                this.citations = value;
                return this;
            }

            /**
             * Sets the {@code fusion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fusion(FusionAttribution value) {
                this.fusion = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantMessageEventData build() {
                if (messageId == null) throw new IllegalStateException("Missing required property: messageId");
                if (content == null) throw new IllegalStateException("Missing required property: content");
                return new AssistantMessageEventData(messageId, originatingMessageId, model, providerId, content, toolRequests, reasoningOpaque, reasoningText, reasoningWireField, encryptedContent, phase, chunkIndex, chunkCount, outputTokens, interactionId, requestId, clientRequestId, serviceRequestId, rte, apiCallId, serverTools, reasoningBlocks, turnId, parentToolCallId, citations, fusion);
            }
        }
    }
}
