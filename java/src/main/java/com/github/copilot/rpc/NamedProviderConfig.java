/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.github.copilot.CopilotExperimental;

/**
 * A named BYOK (Bring Your Own Key) provider connection in the multi-provider
 * registry.
 * <p>
 * Unlike {@link ProviderConfig}, which routes the entire session through a
 * single provider, named providers are additive: the session keeps its default
 * Copilot routing and exposes these providers' models alongside it. Models are
 * attached via {@link ProviderModelConfig}, which references a provider by
 * {@link #getName() name}. All setter methods return {@code this} for method
 * chaining.
 * <p>
 * <strong>Experimental.</strong> Multi-provider BYOK configuration is
 * experimental and may change or be removed in future SDK or CLI releases.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var provider = new NamedProviderConfig().setName("my-openai").setType("openai")
 * 		.setBaseUrl("https://api.openai.com/v1").setApiKey("sk-...");
 * }</pre>
 *
 * @see SessionConfig#setProviders(java.util.List)
 * @see ProviderModelConfig
 * @since 1.0.0
 */
@CopilotExperimental
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NamedProviderConfig {

    @JsonProperty("name")
    private String name;

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

    /**
     * Gets the unique provider name.
     *
     * @return the provider name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique provider name.
     * <p>
     * Referenced by {@link ProviderModelConfig#setProvider(String)} to attach
     * models to this connection.
     *
     * @param name
     *            the provider name
     * @return this config for method chaining
     */
    public NamedProviderConfig setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets the provider type.
     *
     * @return the provider type (e.g., "openai", "azure", "anthropic")
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
     * <li>"anthropic" - Anthropic API</li>
     * </ul>
     *
     * @param type
     *            the provider type
     * @return this config for method chaining
     */
    public NamedProviderConfig setType(String type) {
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
     * Sets the wire API format (openai/azure only).
     * <p>
     * Either "completions" or "responses". Defaults to "completions".
     *
     * @param wireApi
     *            the wire API format
     * @return this config for method chaining
     */
    public NamedProviderConfig setWireApi(String wireApi) {
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
    public NamedProviderConfig setBaseUrl(String baseUrl) {
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
     * Sets the API key for authentication. Optional for local providers like
     * Ollama.
     *
     * @param apiKey
     *            the API key
     * @return this config for method chaining
     */
    public NamedProviderConfig setApiKey(String apiKey) {
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
     * Sets the {@code Authorization} header directly and takes precedence over
     * {@link #setApiKey(String)} when both are set.
     * <p>
     * <strong>Note:</strong> The bearer token is a <strong>static token
     * string</strong>. The SDK does not refresh this token automatically.
     *
     * @param bearerToken
     *            the bearer token
     * @return this config for method chaining
     */
    public NamedProviderConfig setBearerToken(String bearerToken) {
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
    public NamedProviderConfig setAzure(AzureOptions azure) {
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
     *
     * @param headers
     *            the headers map
     * @return this config for method chaining
     */
    public NamedProviderConfig setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }
}
