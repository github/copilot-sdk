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
 * Token usage breakdown
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShutdownModelMetricUsage(
    /** Total input tokens consumed across all requests to this model */
    @JsonProperty("inputTokens") Long inputTokens,
    /** Total output tokens produced across all requests to this model */
    @JsonProperty("outputTokens") Long outputTokens,
    /** Total tokens read from prompt cache across all requests */
    @JsonProperty("cacheReadTokens") Long cacheReadTokens,
    /** Total tokens written to prompt cache across all requests */
    @JsonProperty("cacheWriteTokens") Long cacheWriteTokens,
    /** Total reasoning tokens produced across all requests to this model */
    @JsonProperty("reasoningTokens") Long reasoningTokens
) {
}
