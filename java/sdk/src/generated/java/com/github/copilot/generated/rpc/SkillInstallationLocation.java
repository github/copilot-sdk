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
 * A user-facing personal Skill installation location without absolute host paths.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillInstallationLocation(
    /** Installation scope. Agent Finder Skills are installed in the user's personal Copilot home. */
    @JsonProperty("scope") SkillInstallationScope scope,
    /** Path relative to the Copilot home. */
    @JsonProperty("relativePath") String relativePath,
    /** Safe display label, for example ~/.copilot/skills/run-checks. */
    @JsonProperty("displayLabel") String displayLabel,
    /** Diagnostics-only absolute host path. Hosts must not display it by default. */
    @JsonProperty("diagnosticsAbsolutePath") String diagnosticsAbsolutePath
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillInstallationLocation {}

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
        private SkillInstallationScope scope;
        private String relativePath;
        private String displayLabel;
        private String diagnosticsAbsolutePath;

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder scope(SkillInstallationScope value) {
            this.scope = value;
            return this;
        }

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
         * Sets the {@code diagnosticsAbsolutePath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder diagnosticsAbsolutePath(String value) {
            this.diagnosticsAbsolutePath = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillInstallationLocation build() {
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            if (relativePath == null) throw new IllegalStateException("Missing required property: relativePath");
            if (displayLabel == null) throw new IllegalStateException("Missing required property: displayLabel");
            return new SkillInstallationLocation(scope, relativePath, displayLabel, diagnosticsAbsolutePath);
        }
    }
}
