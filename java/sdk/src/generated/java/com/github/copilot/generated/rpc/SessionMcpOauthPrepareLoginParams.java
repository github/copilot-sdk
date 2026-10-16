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
 * Effect-free preparation bound to the existing local session, requester and installation, with frozen options.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpOauthPrepareLoginParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Name recorded by the authoritative owned installation receipt. */
    @JsonProperty("serverName") String serverName,
    /** Exact installation identity from owned inventory, never a server-name alias. */
    @JsonProperty("expectedInstallationId") String expectedInstallationId,
    /** Request a new authorisation rather than accepting a usable cached grant. */
    @JsonProperty("forceReauth") Boolean forceReauth,
    /** Display name used by the incumbent OAuth client-registration flow. */
    @JsonProperty("clientName") String clientName,
    /** Text shown on the loopback callback page after successful authorisation. */
    @JsonProperty("callbackSuccessMessage") String callbackSuccessMessage
) {
}
