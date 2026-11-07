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
 * Selects the repository whose checked-in settings to read.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsLoadRepoParams(
    /** Absolute path of the repository root whose settings to read. When this is the user's home directory, the Claude-compatible configuration is skipped, so a home-directory session does not inherit a personal config as if it were repository policy. */
    @JsonProperty("repoRoot") String repoRoot
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsLoadRepoParams {}

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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsLoadRepoParams build() {
            if (repoRoot == null) throw new IllegalStateException("Missing required property: repoRoot");
            return new UserSettingsLoadRepoParams(repoRoot);
        }
    }
}
