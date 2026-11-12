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
 * A credential-free account choice after sign-in.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthLoginAccount(
    /** Opaque identifier supplied to the next login step to select this account. */
    @JsonProperty("selectionId") String selectionId,
    /** Host coordinate owned by the selected account's provider. */
    @JsonProperty("host") String host,
    /** Human-readable login for the account choice. */
    @JsonProperty("login") String login,
    /** Provider kind that owns this account choice. */
    @JsonProperty("kind") AccountKind kind
) {
}
