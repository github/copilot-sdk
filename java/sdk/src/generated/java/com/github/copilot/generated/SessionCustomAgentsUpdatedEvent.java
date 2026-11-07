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
 * Session event "session.custom_agents_updated". Payload of `session.custom_agents_updated` with loaded custom agents plus non-fatal warnings and fatal errors.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCustomAgentsUpdatedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.custom_agents_updated"; }

    @JsonProperty("data")
    private SessionCustomAgentsUpdatedEventData data;

    public SessionCustomAgentsUpdatedEventData getData() { return data; }
    public void setData(SessionCustomAgentsUpdatedEventData data) { this.data = data; }

    /** Data payload for {@link SessionCustomAgentsUpdatedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCustomAgentsUpdatedEventData(
        /** Array of loaded custom agent metadata */
        @JsonProperty("agents") List<CustomAgentsUpdatedAgent> agents,
        /** Non-fatal warnings from agent loading */
        @JsonProperty("warnings") List<String> warnings,
        /** Fatal errors from agent loading */
        @JsonProperty("errors") List<String> errors
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionCustomAgentsUpdatedEventData {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        public static final class Builder {
            private Builder() {}
            private List<CustomAgentsUpdatedAgent> agents;
            private List<String> warnings;
            private List<String> errors;

            /**
             * Sets the {@code agents} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agents(List<CustomAgentsUpdatedAgent> value) {
                this.agents = value;
                return this;
            }

            /**
             * Sets the {@code warnings} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder warnings(List<String> value) {
                this.warnings = value;
                return this;
            }

            /**
             * Sets the {@code errors} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder errors(List<String> value) {
                this.errors = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionCustomAgentsUpdatedEventData build() {
                if (agents == null) throw new IllegalStateException("Missing required property: agents");
                if (warnings == null) throw new IllegalStateException("Missing required property: warnings");
                if (errors == null) throw new IllegalStateException("Missing required property: errors");
                return new SessionCustomAgentsUpdatedEventData(agents, warnings, errors);
            }
        }
    }
}
