/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Durable configuration ownership is distinct from session-specific usability.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpInstallationSummary(
    /** Exact durable installation receipt identity. */
    @JsonProperty("installationId") String installationId,
    /** Original installing operation, not a fresh management operation. */
    @JsonProperty("operationId") String operationId,
    /** Identity retained from the validated original plan. */
    @JsonProperty("identity") McpPlanResourceIdentity identity,
    /** Exact alternative retained in the installing receipt. */
    @JsonProperty("choiceId") String choiceId,
    /** Ownership or setup state, never inferred proof of tool usability. */
    @JsonProperty("state") McpInstallationState state,
    /** Catalogue identity retained from the installed plan when available. */
    @JsonProperty("catalogue") InstallationCatalogueIdentity catalogue,
    /** ISO 8601 wall-clock installation time when available. */
    @JsonProperty("installedAt") String installedAt
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpInstallationSummary {}

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
        private String installationId;
        private String operationId;
        private McpPlanResourceIdentity identity;
        private String choiceId;
        private McpInstallationState state;
        private InstallationCatalogueIdentity catalogue;
        private String installedAt;

        /**
         * Sets the {@code installationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installationId(String value) {
            this.installationId = value;
            return this;
        }

        /**
         * Sets the {@code operationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder operationId(String value) {
            this.operationId = value;
            return this;
        }

        /**
         * Sets the {@code identity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder identity(McpPlanResourceIdentity value) {
            this.identity = value;
            return this;
        }

        /**
         * Sets the {@code choiceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder choiceId(String value) {
            this.choiceId = value;
            return this;
        }

        /**
         * Sets the {@code state} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder state(McpInstallationState value) {
            this.state = value;
            return this;
        }

        /**
         * Sets the {@code catalogue} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder catalogue(InstallationCatalogueIdentity value) {
            this.catalogue = value;
            return this;
        }

        /**
         * Sets the {@code installedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installedAt(String value) {
            this.installedAt = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpInstallationSummary build() {
            if (installationId == null) throw new IllegalStateException("Missing required property: installationId");
            if (operationId == null) throw new IllegalStateException("Missing required property: operationId");
            if (identity == null) throw new IllegalStateException("Missing required property: identity");
            if (choiceId == null) throw new IllegalStateException("Missing required property: choiceId");
            if (state == null) throw new IllegalStateException("Missing required property: state");
            return new McpInstallationSummary(installationId, operationId, identity, choiceId, state, catalogue, installedAt);
        }
    }
}
