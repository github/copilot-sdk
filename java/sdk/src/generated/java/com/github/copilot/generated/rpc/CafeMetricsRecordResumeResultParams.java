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
 * Terminal result of one resume attempt.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CafeMetricsRecordResumeResultParams(
    /** Last phase reached. */
    @JsonProperty("stage") CafeMetricsResumeStage stage,
    /** Terminal outcome. */
    @JsonProperty("outcome") CafeMetricsResumeOutcome outcome,
    /** Bounded result classification. */
    @JsonProperty("reason") CafeMetricsResumeReason reason,
    /** Whether an event-log recovery projection was used. */
    @JsonProperty("recovered") Boolean recovered
) {
}
