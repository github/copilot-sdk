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
import com.github.copilot.generated.McpOauthWWWAuthenticateParams;
import javax.annotation.processing.Generated;

/**
 * Full host-driven authentication for an arbitrary, possibly unsaved, MCP URL.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionMcpOauthAuthenticateParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Correlation identity reserved through prepareFlow. */
    @JsonProperty("flowId") String flowId,
    /** MCP endpoint, without a requirement for saved configuration. */
    @JsonProperty("serverUrl") String serverUrl,
    /** Use environment session claims for a hostless compatibility call, while retaining this requester's egress authority. */
    @JsonProperty("sessionless") Boolean sessionless,
    /** Credential owner. Omission uses the authoritative session store. */
    @JsonProperty("store") Object store,
    /** Proxy fallback when the session has not published an egress entry. */
    @JsonProperty("sandboxProxy") String sandboxProxy,
    /** Static OAuth client settings for this attempt. */
    @JsonProperty("staticClientConfig") McpOauthFlowStaticClientConfig staticClientConfig,
    /** Bypass cached grants for explicit account selection. */
    @JsonProperty("forceReauth") Boolean forceReauth,
    /** Fail with browser_required when silent authentication cannot complete. */
    @JsonProperty("browserless") Boolean browserless,
    /** Start device authorization instead of a browser flow. */
    @JsonProperty("forceDeviceCode") Boolean forceDeviceCode,
    /** Let the host present the authorization URL without a runtime browser launch. */
    @JsonProperty("skipBrowserOpen") Boolean skipBrowserOpen,
    /** Authorization timeout in milliseconds. Defaults to five minutes. */
    @JsonProperty("authorizationTimeoutMs") Long authorizationTimeoutMs,
    /** Override the device authorization poll interval in milliseconds. */
    @JsonProperty("deviceCodePollIntervalMs") Long deviceCodePollIntervalMs,
    /** Delay before the runtime offers device authorization. */
    @JsonProperty("deviceCodeOfferTimeoutMs") Long deviceCodeOfferTimeoutMs,
    /** Display name for new dynamic client registrations. */
    @JsonProperty("clientName") String clientName,
    /** Client metadata document URL. Omission uses the session identity. */
    @JsonProperty("clientIdMetadataUrl") String clientIdMetadataUrl,
    /** Text displayed after a successful loopback callback. */
    @JsonProperty("callbackSuccessMessage") String callbackSuccessMessage,
    /** Fixed callback port. A busy port fails without fallback. */
    @JsonProperty("redirectPort") Long redirectPort,
    /** Reject the cached access token but permit a refresh grant. */
    @JsonProperty("skipCachedAccessToken") Boolean skipCachedAccessToken,
    /** Preserve previously granted scopes during additional consent. */
    @JsonProperty("includeExistingTokenScopes") Boolean includeExistingTokenScopes,
    /** Preserve replacement policy when another pending flow owns consent. */
    @JsonProperty("preservePendingFlowReplacementFlags") Boolean preservePendingFlowReplacementFlags,
    /** Challenge captured by the host, including an empty challenge. */
    @JsonProperty("wwwAuthenticateParams") McpOauthWWWAuthenticateParams wwwAuthenticateParams,
    /** Raw protected-resource metadata already fetched by the host. */
    @JsonProperty("resourceMetadata") String resourceMetadata
) {
}
