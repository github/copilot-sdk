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
 * Per-plugin result from updating all plugins, with versions, skills installed, success flag, and optional error.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginUpdateAllEntry(
    /** Plugin name that was updated */
    @JsonProperty("name") String name,
    /** Marketplace the plugin came from. Empty string ("") for direct installs. */
    @JsonProperty("marketplace") String marketplace,
    /** Whether the update succeeded for this plugin */
    @JsonProperty("success") Boolean success,
    /** Previously installed version, when available */
    @JsonProperty("previousVersion") String previousVersion,
    /** Version after the update, when available */
    @JsonProperty("newVersion") String newVersion,
    /** Number of skills installed after the update (success only) */
    @JsonProperty("skillsInstalled") Long skillsInstalled,
    /** Error message (failure only) */
    @JsonProperty("error") String error
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PluginUpdateAllEntry {}

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
        private String marketplace;
        private Boolean success;
        private String previousVersion;
        private String newVersion;
        private Long skillsInstalled;
        private String error;

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
         * Sets the {@code marketplace} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder marketplace(String value) {
            this.marketplace = value;
            return this;
        }

        /**
         * Sets the {@code success} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder success(Boolean value) {
            this.success = value;
            return this;
        }

        /**
         * Sets the {@code previousVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder previousVersion(String value) {
            this.previousVersion = value;
            return this;
        }

        /**
         * Sets the {@code newVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder newVersion(String value) {
            this.newVersion = value;
            return this;
        }

        /**
         * Sets the {@code skillsInstalled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder skillsInstalled(Long value) {
            this.skillsInstalled = value;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder error(String value) {
            this.error = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PluginUpdateAllEntry build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (marketplace == null) throw new IllegalStateException("Missing required property: marketplace");
            if (success == null) throw new IllegalStateException("Missing required property: success");
            return new PluginUpdateAllEntry(name, marketplace, success, previousVersion, newVersion, skillsInstalled, error);
        }
    }
}
