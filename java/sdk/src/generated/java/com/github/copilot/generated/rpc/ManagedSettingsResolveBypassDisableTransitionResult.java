/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The bypass-permissions state a host should hold after the resolution, and what it must do to get there.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsResolveBypassDisableTransitionResult(
    /** Which layer disables bypass mode after this resolution. Omitted when none does, which is what a lifted disable looks like. */
    @JsonProperty("source") ManagedSettingsBypassDisableSource source,
    /** The account the returned `managed` source is pinned to. Store it alongside `source`; the next call needs it to decide whether an account switch lifts the disable. */
    @JsonProperty("managedIdentity") String managedIdentity,
    /** The value to store in the host's own bypass-disabled flag. Omitted means leave it exactly as it is, which is not the same as being told to store `false`. */
    @JsonProperty("setDisabledState") Boolean setDisabledState,
    /** Whether a bypass-permissions session already running must be turned off. True only when this resolution newly disables bypass mode, so a host that is already compliant is not interrupted. */
    @JsonProperty("suppressRuntimeAllowAll") Boolean suppressRuntimeAllowAll
) {
}
