/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
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
    /** Summary text produced by compaction. Omitted when compaction did not produce a summary (e.g. failure path). */
    @JsonProperty("summaryContent") String summaryContent,
    /** Post-compaction context window usage breakdown */
    @JsonProperty("contextWindow") HistoryCompactContextWindow contextWindow
) {
}
