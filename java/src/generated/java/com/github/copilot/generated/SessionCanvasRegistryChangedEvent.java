/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "session.canvas.registry_changed".
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCanvasRegistryChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.canvas.registry_changed"; }

    @JsonProperty("data")
    private SessionCanvasRegistryChangedEventData data;

    public SessionCanvasRegistryChangedEventData getData() { return data; }
    public void setData(SessionCanvasRegistryChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionCanvasRegistryChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCanvasRegistryChangedEventData(
        /** Canvas declarations currently available */
        @JsonProperty("canvases") List<CanvasRegistryChangedCanvas> canvases
    ) {
    }
}
