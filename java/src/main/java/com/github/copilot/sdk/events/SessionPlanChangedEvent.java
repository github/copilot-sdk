/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.plan_changed
 *
 * @since 1.0.10
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionPlanChangedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionPlanChangedData data;

    @Override
    public String getType() {
        return "session.plan_changed";
    }

    public SessionPlanChangedData getData() {
        return data;
    }

    public void setData(SessionPlanChangedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionPlanChangedData(@JsonProperty("operation") String operation) {
    }
}
