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
 * The {@code session.usage_info} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionUsageInfoEvent extends SessionEvent {

    @Override
    public String getType() { return "session.usage_info"; }

    @JsonProperty("data")
    private SessionUsageInfoEventData data;

    public SessionUsageInfoEventData getData() { return data; }
    public void setData(SessionUsageInfoEventData data) { this.data = data; }

    /** Data payload for {@link SessionUsageInfoEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionUsageInfoEventData(
        /** Maximum token count for the model's context window */
        @JsonProperty("tokenLimit") Double tokenLimit,
        /** Current number of tokens in the context window */
        @JsonProperty("currentTokens") Double currentTokens,
        /** Current number of messages in the conversation */
        @JsonProperty("messagesLength") Double messagesLength,
        /** Token count from system message(s) */
        @JsonProperty("systemTokens") Double systemTokens,
        /** Token count from non-system messages (user, assistant, tool) */
        @JsonProperty("conversationTokens") Double conversationTokens,
        /** Token count from tool definitions */
        @JsonProperty("toolDefinitionsTokens") Double toolDefinitionsTokens,
        /** Whether this is the first usage_info event emitted in this session */
        @JsonProperty("isInitial") Boolean isInitial
    ) {
    }
}
