/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Prompt messages returned by the MCP server without sending them to the model.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpPromptsGetResult(
    /** Description of the prompt */
    @JsonProperty("description") String description,
    /** Ordered prompt messages */
    @JsonProperty("messages") List<McpPromptMessage> messages,
    /** MCP result metadata */
    @JsonProperty("_meta") Map<String, Object> meta,
    /** Server-provided non-standard result fields */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMcpPromptsGetResult {}

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
        private String description;
        private List<McpPromptMessage> messages;
        private Map<String, Object> meta;
        private Map<String, Object> additionalProperties;

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code messages} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder messages(List<McpPromptMessage> value) {
            this.messages = value;
            return this;
        }

        /**
         * Sets the {@code _meta} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder meta(Map<String, Object> value) {
            this.meta = value;
            return this;
        }

        /**
         * Sets the {@code additionalProperties} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder additionalProperties(Map<String, Object> value) {
            this.additionalProperties = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMcpPromptsGetResult build() {
            if (messages == null) throw new IllegalStateException("Missing required property: messages");
            return new SessionMcpPromptsGetResult(description, messages, meta, additionalProperties);
        }
    }
}
