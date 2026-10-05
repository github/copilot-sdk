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
 * Private credentials delivered only to a runtime-owned Mission Control hosting participant.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostGetEnvironmentCredentialsResult(
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
