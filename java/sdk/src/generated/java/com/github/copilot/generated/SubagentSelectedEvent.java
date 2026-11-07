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
 * Session event "subagent.selected". Custom agent selection details including name and available tools
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SubagentSelectedEvent extends SessionEvent {

    @Override
    public String getType() { return "subagent.selected"; }

    @JsonProperty("data")
    private SubagentSelectedEventData data;

    public SubagentSelectedEventData getData() { return data; }
    public void setData(SubagentSelectedEventData data) { this.data = data; }

    /** Data payload for {@link SubagentSelectedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubagentSelectedEventData(
        /** Internal name of the selected custom agent */
        @JsonProperty("agentName") String agentName,
        /** Human-readable display name of the selected custom agent */
        @JsonProperty("agentDisplayName") String agentDisplayName,
        /** List of tool names available to this agent, or null for all tools */
        @JsonProperty("tools") List<String> tools
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SubagentSelectedEventData {}

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
            private String agentName;
            private String agentDisplayName;
            private List<String> tools;
            private boolean $presence2;

            /**
             * Sets the {@code agentName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agentName(String value) {
                this.agentName = value;
                return this;
            }

            /**
             * Sets the {@code agentDisplayName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agentDisplayName(String value) {
                this.agentDisplayName = value;
                return this;
            }

            /**
             * Sets the {@code tools} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder tools(List<String> value) {
                this.tools = value;
                this.$presence2 = true;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SubagentSelectedEventData build() {
                if (agentName == null) throw new IllegalStateException("Missing required property: agentName");
                if (agentDisplayName == null) throw new IllegalStateException("Missing required property: agentDisplayName");
                if (!$presence2) throw new IllegalStateException("Missing required property: tools");
                return new SubagentSelectedEventData(agentName, agentDisplayName, tools);
            }
        }
    }
}
