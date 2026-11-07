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
import javax.annotation.processing.Generated;

/**
 * Non-secret host-managed HTTP MCP server configuration. The containing map key is the stable managed identity; credentials are supplied dynamically by the host.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedMcpServerConfig(
    /** Human-readable catalog display name. */
    @JsonProperty("displayName") String displayName,
    /** Hosted MCP streamable HTTP endpoint. */
    @JsonProperty("url") String url,
    /** Tools to include. Defaults to all tools when omitted. */
    @JsonProperty("tools") List<String> tools,
    /** Timeout in milliseconds for tool discovery and tool calls. */
    @JsonProperty("timeout") Long timeout,
    /** Maximum dynamic-header cache lifetime in milliseconds. */
    @JsonProperty("headersRefreshTtlMs") Long headersRefreshTtlMs
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedMcpServerConfig {}

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
        private String displayName;
        private String url;
        private List<String> tools;
        private Long timeout;
        private Long headersRefreshTtlMs;

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayName(String value) {
            this.displayName = value;
            return this;
        }

        /**
         * Sets the {@code url} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder url(String value) {
            this.url = value;
            return this;
        }

        /**
         * Sets the {@code tools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tools(List<String> value) {
            this.tools = value;
            return this;
        }

        /**
         * Sets the {@code timeout} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder timeout(Long value) {
            this.timeout = value;
            return this;
        }

        /**
         * Sets the {@code headersRefreshTtlMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder headersRefreshTtlMs(Long value) {
            this.headersRefreshTtlMs = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedMcpServerConfig build() {
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (url == null) throw new IllegalStateException("Missing required property: url");
            return new ManagedMcpServerConfig(displayName, url, tools, timeout, headersRefreshTtlMs);
        }
    }
}
