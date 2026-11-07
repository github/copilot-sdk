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
 * A server-advertised routing preference. Identifiers and execution types are extensible.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AutoTierDescriptor(
    /** Opaque routing identifier transmitted unchanged to the provider. */
    @JsonProperty("id") String id,
    /** Human-readable label, not a routing identifier. */
    @JsonProperty("displayName") String displayName,
    /** Description displayed beside the preference. */
    @JsonProperty("description") String description,
    /** Execution kind; supported preferences use `auto` or the runtime's `fusion` modes. */
    @JsonProperty("type") String type,
    /** Runtime-owned managed Fusion tier represented by this execution descriptor, if any. */
    @JsonProperty("fusionTier") String fusionTier,
    /** Current account-specific availability. */
    @JsonProperty("status") AutoTierStatus status,
    /** Billing information for this preference, such as an announced or active promotion. */
    @JsonProperty("billing") AutoTierBilling billing
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AutoTierDescriptor {}

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
        private String id;
        private String displayName;
        private String description;
        private String type;
        private String fusionTier;
        private AutoTierStatus status;
        private AutoTierBilling billing;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayName(String value) {
            this.displayName = value;
            return this;
        }

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder type(String value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code fusionTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder fusionTier(String value) {
            this.fusionTier = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(AutoTierStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code billing} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder billing(AutoTierBilling value) {
            this.billing = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AutoTierDescriptor build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            if (type == null) throw new IllegalStateException("Missing required property: type");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            return new AutoTierDescriptor(id, displayName, description, type, fusionTier, status, billing);
        }
    }
}
