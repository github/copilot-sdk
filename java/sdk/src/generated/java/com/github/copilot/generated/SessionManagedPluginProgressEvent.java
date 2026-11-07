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
 * Session event "session.managed_plugin_progress". Experimental transient presentation-neutral progress for organization-required plugin preparation. Clients should localize the phase copy and display plugin specs without translating them.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionManagedPluginProgressEvent extends SessionEvent {

    @Override
    public String getType() { return "session.managed_plugin_progress"; }

    @JsonProperty("data")
    private SessionManagedPluginProgressEventData data;

    public SessionManagedPluginProgressEventData getData() { return data; }
    public void setData(SessionManagedPluginProgressEventData data) { this.data = data; }

    /** Data payload for {@link SessionManagedPluginProgressEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionManagedPluginProgressEventData(
        /** Current preparation phase. */
        @JsonProperty("phase") ManagedPluginProgressPhase phase,
        /** Ordered plugin install specs affected by this phase. Empty while managed settings initialize and when preparation completes. */
        @JsonProperty("pluginSpecs") List<String> pluginSpecs
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionManagedPluginProgressEventData {}

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
            private ManagedPluginProgressPhase phase;
            private List<String> pluginSpecs;

            /**
             * Sets the {@code phase} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder phase(ManagedPluginProgressPhase value) {
                this.phase = value;
                return this;
            }

            /**
             * Sets the {@code pluginSpecs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder pluginSpecs(List<String> value) {
                this.pluginSpecs = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionManagedPluginProgressEventData build() {
                if (phase == null) throw new IllegalStateException("Missing required property: phase");
                if (pluginSpecs == null) throw new IllegalStateException("Missing required property: pluginSpecs");
                return new SessionManagedPluginProgressEventData(phase, pluginSpecs);
            }
        }
    }
}
