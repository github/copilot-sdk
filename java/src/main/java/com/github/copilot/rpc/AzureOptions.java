/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Azure OpenAI-specific configuration options.
 * <p>
 * When using a BYOK (Bring Your Own Key) setup with Azure OpenAI, this class
 * allows you to specify Azure-specific settings such as the API version to use.
 * When no API version is set, the runtime uses the GA versionless v1 route.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var provider = new ProviderConfig().setType("azure-openai").setHost("your-resource.openai.azure.com")
 * 		.setApiKey("your-api-key").setAzure(new AzureOptions().setApiVersion("2024-02-01"));
 * }</pre>
 *
 * @see ProviderConfig#setAzure(AzureOptions)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AzureOptions {

    @JsonProperty("apiVersion")
    private String apiVersion;

    /**
     * Gets the Azure OpenAI API version.
     *
     * @return the API version string, or {@code null} to use the GA versionless v1
     *         route
     */
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * Sets the Azure OpenAI API version to use.
     * <p>
     * Examples: {@code "2024-02-01"}, {@code "2023-12-01-preview"} When this option
     * is not set, the runtime uses the GA versionless v1 route.
     *
     * @param apiVersion
     *            the API version string
     * @return this options object for method chaining
     */
    public AzureOptions setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }
}
