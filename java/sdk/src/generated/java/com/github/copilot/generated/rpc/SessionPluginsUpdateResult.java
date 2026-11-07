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
 * Result of updating a single plugin.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPluginsUpdateResult(
    /** Version that was previously installed, when available */
    @JsonProperty("previousVersion") String previousVersion,
    /** Version after the update, when reported by the plugin manifest */
    @JsonProperty("newVersion") String newVersion,
    /** Number of skills discovered and installed after the update */
    @JsonProperty("skillsInstalled") Long skillsInstalled
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPluginsUpdateResult {}

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
        private String previousVersion;
        private String newVersion;
        private Long skillsInstalled;

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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPluginsUpdateResult build() {
            if (skillsInstalled == null) throw new IllegalStateException("Missing required property: skillsInstalled");
            return new SessionPluginsUpdateResult(previousVersion, newVersion, skillsInstalled);
        }
    }
}
