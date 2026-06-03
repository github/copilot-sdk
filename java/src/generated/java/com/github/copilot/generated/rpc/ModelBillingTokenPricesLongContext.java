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
 * Long context tier pricing (available for models with extended context windows)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelBillingTokenPricesLongContext(
    /** AI Credits cost per billing batch of input tokens */
    @JsonProperty("inputPrice") Double inputPrice,
    /** AI Credits cost per billing batch of output tokens */
    @JsonProperty("outputPrice") Double outputPrice,
    /** AI Credits cost per billing batch of cached tokens */
    @JsonProperty("cachePrice") Double cachePrice,
    /** Prompt token budget (max_prompt_tokens) for the long context tier. The total context window is this value plus the model's max_output_tokens. */
    @JsonProperty("contextMax") Long contextMax
) {
}
