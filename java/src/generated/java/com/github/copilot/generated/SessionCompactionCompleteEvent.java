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
 * Session event "session.compaction_complete". Conversation compaction results including success status, metrics, and optional error details
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
        @JsonProperty("preCompactionTokens") Long preCompactionTokens,
        /** Total tokens in conversation after compaction */
        @JsonProperty("postCompactionTokens") Long postCompactionTokens,
        /** Number of messages before compaction */
        @JsonProperty("preCompactionMessagesLength") Long preCompactionMessagesLength,
        /** Number of messages removed during compaction */
        @JsonProperty("messagesRemoved") Long messagesRemoved,
        /** Number of tokens removed during compaction */
        @JsonProperty("tokensRemoved") Long tokensRemoved,
        /** User-supplied focus instructions provided to a manual `/compact` invocation. Omitted for automatic compaction and for manual compaction with no focus text. */
        @JsonProperty("customInstructions") String customInstructions,
        /** LLM-generated summary of the compacted conversation history */
        @JsonProperty("summaryContent") String summaryContent,
        /** Checkpoint snapshot number created for recovery */
        @JsonProperty("checkpointNumber") Long checkpointNumber,
        /** File path where the checkpoint was stored */
        @JsonProperty("checkpointPath") String checkpointPath,
        /** Token usage breakdown for the compaction LLM call (aligned with assistant.usage format) */
        @JsonProperty("compactionTokensUsed") CompactionCompleteCompactionTokensUsed compactionTokensUsed,
        /** GitHub request tracing ID (x-github-request-id header) for the compaction LLM call */
        @JsonProperty("requestId") String requestId,
        /** Copilot service request ID (x-copilot-service-request-id header) for the compaction LLM call */
        @JsonProperty("serviceRequestId") String serviceRequestId,
        /** Token count from system message(s) after compaction */
        @JsonProperty("systemTokens") Long systemTokens,
        /** Token count from non-system messages (user, assistant, tool) after compaction */
        @JsonProperty("conversationTokens") Long conversationTokens,
        /** Token count from tool definitions after compaction */
        @JsonProperty("toolDefinitionsTokens") Long toolDefinitionsTokens
    ) {
    }
}
