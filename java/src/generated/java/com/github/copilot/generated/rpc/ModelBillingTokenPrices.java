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
    /** Price per billing batch of input tokens in nano-AIUs (1 nano-AIU = 0.000000001 AIU, 1 AIU = $0.01 USD) */
    @JsonProperty("inputPrice") Long inputPrice,
    /** Price per billing batch of output tokens in nano-AIUs (1 nano-AIU = 0.000000001 AIU, 1 AIU = $0.01 USD) */
    @JsonProperty("outputPrice") Long outputPrice,
    /** Price per billing batch of cached tokens in nano-AIUs (1 nano-AIU = 0.000000001 AIU, 1 AIU = $0.01 USD) */
    @JsonProperty("cachePrice") Long cachePrice,
    /** Number of tokens per standard billing batch */
    @JsonProperty("batchSize") Long batchSize
) {
}
