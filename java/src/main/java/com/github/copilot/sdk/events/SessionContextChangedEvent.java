/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.sdk.json.SessionContext;

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
    private SessionContext data;

    @Override
    public String getType() {
        return "session.context_changed";
    }

    public SessionContext getData() {
        return data;
    }

    public void setData(SessionContext data) {
        this.data = data;
    }
}
