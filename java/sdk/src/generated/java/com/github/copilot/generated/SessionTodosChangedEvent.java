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
 * Session event "session.todos_changed". Signal-only event: the agent's todos or todo_deps table was written to. No payload — clients should call session.plan.readSqlTodosWithDependencies() to fetch the current state. Events arrive in order; clients can debounce on arrival if needed.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionTodosChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.todos_changed"; }

    @JsonProperty("data")
    private SessionTodosChangedEventData data;

    public SessionTodosChangedEventData getData() { return data; }
    public void setData(SessionTodosChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionTodosChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionTodosChangedEventData() {
    }
}
