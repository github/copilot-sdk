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
 * Feature availability.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectorsGetCapabilitiesResult(
    /** API version. */
    @JsonProperty("apiVersion") Long apiVersion,
    /** Availability. */
    @JsonProperty("availability") ConnectorDiscoveryAvailability availability,
    /** Whether accounts are selected by opaque ID. */
    @JsonProperty("opaqueAccountSelection") Boolean opaqueAccountSelection,
    /** Whether results are cached. */
    @JsonProperty("conditionalCache") Boolean conditionalCache
) {
}
