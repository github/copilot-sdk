/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: hook.start
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
    public static class HookStartData {

        @JsonProperty("hookInvocationId")
        private String hookInvocationId;

        @JsonProperty("hookType")
        private String hookType;

        @JsonProperty("input")
        private Object input;

        public String getHookInvocationId() {
            return hookInvocationId;
        }

        public void setHookInvocationId(String hookInvocationId) {
            this.hookInvocationId = hookInvocationId;
        }

        public String getHookType() {
            return hookType;
        }

        public void setHookType(String hookType) {
            this.hookType = hookType;
        }

        public Object getInput() {
            return input;
        }

        public void setInput(Object input) {
            this.input = input;
        }
    }
}
