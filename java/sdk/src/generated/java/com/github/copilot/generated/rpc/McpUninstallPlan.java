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
 * Exact inert removal plan. No configuration or credentials have changed.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpUninstallPlan(
    /** One-use original connection and authority-bound plan handle. */
    @JsonProperty("planHandle") String planHandle,
    /** The original operation, inspectable and cancellable on this same connection. */
    @JsonProperty("operationId") String operationId,
    /** Original wall-clock expiry in milliseconds. Applying never renews it. */
    @JsonProperty("expiresAtEpochMs") Long expiresAtEpochMs,
    /** Original owned receipt being removed. */
    @JsonProperty("installation") McpInstallationSummary installation,
    /** Whether removal restores a protected earlier configuration. */
    @JsonProperty("restoresPreviousConfiguration") Boolean restoresPreviousConfiguration,
    /** Exact configured input slots owned by this installation, never shared OAuth tokens. */
    @JsonProperty("ownedSecretCount") Long ownedSecretCount,
    /** Shared authentication is deliberately retained; revocation is a separate action. */
    @JsonProperty("preservesSharedAuthentication") Boolean preservesSharedAuthentication
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpUninstallPlan {}

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
        private String planHandle;
        private String operationId;
        private Long expiresAtEpochMs;
        private McpInstallationSummary installation;
        private Boolean restoresPreviousConfiguration;
        private Long ownedSecretCount;
        private Boolean preservesSharedAuthentication;

        /**
         * Sets the {@code planHandle} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder planHandle(String value) {
            this.planHandle = value;
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
         * Sets the {@code expiresAtEpochMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expiresAtEpochMs(Long value) {
            this.expiresAtEpochMs = value;
            return this;
        }

        /**
         * Sets the {@code installation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installation(McpInstallationSummary value) {
            this.installation = value;
            return this;
        }

        /**
         * Sets the {@code restoresPreviousConfiguration} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder restoresPreviousConfiguration(Boolean value) {
            this.restoresPreviousConfiguration = value;
            return this;
        }

        /**
         * Sets the {@code ownedSecretCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ownedSecretCount(Long value) {
            this.ownedSecretCount = value;
            return this;
        }

        /**
         * Sets the {@code preservesSharedAuthentication} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder preservesSharedAuthentication(Boolean value) {
            this.preservesSharedAuthentication = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpUninstallPlan build() {
            if (planHandle == null) throw new IllegalStateException("Missing required property: planHandle");
            if (operationId == null) throw new IllegalStateException("Missing required property: operationId");
            if (expiresAtEpochMs == null) throw new IllegalStateException("Missing required property: expiresAtEpochMs");
            if (installation == null) throw new IllegalStateException("Missing required property: installation");
            if (restoresPreviousConfiguration == null) throw new IllegalStateException("Missing required property: restoresPreviousConfiguration");
            if (ownedSecretCount == null) throw new IllegalStateException("Missing required property: ownedSecretCount");
            if (preservesSharedAuthentication == null) throw new IllegalStateException("Missing required property: preservesSharedAuthentication");
            return new McpUninstallPlan(planHandle, operationId, expiresAtEpochMs, installation, restoresPreviousConfiguration, ownedSecretCount, preservesSharedAuthentication);
        }
    }
}
