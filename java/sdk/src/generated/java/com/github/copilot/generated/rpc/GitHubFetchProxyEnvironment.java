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
 * Proxy environment for one authenticated GitHub request.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubFetchProxyEnvironment(
    /** Proxy for HTTP requests. */
    @JsonProperty("httpProxy") String httpProxy,
    /** Proxy for HTTPS requests. */
    @JsonProperty("httpsProxy") String httpsProxy,
    /** Hosts that bypass the proxy. */
    @JsonProperty("noProxy") String noProxy
) {
}
