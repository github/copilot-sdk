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
 * Session event "tool_search.activated". Persisted generic client-side tool activations restored when a session resumes.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolSearchActivatedEvent extends SessionEvent {

    @Override
    public String getType() { return "tool_search.activated"; }

    @JsonProperty("data")
    private ToolSearchActivatedEventData data;

    public ToolSearchActivatedEventData getData() { return data; }
    public void setData(ToolSearchActivatedEventData data) { this.data = data; }

    /** Data payload for {@link ToolSearchActivatedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolSearchActivatedEventData(
        /** Tool-search strategy that activated the definitions. */
        @JsonProperty("strategy") String strategy,
        /** Names of tool definitions activated by this search invocation. */
        @JsonProperty("toolNames") List<String> toolNames
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ToolSearchActivatedEventData {}

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
            private String strategy;
            private List<String> toolNames;

            /**
             * Sets the {@code strategy} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder strategy(String value) {
                this.strategy = value;
                return this;
            }

            /**
             * Sets the {@code toolNames} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolNames(List<String> value) {
                this.toolNames = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public ToolSearchActivatedEventData build() {
                if (strategy == null) throw new IllegalStateException("Missing required property: strategy");
                if (toolNames == null) throw new IllegalStateException("Missing required property: toolNames");
                return new ToolSearchActivatedEventData(strategy, toolNames);
            }
        }
    }
}
