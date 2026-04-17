/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Result for the {@code session.history.compact} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionHistoryCompactResult(
    /** Whether compaction completed successfully */
    @JsonProperty("success") Boolean success,
    /** Number of tokens freed by compaction */
    @JsonProperty("tokensRemoved") Double tokensRemoved,
    /** Number of messages removed during compaction */
    @JsonProperty("messagesRemoved") Double messagesRemoved,
    /** Post-compaction context window usage breakdown */
    @JsonProperty("contextWindow") SessionHistoryCompactResultContextWindow contextWindow
) {

    /** Post-compaction context window usage breakdown */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionHistoryCompactResultContextWindow(
        /** Maximum token count for the model's context window */
        @JsonProperty("tokenLimit") Double tokenLimit,
        /** Current total tokens in the context window (system + conversation + tool definitions) */
        @JsonProperty("currentTokens") Double currentTokens,
        /** Current number of messages in the conversation */
        @JsonProperty("messagesLength") Double messagesLength,
        /** Token count from system message(s) */
        @JsonProperty("systemTokens") Double systemTokens,
        /** Token count from non-system messages (user, assistant, tool) */
        @JsonProperty("conversationTokens") Double conversationTokens,
        /** Token count from tool definitions */
        @JsonProperty("toolDefinitionsTokens") Double toolDefinitionsTokens
    ) {
    }
}
