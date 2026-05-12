/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.OptionalInt;

/**
 * Configuration for a custom API provider (BYOK - Bring Your Own Key).
 * <p>
 * This allows using your own OpenAI, Azure OpenAI, or other compatible API
 * endpoints instead of the default Copilot backend. All setter methods return
 * {@code this} for method chaining.
 *
 * <h2>Example Usage - OpenAI</h2>
 *
 * <pre>{@code
 * var provider = new ProviderConfig().setType("openai").setBaseUrl("https://api.openai.com/v1").setApiKey("sk-...");
 * }</pre>
 *
 * <h2>Example Usage - Azure OpenAI</h2>
 *
 * <pre>{@code
 * var provider = new ProviderConfig().setType("azure")
 * 		.setAzure(new AzureOptions().setEndpoint("https://my-resource.openai.azure.com").setDeployment("gpt-4"));
 * }</pre>
 *
 * @see SessionConfig#setProvider(ProviderConfig)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderConfig {

    @JsonProperty("type")
    private String type;

    @JsonProperty("wireApi")
    private String wireApi;

    @JsonProperty("baseUrl")
    private String baseUrl;

    @JsonProperty("apiKey")
    private String apiKey;

    @JsonProperty("bearerToken")
    private String bearerToken;

    @JsonProperty("azure")
    private AzureOptions azure;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("modelId")
    private String modelId;

    @JsonProperty("wireModel")
    private String wireModel;

    @JsonProperty("maxPromptTokens")
    private Integer maxPromptTokens;

    @JsonProperty("maxOutputTokens")
    private Integer maxOutputTokens;

    /**
     * Gets the provider type.
     *
     * @return the provider type (e.g., "openai", "azure")
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the provider type.
     * <p>
     * Supported types include:
     * <ul>
     * <li>"openai" - OpenAI API</li>
     * <li>"azure" - Azure OpenAI Service</li>
     * </ul>
     *
     * @param type
     *            the provider type
     * @return this config for method chaining
     */
    public ProviderConfig setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Gets the wire API format.
     *
     * @return the wire API format
     */
    public String getWireApi() {
        return wireApi;
    }

    /**
     * Sets the wire API format for custom providers.
     * <p>
     * This specifies the API format when using a custom provider that has a
     * different wire protocol.
     *
     * @param wireApi
     *            the wire API format
     * @return this config for method chaining
     */
    public ProviderConfig setWireApi(String wireApi) {
        this.wireApi = wireApi;
        return this;
    }

    /**
     * Gets the base URL for the API.
     *
     * @return the API base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Sets the base URL for the API.
     * <p>
     * For OpenAI, this is typically "https://api.openai.com/v1".
     *
     * @param baseUrl
     *            the API base URL
     * @return this config for method chaining
     */
    public ProviderConfig setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Gets the API key.
     *
     * @return the API key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the API key for authentication.
     *
     * @param apiKey
     *            the API key
     * @return this config for method chaining
     */
    public ProviderConfig setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    /**
     * Gets the bearer token.
     *
     * @return the bearer token
     */
    public String getBearerToken() {
        return bearerToken;
    }

    /**
     * Sets a bearer token for authentication.
     * <p>
     * This is an alternative to API key authentication.
     * <p>
     * <strong>Note:</strong> The bearer token is a <strong>static token
     * string</strong>. The SDK does not refresh this token automatically. If your
     * token expires, requests will fail and you'll need to create a new session
     * with a fresh token.
     *
     * @param bearerToken
     *            the bearer token
     * @return this config for method chaining
     */
    public ProviderConfig setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
        return this;
    }

    /**
     * Gets the Azure-specific options.
     *
     * @return the Azure options
     */
    public AzureOptions getAzure() {
        return azure;
    }

    /**
     * Sets Azure-specific options for Azure OpenAI Service.
     *
     * @param azure
     *            the Azure options
     * @return this config for method chaining
     * @see AzureOptions
     */
    public ProviderConfig setAzure(AzureOptions azure) {
        this.azure = azure;
        return this;
    }

    /**
     * Gets the custom HTTP headers for outbound provider requests.
     *
     * @return the headers map, or {@code null} if not set
     */
    public Map<String, String> getHeaders() {
        return headers == null ? null : Collections.unmodifiableMap(headers);
    }

    /**
     * Sets custom HTTP headers to include in outbound provider requests.
     * <p>
     * Use this to pass additional authentication headers or custom metadata to the
     * provider API.
     *
     * @param headers
     *            the headers map
     * @return this config for method chaining
     */
    public ProviderConfig setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    /**
     * Gets the well-known model name used by the runtime.
     * <p>
     * Used to look up agent configuration (tools, prompts, reasoning behavior) and
     * default token limits. Also used as the wire model when
     * {@link #getWireModel()} is not set.
     *
     * @return the model ID, or {@code null} if not set
     */
    public String getModelId() {
        return modelId;
    }

    /**
     * Sets the well-known model name used by the runtime.
     * <p>
     * Used to look up agent configuration (tools, prompts, reasoning behavior) and
     * default token limits. Also used as the wire model when
     * {@link #getWireModel()} is not set. Falls back to
     * {@link SessionConfig#getModel()}.
     *
     * @param modelId
     *            the model ID
     * @return this config for method chaining
     */
    public ProviderConfig setModelId(String modelId) {
        this.modelId = modelId;
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
     * Use this when the provider's model name (e.g. an Azure deployment name or a
     * custom fine-tune name) differs from {@link #getModelId()}. Falls back to
     * {@link #getModelId()}, then {@link SessionConfig#getModel()}.
     *
     * @param wireModel
     *            the wire model name
     * @return this config for method chaining
     */
    public ProviderConfig setWireModel(String wireModel) {
        this.wireModel = wireModel;
        return this;
    }

    /**
     * Gets the maximum prompt token override.
     *
     * @return the max prompt tokens, or {@code null} if not set
     */
    @JsonIgnore
    public OptionalInt getMaxPromptTokens() {
        return maxPromptTokens == null ? OptionalInt.empty() : OptionalInt.of(maxPromptTokens);
    }

    /**
     * Sets the maximum prompt tokens override.
     * <p>
     * Overrides the resolved model's default max prompt tokens. The runtime
     * triggers conversation compaction before sending a request when the prompt
     * (system message, history, tool definitions, user message) would exceed this
     * limit.
     *
     * @param maxPromptTokens
     *            the max prompt tokens
     * @return this config for method chaining
     */
    public ProviderConfig setMaxPromptTokens(int maxPromptTokens) {
        this.maxPromptTokens = maxPromptTokens;
        return this;
    }

    /**
     * Clears the maxPromptTokens setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public ProviderConfig clearMaxPromptTokens() {
        this.maxPromptTokens = null;
        return this;
    }

    /**
     * Gets the maximum output token override.
     *
     * @return the max output tokens, or {@code null} if not set
     */
    @JsonIgnore
    public OptionalInt getMaxOutputTokens() {
        return maxOutputTokens == null ? OptionalInt.empty() : OptionalInt.of(maxOutputTokens);
    }

    /**
     * Sets the maximum output tokens override.
     * <p>
     * Overrides the resolved model's default max output tokens. When hit, the model
     * stops generating and returns a truncated response.
     *
     * @param maxOutputTokens
     *            the max output tokens
     * @return this config for method chaining
     */
    public ProviderConfig setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
        return this;
    }

    /**
     * Clears the maxOutputTokens setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public ProviderConfig clearMaxOutputTokens() {
        this.maxOutputTokens = null;
        return this;
    }

}
