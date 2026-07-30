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
 * A single large message currently in context.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContextHeaviestMessage(
    /** Stable identifier for this message within the snapshot. */
    @JsonProperty("id") String id,
    /** Human-readable source label, e.g. `tool: bash` or `skill: tmux`. Presentation-only. */
    @JsonProperty("label") String label,
    /** Role of the chat message (`user`, `assistant`, or `tool`). */
    @JsonProperty("role") String role,
    /** Token count currently in context for this individual message. */
    @JsonProperty("tokens") Long tokens
) {
}
