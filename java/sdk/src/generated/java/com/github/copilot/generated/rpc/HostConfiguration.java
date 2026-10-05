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
 * Normalized listener settings delivered only to the supervised hosting participant.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record HostConfiguration(
    /** Normalized local listener settings, absent for relay-only hosts. */
    @JsonProperty("localServer") HostLocalServerConfiguration localServer,
    /** Requested GitHub Mission Control registration. */
    @JsonProperty("githubEnvironment") HostGitHubEnvironmentOptions gitHubEnvironment,
    /** Whether session materialization is delegated to the owning application. */
    @JsonProperty("sessionFactory") Boolean sessionFactory,
    /** Whether app-owned durable sessions are resumed by the owning application. */
    @JsonProperty("resumeFactory") Boolean resumeFactory
) {
}
