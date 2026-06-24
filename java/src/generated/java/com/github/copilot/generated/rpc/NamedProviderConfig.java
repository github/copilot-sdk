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
 * A named BYOK provider connection (transport + credentials).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NamedProviderConfig(
    /** Stable identifier referenced by BYOK model definitions. Must not contain '/'. */
    @JsonProperty("name") String name,
    /** Provider type. Defaults to "openai" for generic OpenAI-compatible APIs. */
    @JsonProperty("type") ProviderConfigType type,
    /** Wire API format (openai/azure only). Defaults to "completions". */
    @JsonProperty("wireApi") ProviderConfigWireApi wireApi,
    /** Provider transport. Defaults to "http". */
    @JsonProperty("transport") ProviderConfigTransport transport,
    /** API endpoint URL. */
    @JsonProperty("baseUrl") String baseUrl,
    /** API key. Optional for local providers like Ollama. */
    @JsonProperty("apiKey") String apiKey,
    /** Bearer token for authentication. Sets the Authorization header directly. Takes precedence over apiKey when both are set. */
    @JsonProperty("bearerToken") String bearerToken,
    /** Azure-specific provider options. */
    @JsonProperty("azure") ProviderConfigAzure azure,
    /** Custom HTTP headers to include in all outbound requests to the provider. */
    @JsonProperty("headers") Map<String, String> headers,
    /** When true, the SDK client supplies bearer tokens on demand: the runtime calls the client-session `providerToken.getToken` callback before each request and applies the returned token as an `Authorization: Bearer <token>` header. This is the bearer/OAuth scheme used by Azure AD / managed-identity tokens and provider OAuth access tokens (including Anthropic's), not a provider-specific API-key header such as Anthropic's `x-api-key`. The token-acquiring function itself stays on the SDK side and is never serialized; only this flag crosses the wire. When set alongside `apiKey`/`bearerToken`, the callback takes precedence: the runtime applies the token returned by `providerToken.getToken` as the `Authorization: Bearer` header for each request and does not send the static credential. */
    @JsonProperty("hasBearerTokenProvider") Boolean hasBearerTokenProvider
) {
}
