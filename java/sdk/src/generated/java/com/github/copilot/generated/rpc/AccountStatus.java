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
 * One signed-in account in the roster forest.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountStatus(
    /** Authentication host URL. */
    @JsonProperty("host") String host,
    /** Authenticated login/username. */
    @JsonProperty("login") String login,
    /** The provider kind of this account. */
    @JsonProperty("kind") AccountKind kind,
    /** Opaque id of the account this one was derived from (e.g. an EMU account's base Entra identity); absent for a root account. Matches the base identity account's selectionId, forming the derivation edge. */
    @JsonProperty("derivedFrom") String derivedFrom,
    /** Whether this is the active account. */
    @JsonProperty("active") Boolean active,
    /** Opaque selection id used to switch to, or log out, this account. */
    @JsonProperty("selectionId") String selectionId
) {
}
