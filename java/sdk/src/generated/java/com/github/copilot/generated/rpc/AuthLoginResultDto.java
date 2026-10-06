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
 * Result of an interactive login flow. Pending consent or account selection is not terminal.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthLoginResultDto(
    /** Current disposition of the login, including pending user decisions. */
    @JsonProperty("status") AuthLoginResultStatus status,
    /** Host that was signed in, when completed. */
    @JsonProperty("host") String host,
    /** Login that was signed in, when completed. */
    @JsonProperty("login") String login,
    /** Available accounts when sign-in is awaiting account selection, ordered with Microsoft 365 first. */
    @JsonProperty("accounts") List<AuthLoginAccount> accounts
) {
}
