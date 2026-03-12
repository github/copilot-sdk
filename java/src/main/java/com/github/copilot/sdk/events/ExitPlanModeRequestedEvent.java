/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: exit_plan_mode.requested
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ExitPlanModeRequestedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private ExitPlanModeRequestedData data;

    @Override
    public String getType() {
        return "exit_plan_mode.requested";
    }

    public ExitPlanModeRequestedData getData() {
        return data;
    }

    public void setData(ExitPlanModeRequestedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExitPlanModeRequestedData(@JsonProperty("requestId") String requestId,
            @JsonProperty("summary") String summary, @JsonProperty("planContent") String planContent,
            @JsonProperty("actions") String[] actions, @JsonProperty("recommendedAction") String recommendedAction) {
    }
}
