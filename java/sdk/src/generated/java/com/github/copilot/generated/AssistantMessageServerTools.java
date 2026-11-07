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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Neutral provider-tagged server-side tool-use payload (tool search, advisor) for verbatim round-tripping
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantMessageServerTools(
    /** Model provider that produced this server-tool payload. */
    @JsonProperty("provider") String provider,
    /** Provider-native server-tool call and output items preserved verbatim for replay. */
    @JsonProperty("items") List<Object> items,
    /** Provider function-call namespaces keyed by function-call identifier. */
    @JsonProperty("functionCallNamespaces") Map<String, String> functionCallNamespaces,
    /** Raw provider content blocks retained for verbatim round-tripping. */
    @JsonProperty("rawContentBlocks") List<Object> rawContentBlocks,
    /** Advisor model identifier associated with the server-tool payload. */
    @JsonProperty("advisorModel") String advisorModel
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AssistantMessageServerTools {}

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
        private String provider;
        private List<Object> items;
        private Map<String, String> functionCallNamespaces;
        private List<Object> rawContentBlocks;
        private String advisorModel;

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder provider(String value) {
            this.provider = value;
            return this;
        }

        /**
         * Sets the {@code items} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder items(List<Object> value) {
            this.items = value;
            return this;
        }

        /**
         * Sets the {@code functionCallNamespaces} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder functionCallNamespaces(Map<String, String> value) {
            this.functionCallNamespaces = value;
            return this;
        }

        /**
         * Sets the {@code rawContentBlocks} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder rawContentBlocks(List<Object> value) {
            this.rawContentBlocks = value;
            return this;
        }

        /**
         * Sets the {@code advisorModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder advisorModel(String value) {
            this.advisorModel = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AssistantMessageServerTools build() {
            if (provider == null) throw new IllegalStateException("Missing required property: provider");
            return new AssistantMessageServerTools(provider, items, functionCallNamespaces, rawContentBlocks, advisorModel);
        }
    }
}
