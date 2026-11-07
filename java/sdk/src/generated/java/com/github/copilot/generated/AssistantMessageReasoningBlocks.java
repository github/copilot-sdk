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
 * Neutral provider-tagged reasoning content blocks preserved verbatim for round-tripping
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantMessageReasoningBlocks(
    /** Model provider that produced these reasoning blocks. */
    @JsonProperty("provider") String provider,
    /** Provider-native reasoning items or content blocks preserved verbatim, in order. A single response can carry several, and provider signatures or identifiers may depend on their exact content and ordering. */
    @JsonProperty("blocks") List<Object> blocks,
    /** Anthropic Messages assistant block ordering preserved when the legacy reasoning-only representation cannot reproduce it exactly. Thinking and text blocks remain verbatim; tool-use entries retain identity and a payload fingerprint when later signed reasoning depends on them, and are hydrated from the message's tool requests during replay. */
    @JsonProperty("orderedBlocks") List<Object> orderedBlocks
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AssistantMessageReasoningBlocks {}

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
        private List<Object> blocks;
        private List<Object> orderedBlocks;

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
         * Sets the {@code blocks} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder blocks(List<Object> value) {
            this.blocks = value;
            return this;
        }

        /**
         * Sets the {@code orderedBlocks} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder orderedBlocks(List<Object> value) {
            this.orderedBlocks = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AssistantMessageReasoningBlocks build() {
            if (provider == null) throw new IllegalStateException("Missing required property: provider");
            return new AssistantMessageReasoningBlocks(provider, blocks, orderedBlocks);
        }
    }
}
