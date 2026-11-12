/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Short-lived, rotating credential the caller must send on every request, in addition to `apiKey` if one is present. Omitted when the endpoint does not require one.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderSessionToken(
    /** The short-lived token value. */
    @JsonProperty("token") String token,
    /** HTTP header name the token must be sent under. */
    @JsonProperty("header") String header,
    /** The model the token is bound to, when applicable. When set, the token is only valid for requests against this model. */
    @JsonProperty("model") String model,
    /** When the token expires, if known. Callers should refresh by calling `getEndpoint` again before this time, or reactively on any 401/403 response from `baseUrl`. */
    @JsonProperty("expiresAt") OffsetDateTime expiresAt
) {
}
