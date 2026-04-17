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
 * The {@code session.compaction_start} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCompactionStartEvent extends SessionEvent {

    @Override
    public String getType() { return "session.compaction_start"; }

    @JsonProperty("data")
    private SessionCompactionStartEventData data;

    public SessionCompactionStartEventData getData() { return data; }
    public void setData(SessionCompactionStartEventData data) { this.data = data; }

    /** Data payload for {@link SessionCompactionStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCompactionStartEventData(
        /** Token count from system message(s) at compaction start */
        @JsonProperty("systemTokens") Double systemTokens,
        /** Token count from non-system messages (user, assistant, tool) at compaction start */
        @JsonProperty("conversationTokens") Double conversationTokens,
        /** Token count from tool definitions at compaction start */
        @JsonProperty("toolDefinitionsTokens") Double toolDefinitionsTokens
    ) {
    }
}
