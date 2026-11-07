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
import javax.annotation.processing.Generated;

/**
 * One page of resources advertised by the named MCP server.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpResourcesListResult(
    /** Resources advertised by the server (proxied MCP `resources/list`) */
    @JsonProperty("resources") List<McpResource> resources,
    /** Opaque cursor for the next page, if the server has more resources */
    @JsonProperty("nextCursor") String nextCursor
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMcpResourcesListResult {}

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
        private List<McpResource> resources;
        private String nextCursor;

        /**
         * Sets the {@code resources} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resources(List<McpResource> value) {
            this.resources = value;
            return this;
        }

        /**
         * Sets the {@code nextCursor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder nextCursor(String value) {
            this.nextCursor = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMcpResourcesListResult build() {
            if (resources == null) throw new IllegalStateException("Missing required property: resources");
            return new SessionMcpResourcesListResult(resources, nextCursor);
        }
    }
}
