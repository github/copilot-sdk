/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: hook.start
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class HookStartEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private HookStartData data;

    @Override
    public String getType() {
        return "hook.start";
    }

    public HookStartData getData() {
        return data;
    }

    public void setData(HookStartData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HookStartData(@JsonProperty("hookInvocationId") String hookInvocationId,
            @JsonProperty("hookType") String hookType, @JsonProperty("input") Object input) {
    }
}
