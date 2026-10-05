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
 * Private credentials delivered only to a runtime-owned Mission Control hosting participant.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record HostEnvironmentCredentials(
    /** Current bearer token for the authenticated GitHub identity. */
    @JsonProperty("token") String token,
    /** Hostname of the authenticated GitHub service. */
    @JsonProperty("githubHost") String gitHubHost,
    /** GitHub API base URL for the authenticated service. */
    @JsonProperty("githubApiUrl") String gitHubApiUrl,
    /** Mission Control API origin for environment registration and management. */
    @JsonProperty("missionControlUrl") String missionControlUrl
) {
}
