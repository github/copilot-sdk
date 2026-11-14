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
 * Current health information for a provider instance.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProvidersGetStatusResult(
    /** Typed operation outcome. */
    @JsonProperty("outcome") ModelProviderOperationOutcome outcome,
    /** Normalized provider instance. */
    @JsonProperty("instance") ModelProviderInstance instance,
    /** Open provider status value, such as `healthy`, `unreachable`, or `notInstalled`. */
    @JsonProperty("status") String status,
    /** Provider-reported version. */
    @JsonProperty("version") String version
) {
}
