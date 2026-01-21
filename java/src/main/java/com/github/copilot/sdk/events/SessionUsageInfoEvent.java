/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.usage_info
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionUsageInfoEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionUsageInfoData data;

    @Override
    public String getType() {
        return "session.usage_info";
    }

    public SessionUsageInfoData getData() {
        return data;
    }

    public void setData(SessionUsageInfoData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionUsageInfoData {

        @JsonProperty("tokenLimit")
        private double tokenLimit;

        @JsonProperty("currentTokens")
        private double currentTokens;

        @JsonProperty("messagesLength")
        private double messagesLength;

        public double getTokenLimit() {
            return tokenLimit;
        }

        public void setTokenLimit(double tokenLimit) {
            this.tokenLimit = tokenLimit;
        }

        public double getCurrentTokens() {
            return currentTokens;
        }

        public void setCurrentTokens(double currentTokens) {
            this.currentTokens = currentTokens;
        }

        public double getMessagesLength() {
            return messagesLength;
        }

        public void setMessagesLength(double messagesLength) {
            this.messagesLength = messagesLength;
        }
    }
}
