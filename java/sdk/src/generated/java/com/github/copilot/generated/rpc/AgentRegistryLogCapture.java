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
 * Canonical process-log discovery outcome; populated from spawnLiveTarget.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentRegistryLogCapture(
    /** Whether a canonical process log was discovered for this managed spawn */
    @JsonProperty("enabled") Boolean enabled,
    /** Absolute path to the managed spawn's process-<timestamp>-<pid>.log file (only set when enabled) */
    @JsonProperty("path") String path,
    /** Why no canonical process log could be opened for this managed spawn (set only when enabled is false) */
    @JsonProperty("openError") String openError,
    /** Categorized reason no canonical process log could be opened (set only when enabled is false) */
    @JsonProperty("openErrorReason") AgentRegistryLogCaptureOpenErrorReason openErrorReason
) {
}
