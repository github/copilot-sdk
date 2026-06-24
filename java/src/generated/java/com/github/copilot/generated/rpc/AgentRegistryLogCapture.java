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
 * Per-spawn log-capture outcome; populated from spawnLiveTarget.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentRegistryLogCapture(
    /** Whether per-spawn log capture is on (false when env-disabled or open failed) */
    @JsonProperty("enabled") Boolean enabled,
    /** Absolute path to the per-spawn log file (only set when enabled) */
    @JsonProperty("path") String path,
    /** Human-readable open failure message (only set when enabled === false AND the env-disable opt-out was NOT used) */
    @JsonProperty("openError") String openError,
    /** Categorized reason for log-open failure */
    @JsonProperty("openErrorReason") AgentRegistryLogCaptureOpenErrorReason openErrorReason
) {
}
