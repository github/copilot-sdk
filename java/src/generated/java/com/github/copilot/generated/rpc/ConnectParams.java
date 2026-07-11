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
 * Parameters for the `server.connect` handshake: an optional connection token and optional connection-level opt-ins (e.g. GitHub telemetry forwarding).
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectParams(
    /** Connection token; required when the server was started with COPILOT_CONNECTION_TOKEN */
    @JsonProperty("token") String token,
    /** Opt this connection in to GitHub telemetry forwarding for its lifetime. When set, the runtime forwards every internal telemetry event it emits — across all sessions, plus sessionless events — to this connection over the `gitHubTelemetry.event` notification, in addition to the runtime's normal GitHub/CTS emission (dual-write). Intended for first-party hosts that re-emit the events into their own telemetry stores. Both unrestricted and restricted events are forwarded, each tagged with a `restricted` discriminator; a backstop drops restricted events when restricted telemetry is disabled. */
    @JsonProperty("enableGitHubTelemetryForwarding") Boolean enableGitHubTelemetryForwarding
) {
}
