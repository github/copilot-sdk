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
 * The {@code session.task_complete} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionTaskCompleteEvent extends SessionEvent {

    @Override
    public String getType() { return "session.task_complete"; }

    @JsonProperty("data")
    private SessionTaskCompleteEventData data;

    public SessionTaskCompleteEventData getData() { return data; }
    public void setData(SessionTaskCompleteEventData data) { this.data = data; }

    /** Data payload for {@link SessionTaskCompleteEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionTaskCompleteEventData(
        /** Summary of the completed task, provided by the agent */
        @JsonProperty("summary") String summary,
        /** Whether the tool call succeeded. False when validation failed (e.g., invalid arguments) */
        @JsonProperty("success") Boolean success
    ) {
    }
}
