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
 * Session event "mcp.headers_refresh_required". Dynamic headers refresh request for a remote MCP server
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpHeadersRefreshRequiredEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp.headers_refresh_required"; }

    @JsonProperty("data")
    private McpHeadersRefreshRequiredEventData data;

    public McpHeadersRefreshRequiredEventData getData() { return data; }
    public void setData(McpHeadersRefreshRequiredEventData data) { this.data = data; }

    /** Data payload for {@link McpHeadersRefreshRequiredEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpHeadersRefreshRequiredEventData(
        /** Unique identifier for this headers refresh request; used to respond via session.mcp.headers.handlePendingHeadersRefreshRequest() */
        @JsonProperty("requestId") String requestId,
        /** Display name of the remote MCP server requesting headers */
        @JsonProperty("serverName") String serverName,
        /** URL of the remote MCP server requesting headers */
        @JsonProperty("serverUrl") String serverUrl,
        /** Why dynamic headers are being requested. */
        @JsonProperty("reason") McpHeadersRefreshRequiredReason reason
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public McpHeadersRefreshRequiredEventData {}

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
            private String serverUrl;
            private McpHeadersRefreshRequiredReason reason;

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
             * Sets the {@code serverUrl} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder serverUrl(String value) {
                this.serverUrl = value;
                return this;
            }

            /**
             * Sets the {@code reason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reason(McpHeadersRefreshRequiredReason value) {
                this.reason = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public McpHeadersRefreshRequiredEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
                if (serverUrl == null) throw new IllegalStateException("Missing required property: serverUrl");
                if (reason == null) throw new IllegalStateException("Missing required property: reason");
                return new McpHeadersRefreshRequiredEventData(requestId, serverName, serverUrl, reason);
            }
        }
    }
}
