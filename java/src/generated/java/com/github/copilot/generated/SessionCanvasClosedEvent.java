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
 * Session event "session.canvas.closed".
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCanvasClosedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.canvas.closed"; }

    @JsonProperty("data")
    private SessionCanvasClosedEventData data;

    public SessionCanvasClosedEventData getData() { return data; }
    public void setData(SessionCanvasClosedEventData data) { this.data = data; }

    /** Data payload for {@link SessionCanvasClosedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCanvasClosedEventData(
        /** Stable caller-supplied identifier of the canvas instance that was closed */
        @JsonProperty("instanceId") String instanceId,
        /** Owning provider identifier */
        @JsonProperty("extensionId") String extensionId,
        /** Provider-local canvas identifier */
        @JsonProperty("canvasId") String canvasId
    ) {
    }
}
