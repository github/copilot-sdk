/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.workspace_file_changed
 *
 * @since 1.0.10
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionWorkspaceFileChangedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionWorkspaceFileChangedData data;

    @Override
    public String getType() {
        return "session.workspace_file_changed";
    }

    public SessionWorkspaceFileChangedData getData() {
        return data;
    }

    public void setData(SessionWorkspaceFileChangedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionWorkspaceFileChangedData(@JsonProperty("path") String path,
            @JsonProperty("operation") String operation) {
    }
}
