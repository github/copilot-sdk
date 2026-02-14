/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.context_changed
 * <p>
 * Fired when the working directory context changes between turns. Contains the
 * updated context information including cwd, git root, repository, and branch.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionContextChangedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionContextChangedData data;

    @Override
    public String getType() {
        return "session.context_changed";
    }

    public SessionContextChangedData getData() {
        return data;
    }

    public void setData(SessionContextChangedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionContextChangedData(@JsonProperty("cwd") String cwd, @JsonProperty("gitRoot") String gitRoot,
            @JsonProperty("repository") String repository, @JsonProperty("branch") String branch) {
    }
}
