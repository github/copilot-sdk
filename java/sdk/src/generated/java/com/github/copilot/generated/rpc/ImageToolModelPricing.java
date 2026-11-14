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
 * Default-tier text and image token prices reported for an image model, in AI credits per billing batch. Missing rates remain absent; these are not per-image quotes or actual billed usage.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageToolModelPricing(
    /** Number of tokens per billing batch. */
    @JsonProperty("batchSize") Long batchSize,
    /** AI credits per batch of text-input tokens, including a reported zero. */
    @JsonProperty("textInputPrice") Double textInputPrice,
    /** AI credits per batch of text-output tokens, not generated image tokens. */
    @JsonProperty("textOutputPrice") Double textOutputPrice,
    /** Distinct AI credits per batch of image-input tokens, when reported, including zero. */
    @JsonProperty("imageInputPrice") Double imageInputPrice,
    /** Distinct AI credits per batch of image-output tokens, when reported, including zero. */
    @JsonProperty("imageOutputPrice") Double imageOutputPrice
) {
}
