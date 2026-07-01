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
 * Payload for a `gitHubTelemetry.event` notification: a single GitHub telemetry event the runtime forwards to a host connection that opted into telemetry forwarding for the session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubTelemetryNotification(
    /** Session the telemetry event belongs to. */
    @JsonProperty("sessionId") String sessionId,
    /** Whether this is a restricted telemetry event (cli.restricted_telemetry). Hosts must route restricted events to first-party Microsoft stores only. */
    @JsonProperty("restricted") Boolean restricted,
    /** The telemetry event, in the runtime's native GitHub-shaped telemetry format. */
    @JsonProperty("event") GitHubTelemetryEvent event
) {
}
