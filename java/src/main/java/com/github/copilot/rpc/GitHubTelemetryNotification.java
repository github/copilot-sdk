/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.github.copilot.CopilotExperimental;

/**
 * Payload for a {@code gitHubTelemetry.event} notification: a single GitHub
 * telemetry event the runtime forwards to a host connection that opted into
 * telemetry redirection for the session.
 *
 * <p>
 * Internal/experimental: this type is part of the GitHub telemetry redirection
 * surface and may change or be removed without notice.
 *
 * @since 1.0.0
 */
@CopilotExperimental
public class GitHubTelemetryNotification {

    @JsonProperty("event")
    private GitHubTelemetryEvent event = new GitHubTelemetryEvent();

    @JsonProperty("restricted")
    private boolean restricted;

    @JsonProperty("sessionId")
    private String sessionId = "";

    /**
     * Gets the telemetry event, in the runtime's native GitHub-shaped telemetry
     * format.
     *
     * @return the telemetry event
     */
    public GitHubTelemetryEvent getEvent() {
        return event;
    }

    /**
     * Gets whether this is a restricted telemetry event (cli.restricted_telemetry).
     * Hosts must route restricted events to first-party Microsoft stores only.
     *
     * @return {@code true} if the event is restricted
     */
    public boolean isRestricted() {
        return restricted;
    }

    /**
     * Gets the session the telemetry event belongs to.
     *
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }
}
