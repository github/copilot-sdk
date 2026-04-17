/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Request parameters for the {@code session.model.switchTo} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelSwitchToParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Model identifier to switch to */
    @JsonProperty("modelId") String modelId,
    /** Reasoning effort level to use for the model */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Override individual model capabilities resolved by the runtime */
    @JsonProperty("modelCapabilities") SessionModelSwitchToParamsModelCapabilities modelCapabilities
) {

    /** Override individual model capabilities resolved by the runtime */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionModelSwitchToParamsModelCapabilities(
        /** Feature flags indicating what the model supports */
        @JsonProperty("supports") SessionModelSwitchToParamsModelCapabilitiesSupports supports,
        /** Token limits for prompts, outputs, and context window */
        @JsonProperty("limits") SessionModelSwitchToParamsModelCapabilitiesLimits limits
    ) {

        /** Feature flags indicating what the model supports */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionModelSwitchToParamsModelCapabilitiesSupports(
            @JsonProperty("vision") Boolean vision,
            @JsonProperty("reasoningEffort") Boolean reasoningEffort
        ) {
        }

        /** Token limits for prompts, outputs, and context window */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionModelSwitchToParamsModelCapabilitiesLimits(
            @JsonProperty("max_prompt_tokens") Double maxPromptTokens,
            @JsonProperty("max_output_tokens") Double maxOutputTokens,
            /** Maximum total context window size in tokens */
            @JsonProperty("max_context_window_tokens") Double maxContextWindowTokens,
            @JsonProperty("vision") SessionModelSwitchToParamsModelCapabilitiesLimitsVision vision
        ) {

            @JsonIgnoreProperties(ignoreUnknown = true)
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record SessionModelSwitchToParamsModelCapabilitiesLimitsVision(
                /** MIME types the model accepts */
                @JsonProperty("supported_media_types") List<String> supportedMediaTypes,
                /** Maximum number of images per prompt */
                @JsonProperty("max_prompt_images") Double maxPromptImages,
                /** Maximum image size in bytes */
                @JsonProperty("max_prompt_image_size") Double maxPromptImageSize
            ) {
            }
        }
    }
}
