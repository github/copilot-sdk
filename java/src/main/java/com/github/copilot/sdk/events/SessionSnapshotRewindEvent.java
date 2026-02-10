/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.snapshot_rewind
 * <p>
 * Indicates that the session has been rewound to a previous snapshot.
 *
 * @since 1.0.4
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionSnapshotRewindEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionSnapshotRewindData data;

    @Override
    public String getType() {
        return "session.snapshot_rewind";
    }

    public SessionSnapshotRewindData getData() {
        return data;
    }

    public void setData(SessionSnapshotRewindData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionSnapshotRewindData(@JsonProperty("upToEventId") String upToEventId,
            @JsonProperty("eventsRemoved") int eventsRemoved) {
    }
}
