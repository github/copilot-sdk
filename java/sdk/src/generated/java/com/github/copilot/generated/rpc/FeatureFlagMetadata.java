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
 * The declared shape of one feature flag, independent of any caller population.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record FeatureFlagMetadata(
    /** The flag's name, matching the key it is listed under. */
    @JsonProperty("name") String name,
    /** The population the flag is declared for before any environment or user-settings override is applied. */
    @JsonProperty("availability") FeatureFlagAvailability availability,
    /** Whether the flag participates in the CAPI sanity suite. */
    @JsonProperty("capiSanity") Boolean capiSanity,
    /** Text shown to a user choosing whether to turn the flag on in experimental mode. Absent when the flag is not user-facing. */
    @JsonProperty("experimentalDescription") String experimentalDescription,
    /** The online-experiment (ExP) flag this maps to, when the flag is assigned by an experiment rather than declared statically. */
    @JsonProperty("expFlag") String expFlag,
    /** Environment variable that overrides this flag's experiment assignment locally in the experiment service, not in `featureFlags.resolve`. Present exactly when `expFlag` is, and derived from it, so a caller reads a local override without reproducing the naming rule. */
    @JsonProperty("expFlagEnvVar") String expFlagEnvVar
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    FeatureFlagMetadata {}

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
        private String name;
        private FeatureFlagAvailability availability;
        private Boolean capiSanity;
        private String experimentalDescription;
        private String expFlag;
        private String expFlagEnvVar;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code availability} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder availability(FeatureFlagAvailability value) {
            this.availability = value;
            return this;
        }

        /**
         * Sets the {@code capiSanity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder capiSanity(Boolean value) {
            this.capiSanity = value;
            return this;
        }

        /**
         * Sets the {@code experimentalDescription} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder experimentalDescription(String value) {
            this.experimentalDescription = value;
            return this;
        }

        /**
         * Sets the {@code expFlag} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expFlag(String value) {
            this.expFlag = value;
            return this;
        }

        /**
         * Sets the {@code expFlagEnvVar} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expFlagEnvVar(String value) {
            this.expFlagEnvVar = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FeatureFlagMetadata build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (availability == null) throw new IllegalStateException("Missing required property: availability");
            if (capiSanity == null) throw new IllegalStateException("Missing required property: capiSanity");
            return new FeatureFlagMetadata(name, availability, capiSanity, experimentalDescription, expFlag, expFlagEnvVar);
        }
    }
}
