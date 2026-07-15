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
 * Session event "session.memory_changed". Signal-only event: the agent successfully stored a memory (store_memory) or voted on one (vote_memory). No payload — consumers should re-fetch memories to pick up the change. Used to refresh memory context (e.g. re-running the context sidekick) so newly written memories surface in subsequent turns.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionMemoryChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.memory_changed"; }

    @JsonProperty("data")
    private SessionMemoryChangedEventData data;

    public SessionMemoryChangedEventData getData() { return data; }
    public void setData(SessionMemoryChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionMemoryChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMemoryChangedEventData() {
    }
}
