/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * UI capability changes
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CapabilitiesChangedUI(
    /** Whether elicitation is now supported */
    @JsonProperty("elicitation") Boolean elicitation,
    /** Whether MCP Apps (SEP-1865) UI passthrough is now supported */
    @JsonProperty("mcpApps") Boolean mcpApps,
    /** Whether canvas rendering is now supported */
    @JsonProperty("canvases") Boolean canvases
) {
}
