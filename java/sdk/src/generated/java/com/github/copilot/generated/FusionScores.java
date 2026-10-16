/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Validated HydraFusion routing capability scores.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FusionScores(
    /** Reasoning capability score returned by the authenticated router. */
    @JsonProperty("reasoning") Double reasoning,
    /** Code-generation capability score returned by the authenticated router. */
    @JsonProperty("codeGen") Double codeGen,
    /** Debugging capability score returned by the authenticated router. */
    @JsonProperty("debugging") Double debugging,
    /** Tool-use capability score returned by the authenticated router. */
    @JsonProperty("toolUse") Double toolUse
) {
}
