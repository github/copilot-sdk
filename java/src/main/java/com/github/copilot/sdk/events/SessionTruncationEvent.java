/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.truncation
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionTruncationEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionTruncationData data;

    @Override
    public String getType() {
        return "session.truncation";
    }

    public SessionTruncationData getData() {
        return data;
    }

    public void setData(SessionTruncationData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionTruncationData {

        @JsonProperty("tokenLimit")
        private double tokenLimit;

        @JsonProperty("preTruncationTokensInMessages")
        private double preTruncationTokensInMessages;

        @JsonProperty("preTruncationMessagesLength")
        private double preTruncationMessagesLength;

        @JsonProperty("postTruncationTokensInMessages")
        private double postTruncationTokensInMessages;

        @JsonProperty("postTruncationMessagesLength")
        private double postTruncationMessagesLength;

        @JsonProperty("tokensRemovedDuringTruncation")
        private double tokensRemovedDuringTruncation;

        @JsonProperty("messagesRemovedDuringTruncation")
        private double messagesRemovedDuringTruncation;

        @JsonProperty("performedBy")
        private String performedBy;

        public double getTokenLimit() {
            return tokenLimit;
        }

        public void setTokenLimit(double tokenLimit) {
            this.tokenLimit = tokenLimit;
        }

        public double getPreTruncationTokensInMessages() {
            return preTruncationTokensInMessages;
        }

        public void setPreTruncationTokensInMessages(double preTruncationTokensInMessages) {
            this.preTruncationTokensInMessages = preTruncationTokensInMessages;
        }

        public double getPreTruncationMessagesLength() {
            return preTruncationMessagesLength;
        }

        public void setPreTruncationMessagesLength(double preTruncationMessagesLength) {
            this.preTruncationMessagesLength = preTruncationMessagesLength;
        }

        public double getPostTruncationTokensInMessages() {
            return postTruncationTokensInMessages;
        }

        public void setPostTruncationTokensInMessages(double postTruncationTokensInMessages) {
            this.postTruncationTokensInMessages = postTruncationTokensInMessages;
        }

        public double getPostTruncationMessagesLength() {
            return postTruncationMessagesLength;
        }

        public void setPostTruncationMessagesLength(double postTruncationMessagesLength) {
            this.postTruncationMessagesLength = postTruncationMessagesLength;
        }

        public double getTokensRemovedDuringTruncation() {
            return tokensRemovedDuringTruncation;
        }

        public void setTokensRemovedDuringTruncation(double tokensRemovedDuringTruncation) {
            this.tokensRemovedDuringTruncation = tokensRemovedDuringTruncation;
        }

        public double getMessagesRemovedDuringTruncation() {
            return messagesRemovedDuringTruncation;
        }

        public void setMessagesRemovedDuringTruncation(double messagesRemovedDuringTruncation) {
            this.messagesRemovedDuringTruncation = messagesRemovedDuringTruncation;
        }

        public String getPerformedBy() {
            return performedBy;
        }

        public void setPerformedBy(String performedBy) {
            this.performedBy = performedBy;
        }
    }
}
