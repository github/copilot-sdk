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
 * Schema for the `UIExitPlanModeResponse` type.
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
    @JsonProperty("feedback") String feedback
) {
}
