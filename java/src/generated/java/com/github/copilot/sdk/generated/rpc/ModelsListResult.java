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
 * Result for the {@code models.list} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelsListResult(
    /** List of available models with full metadata */
    @JsonProperty("models") List<ModelsListResultModelsItem> models
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ModelsListResultModelsItem(
        /** Model identifier (e.g., "claude-sonnet-4.5") */
        @JsonProperty("id") String id,
        /** Display name */
        @JsonProperty("name") String name,
        /** Model capabilities and limits */
        @JsonProperty("capabilities") ModelsListResultModelsItemCapabilities capabilities,
        /** Policy state (if applicable) */
        @JsonProperty("policy") ModelsListResultModelsItemPolicy policy,
        /** Billing information */
        @JsonProperty("billing") ModelsListResultModelsItemBilling billing,
        /** Supported reasoning effort levels (only present if model supports reasoning effort) */
        @JsonProperty("supportedReasoningEfforts") List<String> supportedReasoningEfforts,
        /** Default reasoning effort level (only present if model supports reasoning effort) */
        @JsonProperty("defaultReasoningEffort") String defaultReasoningEffort
    ) {

        /** Model capabilities and limits */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record ModelsListResultModelsItemCapabilities(
            /** Feature flags indicating what the model supports */
            @JsonProperty("supports") ModelsListResultModelsItemCapabilitiesSupports supports,
            /** Token limits for prompts, outputs, and context window */
            @JsonProperty("limits") ModelsListResultModelsItemCapabilitiesLimits limits
        ) {

            /** Feature flags indicating what the model supports */
            @JsonIgnoreProperties(ignoreUnknown = true)
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record ModelsListResultModelsItemCapabilitiesSupports(
                /** Whether this model supports vision/image input */
                @JsonProperty("vision") Boolean vision,
                /** Whether this model supports reasoning effort configuration */
                @JsonProperty("reasoningEffort") Boolean reasoningEffort
            ) {
            }

            /** Token limits for prompts, outputs, and context window */
            @JsonIgnoreProperties(ignoreUnknown = true)
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record ModelsListResultModelsItemCapabilitiesLimits(
                /** Maximum number of prompt/input tokens */
                @JsonProperty("max_prompt_tokens") Double maxPromptTokens,
                /** Maximum number of output/completion tokens */
                @JsonProperty("max_output_tokens") Double maxOutputTokens,
                /** Maximum total context window size in tokens */
                @JsonProperty("max_context_window_tokens") Double maxContextWindowTokens,
                /** Vision-specific limits */
                @JsonProperty("vision") ModelsListResultModelsItemCapabilitiesLimitsVision vision
            ) {

                /** Vision-specific limits */
                @JsonIgnoreProperties(ignoreUnknown = true)
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public record ModelsListResultModelsItemCapabilitiesLimitsVision(
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

        /** Policy state (if applicable) */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record ModelsListResultModelsItemPolicy(
            /** Current policy state for this model */
            @JsonProperty("state") String state,
            /** Usage terms or conditions for this model */
            @JsonProperty("terms") String terms
        ) {
        }

        /** Billing information */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record ModelsListResultModelsItemBilling(
            /** Billing cost multiplier relative to the base rate */
            @JsonProperty("multiplier") Double multiplier
        ) {
        }
    }
}
