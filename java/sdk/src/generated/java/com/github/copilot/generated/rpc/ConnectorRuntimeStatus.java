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
 * Live status of one session-owned MCP projection.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectorRuntimeStatus(
    /** Opaque runtime server ID. */
    @JsonProperty("runtimeServerId") String runtimeServerId,
    /** Canonical Connector name that owns this server. */
    @JsonProperty("connectorName") String connectorName,
    /** Current live MCP host status. */
    @JsonProperty("status") ConnectorMcpStatus status
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ConnectorRuntimeStatus {}

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
        private String runtimeServerId;
        private String connectorName;
        private ConnectorMcpStatus status;

        /**
         * Sets the {@code runtimeServerId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder runtimeServerId(String value) {
            this.runtimeServerId = value;
            return this;
        }

        /**
         * Sets the {@code connectorName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder connectorName(String value) {
            this.connectorName = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder status(ConnectorMcpStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ConnectorRuntimeStatus build() {
            if (runtimeServerId == null) throw new IllegalStateException("Missing required property: runtimeServerId");
            if (connectorName == null) throw new IllegalStateException("Missing required property: connectorName");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            return new ConnectorRuntimeStatus(runtimeServerId, connectorName, status);
        }
    }
}
