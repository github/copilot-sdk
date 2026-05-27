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
 * Session event "session.canvas.opened".
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCanvasOpenedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.canvas.opened"; }

    @JsonProperty("data")
    private SessionCanvasOpenedEventData data;

    public SessionCanvasOpenedEventData getData() { return data; }
    public void setData(SessionCanvasOpenedEventData data) { this.data = data; }

    /** Data payload for {@link SessionCanvasOpenedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCanvasOpenedEventData(
        /** Stable caller-supplied canvas instance identifier */
        @JsonProperty("instanceId") String instanceId,
        /** Owning provider identifier */
        @JsonProperty("extensionId") String extensionId,
        /** Owning extension display name, when available */
        @JsonProperty("extensionName") String extensionName,
        /** Provider-local canvas identifier */
        @JsonProperty("canvasId") String canvasId,
        /** Rendered title */
        @JsonProperty("title") String title,
        /** Provider-supplied status text */
        @JsonProperty("status") String status,
        /** URL for web-rendered canvases */
        @JsonProperty("url") String url,
        /** Input supplied when the instance was opened */
        @JsonProperty("input") Object input,
        /** Whether this notification represents an idempotent reopen */
        @JsonProperty("reopen") Boolean reopen,
        /** Runtime-controlled routing state for the instance. "ready" when the provider connection is live; "stale" when the provider has gone away and the instance is awaiting rebinding. */
        @JsonProperty("availability") CanvasOpenedAvailability availability
    ) {
    }
}
