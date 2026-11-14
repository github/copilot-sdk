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
 * The model to switch to, and the warning to show when the agent's preference could not be met.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AgentsCustomAgentInitialModelDecisionResult(
    /** The first available model that matches the agent's preferences. Absent when none of the requested models is available. */
    @JsonProperty("targetModel") String targetModel,
    /** The reasoning effort attached to the selected model preference. Absent when that preference does not specify an effort. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** What to tell the user about an unmet preference. Absent when the preference was met. A warning with no `targetModel` means the agent's models are all unavailable. */
    @JsonProperty("warning") String warning
) {
}
