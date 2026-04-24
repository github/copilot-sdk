/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Result for the {@code session.auth.getStatus} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionAuthGetStatusResult(
    /** Whether the session has resolved authentication */
    @JsonProperty("isAuthenticated") Boolean isAuthenticated,
    /** Authentication type */
    @JsonProperty("authType") AuthInfoType authType,
    /** Authentication host URL */
    @JsonProperty("host") String host,
    /** Authenticated login/username, if available */
    @JsonProperty("login") String login,
    /** Human-readable authentication status description */
    @JsonProperty("statusMessage") String statusMessage,
    /** Copilot plan tier (e.g., individual_pro, business) */
    @JsonProperty("copilotPlan") String copilotPlan
) {
}
