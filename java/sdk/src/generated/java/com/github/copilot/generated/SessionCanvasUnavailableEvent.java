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
 * Session event "session.canvas.unavailable". Transient signal that an open canvas instance's provider has dropped (for example the extension is reloading mid-session). The host should keep the panel mounted and surface a reconnecting affordance rather than tearing it down; a subsequent `session.canvas.opened` for the same instanceId clears the affordance once the provider reconnects with a fresh url. Ephemeral and never persisted, so it is never replayed on cold resume.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
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
    @CopilotExperimental
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

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionCanvasUnavailableEventData {}

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
            private String instanceId;
            private String extensionId;
            private String canvasId;

            /**
             * Sets the {@code instanceId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder instanceId(String value) {
                this.instanceId = value;
                return this;
            }

            /**
             * Sets the {@code extensionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder extensionId(String value) {
                this.extensionId = value;
                return this;
            }

            /**
             * Sets the {@code canvasId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder canvasId(String value) {
                this.canvasId = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionCanvasUnavailableEventData build() {
                if (instanceId == null) throw new IllegalStateException("Missing required property: instanceId");
                if (extensionId == null) throw new IllegalStateException("Missing required property: extensionId");
                if (canvasId == null) throw new IllegalStateException("Missing required property: canvasId");
                return new SessionCanvasUnavailableEventData(instanceId, extensionId, canvasId);
            }
        }
    }
}
