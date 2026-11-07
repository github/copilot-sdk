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
 * Versions and provider configuration for CAFE.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CafeMetricsStartParams(
    /** Effective comparison version and appVersion tag. */
    @JsonProperty("appVersion") String appVersion,
    /** Raw package build version. */
    @JsonProperty("buildVersion") String buildVersion,
    /** Embedded package identity. Omission enables the unbundled development fallback. */
    @JsonProperty("embeddedPackageIdentity") CafeMetricsEmbeddedPackageIdentity embeddedPackageIdentity,
    /** Whether the host uses a custom model provider. */
    @JsonProperty("customProviderConfigured") Boolean customProviderConfigured
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    CafeMetricsStartParams {}

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
        private String appVersion;
        private String buildVersion;
        private CafeMetricsEmbeddedPackageIdentity embeddedPackageIdentity;
        private Boolean customProviderConfigured;

        /**
         * Sets the {@code appVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder appVersion(String value) {
            this.appVersion = value;
            return this;
        }

        /**
         * Sets the {@code buildVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder buildVersion(String value) {
            this.buildVersion = value;
            return this;
        }

        /**
         * Sets the {@code embeddedPackageIdentity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder embeddedPackageIdentity(CafeMetricsEmbeddedPackageIdentity value) {
            this.embeddedPackageIdentity = value;
            return this;
        }

        /**
         * Sets the {@code customProviderConfigured} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder customProviderConfigured(Boolean value) {
            this.customProviderConfigured = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CafeMetricsStartParams build() {
            if (appVersion == null) throw new IllegalStateException("Missing required property: appVersion");
            if (buildVersion == null) throw new IllegalStateException("Missing required property: buildVersion");
            if (customProviderConfigured == null) throw new IllegalStateException("Missing required property: customProviderConfigured");
            return new CafeMetricsStartParams(appVersion, buildVersion, embeddedPackageIdentity, customProviderConfigured);
        }
    }
}
