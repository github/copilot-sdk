/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.OptionalInt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.github.copilot.CopilotExperimental;

/**
 * A BYOK (Bring Your Own Key) model definition in the multi-provider registry.
 * <p>
 * References a {@link NamedProviderConfig} by {@link #getProvider() provider}
 * and becomes selectable under the provider-qualified id {@code provider/id}.
 * All setter methods return {@code this} for method chaining.
 * <p>
 * <strong>Experimental.</strong> Multi-provider BYOK configuration is
 * experimental and may change or be removed in future SDK or CLI releases.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var model = new ProviderModelConfig().setId("gpt-x").setProvider("my-openai").setWireModel("gpt-x-2025");
 * }</pre>
 *
 * @see SessionConfig#setModels(java.util.List)
 * @see NamedProviderConfig
 * @since 1.0.0
 */
@CopilotExperimental
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderModelConfig {

    @JsonProperty("id")
    private String id;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("wireModel")
    private String wireModel;

    @JsonProperty("modelId")
    private String modelId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("maxPromptTokens")
    private Integer maxPromptTokens;

    @JsonProperty("maxContextWindowTokens")
    private Integer maxContextWindowTokens;

    @JsonProperty("maxOutputTokens")
    private Integer maxOutputTokens;

    @JsonProperty("capabilities")
    private ModelCapabilitiesOverride capabilities;

    /**
     * Gets the model identifier.
     *
     * @return the model id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the model identifier, unique within its provider.
     * <p>
     * Combined with {@link #getProvider() provider} to form the selection id
     * {@code provider/id}.
     *
     * @param id
     *            the model id
     * @return this config for method chaining
     */
    public ProviderModelConfig setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Gets the name of the provider this model is served by.
     *
     * @return the provider name
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Sets the name of the {@link NamedProviderConfig} this model is served by.
     *
     * @param provider
     *            the provider name
     * @return this config for method chaining
     */
    public ProviderModelConfig setProvider(String provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Gets the model name sent to the provider API for inference.
     *
     * @return the wire model name, or {@code null} if not set
     */
    public String getWireModel() {
        return wireModel;
    }

    /**
     * Sets the model name sent to the provider API for inference.
     * <p>
     * Use this when the provider's model name differs from {@link #getId() id}.
     *
     * @param wireModel
     *            the wire model name
     * @return this config for method chaining
     */
    public ProviderModelConfig setWireModel(String wireModel) {
        this.wireModel = wireModel;
        return this;
    }

    /**
     * Gets the well-known model ID used to look up agent config and default token
     * limits.
     *
     * @return the model ID, or {@code null} if not set
     */
    public String getModelId() {
        return modelId;
    }

    /**
     * Sets the well-known model ID used to look up agent config and default token
     * limits.
     *
     * @param modelId
     *            the model ID
     * @return this config for method chaining
     */
    public ProviderModelConfig setModelId(String modelId) {
        this.modelId = modelId;
        return this;
    }

    /**
     * Gets the human-readable display name.
     *
     * @return the display name, or {@code null} if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the human-readable display name.
     *
     * @param name
     *            the display name
     * @return this config for method chaining
     */
    public ProviderModelConfig setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets the maximum prompt token override.
     *
     * @return an {@link java.util.OptionalInt} containing the max prompt tokens, or
     *         {@link java.util.OptionalInt#empty()} if not set
     */
    @JsonIgnore
    public OptionalInt getMaxPromptTokens() {
        return maxPromptTokens == null ? OptionalInt.empty() : OptionalInt.of(maxPromptTokens);
    }

    /**
     * Sets the maximum prompt tokens override.
     *
     * @param maxPromptTokens
     *            the max prompt tokens
     * @return this config for method chaining
     */
    public ProviderModelConfig setMaxPromptTokens(int maxPromptTokens) {
        this.maxPromptTokens = maxPromptTokens;
        return this;
    }

    /**
     * Clears the maxPromptTokens setting, reverting to the default behavior.
     *
     * @return this config for method chaining
     */
    public ProviderModelConfig clearMaxPromptTokens() {
        this.maxPromptTokens = null;
        return this;
    }

    /**
     * Gets the maximum context window token override.
     *
     * @return an {@link java.util.OptionalInt} containing the max context window
     *         tokens, or {@link java.util.OptionalInt#empty()} if not set
     */
    @JsonIgnore
    public OptionalInt getMaxContextWindowTokens() {
        return maxContextWindowTokens == null ? OptionalInt.empty() : OptionalInt.of(maxContextWindowTokens);
    }

    /**
     * Sets the maximum context window tokens override.
     *
     * @param maxContextWindowTokens
     *            the max context window tokens
     * @return this config for method chaining
     */
    public ProviderModelConfig setMaxContextWindowTokens(int maxContextWindowTokens) {
        this.maxContextWindowTokens = maxContextWindowTokens;
        return this;
    }

    /**
     * Clears the maxContextWindowTokens setting, reverting to the default behavior.
     *
     * @return this config for method chaining
     */
    public ProviderModelConfig clearMaxContextWindowTokens() {
        this.maxContextWindowTokens = null;
        return this;
    }

    /**
     * Gets the maximum output token override.
     *
     * @return an {@link java.util.OptionalInt} containing the max output tokens, or
     *         {@link java.util.OptionalInt#empty()} if not set
     */
    @JsonIgnore
    public OptionalInt getMaxOutputTokens() {
        return maxOutputTokens == null ? OptionalInt.empty() : OptionalInt.of(maxOutputTokens);
    }

    /**
     * Sets the maximum output tokens override.
     *
     * @param maxOutputTokens
     *            the max output tokens
     * @return this config for method chaining
     */
    public ProviderModelConfig setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
        return this;
    }

    /**
     * Clears the maxOutputTokens setting, reverting to the default behavior.
     *
     * @return this config for method chaining
     */
    public ProviderModelConfig clearMaxOutputTokens() {
        this.maxOutputTokens = null;
        return this;
    }

    /**
     * Gets the per-property model capability overrides.
     *
     * @return the capabilities override, or {@code null} if not set
     */
    public ModelCapabilitiesOverride getCapabilities() {
        return capabilities;
    }

    /**
     * Sets per-property model capability overrides, deep-merged over runtime
     * defaults.
     *
     * @param capabilities
     *            the capabilities override
     * @return this config for method chaining
     */
    public ProviderModelConfig setCapabilities(ModelCapabilitiesOverride capabilities) {
        this.capabilities = capabilities;
        return this;
    }
}
