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
 * Post-compaction context window usage breakdown
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HistoryCompactContextWindow(
    /** Maximum token count for the model's context window */
    @JsonProperty("tokenLimit") Long tokenLimit,
    /** Current total tokens in the context window (system + conversation + tool definitions) */
    @JsonProperty("currentTokens") Long currentTokens,
    /** Current number of messages in the conversation */
    @JsonProperty("messagesLength") Long messagesLength,
    /** Token count from system message(s) */
    @JsonProperty("systemTokens") Long systemTokens,
    /** Token count from non-system messages (user, assistant, tool) */
    @JsonProperty("conversationTokens") Long conversationTokens,
    /** Token count from tool definitions */
    @JsonProperty("toolDefinitionsTokens") Long toolDefinitionsTokens
) {
}
