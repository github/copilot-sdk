/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Event: assistant.usage
 *
 * @since 1.0.0
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
    public record AssistantUsageData(@JsonProperty("model") String model,
            @JsonProperty("inputTokens") Double inputTokens, @JsonProperty("outputTokens") Double outputTokens,
            @JsonProperty("cacheReadTokens") Double cacheReadTokens,
            @JsonProperty("cacheWriteTokens") Double cacheWriteTokens, @JsonProperty("cost") Double cost,
            @JsonProperty("duration") Double duration, @JsonProperty("initiator") String initiator,
            @JsonProperty("apiCallId") String apiCallId, @JsonProperty("providerCallId") String providerCallId,
            @JsonProperty("parentToolCallId") String parentToolCallId,
            @JsonProperty("quotaSnapshots") Map<String, Object> quotaSnapshots,
            @JsonProperty("copilotUsage") CopilotUsage copilotUsage) {

        /** Returns a defensive copy of the quota snapshots map. */
        @Override
        public Map<String, Object> quotaSnapshots() {
            return quotaSnapshots == null ? Collections.emptyMap() : Collections.unmodifiableMap(quotaSnapshots);
        }
    }

    /**
     * Copilot usage details including token cost information.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CopilotUsage(@JsonProperty("tokenDetails") List<TokenDetails> tokenDetails,
            @JsonProperty("totalNanoAiu") double totalNanoAiu) {

        /** Returns a defensive copy of the token details list. */
        @Override
        public List<TokenDetails> tokenDetails() {
            return tokenDetails == null ? null : Collections.unmodifiableList(tokenDetails);
        }
    }

    /**
     * Token details for a specific token type.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenDetails(@JsonProperty("batchSize") double batchSize,
            @JsonProperty("costPerBatch") double costPerBatch, @JsonProperty("tokenCount") double tokenCount,
            @JsonProperty("tokenType") String tokenType) {
    }
}
