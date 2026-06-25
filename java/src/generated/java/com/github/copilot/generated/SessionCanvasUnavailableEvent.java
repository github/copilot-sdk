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
 * Session event "session.canvas.unavailable". Transient signal that an open canvas instance's provider has dropped (for example the extension is reloading mid-session). The host should keep the panel mounted and surface a reconnecting affordance rather than tearing it down; a subsequent `session.canvas.opened` for the same instanceId clears the affordance once the provider reconnects with a fresh url. Ephemeral and never persisted, so it is never replayed on cold resume.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCanvasUnavailableEvent extends SessionEvent {

    @Override
    public String getType() { return "session.canvas.unavailable"; }

    @JsonProperty("data")
    private SessionCanvasUnavailableEventData data;

    public SessionCanvasUnavailableEventData getData() { return data; }
    public void setData(SessionCanvasUnavailableEventData data) { this.data = data; }

    /** Data payload for {@link SessionCanvasUnavailableEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCanvasUnavailableEventData(
        /** Stable caller-supplied identifier of the canvas instance whose provider became unavailable */
        @JsonProperty("instanceId") String instanceId,
        /** Owning provider identifier */
        @JsonProperty("extensionId") String extensionId,
        /** Provider-local canvas identifier */
        @JsonProperty("canvasId") String canvasId
    ) {
    }
}
