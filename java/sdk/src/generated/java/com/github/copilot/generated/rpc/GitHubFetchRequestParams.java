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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * A GitHub API request without caller-supplied auth identity or credential fields.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubFetchRequestParams(
    /** A fresh request id. */
    @JsonProperty("requestId") Long requestId,
    /** Session whose auth manager to use. Omission selects the process manager. */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque selector for a live host-owned credential binding. Only the owning host connection may use it. */
    @JsonProperty("authOwnerId") String authOwnerId,
    /** HTTP method. */
    @JsonProperty("method") String method,
    /** Absolute URL on the resolved GitHub API origin. */
    @JsonProperty("url") String url,
    /** Caller-supplied HTTP headers. */
    @JsonProperty("headers") List<GitHubFetchHeader> headers,
    /** Request bytes encoded as base64. Absent for a request without a body. */
    @JsonProperty("bodyBase64") String bodyBase64,
    /** API label used in runtime diagnostics. */
    @JsonProperty("apiName") String apiName,
    /** Optional retry overrides. */
    @JsonProperty("retryOptions") GitHubFetchRetryConfig retryOptions,
    /** Redirect behavior. Defaults to following redirects. */
    @JsonProperty("redirect") String redirect,
    /** Total request timeout in milliseconds. */
    @JsonProperty("timeoutMs") Long timeoutMs,
    /** Response read timeout in milliseconds. Defaults to 700000. */
    @JsonProperty("readTimeoutMs") Long readTimeoutMs,
    /** Maximum response body size in bytes. */
    @JsonProperty("bodyLimitBytes") Long bodyLimitBytes,
    /** Whether the request bypasses idle connections. */
    @JsonProperty("disableConnectionReuse") Boolean disableConnectionReuse,
    /** Explicit proxy environment. Omission uses the process environment. */
    @JsonProperty("proxyEnv") GitHubFetchProxyEnvironment proxyEnv,
    /** Addresses to pin for this request, without another DNS lookup. */
    @JsonProperty("pinnedAddresses") List<GitHubFetchPinnedAddress> pinnedAddresses,
    /** Whether the request permits loopback addresses. Defaults to true. */
    @JsonProperty("allowLocalhost") Boolean allowLocalhost,
    /** Whether the proxy uses Negotiate authentication. */
    @JsonProperty("proxyNegotiateAuth") Boolean proxyNegotiateAuth,
    /** Optional service principal for proxy Negotiate authentication. */
    @JsonProperty("proxyNegotiateSpn") String proxyNegotiateSpn
) {
}
