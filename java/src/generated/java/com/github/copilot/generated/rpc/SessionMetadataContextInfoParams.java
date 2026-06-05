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
 * Model identifier and token limits used to compute the context-info breakdown.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataContextInfoParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Maximum prompt tokens allowed by the target model. Pass 0 to use the runtime default. */
    @JsonProperty("promptTokenLimit") Long promptTokenLimit,
    /** Maximum output tokens allowed by the target model. Pass 0 if unknown. */
    @JsonProperty("outputTokenLimit") Long outputTokenLimit,
    /** Model identifier used for tokenization. Omit to use the session default. Used both for token counting and to compute display values. */
    @JsonProperty("selectedModel") String selectedModel
) {
}
