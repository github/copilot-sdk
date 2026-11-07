/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Canonical identity and location of the effective MCP server declaration. The declaration is uniquely addressed by this source id together with the discovered server name.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpSourceRef(
    /** Open source-kind identifier. Known values include user, workspace, invocation, plugin, builtin, and device-registry. */
    @JsonProperty("kind") String kind,
    /** Opaque stable identity for the configuration source. Clients must not parse this value. */
    @JsonProperty("id") String id,
    /** Open semantic editability identifier. Known values are editable and read-only. */
    @JsonProperty("editability") String editability,
    /** Configuration file location, when the declaration is file-backed. */
    @JsonProperty("file") McpSourceFile file,
    /** Plugin identity, when the declaration is plugin-provided. */
    @JsonProperty("plugin") McpSourcePlugin plugin
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpSourceRef {}

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
        private String kind;
        private String id;
        private String editability;
        private McpSourceFile file;
        private McpSourcePlugin plugin;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(String value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code editability} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder editability(String value) {
            this.editability = value;
            return this;
        }

        /**
         * Sets the {@code file} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder file(McpSourceFile value) {
            this.file = value;
            return this;
        }

        /**
         * Sets the {@code plugin} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder plugin(McpSourcePlugin value) {
            this.plugin = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpSourceRef build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (editability == null) throw new IllegalStateException("Missing required property: editability");
            return new McpSourceRef(kind, id, editability, file, plugin);
        }
    }
}
