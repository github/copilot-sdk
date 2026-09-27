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
 * Neutral authentication status summary.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthStatusDto(
    /** Whether the session has resolved authentication. */
    @JsonProperty("isAuthenticated") Boolean isAuthenticated,
    /** Active account login, if authenticated. */
    @JsonProperty("activeLogin") String activeLogin,
    /** Active account host, if authenticated. */
    @JsonProperty("activeHost") String activeHost,
    /** Copilot plan tier of the active account, if known. */
    @JsonProperty("copilotPlan") String copilotPlan,
    /** Number of signed-in accounts in the roster. */
    @JsonProperty("accountCount") Long accountCount
) {
}
