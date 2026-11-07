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
 * Authoritative session connector state. Account IDs are opaque routing identifiers and credentials are never included.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionConnectorsReconcileResult(
    /** Connector API contract version. */
    @JsonProperty("apiVersion") Long apiVersion,
    /** Current feature and session availability. */
    @JsonProperty("availability") ConnectorAvailability availability,
    /** Opaque account selection pinned to this session, when one has been selected. */
    @JsonProperty("accountId") String accountId,
    /** Latest validated catalog snapshot, when available. */
    @JsonProperty("catalog") ConnectorCatalogResult catalog,
    /** Exact selected account and stable scope requiring an authorization update, when proven by the Connector service. */
    @JsonProperty("authorizationRequirement") ConnectorAuthorizationRequirement authorizationRequirement,
    /** Live MCP status for every Connector-owned runtime server. */
    @JsonProperty("runtimeServers") List<ConnectorRuntimeStatus> runtimeServers,
    /** Number of active opaque connection continuations. */
    @JsonProperty("pendingConnections") Long pendingConnections
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionConnectorsReconcileResult {}

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
        private Long apiVersion;
        private ConnectorAvailability availability;
        private String accountId;
        private ConnectorCatalogResult catalog;
        private ConnectorAuthorizationRequirement authorizationRequirement;
        private List<ConnectorRuntimeStatus> runtimeServers;
        private Long pendingConnections;

        /**
         * Sets the {@code apiVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder apiVersion(Long value) {
            this.apiVersion = value;
            return this;
        }

        /**
         * Sets the {@code availability} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder availability(ConnectorAvailability value) {
            this.availability = value;
            return this;
        }

        /**
         * Sets the {@code accountId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder accountId(String value) {
            this.accountId = value;
            return this;
        }

        /**
         * Sets the {@code catalog} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder catalog(ConnectorCatalogResult value) {
            this.catalog = value;
            return this;
        }

        /**
         * Sets the {@code authorizationRequirement} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authorizationRequirement(ConnectorAuthorizationRequirement value) {
            this.authorizationRequirement = value;
            return this;
        }

        /**
         * Sets the {@code runtimeServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder runtimeServers(List<ConnectorRuntimeStatus> value) {
            this.runtimeServers = value;
            return this;
        }

        /**
         * Sets the {@code pendingConnections} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder pendingConnections(Long value) {
            this.pendingConnections = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionConnectorsReconcileResult build() {
            if (apiVersion == null) throw new IllegalStateException("Missing required property: apiVersion");
            if (availability == null) throw new IllegalStateException("Missing required property: availability");
            if (runtimeServers == null) throw new IllegalStateException("Missing required property: runtimeServers");
            if (pendingConnections == null) throw new IllegalStateException("Missing required property: pendingConnections");
            return new SessionConnectorsReconcileResult(apiVersion, availability, accountId, catalog, authorizationRequirement, runtimeServers, pendingConnections);
        }
    }
}
