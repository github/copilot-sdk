/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Custom model-provider configuration (BYOK).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderConfig(
    /** Provider type. Defaults to "openai" for generic OpenAI-compatible APIs. */
    @JsonProperty("type") ProviderConfigType type,
    /** Wire API format (openai/azure only). Defaults to "completions". */
    @JsonProperty("wireApi") ProviderConfigWireApi wireApi,
    /** API endpoint URL. */
    @JsonProperty("baseUrl") String baseUrl,
    /** API key. Optional for local providers like Ollama. */
    @JsonProperty("apiKey") String apiKey,
    /** Bearer token for authentication. Sets the Authorization header directly. Takes precedence over apiKey when both are set. */
    @JsonProperty("bearerToken") String bearerToken,
    /** Azure-specific provider options. */
    @JsonProperty("azure") ProviderConfigAzure azure,
    /** Well-known model ID used for capability lookup. When set, agent behavior config and token limits are inferred from this model. */
    @JsonProperty("modelId") String modelId,
    /** The model identifier sent to the provider API for inference (the "wire" model), as opposed to modelId which is the well-known base. */
    @JsonProperty("wireModel") String wireModel,
    /** Maximum prompt/input tokens for the model. */
    @JsonProperty("maxPromptTokens") Double maxPromptTokens,
    /** Maximum context window tokens for the model. */
    @JsonProperty("maxContextWindowTokens") Double maxContextWindowTokens,
    /** Maximum output tokens for the model. */
    @JsonProperty("maxOutputTokens") Double maxOutputTokens,
    /** Custom HTTP headers to include in all outbound requests to the provider. */
    @JsonProperty("headers") Map<String, String> headers
) {
}
