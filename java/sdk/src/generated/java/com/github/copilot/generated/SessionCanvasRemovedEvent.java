/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Session event "session.canvas.removed". Durable record that a canvas instance was closed, superseding a prior instance_recorded during resume replay.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCanvasRemovedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.canvas.removed"; }

    @JsonProperty("data")
    private SessionCanvasRemovedEventData data;

    public SessionCanvasRemovedEventData getData() { return data; }
    public void setData(SessionCanvasRemovedEventData data) { this.data = data; }

    /** Data payload for {@link SessionCanvasRemovedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCanvasRemovedEventData(
        /** Stable caller-supplied identifier of the canvas instance that was closed */
        @JsonProperty("instanceId") String instanceId,
        /** Owning provider identifier */
        @JsonProperty("extensionId") String extensionId,
        /** Provider-local canvas identifier */
        @JsonProperty("canvasId") String canvasId
    ) {
    }
}
