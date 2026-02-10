/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.compaction_start
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionCompactionStartEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionCompactionStartData data;

    @Override
    public String getType() {
        return "session.compaction_start";
    }

    public SessionCompactionStartData getData() {
        return data;
    }

    public void setData(SessionCompactionStartData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionCompactionStartData() {
    }
}
