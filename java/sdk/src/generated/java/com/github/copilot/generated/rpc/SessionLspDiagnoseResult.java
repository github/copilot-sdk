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
 * Result of a language-server startup probe.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionLspDiagnoseResult(
    /** Runtime selection outcome: started, disabled, missing, or unavailable. */
    @JsonProperty("status") String status,
    /** Effective server identifiers, used when the requested server is absent. */
    @JsonProperty("availableServers") List<String> availableServers,
    /** Child process identifier, or null when no process identifier exists. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("pid") Long pid,
    /** Time spent on process startup, in milliseconds. */
    @JsonProperty("spawnTimeMs") Long spawnTimeMs,
    /** Child exit observed during the startup window. */
    @JsonProperty("immediateExit") LspProbeExitStatus immediateExit,
    /** Whether the runtime confined the probe in the session sandbox. */
    @JsonProperty("sandboxed") Boolean sandboxed
) {
}
