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
import javax.annotation.processing.Generated;

/**
 * Registration parameters for an external MCP client.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionMcpRegisterExternalClientParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Logical server name for the external client */
    @JsonProperty("serverName") String serverName,
    /** In-process MCP Client instance. Marked internal: cannot be serialized across the JSON-RPC boundary. */
    @JsonProperty("client") Object client,
    /** In-process MCP Transport instance. Marked internal: cannot be serialized across the JSON-RPC boundary. */
    @JsonProperty("transport") Object transport,
    /** In-process server config (MCPServerConfig) paired with the in-process client/transport. Marked internal alongside its companions. */
    @JsonProperty("config") Object config
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionMcpRegisterExternalClientParams {}

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
        private String sessionId;
        private String serverName;
        private Object client;
        private Object transport;
        private Object config;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

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
         * Sets the {@code client} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder client(Object value) {
            this.client = value;
            return this;
        }

        /**
         * Sets the {@code transport} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder transport(Object value) {
            this.transport = value;
            return this;
        }

        /**
         * Sets the {@code config} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder config(Object value) {
            this.config = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMcpRegisterExternalClientParams build() {
            if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
            if (client == null) throw new IllegalStateException("Missing required property: client");
            if (transport == null) throw new IllegalStateException("Missing required property: transport");
            if (config == null) throw new IllegalStateException("Missing required property: config");
            return new SessionMcpRegisterExternalClientParams(sessionId, serverName, client, transport, config);
        }
    }
}
