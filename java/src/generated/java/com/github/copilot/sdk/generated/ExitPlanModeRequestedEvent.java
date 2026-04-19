/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The {@code exit_plan_mode.requested} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ExitPlanModeRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "exit_plan_mode.requested"; }

    @JsonProperty("data")
    private ExitPlanModeRequestedEventData data;

    public ExitPlanModeRequestedEventData getData() { return data; }
    public void setData(ExitPlanModeRequestedEventData data) { this.data = data; }

    /** Data payload for {@link ExitPlanModeRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExitPlanModeRequestedEventData(
        /** Unique identifier for this request; used to respond via session.respondToExitPlanMode() */
        @JsonProperty("requestId") String requestId,
        /** Summary of the plan that was created */
        @JsonProperty("summary") String summary,
        /** Full content of the plan file */
        @JsonProperty("planContent") String planContent,
        /** Available actions the user can take (e.g., approve, edit, reject) */
        @JsonProperty("actions") List<String> actions,
        /** The recommended action for the user to take */
        @JsonProperty("recommendedAction") String recommendedAction
    ) {
    }
}
