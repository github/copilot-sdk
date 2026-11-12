/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "session.context_cleared". Context-cleared details emitted when the host clears the conversation (the session.history.clearContext RPC / Session.clearContextMessages)
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionContextClearedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.context_cleared"; }

    @JsonProperty("data")
    private SessionContextClearedEventData data;

    public SessionContextClearedEventData getData() { return data; }
    public void setData(SessionContextClearedEventData data) { this.data = data; }

    /** Data payload for {@link SessionContextClearedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionContextClearedEventData(
        /** Optional initial message set after clearing */
        @JsonProperty("initialMessage") String initialMessage,
        /** Number of conversation messages that were cleared */
        @JsonProperty("messagesCleared") Long messagesCleared
    ) {
    }
}
