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
 * One canonical signed-in account entry shared by account-list and account-switch surfaces.
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
    /** Stable opaque account id used for forest lineage and account management. */
    @JsonProperty("accountId") String accountId,
    /** Authentication host URL. */
    @JsonProperty("host") String host,
    /** Authenticated login/username. */
    @JsonProperty("login") String login,
    /** Human-readable identity and provider label owned by the runtime. */
    @JsonProperty("label") String label,
    /** The provider kind of this account. */
    @JsonProperty("kind") AccountKind kind,
    /** Human-readable credential source used to authenticate this account, such as `gh` or `GITHUB_TOKEN`. */
    @JsonProperty("authSource") String authSource,
    /** Opaque accountId of the account this one was derived from; absent for a root account. */
    @JsonProperty("derivedFrom") String derivedFrom,
    /** Whether this is the active account. */
    @JsonProperty("active") Boolean active,
    /** Opaque selection id accepted by switchActive. Absent when switching is not currently valid. */
    @JsonProperty("selectionId") String selectionId,
    /** Whether the account is currently a valid switch target. */
    @JsonProperty("switchable") Boolean switchable,
    /** Canonical availability treatment for this account. */
    @JsonProperty("health") AccountHealth health,
    /** Human-readable availability or recovery reason. */
    @JsonProperty("statusMessage") String statusMessage
) {
}
