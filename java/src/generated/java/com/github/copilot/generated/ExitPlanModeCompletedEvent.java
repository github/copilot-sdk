/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "exit_plan_mode.completed". Plan mode exit completion with the user's approval decision and optional feedback
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ExitPlanModeCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "exit_plan_mode.completed"; }

    @JsonProperty("data")
    private ExitPlanModeCompletedEventData data;

    public ExitPlanModeCompletedEventData getData() { return data; }
    public void setData(ExitPlanModeCompletedEventData data) { this.data = data; }

    /** Data payload for {@link ExitPlanModeCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExitPlanModeCompletedEventData(
        /** Request ID of the resolved exit plan mode request; clients should dismiss any UI for this request */
        @JsonProperty("requestId") String requestId,
        /** Whether the plan was approved by the user */
        @JsonProperty("approved") Boolean approved,
        /** Action selected by the user */
        @JsonProperty("selectedAction") ExitPlanModeAction selectedAction,
        /** Whether edits should be auto-approved without confirmation */
        @JsonProperty("autoApproveEdits") Boolean autoApproveEdits,
        /** Free-form feedback from the user if they requested changes to the plan */
        @JsonProperty("feedback") String feedback
    ) {
    }
}
