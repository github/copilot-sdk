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
 * Presentation-neutral phase planned for a HydraFusion turn.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FusionPhasePlanStep(
    /** Kind of phase that may execute. */
    @JsonProperty("kind") FusionPhaseKind kind,
    /** Semantic role assigned to the phase. */
    @JsonProperty("role") String role,
    /** Conversation scope in which the phase executes. */
    @JsonProperty("scope") FusionConversationScope scope,
    /** Whether the phase executes only when an earlier phase requests it. */
    @JsonProperty("conditional") Boolean conditional
) {
}
