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
import javax.annotation.processing.Generated;

/**
 * Derive a credential key through the selected account and store owner.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionMcpOauthStoreKeyParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** MCP endpoint whose credentials the key identifies. */
    @JsonProperty("serverUrl") String serverUrl,
    /** Static OAuth client identity, when configured. */
    @JsonProperty("clientId") String clientId,
    /** Metadata document identity when no static client is configured. */
    @JsonProperty("clientIdMetadataUrl") String clientIdMetadataUrl,
    /** Credential owner. Omission uses the authoritative session store. */
    @JsonProperty("store") Object store,
    /** Preserve hostless credential and account selection. */
    @JsonProperty("sessionless") Boolean sessionless
) {
}
