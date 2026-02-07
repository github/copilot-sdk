/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.compaction_complete
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionCompactionCompleteEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionCompactionCompleteData data;

    @Override
    public String getType() {
        return "session.compaction_complete";
    }

    public SessionCompactionCompleteData getData() {
        return data;
    }

    public void setData(SessionCompactionCompleteData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionCompactionCompleteData {

        @JsonProperty("success")
        private boolean success;

        @JsonProperty("error")
        private String error;

        @JsonProperty("preCompactionTokens")
        private Double preCompactionTokens;

        @JsonProperty("postCompactionTokens")
        private Double postCompactionTokens;

        @JsonProperty("preCompactionMessagesLength")
        private Double preCompactionMessagesLength;

        @JsonProperty("messagesRemoved")
        private Double messagesRemoved;

        @JsonProperty("tokensRemoved")
        private Double tokensRemoved;

        @JsonProperty("summaryContent")
        private String summaryContent;

        @JsonProperty("checkpointNumber")
        private Double checkpointNumber;

        @JsonProperty("checkpointPath")
        private String checkpointPath;

        @JsonProperty("compactionTokensUsed")
        private CompactionTokensUsed compactionTokensUsed;

        @JsonProperty("requestId")
        private String requestId;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public Double getPreCompactionTokens() {
            return preCompactionTokens;
        }

        public void setPreCompactionTokens(Double preCompactionTokens) {
            this.preCompactionTokens = preCompactionTokens;
        }

        public Double getPostCompactionTokens() {
            return postCompactionTokens;
        }

        public void setPostCompactionTokens(Double postCompactionTokens) {
            this.postCompactionTokens = postCompactionTokens;
        }

        public Double getPreCompactionMessagesLength() {
            return preCompactionMessagesLength;
        }

        public void setPreCompactionMessagesLength(Double preCompactionMessagesLength) {
            this.preCompactionMessagesLength = preCompactionMessagesLength;
        }

        public Double getMessagesRemoved() {
            return messagesRemoved;
        }

        public void setMessagesRemoved(Double messagesRemoved) {
            this.messagesRemoved = messagesRemoved;
        }

        public Double getTokensRemoved() {
            return tokensRemoved;
        }

        public void setTokensRemoved(Double tokensRemoved) {
            this.tokensRemoved = tokensRemoved;
        }

        public String getSummaryContent() {
            return summaryContent;
        }

        public void setSummaryContent(String summaryContent) {
            this.summaryContent = summaryContent;
        }

        public Double getCheckpointNumber() {
            return checkpointNumber;
        }

        public void setCheckpointNumber(Double checkpointNumber) {
            this.checkpointNumber = checkpointNumber;
        }

        public String getCheckpointPath() {
            return checkpointPath;
        }

        public void setCheckpointPath(String checkpointPath) {
            this.checkpointPath = checkpointPath;
        }

        public CompactionTokensUsed getCompactionTokensUsed() {
            return compactionTokensUsed;
        }

        public void setCompactionTokensUsed(CompactionTokensUsed compactionTokensUsed) {
            this.compactionTokensUsed = compactionTokensUsed;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
    }

    /**
     * Token usage information for the compaction operation.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompactionTokensUsed {

        @JsonProperty("input")
        private double input;

        @JsonProperty("output")
        private double output;

        @JsonProperty("cachedInput")
        private double cachedInput;

        public double getInput() {
            return input;
        }

        public void setInput(double input) {
            this.input = input;
        }

        public double getOutput() {
            return output;
        }

        public void setOutput(double output) {
            this.output = output;
        }

        public double getCachedInput() {
            return cachedInput;
        }

        public void setCachedInput(double cachedInput) {
            this.cachedInput = cachedInput;
        }
    }
}
