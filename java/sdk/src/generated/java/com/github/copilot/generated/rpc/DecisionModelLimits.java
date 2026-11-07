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
 * Request limits for one decision model.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionModelLimits(
    /** Maximum questions per request. */
    @JsonProperty("maxQuestions") Long maxQuestions,
    /** Minimum options per choice question. */
    @JsonProperty("minOptions") Long minOptions,
    /** Maximum options per choice question. */
    @JsonProperty("maxOptions") Long maxOptions,
    /** Minimum levels per score question. */
    @JsonProperty("minLevels") Long minLevels,
    /** Maximum levels per score question. */
    @JsonProperty("maxLevels") Long maxLevels,
    /** Maximum size of the serialized request, in bytes. */
    @JsonProperty("maxRequestBytes") Long maxRequestBytes,
    /** Maximum nesting depth of JSON context. */
    @JsonProperty("maxJsonDepth") Long maxJsonDepth,
    /** Token budget for the context plus any one question, when the model has one. */
    @JsonProperty("contextPlusQuestionTokens") Long contextPlusQuestionTokens
) {
}
