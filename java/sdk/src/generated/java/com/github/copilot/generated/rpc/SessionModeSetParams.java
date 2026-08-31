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
 * Agent interaction mode to apply to the session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModeSetParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The session mode the agent is operating in */
    @JsonProperty("mode") SessionMode mode,
    /** Session whose plan-mode base state should be inherited. */
    @JsonProperty("inheritPlanBaseFromSessionId") String inheritPlanBaseFromSessionId,
    /** Whether a dedicated plan model is configured. */
    @JsonProperty("planModelConfigured") Boolean planModelConfigured,
    /** Dedicated model to use in plan mode, when configured. */
    @JsonProperty("planModel") String planModel,
    /** Reasoning effort to use with the dedicated plan model. */
    @JsonProperty("planReasoningEffort") String planReasoningEffort,
    /** Context tier to use with the dedicated plan model. */
    @JsonProperty("planContextTier") String planContextTier,
    /** Explicit response to a model-switch compaction preflight. */
    @JsonProperty("compactionDecision") String compactionDecision,
    /** Whether leaving plan mode should restore the session's previous model. */
    @JsonProperty("restorePlanModel") Boolean restorePlanModel,
    /** Whether the selected plan model should be persisted. */
    @JsonProperty("persistPlanSelection") Boolean persistPlanSelection,
    /** Settings context used when persisting the selected plan model. */
    @JsonProperty("pickerSettingsContext") ModelPickerSettingsContext pickerSettingsContext,
    /** Action to perform when leaving plan mode. */
    @JsonProperty("planExitAction") String planExitAction
) {
}
