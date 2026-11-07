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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The decision model's answers.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionsAskResult(
    /** Decision model id that answered; valid as `model` in a later request. */
    @JsonProperty("model") String model,
    /** Model name the backend reported, when it differs from `model`. Informational only. */
    @JsonProperty("upstreamModel") String upstreamModel,
    /** One answer per question, in request order. */
    @JsonProperty("answers") List<Object> answers,
    /** Token usage, when the backend reports it. */
    @JsonProperty("usage") DecisionUsage usage,
    /** Milliseconds from receiving the request to producing the result. */
    @JsonProperty("latencyMs") Long latencyMs
) {
}
