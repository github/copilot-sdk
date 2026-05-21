/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Session lifecycle event notification.
 * <p>
 * Lifecycle events are emitted when sessions are created, deleted, updated, or
 * change foreground/background state (in TUI+server mode).
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionLifecycleEvent {

    @JsonProperty("type")
    private String type;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("metadata")
    private SessionLifecycleEventMetadata metadata;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public SessionLifecycleEventMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(SessionLifecycleEventMetadata metadata) {
        this.metadata = metadata;
    }
}
