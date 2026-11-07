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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "session.canvas.registry_changed". Payload of `session.canvas.registry_changed` listing the canvas declarations currently available.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
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
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCanvasRegistryChangedEventData(
        /** Canvas declarations currently available */
        @JsonProperty("canvases") List<CanvasRegistryChangedCanvas> canvases
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionCanvasRegistryChangedEventData {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public static final class Builder {
            private Builder() {}
            private List<CanvasRegistryChangedCanvas> canvases;

            /**
             * Sets the {@code canvases} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder canvases(List<CanvasRegistryChangedCanvas> value) {
                this.canvases = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionCanvasRegistryChangedEventData build() {
                if (canvases == null) throw new IllegalStateException("Missing required property: canvases");
                return new SessionCanvasRegistryChangedEventData(canvases);
            }
        }
    }
}
