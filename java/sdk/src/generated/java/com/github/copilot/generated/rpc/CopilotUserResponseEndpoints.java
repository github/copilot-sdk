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
 * Endpoint URLs from the raw Copilot `/copilot_internal/v2/token` user-response passthrough.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CopilotUserResponseEndpoints(
    /** Copilot API endpoint URL. */
    @JsonProperty("api") String api,
    /** Origin-tracker endpoint URL. */
    @JsonProperty("origin-tracker") String originTracker,
    /** Copilot proxy endpoint URL. */
    @JsonProperty("proxy") String proxy,
    /** Copilot telemetry endpoint URL. */
    @JsonProperty("telemetry") String telemetry,
    /** Experimental-service endpoint URL. */
    @JsonProperty("exp") String exp
) {
}
