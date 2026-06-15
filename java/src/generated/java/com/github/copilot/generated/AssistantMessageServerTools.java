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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Neutral provider-tagged server-side tool-use payload (tool search, advisor) for verbatim round-tripping
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantMessageServerTools(
    @JsonProperty("provider") String provider,
    @JsonProperty("items") List<Object> items,
    @JsonProperty("functionCallNamespaces") Map<String, String> functionCallNamespaces,
    @JsonProperty("rawContentBlocks") List<Object> rawContentBlocks,
    @JsonProperty("advisorModel") String advisorModel
) {
}
