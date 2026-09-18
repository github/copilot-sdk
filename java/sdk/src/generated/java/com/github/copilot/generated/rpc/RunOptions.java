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
 * Options controlling factory invocation.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RunOptions(
    /** Per-invocation resource ceiling overrides. */
    @JsonProperty("limits") FactoryRunLimits limits,
    /** Whether to notify the originating session when the factory completes. */
    @JsonProperty("notifyOnComplete") Boolean notifyOnComplete,
    /** Whether to emit factory phase names to the session transcript. */
    @JsonProperty("logPhaseNames") Boolean logPhaseNames,
    /** Run identifier whose journal and progress should seed this resumed run. */
    @JsonProperty("resumeFromRunId") String resumeFromRunId
) {
}
