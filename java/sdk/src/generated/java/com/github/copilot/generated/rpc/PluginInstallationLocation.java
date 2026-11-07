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
 * Where a plugin is installed, without an absolute host path.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginInstallationLocation(
    /** Path relative to the Copilot home, under installed-plugins/_direct like any direct plugin install. */
    @JsonProperty("relativePath") String relativePath,
    /** Safe display label, for example ~/.copilot/installed-plugins/_direct/owner--repo--plugins-work-hub. */
    @JsonProperty("displayLabel") String displayLabel
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PluginInstallationLocation {}

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
        private String relativePath;
        private String displayLabel;

        /**
         * Sets the {@code relativePath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder relativePath(String value) {
            this.relativePath = value;
            return this;
        }

        /**
         * Sets the {@code displayLabel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayLabel(String value) {
            this.displayLabel = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PluginInstallationLocation build() {
            if (relativePath == null) throw new IllegalStateException("Missing required property: relativePath");
            if (displayLabel == null) throw new IllegalStateException("Missing required property: displayLabel");
            return new PluginInstallationLocation(relativePath, displayLabel);
        }
    }
}
