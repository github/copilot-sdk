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
 * Token limits for prompts, outputs, and context window
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelCapabilitiesOverrideLimits(
    @JsonProperty("max_prompt_tokens") Long maxPromptTokens,
    @JsonProperty("max_output_tokens") Long maxOutputTokens,
    /** Maximum total context window size in tokens */
    @JsonProperty("max_context_window_tokens") Long maxContextWindowTokens,
    @JsonProperty("vision") ModelCapabilitiesOverrideLimitsVision vision
) {
}
