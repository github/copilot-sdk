/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: exit_plan_mode.completed
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ExitPlanModeCompletedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private ExitPlanModeCompletedData data;

    @Override
    public String getType() {
        return "exit_plan_mode.completed";
    }

    public ExitPlanModeCompletedData getData() {
        return data;
    }

    public void setData(ExitPlanModeCompletedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExitPlanModeCompletedData(@JsonProperty("requestId") String requestId) {
    }
}
