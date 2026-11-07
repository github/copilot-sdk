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
 * Public MCP skill provenance. The server name and resource URI address session.mcp.resources.read without invoking the skill; they do not grant permission or identify a durable server registration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpSkillOrigin(
    /** Name of the MCP server hosting the skill, matching serverName in session.mcp.resources.read. */
    @JsonProperty("serverName") String serverName,
    /** Exact URI of the skill's SKILL.md resource, matching uri in session.mcp.resources.read. */
    @JsonProperty("uri") String uri
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpSkillOrigin {}

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
        private String serverName;
        private String uri;

        /**
         * Sets the {@code serverName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverName(String value) {
            this.serverName = value;
            return this;
        }

        /**
         * Sets the {@code uri} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder uri(String value) {
            this.uri = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpSkillOrigin build() {
            if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
            if (uri == null) throw new IllegalStateException("Missing required property: uri");
            return new McpSkillOrigin(serverName, uri);
        }
    }
}
