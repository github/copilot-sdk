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
 * Feature availability.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectorsGetCapabilitiesResult(
    /** API version. */
    @JsonProperty("apiVersion") Long apiVersion,
    /** Availability. */
    @JsonProperty("availability") ConnectorDiscoveryAvailability availability,
    /** Whether accounts are selected by opaque ID. */
    @JsonProperty("opaqueAccountSelection") Boolean opaqueAccountSelection,
    /** Whether results are cached. */
    @JsonProperty("conditionalCache") Boolean conditionalCache
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ConnectorsGetCapabilitiesResult {}

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
        private ConnectorDiscoveryAvailability availability;
        private Boolean opaqueAccountSelection;
        private Boolean conditionalCache;

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
        public Builder availability(ConnectorDiscoveryAvailability value) {
            this.availability = value;
            return this;
        }

        /**
         * Sets the {@code opaqueAccountSelection} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder opaqueAccountSelection(Boolean value) {
            this.opaqueAccountSelection = value;
            return this;
        }

        /**
         * Sets the {@code conditionalCache} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder conditionalCache(Boolean value) {
            this.conditionalCache = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ConnectorsGetCapabilitiesResult build() {
            if (apiVersion == null) throw new IllegalStateException("Missing required property: apiVersion");
            if (availability == null) throw new IllegalStateException("Missing required property: availability");
            if (opaqueAccountSelection == null) throw new IllegalStateException("Missing required property: opaqueAccountSelection");
            if (conditionalCache == null) throw new IllegalStateException("Missing required property: conditionalCache");
            return new ConnectorsGetCapabilitiesResult(apiVersion, availability, opaqueAccountSelection, conditionalCache);
        }
    }
}
