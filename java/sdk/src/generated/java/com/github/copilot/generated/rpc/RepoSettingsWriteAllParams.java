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
 * The complete set of repository-overridable settings to store.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RepoSettingsWriteAllParams(
    /** Absolute path of the repository root. */
    @JsonProperty("repoRoot") String repoRoot,
    /** Use repo for shared settings or local for settings.local.json. Other scopes are rejected. */
    @JsonProperty("scope") String scope,
    /** Object of allowed settings to store. Missing allowlisted keys are removed. Other input keys are ignored, while unknown stored keys are preserved. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("settings") Object settings
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    RepoSettingsWriteAllParams {}

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
        private String repoRoot;
        private String scope;
        private Object settings;
        private boolean $presence2;

        /**
         * Sets the {@code repoRoot} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repoRoot(String value) {
            this.repoRoot = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder scope(String value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code settings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder settings(Object value) {
            this.settings = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RepoSettingsWriteAllParams build() {
            if (repoRoot == null) throw new IllegalStateException("Missing required property: repoRoot");
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            if (!$presence2) throw new IllegalStateException("Missing required property: settings");
            return new RepoSettingsWriteAllParams(repoRoot, scope, settings);
        }
    }
}
