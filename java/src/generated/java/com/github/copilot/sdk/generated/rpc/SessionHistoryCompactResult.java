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
    @JsonProperty("tokensRemoved") Long tokensRemoved,
    /** Number of messages removed during compaction */
    @JsonProperty("messagesRemoved") Long messagesRemoved,
    /** Post-compaction context window usage breakdown */
    @JsonProperty("contextWindow") HistoryCompactContextWindow contextWindow
) {
}
