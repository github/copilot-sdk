/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "sampling.requested". Sampling request from an MCP server; contains the server name and a requestId for correlation
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SamplingRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "sampling.requested"; }

    @JsonProperty("data")
    private SamplingRequestedEventData data;

    public SamplingRequestedEventData getData() { return data; }
    public void setData(SamplingRequestedEventData data) { this.data = data; }

    /** Data payload for {@link SamplingRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SamplingRequestedEventData(
        /** Unique identifier for this sampling request; used to respond via session.respondToSampling() */
        @JsonProperty("requestId") String requestId,
        /** Name of the MCP server that initiated the sampling request */
        @JsonProperty("serverName") String serverName,
        /** Display label for a Connector-owned MCP server, from the trusted Connector catalog; absent for other servers. `serverName` stays the identity for request matching. */
        @JsonProperty("serverDisplayName") String serverDisplayName,
        /** The JSON-RPC request ID from the MCP protocol */
        @JsonProperty("mcpRequestId") Object mcpRequestId
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SamplingRequestedEventData {}

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
            private String requestId;
            private String serverName;
            private String serverDisplayName;
            private Object mcpRequestId;
            private boolean $presence3;

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestId(String value) {
                this.requestId = value;
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
             * Sets the {@code serverDisplayName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder serverDisplayName(String value) {
                this.serverDisplayName = value;
                return this;
            }

            /**
             * Sets the {@code mcpRequestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcpRequestId(Object value) {
                this.mcpRequestId = value;
                this.$presence3 = true;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SamplingRequestedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
                if (!$presence3) throw new IllegalStateException("Missing required property: mcpRequestId");
                return new SamplingRequestedEventData(requestId, serverName, serverDisplayName, mcpRequestId);
            }
        }
    }
}
