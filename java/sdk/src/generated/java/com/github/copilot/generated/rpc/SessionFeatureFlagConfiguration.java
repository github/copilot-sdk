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

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionFeatureFlagConfiguration(
    /** Host defaults applied during construction, before live flag reads. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("initOptions") Object initOptions,
    /** Assignment source owned by the session lifecycle. */
    @JsonProperty("mode") SessionFeatureFlagsMode mode,
    /** Whether experiment requests suppress account segmentation. */
    @JsonProperty("streamerMode") Boolean streamerMode,
    /** Installation first-launch time for experiment segmentation. */
    @JsonProperty("firstLaunchAtMillis") Double firstLaunchAtMillis,
    /** Whether the defaults came from the live settings document. */
    @JsonProperty("settingsAreLive") Boolean settingsAreLive
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionFeatureFlagConfiguration {}

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
        private Object initOptions;
        private boolean $presence0;
        private SessionFeatureFlagsMode mode;
        private Boolean streamerMode;
        private Double firstLaunchAtMillis;
        private Boolean settingsAreLive;

        /**
         * Sets the {@code initOptions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder initOptions(Object value) {
            this.initOptions = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code mode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mode(SessionFeatureFlagsMode value) {
            this.mode = value;
            return this;
        }

        /**
         * Sets the {@code streamerMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder streamerMode(Boolean value) {
            this.streamerMode = value;
            return this;
        }

        /**
         * Sets the {@code firstLaunchAtMillis} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder firstLaunchAtMillis(Double value) {
            this.firstLaunchAtMillis = value;
            return this;
        }

        /**
         * Sets the {@code settingsAreLive} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder settingsAreLive(Boolean value) {
            this.settingsAreLive = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFeatureFlagConfiguration build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: initOptions");
            if (mode == null) throw new IllegalStateException("Missing required property: mode");
            return new SessionFeatureFlagConfiguration(initOptions, mode, streamerMode, firstLaunchAtMillis, settingsAreLive);
        }
    }
}
