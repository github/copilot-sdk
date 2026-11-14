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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Hosting capabilities and session capacity advertised by an environment.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EnvironmentCapabilities(
    /** Advertised Agent Host Protocol version. */
    @JsonProperty("ahpVersion") String ahpVersion,
    /** Feature identifiers advertised by the environment. */
    @JsonProperty("features") List<String> features,
    /** Maximum session capacity, when advertised. */
    @JsonProperty("maxSessions") Long maxSessions,
    /** Current session count, when advertised. */
    @JsonProperty("currentSessions") Long currentSessions
) {
}
