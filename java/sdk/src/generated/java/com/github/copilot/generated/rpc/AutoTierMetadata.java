/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Account-bound discovery metadata for the virtual `auto` model.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AutoTierMetadata(
    /** Provider-default preference, used only when no explicit preference exists. */
    @JsonProperty("defaultTier") String defaultTier,
    /** Routing preferences in the server's presentation order. */
    @JsonProperty("tiers") List<AutoTierDescriptor> tiers,
    /** Provider that supplied this metadata, when the catalog is provider-attributed. */
    @JsonProperty("providerId") String providerId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AutoTierMetadata {}

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
        private String defaultTier;
        private List<AutoTierDescriptor> tiers;
        private String providerId;

        /**
         * Sets the {@code defaultTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder defaultTier(String value) {
            this.defaultTier = value;
            return this;
        }

        /**
         * Sets the {@code tiers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tiers(List<AutoTierDescriptor> value) {
            this.tiers = value;
            return this;
        }

        /**
         * Sets the {@code providerId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder providerId(String value) {
            this.providerId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AutoTierMetadata build() {
            if (defaultTier == null) throw new IllegalStateException("Missing required property: defaultTier");
            if (tiers == null) throw new IllegalStateException("Missing required property: tiers");
            return new AutoTierMetadata(defaultTier, tiers, providerId);
        }
    }
}
