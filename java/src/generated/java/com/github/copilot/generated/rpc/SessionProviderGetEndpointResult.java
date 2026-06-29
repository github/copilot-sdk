/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * A snapshot of the provider endpoint the session is currently configured to talk to.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProviderGetEndpointResult(
    /** Provider family. Matches the `type` field of a BYOK provider config. */
    @JsonProperty("type") ProviderEndpointType type,
    /** Wire API to be used, when required for the provider type. */
    @JsonProperty("wireApi") ProviderEndpointWireApi wireApi,
    /** Transport to be used for provider requests. */
    @JsonProperty("transport") ProviderEndpointTransport transport,
    /** Base URL to pass to the LLM client library. */
    @JsonProperty("baseUrl") String baseUrl,
    /** A credential the caller should use with this endpoint. Omitted only when the endpoint accepts unauthenticated requests. */
    @JsonProperty("apiKey") String apiKey,
    /** HTTP headers the caller must include on every outbound request. */
    @JsonProperty("headers") Map<String, String> headers,
    /** Short-lived, rotating credential the caller must send on every request, in addition to `apiKey` if one is present. Omitted when the endpoint does not require one. */
    @JsonProperty("sessionToken") ProviderSessionToken sessionToken
) {
}
