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
 * Feature detection and hard polling limits for the EXPERIMENTAL session connector API.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionConnectorsGetCapabilitiesResult(
    /** Connector API contract version. */
    @JsonProperty("apiVersion") Long apiVersion,
    /** Current session availability. Disabled availability is reported without making a Connector request. */
    @JsonProperty("availability") ConnectorAvailability availability,
    /** Whether connect and reconnect can return an opaque continuation for bounded consent polling. */
    @JsonProperty("consentContinuation") Boolean consentContinuation,
    /** Whether callers select a host-owned GitHub account through an opaque selection ID rather than supplying a provider token. */
    @JsonProperty("opaqueAccountSelection") Boolean opaqueAccountSelection,
    /** Maximum accepted polling attempts for one continuation call. */
    @JsonProperty("maxPollAttempts") Long maxPollAttempts,
    /** Maximum accepted delay in milliseconds between polling attempts. */
    @JsonProperty("maxPollIntervalMs") Long maxPollIntervalMs,
    /** Maximum accepted wall-clock deadline in milliseconds for one continuation call. */
    @JsonProperty("maxDeadlineMs") Long maxDeadlineMs
) {
}
