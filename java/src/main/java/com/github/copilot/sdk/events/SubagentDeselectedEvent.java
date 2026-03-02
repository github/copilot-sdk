/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: subagent.deselected
 *
 * @since 1.0.11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SubagentDeselectedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SubagentDeselectedData data;

    @Override
    public String getType() {
        return "subagent.deselected";
    }

    public SubagentDeselectedData getData() {
        return data;
    }

    public void setData(SubagentDeselectedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubagentDeselectedData() {
    }
}
