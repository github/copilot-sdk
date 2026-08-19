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
 * Connection-level opt-ins for the `server.connect` handshake. Transport authentication is consumed by the native protocol boundary before dispatch.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectParams(
    /** Opt this connection in to GitHub telemetry forwarding for its lifetime. When set, the runtime forwards every internal telemetry event it emits — across all sessions, plus sessionless events — to this connection over the `gitHubTelemetry.event` notification. Regular events are also written to the runtime's normal GitHub/CTS path (dual-write); host-only compatibility events are forward-only and intentionally skip that path. Intended for first-party hosts that re-emit the events into their own telemetry stores. Both unrestricted and restricted events are forwarded, each tagged with a `restricted` discriminator; a backstop drops restricted events when restricted telemetry is disabled — using the process-global gate for ordinary events and an explicit session-scoped decision for host-only events. */
    @JsonProperty("enableGitHubTelemetryForwarding") Boolean enableGitHubTelemetryForwarding,
    /** Connection token; required when the server was started with COPILOT_CONNECTION_TOKEN */
    @JsonProperty("token") String token
) {
}
