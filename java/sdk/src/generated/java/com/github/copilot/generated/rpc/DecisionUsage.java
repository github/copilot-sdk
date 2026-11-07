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
 * Token usage the backend reported for one request.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionUsage(
    /** Input tokens, including cached and cache-write tokens. */
    @JsonProperty("inputTokens") Long inputTokens,
    /** Input tokens served from the backend's prompt cache, when reported. */
    @JsonProperty("cachedInputTokens") Long cachedInputTokens,
    /** Input tokens written to the backend's prompt cache, when reported. */
    @JsonProperty("cacheWriteTokens") Long cacheWriteTokens,
    /** Output tokens, including reasoning tokens. */
    @JsonProperty("outputTokens") Long outputTokens,
    /** Reasoning tokens, when reported. */
    @JsonProperty("reasoningTokens") Long reasoningTokens
) {
}
