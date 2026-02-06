/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/**
 * Event: tool.execution_complete
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ToolExecutionCompleteEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private ToolExecutionCompleteData data;

    @Override
    public String getType() {
        return "tool.execution_complete";
    }

    public ToolExecutionCompleteData getData() {
        return data;
    }

    public void setData(ToolExecutionCompleteData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolExecutionCompleteData {

        @JsonProperty("toolCallId")
        private String toolCallId;

        @JsonProperty("success")
        private boolean success;

        @JsonProperty("isUserRequested")
        private Boolean isUserRequested;

        @JsonProperty("result")
        private Result result;

        @JsonProperty("error")
        private Error error;

        @JsonProperty("toolTelemetry")
        private Map<String, Object> toolTelemetry;

        @JsonProperty("parentToolCallId")
        private String parentToolCallId;

        public String getToolCallId() {
            return toolCallId;
        }

        public void setToolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public Boolean getIsUserRequested() {
            return isUserRequested;
        }

        public void setIsUserRequested(Boolean isUserRequested) {
            this.isUserRequested = isUserRequested;
        }

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }

        public Error getError() {
            return error;
        }

        public void setError(Error error) {
            this.error = error;
        }

        public Map<String, Object> getToolTelemetry() {
            return toolTelemetry == null ? null : Collections.unmodifiableMap(toolTelemetry);
        }

        public void setToolTelemetry(Map<String, Object> toolTelemetry) {
            this.toolTelemetry = toolTelemetry;
        }

        public String getParentToolCallId() {
            return parentToolCallId;
        }

        public void setParentToolCallId(String parentToolCallId) {
            this.parentToolCallId = parentToolCallId;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Result {

            @JsonProperty("content")
            private String content;

            @JsonProperty("detailedContent")
            private String detailedContent;

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }

            public String getDetailedContent() {
                return detailedContent;
            }

            public void setDetailedContent(String detailedContent) {
                this.detailedContent = detailedContent;
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Error {

            @JsonProperty("message")
            private String message;

            @JsonProperty("code")
            private String code;

            public String getMessage() {
                return message;
            }

            public void setMessage(String message) {
                this.message = message;
            }

            public String getCode() {
                return code;
            }

            public void setCode(String code) {
                this.code = code;
            }
        }
    }
}
