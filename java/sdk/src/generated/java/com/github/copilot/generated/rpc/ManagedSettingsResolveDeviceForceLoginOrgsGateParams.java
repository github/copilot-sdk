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
 * The credential and environment to evaluate the device-channel sign-in pin against.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsResolveDeviceForceLoginOrgsGateParams(
    /** The authenticated identity to judge. Omitted when signed out, which is the case the device channel exists to serve: it can refuse a first sign-in before any credential exists. */
    @JsonProperty("authInfo") Object authInfo,
    /** The host's environment variables. Read for the variables that would make an identity's membership snapshot forgeable, so a credential whose provenance cannot be trusted is refused rather than accepted. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("environment") Object environment
) {
}
