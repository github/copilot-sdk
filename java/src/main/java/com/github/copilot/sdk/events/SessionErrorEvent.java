/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.error
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionErrorEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionErrorData data;

    @Override
    public String getType() {
        return "session.error";
    }

    public SessionErrorData getData() {
        return data;
    }

    public void setData(SessionErrorData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionErrorData {

        @JsonProperty("errorType")
        private String errorType;

        @JsonProperty("message")
        private String message;

        @JsonProperty("stack")
        private String stack;

        public String getErrorType() {
            return errorType;
        }

        public void setErrorType(String errorType) {
            this.errorType = errorType;
        }

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
