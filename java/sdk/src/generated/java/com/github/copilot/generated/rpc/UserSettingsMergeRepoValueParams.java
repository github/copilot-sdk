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
 * The user layer and the repository layer to fold together.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsMergeRepoValueParams(
    /** The user's settings, already resolved against any managed layer. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("userSettings") Object userSettings,
    /** The repository's checked-in settings layer. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("repoSettings") Object repoSettings
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsMergeRepoValueParams {}

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
        private Object userSettings;
        private boolean $presence0;
        private Object repoSettings;
        private boolean $presence1;

        /**
         * Sets the {@code userSettings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userSettings(Object value) {
            this.userSettings = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code repoSettings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repoSettings(Object value) {
            this.repoSettings = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsMergeRepoValueParams build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: userSettings");
            if (!$presence1) throw new IllegalStateException("Missing required property: repoSettings");
            return new UserSettingsMergeRepoValueParams(userSettings, repoSettings);
        }
    }
}
