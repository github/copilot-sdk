/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Neutral provider-tagged reasoning content blocks preserved verbatim for round-tripping
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantMessageReasoningBlocks(
    /** Model provider that produced these reasoning blocks. */
    @JsonProperty("provider") String provider,
    /** Provider-native reasoning items or content blocks preserved verbatim, in order. A single response can carry several, and provider signatures or identifiers may depend on their exact content and ordering. */
    @JsonProperty("blocks") List<Object> blocks
) {
}
