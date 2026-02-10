/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: hook.end
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class HookEndEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private HookEndData data;

    @Override
    public String getType() {
        return "hook.end";
    }

    public HookEndData getData() {
        return data;
    }

    public void setData(HookEndData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HookEndData(@JsonProperty("hookInvocationId") String hookInvocationId,
            @JsonProperty("hookType") String hookType, @JsonProperty("output") Object output,
            @JsonProperty("success") boolean success, @JsonProperty("error") HookError error) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record HookError(@JsonProperty("message") String message, @JsonProperty("stack") String stack) {
        }
    }
}
