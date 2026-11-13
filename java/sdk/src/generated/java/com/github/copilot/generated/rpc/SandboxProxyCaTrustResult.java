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
 * Status of the persistent certificate authority of the sandbox credential proxy.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxProxyCaTrustResult(
    /** The state of the certificate authority. */
    @JsonProperty("state") SandboxProxyCaState state,
    /** Human-readable reason for the state. On `installed` or `notInstalled`, present only when the certificate authority must be rotated, and then says why. */
    @JsonProperty("detail") String detail,
    /** Whether this process can add the certificate authority to OS trust without credentials from a different user. False where OS trust is unsupported, and on Windows when the process cannot elevate itself to write the machine trust store. When false, do not offer to set up the certificate authority. */
    @JsonProperty("canInstall") Boolean canInstall
) {
}
