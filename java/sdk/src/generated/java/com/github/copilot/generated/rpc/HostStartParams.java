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
 * Starts a supervised AHP host with at least one explicitly selected transport.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostStartParams(
    /** Caller-generated UUID identifying this connection-owned listener. */
    @JsonProperty("hostId") String hostId,
    /** Enables a local WebSocket listener. */
    @JsonProperty("localServer") HostLocalServerOptions localServer,
    /** Registers a GitHub Mission Control environment and enables its relay transport. */
    @JsonProperty("githubEnvironment") HostGitHubEnvironmentOptions gitHubEnvironment,
    /** Ask the owning SDK application to materialize AHP sessions. */
    @JsonProperty("sessionFactory") Boolean sessionFactory,
    /** Ask the owning application to resume its durable AHP sessions. */
    @JsonProperty("resumeFactory") Boolean resumeFactory
) {
}
