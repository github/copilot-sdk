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
 * Host-level state, omitted when no MCP host is initialized.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpHostState(
    /** Whether third-party MCP servers are policy-enabled for this session. */
    @JsonProperty("mcp3pEnabled") Boolean mcp3pEnabled,
    /** Configured servers that are explicitly disabled. */
    @JsonProperty("disabledServers") List<String> disabledServers,
    /** Configured servers filtered out by MCP server policy. */
    @JsonProperty("filteredServers") List<String> filteredServers,
    /** Names of currently-connected MCP clients. */
    @JsonProperty("clients") List<String> clients,
    /** Names of servers with in-flight connection attempts. */
    @JsonProperty("pendingConnections") List<String> pendingConnections,
    /** Map of server name to recorded connection failure. */
    @JsonProperty("failedServers") Map<String, McpServerFailureInfo> failedServers,
    /** Map of server name to recorded pending-auth state. */
    @JsonProperty("needsAuthServers") Map<String, McpServerNeedsAuthInfo> needsAuthServers
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpHostState {}

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
        private Boolean mcp3pEnabled;
        private List<String> disabledServers;
        private List<String> filteredServers;
        private List<String> clients;
        private List<String> pendingConnections;
        private Map<String, McpServerFailureInfo> failedServers;
        private Map<String, McpServerNeedsAuthInfo> needsAuthServers;

        /**
         * Sets the {@code mcp3pEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcp3pEnabled(Boolean value) {
            this.mcp3pEnabled = value;
            return this;
        }

        /**
         * Sets the {@code disabledServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disabledServers(List<String> value) {
            this.disabledServers = value;
            return this;
        }

        /**
         * Sets the {@code filteredServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder filteredServers(List<String> value) {
            this.filteredServers = value;
            return this;
        }

        /**
         * Sets the {@code clients} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clients(List<String> value) {
            this.clients = value;
            return this;
        }

        /**
         * Sets the {@code pendingConnections} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pendingConnections(List<String> value) {
            this.pendingConnections = value;
            return this;
        }

        /**
         * Sets the {@code failedServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder failedServers(Map<String, McpServerFailureInfo> value) {
            this.failedServers = value;
            return this;
        }

        /**
         * Sets the {@code needsAuthServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder needsAuthServers(Map<String, McpServerNeedsAuthInfo> value) {
            this.needsAuthServers = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpHostState build() {
            if (mcp3pEnabled == null) throw new IllegalStateException("Missing required property: mcp3pEnabled");
            if (disabledServers == null) throw new IllegalStateException("Missing required property: disabledServers");
            if (filteredServers == null) throw new IllegalStateException("Missing required property: filteredServers");
            if (clients == null) throw new IllegalStateException("Missing required property: clients");
            if (pendingConnections == null) throw new IllegalStateException("Missing required property: pendingConnections");
            if (failedServers == null) throw new IllegalStateException("Missing required property: failedServers");
            if (needsAuthServers == null) throw new IllegalStateException("Missing required property: needsAuthServers");
            return new McpHostState(mcp3pEnabled, disabledServers, filteredServers, clients, pendingConnections, failedServers, needsAuthServers);
        }
    }
}
