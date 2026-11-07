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
 * One page of prompts advertised by the named MCP server.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpPromptsListResult(
    /** Prompts advertised by the server */
    @JsonProperty("prompts") List<McpPrompt> prompts,
    /** Opaque cursor for the next page, if the server has more prompts */
    @JsonProperty("nextCursor") String nextCursor,
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
    public SessionMcpPromptsListResult {}

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
        private List<McpPrompt> prompts;
        private String nextCursor;
        private Map<String, Object> meta;
        private Map<String, Object> additionalProperties;

        /**
         * Sets the {@code prompts} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder prompts(List<McpPrompt> value) {
            this.prompts = value;
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
        public SessionMcpPromptsListResult build() {
            if (prompts == null) throw new IllegalStateException("Missing required property: prompts");
            return new SessionMcpPromptsListResult(prompts, nextCursor, meta, additionalProperties);
        }
    }
}
