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
 * The {@code session.compaction_complete} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCompactionCompleteEvent extends SessionEvent {

    @Override
    public String getType() { return "session.compaction_complete"; }

    @JsonProperty("data")
    private SessionCompactionCompleteEventData data;

    public SessionCompactionCompleteEventData getData() { return data; }
    public void setData(SessionCompactionCompleteEventData data) { this.data = data; }

    /** Data payload for {@link SessionCompactionCompleteEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCompactionCompleteEventData(
        /** Whether compaction completed successfully */
        @JsonProperty("success") Boolean success,
        /** Error message if compaction failed */
        @JsonProperty("error") String error,
        /** Total tokens in conversation before compaction */
        @JsonProperty("preCompactionTokens") Double preCompactionTokens,
        /** Total tokens in conversation after compaction */
        @JsonProperty("postCompactionTokens") Double postCompactionTokens,
        /** Number of messages before compaction */
        @JsonProperty("preCompactionMessagesLength") Double preCompactionMessagesLength,
        /** Number of messages removed during compaction */
        @JsonProperty("messagesRemoved") Double messagesRemoved,
        /** Number of tokens removed during compaction */
        @JsonProperty("tokensRemoved") Double tokensRemoved,
        /** LLM-generated summary of the compacted conversation history */
        @JsonProperty("summaryContent") String summaryContent,
        /** Checkpoint snapshot number created for recovery */
        @JsonProperty("checkpointNumber") Double checkpointNumber,
        /** File path where the checkpoint was stored */
        @JsonProperty("checkpointPath") String checkpointPath,
        /** Token usage breakdown for the compaction LLM call */
        @JsonProperty("compactionTokensUsed") SessionCompactionCompleteEventDataCompactionTokensUsed compactionTokensUsed,
        /** GitHub request tracing ID (x-github-request-id header) for the compaction LLM call */
        @JsonProperty("requestId") String requestId,
        /** Token count from system message(s) after compaction */
        @JsonProperty("systemTokens") Double systemTokens,
        /** Token count from non-system messages (user, assistant, tool) after compaction */
        @JsonProperty("conversationTokens") Double conversationTokens,
        /** Token count from tool definitions after compaction */
        @JsonProperty("toolDefinitionsTokens") Double toolDefinitionsTokens
    ) {

        /** Token usage breakdown for the compaction LLM call */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionCompactionCompleteEventDataCompactionTokensUsed(
            /** Input tokens consumed by the compaction LLM call */
            @JsonProperty("input") Double input,
            /** Output tokens produced by the compaction LLM call */
            @JsonProperty("output") Double output,
            /** Cached input tokens reused in the compaction LLM call */
            @JsonProperty("cachedInput") Double cachedInput
        ) {
        }
    }
}
