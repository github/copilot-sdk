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
    /** Model provider that produced this server-tool payload. */
    @JsonProperty("provider") String provider,
    /** Provider-native server-tool call and output items preserved verbatim for replay. */
    @JsonProperty("items") List<Object> items,
    /** Provider function-call namespaces keyed by function-call identifier. */
    @JsonProperty("functionCallNamespaces") Map<String, String> functionCallNamespaces,
    /** Raw provider content blocks retained for verbatim round-tripping. */
    @JsonProperty("rawContentBlocks") List<Object> rawContentBlocks,
    /** Advisor model identifier associated with the server-tool payload. */
    @JsonProperty("advisorModel") String advisorModel
) {
}
