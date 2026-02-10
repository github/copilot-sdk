/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.truncation
 *
 * @since 1.0.0
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
    public record SessionTruncationData(@JsonProperty("tokenLimit") double tokenLimit,
            @JsonProperty("preTruncationTokensInMessages") double preTruncationTokensInMessages,
            @JsonProperty("preTruncationMessagesLength") double preTruncationMessagesLength,
            @JsonProperty("postTruncationTokensInMessages") double postTruncationTokensInMessages,
            @JsonProperty("postTruncationMessagesLength") double postTruncationMessagesLength,
            @JsonProperty("tokensRemovedDuringTruncation") double tokensRemovedDuringTruncation,
            @JsonProperty("messagesRemovedDuringTruncation") double messagesRemovedDuringTruncation,
            @JsonProperty("performedBy") String performedBy) {
    }
}
