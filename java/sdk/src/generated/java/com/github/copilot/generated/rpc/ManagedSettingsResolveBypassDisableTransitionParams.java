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
 * The bypass-permissions state a host holds today, the account now signed in, and what the managed-policy resolution said.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsResolveBypassDisableTransitionParams(
    /** Which layer currently disables bypass mode. Omit it when nothing does. */
    @JsonProperty("currentSource") ManagedSettingsBypassDisableSource currentSource,
    /** The account a `managed` disable is pinned to. Omit it when the current source is not `managed`; it is ignored for the other sources. */
    @JsonProperty("currentManagedIdentity") String currentManagedIdentity,
    /** The account signed in now. A `managed` disable pinned to a different account is lifted. */
    @JsonProperty("activeIdentity") String activeIdentity,
    /** What the resolution said about bypass mode. Send `unknown` rather than omitting the field when there is no verdict. */
    @JsonProperty("policy") ManagedSettingsBypassPolicyResolution policy
) {
}
