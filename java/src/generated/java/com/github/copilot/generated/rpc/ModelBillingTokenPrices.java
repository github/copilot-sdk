/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Token-level pricing information for this model
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelBillingTokenPrices(
    /** AI Credits cost per billing batch of input tokens */
    @JsonProperty("inputPrice") Double inputPrice,
    /** AI Credits cost per billing batch of output tokens */
    @JsonProperty("outputPrice") Double outputPrice,
    /** Deprecated: use cacheReadPrice. AI Credits cost per billing batch of cached tokens */
    @JsonProperty("cachePrice") Double cachePrice,
    /** AI Credits cost per billing batch of cached (read) tokens */
    @JsonProperty("cacheReadPrice") Double cacheReadPrice,
    /** AI Credits cost per billing batch of cache-write (cache creation) tokens. */
    @JsonProperty("cacheWritePrice") Double cacheWritePrice,
    /** Number of tokens per standard billing batch */
    @JsonProperty("batchSize") Long batchSize,
    /** Deprecated: use maxPromptTokens. Prompt token budget for the default tier. The total context window is this value plus the model's max_output_tokens. */
    @JsonProperty("contextMax") Long contextMax,
    /** Prompt token budget for the default tier. The total context window is this value plus the model's max_output_tokens. */
    @JsonProperty("maxPromptTokens") Long maxPromptTokens,
    /** Long context tier pricing (available for models with extended context windows) */
    @JsonProperty("longContext") ModelBillingTokenPricesLongContext longContext
) {
}
