/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
}
