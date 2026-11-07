/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Lightweight metadata for a currently initialized session tool
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrentToolMetadata(
    /** Model-facing tool name */
    @JsonProperty("name") String name,
    /** Optional MCP/config namespaced tool name */
    @JsonProperty("namespacedName") String namespacedName,
    /** Configured MCP server key for MCP-backed tools */
    @JsonProperty("mcpServerName") String mcpServerName,
    /** Raw MCP tool name for MCP-backed tools */
    @JsonProperty("mcpToolName") String mcpToolName,
    /** Tool description */
    @JsonProperty("description") String description,
    /** JSON Schema for tool input */
    @JsonProperty("input_schema") Map<String, Object> inputSchema,
    /** Whether the tool is loaded on demand via tool search */
    @JsonProperty("deferLoading") Boolean deferLoading,
    /** Explicitly configured MCP server display name. */
    @JsonProperty("mcpServerDisplayName") String mcpServerDisplayName
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CurrentToolMetadata {}

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
        private String namespacedName;
        private String mcpServerName;
        private String mcpToolName;
        private String description;
        private Map<String, Object> inputSchema;
        private Boolean deferLoading;
        private String mcpServerDisplayName;

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
         * Sets the {@code namespacedName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder namespacedName(String value) {
            this.namespacedName = value;
            return this;
        }

        /**
         * Sets the {@code mcpServerName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcpServerName(String value) {
            this.mcpServerName = value;
            return this;
        }

        /**
         * Sets the {@code mcpToolName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcpToolName(String value) {
            this.mcpToolName = value;
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
         * Sets the {@code input_schema} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder inputSchema(Map<String, Object> value) {
            this.inputSchema = value;
            return this;
        }

        /**
         * Sets the {@code deferLoading} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deferLoading(Boolean value) {
            this.deferLoading = value;
            return this;
        }

        /**
         * Sets the {@code mcpServerDisplayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcpServerDisplayName(String value) {
            this.mcpServerDisplayName = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CurrentToolMetadata build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            return new CurrentToolMetadata(name, namespacedName, mcpServerName, mcpToolName, description, inputSchema, deferLoading, mcpServerDisplayName);
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Model-facing tool name
     * @param namespacedName Optional MCP/config namespaced tool name
     * @param mcpServerName Configured MCP server key for MCP-backed tools
     * @param mcpToolName Raw MCP tool name for MCP-backed tools
     * @param description Tool description
     * @param inputSchema JSON Schema for tool input
     * @param deferLoading Whether the tool is loaded on demand via tool search
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CurrentToolMetadata(
        String name,
        String namespacedName,
        String mcpServerName,
        String mcpToolName,
        String description,
        Map<String, Object> inputSchema,
        Boolean deferLoading
    ) {
        this(name, namespacedName, mcpServerName, mcpToolName, description, inputSchema, deferLoading, null);
    }
}
