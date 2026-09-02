/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Opaque integrator-owned process launch profile for one extension entrypoint.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtensionLaunchProfile(
    /** Executable used to launch the extension entrypoint. */
    @JsonProperty("executable") String executable,
    /** Opaque integrator-defined arguments passed to the executable. The runtime does not append the extension entrypoint. */
    @JsonProperty("args") List<String> args,
    /** Opaque integrator-defined environment variables. Runtime-owned COPILOT_SDK_PATH, SESSION_ID, and COPILOT_EXTENSION_PARENT_PID values take precedence. */
    @JsonProperty("env") Map<String, String> env
) {
}
