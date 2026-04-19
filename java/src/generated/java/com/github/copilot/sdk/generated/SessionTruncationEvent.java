/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The {@code session.truncation} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionTruncationEvent extends SessionEvent {

    @Override
    public String getType() { return "session.truncation"; }

    @JsonProperty("data")
    private SessionTruncationEventData data;

    public SessionTruncationEventData getData() { return data; }
    public void setData(SessionTruncationEventData data) { this.data = data; }

    /** Data payload for {@link SessionTruncationEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionTruncationEventData(
        /** Maximum token count for the model's context window */
        @JsonProperty("tokenLimit") Double tokenLimit,
        /** Total tokens in conversation messages before truncation */
        @JsonProperty("preTruncationTokensInMessages") Double preTruncationTokensInMessages,
        /** Number of conversation messages before truncation */
        @JsonProperty("preTruncationMessagesLength") Double preTruncationMessagesLength,
        /** Total tokens in conversation messages after truncation */
        @JsonProperty("postTruncationTokensInMessages") Double postTruncationTokensInMessages,
        /** Number of conversation messages after truncation */
        @JsonProperty("postTruncationMessagesLength") Double postTruncationMessagesLength,
        /** Number of tokens removed by truncation */
        @JsonProperty("tokensRemovedDuringTruncation") Double tokensRemovedDuringTruncation,
        /** Number of messages removed by truncation */
        @JsonProperty("messagesRemovedDuringTruncation") Double messagesRemovedDuringTruncation,
        /** Identifier of the component that performed truncation (e.g., "BasicTruncator") */
        @JsonProperty("performedBy") String performedBy
    ) {
    }
}
