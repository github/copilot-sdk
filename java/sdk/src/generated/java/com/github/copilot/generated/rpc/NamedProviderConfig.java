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
 * External SDK input for a named custom model provider. Ingested by the native protocol boundary before host dispatch.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NamedProviderConfig(
    /** Unique provider name used to qualify model selection IDs. */
    @JsonProperty("name") String name,
    /** Provider protocol family. */
    @JsonProperty("type") ProviderConfigType type,
    /** Wire API used to communicate with the provider. */
    @JsonProperty("wireApi") ProviderConfigWireApi wireApi,
    /** Transport used to communicate with the provider. */
    @JsonProperty("transport") ProviderConfigTransport transport,
    /** Base URL for provider API requests. */
    @JsonProperty("baseUrl") String baseUrl,
    /** Static API key used to authenticate provider requests. */
    @JsonProperty("apiKey") String apiKey,
    /** Static bearer token used to authenticate provider requests. */
    @JsonProperty("bearerToken") String bearerToken,
    /** Azure authentication configuration for the provider. */
    @JsonProperty("azure") ProviderConfigAzure azure,
    /** Additional HTTP headers included with provider requests. */
    @JsonProperty("headers") Map<String, String> headers,
    /** Whether the host supplies bearer tokens dynamically. */
    @JsonProperty("hasBearerTokenProvider") Boolean hasBearerTokenProvider
) {
}
