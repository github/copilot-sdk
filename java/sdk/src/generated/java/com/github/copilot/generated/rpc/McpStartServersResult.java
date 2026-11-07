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
 * MCP server startup filtering result.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpStartServersResult(
    /** Servers filtered out before startup */
    @JsonProperty("filteredServers") List<McpFilteredServer> filteredServers,
    /** Non-default servers allowed by policy */
    @JsonProperty("allowedServers") List<McpAllowedServer> allowedServers,
    /** Servers whose connection attempt failed. */
    @JsonProperty("failedServers") List<McpFailedServer> failedServers
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    McpStartServersResult {}

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
        private List<McpFilteredServer> filteredServers;
        private List<McpAllowedServer> allowedServers;
        private List<McpFailedServer> failedServers;

        /**
         * Sets the {@code filteredServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder filteredServers(List<McpFilteredServer> value) {
            this.filteredServers = value;
            return this;
        }

        /**
         * Sets the {@code allowedServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allowedServers(List<McpAllowedServer> value) {
            this.allowedServers = value;
            return this;
        }

        /**
         * Sets the {@code failedServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder failedServers(List<McpFailedServer> value) {
            this.failedServers = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpStartServersResult build() {
            if (filteredServers == null) throw new IllegalStateException("Missing required property: filteredServers");
            return new McpStartServersResult(filteredServers, allowedServers, failedServers);
        }
    }
}
