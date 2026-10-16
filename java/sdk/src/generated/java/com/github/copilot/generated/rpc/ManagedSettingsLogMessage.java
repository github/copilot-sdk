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
 * One diagnostic emitted while ingesting managed settings. Returned rather than logged by the runtime so the host decides where ingestion diagnostics go, which matters because an ingestion runs before a session exists to carry a log sink.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsLogMessage(
    /** Severity of this diagnostic. */
    @JsonProperty("level") ManagedSettingsLogLevel level,
    /** Human-readable diagnostic text. */
    @JsonProperty("message") String message
) {
}
