/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Event: assistant.usage
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AssistantUsageEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private AssistantUsageData data;

    @Override
    public String getType() {
        return "assistant.usage";
    }

    public AssistantUsageData getData() {
        return data;
    }

    public void setData(AssistantUsageData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssistantUsageData {

        @JsonProperty("model")
        private String model;

        @JsonProperty("inputTokens")
        private Double inputTokens;

        @JsonProperty("outputTokens")
        private Double outputTokens;

        @JsonProperty("cacheReadTokens")
        private Double cacheReadTokens;

        @JsonProperty("cacheWriteTokens")
        private Double cacheWriteTokens;

        @JsonProperty("cost")
        private Double cost;

        @JsonProperty("duration")
        private Double duration;

        @JsonProperty("initiator")
        private String initiator;

        @JsonProperty("apiCallId")
        private String apiCallId;

        @JsonProperty("providerCallId")
        private String providerCallId;

        @JsonProperty("quotaSnapshots")
        private Map<String, Object> quotaSnapshots;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Double getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(Double inputTokens) {
            this.inputTokens = inputTokens;
        }

        public Double getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(Double outputTokens) {
            this.outputTokens = outputTokens;
        }

        public Double getCacheReadTokens() {
            return cacheReadTokens;
        }

        public void setCacheReadTokens(Double cacheReadTokens) {
            this.cacheReadTokens = cacheReadTokens;
        }

        public Double getCacheWriteTokens() {
            return cacheWriteTokens;
        }

        public void setCacheWriteTokens(Double cacheWriteTokens) {
            this.cacheWriteTokens = cacheWriteTokens;
        }

        public Double getCost() {
            return cost;
        }

        public void setCost(Double cost) {
            this.cost = cost;
        }

        public Double getDuration() {
            return duration;
        }

        public void setDuration(Double duration) {
            this.duration = duration;
        }

        public String getInitiator() {
            return initiator;
        }

        public void setInitiator(String initiator) {
            this.initiator = initiator;
        }

        public String getApiCallId() {
            return apiCallId;
        }

        public void setApiCallId(String apiCallId) {
            this.apiCallId = apiCallId;
        }

        public String getProviderCallId() {
            return providerCallId;
        }

        public void setProviderCallId(String providerCallId) {
            this.providerCallId = providerCallId;
        }

        public Map<String, Object> getQuotaSnapshots() {
            return quotaSnapshots;
        }

        public void setQuotaSnapshots(Map<String, Object> quotaSnapshots) {
            this.quotaSnapshots = quotaSnapshots;
        }
    }
}
