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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * A session-only local IDE endpoint. Starting it never writes persistent configuration.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpServerConfigLocal(
    /** Local transport discriminator. */
    @JsonProperty("type") McpServerConfigLocalType type,
    /** Existing Unix socket or Windows named pipe on the runtime host. */
    @JsonProperty("socketPath") String socketPath,
    /** Endpoint headers from IDE discovery. */
    @JsonProperty("headers") Map<String, String> headers,
    /** IDE identity and initialize metadata. The server name must be `ide`. */
    @JsonProperty("ide") McpIdeConfig ide
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpServerConfigLocal {}

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
        private McpServerConfigLocalType type;
        private String socketPath;
        private Map<String, String> headers;
        private McpIdeConfig ide;

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder type(McpServerConfigLocalType value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code socketPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder socketPath(String value) {
            this.socketPath = value;
            return this;
        }

        /**
         * Sets the {@code headers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder headers(Map<String, String> value) {
            this.headers = value;
            return this;
        }

        /**
         * Sets the {@code ide} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder ide(McpIdeConfig value) {
            this.ide = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpServerConfigLocal build() {
            if (type == null) throw new IllegalStateException("Missing required property: type");
            if (socketPath == null) throw new IllegalStateException("Missing required property: socketPath");
            if (ide == null) throw new IllegalStateException("Missing required property: ide");
            return new McpServerConfigLocal(type, socketPath, headers, ide);
        }
    }
}
