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
 * User response for a pending exit-plan-mode request, with approval state, selected action, auto-approve flag, and feedback.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UIExitPlanModeResponse(
    /** Whether the plan was approved. */
    @JsonProperty("approved") Boolean approved,
    /** The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true, otherwise 'interactive'. */
    @JsonProperty("selectedAction") UIExitPlanModeAction selectedAction,
    /** Whether subsequent edits should be auto-approved without confirmation. */
    @JsonProperty("autoApproveEdits") Boolean autoApproveEdits,
    /** Feedback from the user when they declined the plan or requested changes. */
    @JsonProperty("feedback") String feedback,
    /** When true, the agent is instructed to end its turn without starting implementation so the client can restore the session model and auto-submit a fresh implementation turn on it. Set only when a distinct plan configuration (a different model, reasoning effort, or context tier) actually ran the planning turn. */
    @JsonProperty("deferImplementation") Boolean deferImplementation
) {
}
