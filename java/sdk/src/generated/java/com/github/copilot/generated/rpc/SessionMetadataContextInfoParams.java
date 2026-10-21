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
 * Model identifier and token limits used to compute the context-info breakdown.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataContextInfoParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Advertised prompt allowance. Pass 0 to resolve the selected model and context tier from the session. */
    @JsonProperty("promptTokenLimit") Long promptTokenLimit,
    /** Requested output allowance to reserve against the combined context ceiling. Pass 0 to resolve the session's request cap, falling back to the model's advertised output limit. */
    @JsonProperty("outputTokenLimit") Long outputTokenLimit,
    /** Model identifier used for tokenization. Omit to use the session default. Used both for token counting and to compute display values. */
    @JsonProperty("selectedModel") String selectedModel,
    /** Provider owning selectedModel. Omit to use the current provider for the current model, or deterministic legacy lookup for a different model. */
    @JsonProperty("providerId") String providerId
) {
}
