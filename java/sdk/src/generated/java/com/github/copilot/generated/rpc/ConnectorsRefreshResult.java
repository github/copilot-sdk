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
 * Entries for the selected account.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectorsRefreshResult(
    /** Opaque account ID. */
    @JsonProperty("accountId") String accountId,
    /** Revision. */
    @JsonProperty("revision") Long revision,
    /** Refresh time in Unix epoch milliseconds. */
    @JsonProperty("refreshedAtMs") Long refreshedAtMs,
    /** Entries. */
    @JsonProperty("connectors") List<ConnectorDiscoveryCatalogEntry> connectors
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ConnectorsRefreshResult {}

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
        private String accountId;
        private Long revision;
        private Long refreshedAtMs;
        private List<ConnectorDiscoveryCatalogEntry> connectors;

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
         * Sets the {@code revision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder revision(Long value) {
            this.revision = value;
            return this;
        }

        /**
         * Sets the {@code refreshedAtMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder refreshedAtMs(Long value) {
            this.refreshedAtMs = value;
            return this;
        }

        /**
         * Sets the {@code connectors} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder connectors(List<ConnectorDiscoveryCatalogEntry> value) {
            this.connectors = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ConnectorsRefreshResult build() {
            if (accountId == null) throw new IllegalStateException("Missing required property: accountId");
            if (revision == null) throw new IllegalStateException("Missing required property: revision");
            if (refreshedAtMs == null) throw new IllegalStateException("Missing required property: refreshedAtMs");
            if (connectors == null) throw new IllegalStateException("Missing required property: connectors");
            return new ConnectorsRefreshResult(accountId, revision, refreshedAtMs, connectors);
        }
    }
}
