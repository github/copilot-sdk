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
 * Token usage breakdown for the compaction LLM call (aligned with assistant.usage format)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompactionCompleteCompactionTokensUsed(
    /** Input tokens consumed by the compaction LLM call */
    @JsonProperty("inputTokens") Double inputTokens,
    /** Output tokens produced by the compaction LLM call */
    @JsonProperty("outputTokens") Double outputTokens,
    /** Cached input tokens reused in the compaction LLM call */
    @JsonProperty("cacheReadTokens") Double cacheReadTokens,
    /** Tokens written to prompt cache in the compaction LLM call */
    @JsonProperty("cacheWriteTokens") Double cacheWriteTokens,
    /** Per-request cost and usage data from the CAPI copilot_usage response field */
    @JsonProperty("copilotUsage") CompactionCompleteCompactionTokensUsedCopilotUsage copilotUsage,
    /** Duration of the compaction LLM call in milliseconds */
    @JsonProperty("duration") Double duration,
    /** Model identifier used for the compaction LLM call */
    @JsonProperty("model") String model
) {
}
