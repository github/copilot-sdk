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
 * Token projection for the existing host-delegated provider.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpOauthAuthenticateResult(
    /** Access token from the runtime-owned store. */
    @JsonProperty("accessToken") String accessToken,
    /** Token scheme. Defaults to Bearer. */
    @JsonProperty("tokenType") String tokenType,
    /** Remaining lifetime in seconds when the issuer supplied an expiry. */
    @JsonProperty("expiresIn") Long expiresIn
) {
}
