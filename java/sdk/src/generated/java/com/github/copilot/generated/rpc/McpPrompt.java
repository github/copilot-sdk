/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * An MCP prompt descriptor. Server-provided non-standard fields are exposed under `additionalProperties`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPrompt(
    /** The programmatic name of the prompt */
    @JsonProperty("name") String name,
    /** Human-readable display title */
    @JsonProperty("title") String title,
    /** Description of what this prompt provides */
    @JsonProperty("description") String description,
    /** Arguments accepted by the prompt */
    @JsonProperty("arguments") List<McpPromptArgument> arguments,
    /** Icons associated with this prompt */
    @JsonProperty("icons") List<McpPromptIcon> icons,
    /** Prompt-level metadata */
    @JsonProperty("_meta") Map<String, Object> meta,
    /** Server-provided non-standard descriptor fields */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpPrompt {}

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
        private String name;
        private String title;
        private String description;
        private List<McpPromptArgument> arguments;
        private List<McpPromptIcon> icons;
        private Map<String, Object> meta;
        private Map<String, Object> additionalProperties;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code title} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder title(String value) {
            this.title = value;
            return this;
        }

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
         * Sets the {@code arguments} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder arguments(List<McpPromptArgument> value) {
            this.arguments = value;
            return this;
        }

        /**
         * Sets the {@code icons} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder icons(List<McpPromptIcon> value) {
            this.icons = value;
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
        public McpPrompt build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            return new McpPrompt(name, title, description, arguments, icons, meta, additionalProperties);
        }
    }
}
