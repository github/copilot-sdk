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
 * Result of one customization reload component.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomizationReloadOutcome(
    /** Component whose reload was attempted or skipped */
    @JsonProperty("subsystem") CustomizationReloadSubsystem subsystem,
    /** Whether the component reloaded, was skipped, or failed */
    @JsonProperty("status") CustomizationReloadStatus status,
    /** Reason for a skipped component or description of a failure, when available */
    @JsonProperty("detail") String detail
) {
}
