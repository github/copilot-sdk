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
 * Results of reloading discovered session customizations. Inspect outcomes for reloaded, skipped, or failed subsystems; a rejection may follow partial mutation. Changes to the model-facing prompt and tools apply on the next turn.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomizationsReloadResult(
    /** Warnings from skill discovery */
    @JsonProperty("warnings") List<String> warnings,
    /** Errors from any component that could not be refreshed */
    @JsonProperty("errors") List<String> errors,
    /** Outcome of each component in reload order; a skipped component was not configured or loaded */
    @JsonProperty("outcomes") List<CustomizationReloadOutcome> outcomes
) {
}
