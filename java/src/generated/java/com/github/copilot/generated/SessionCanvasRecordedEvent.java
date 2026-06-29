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
 * Session event "session.canvas.recorded". Durable record that a canvas instance is open, used to restore open canvases on cold session resume. Intentionally omits the transient url and availability.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCanvasRecordedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.canvas.recorded"; }

    @JsonProperty("data")
    private SessionCanvasRecordedEventData data;

    public SessionCanvasRecordedEventData getData() { return data; }
    public void setData(SessionCanvasRecordedEventData data) { this.data = data; }

    /** Data payload for {@link SessionCanvasRecordedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCanvasRecordedEventData(
        /** Stable caller-supplied canvas instance identifier */
        @JsonProperty("instanceId") String instanceId,
        /** Owning provider identifier */
        @JsonProperty("extensionId") String extensionId,
        /** Provider-local canvas identifier */
        @JsonProperty("canvasId") String canvasId,
        /** Rendered title */
        @JsonProperty("title") String title,
        /** Input supplied when the instance was opened */
        @JsonProperty("input") Object input
    ) {
    }
}
