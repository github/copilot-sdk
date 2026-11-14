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
 * The managed-settings layers to compose into an effective policy. Every layer is optional; omitting all of them yields no effective policy.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsResolveEffectiveValueParams(
    /** The account's server-delivered org policy, as returned by a managed-settings fetch. Omitted before sign-in or when the fetch has not landed. */
    @JsonProperty("server") Object server,
    /** The device layer: native MDM and managed-file settings describing the machine rather than the account. */
    @JsonProperty("mdm") Object mdm,
    /** The device sandbox floor, folded into the device layer under `sandbox` before composition. */
    @JsonProperty("deviceSandboxFloor") Object deviceSandboxFloor,
    /** The policy-helper layer, an enforced channel sourced from a platform policy helper process. */
    @JsonProperty("policyHelper") Object policyHelper
) {
}
