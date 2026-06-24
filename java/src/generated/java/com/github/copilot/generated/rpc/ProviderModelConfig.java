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
 * A BYOK model definition referencing a named provider.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderModelConfig(
    /** Provider-local model id, unique within its provider. The session-wide selection id (shown in the model list and passed to switchTo) is the provider-qualified `provider/id`. */
    @JsonProperty("id") String id,
    /** Name of the NamedProviderConfig that serves this model. */
    @JsonProperty("provider") String provider,
    /** The model name sent to the provider API for inference. Defaults to `id`. */
    @JsonProperty("wireModel") String wireModel,
    /** Well-known base model id used for behavior/capability/config lookup. Defaults to `id`. */
    @JsonProperty("modelId") String modelId,
    /** Display name for model pickers. Defaults to the provider-qualified selection id (`provider/id`). */
    @JsonProperty("name") String name,
    /** Maximum prompt/input tokens for the model. */
    @JsonProperty("maxPromptTokens") Double maxPromptTokens,
    /** Maximum context window tokens for the model. */
    @JsonProperty("maxContextWindowTokens") Double maxContextWindowTokens,
    /** Maximum output tokens for the model. */
    @JsonProperty("maxOutputTokens") Double maxOutputTokens,
    /** Optional capability overrides (vision, tool_calls, reasoning, etc.). */
    @JsonProperty("capabilities") ModelCapabilitiesOverride capabilities
) {
}
