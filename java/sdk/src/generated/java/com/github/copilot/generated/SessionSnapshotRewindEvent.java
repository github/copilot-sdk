/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "session.snapshot_rewind". Session rewind details including target event and count of removed events
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionSnapshotRewindEvent extends SessionEvent {

    @Override
    public String getType() { return "session.snapshot_rewind"; }

    @JsonProperty("data")
    private SessionSnapshotRewindEventData data;

    public SessionSnapshotRewindEventData getData() { return data; }
    public void setData(SessionSnapshotRewindEventData data) { this.data = data; }

    /** Data payload for {@link SessionSnapshotRewindEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionSnapshotRewindEventData(
        /** First removed event. Without `eventIds`, it and every event after it were removed */
        @JsonProperty("upToEventId") String upToEventId,
        /** Number of events that were removed by the rewind */
        @JsonProperty("eventsRemoved") Long eventsRemoved,
        /** The removed events, starting with `upToEventId`. Later events not listed were kept, such as a background agent's events that interleaved with a withdrawn turn */
        @JsonProperty("eventIds") List<String> eventIds
    ) {
    }
}
