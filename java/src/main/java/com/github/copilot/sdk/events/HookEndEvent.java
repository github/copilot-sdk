/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: hook.end
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
    public static class HookEndData {

        @JsonProperty("hookInvocationId")
        private String hookInvocationId;

        @JsonProperty("hookType")
        private String hookType;

        @JsonProperty("output")
        private Object output;

        @JsonProperty("success")
        private boolean success;

        @JsonProperty("error")
        private HookError error;

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

        public Object getOutput() {
            return output;
        }

        public void setOutput(Object output) {
            this.output = output;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public HookError getError() {
            return error;
        }

        public void setError(HookError error) {
            this.error = error;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class HookError {

            @JsonProperty("message")
            private String message;

            @JsonProperty("stack")
            private String stack;

            public String getMessage() {
                return message;
            }

            public void setMessage(String message) {
                this.message = message;
            }

            public String getStack() {
                return stack;
            }

            public void setStack(String stack) {
                this.stack = stack;
            }
        }
    }
}
