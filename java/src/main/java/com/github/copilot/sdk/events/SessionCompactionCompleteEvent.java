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
    public record SessionCompactionCompleteData(@JsonProperty("success") boolean success,
            @JsonProperty("error") String error, @JsonProperty("preCompactionTokens") Double preCompactionTokens,
            @JsonProperty("postCompactionTokens") Double postCompactionTokens,
            @JsonProperty("preCompactionMessagesLength") Double preCompactionMessagesLength,
            @JsonProperty("messagesRemoved") Double messagesRemoved,
            @JsonProperty("tokensRemoved") Double tokensRemoved, @JsonProperty("summaryContent") String summaryContent,
            @JsonProperty("checkpointNumber") Double checkpointNumber,
            @JsonProperty("checkpointPath") String checkpointPath,
            @JsonProperty("compactionTokensUsed") CompactionTokensUsed compactionTokensUsed,
            @JsonProperty("requestId") String requestId) {
    }

    /**
     * Token usage information for the compaction operation.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompactionTokensUsed(@JsonProperty("input") double input, @JsonProperty("output") double output,
            @JsonProperty("cachedInput") double cachedInput) {
    }
}
