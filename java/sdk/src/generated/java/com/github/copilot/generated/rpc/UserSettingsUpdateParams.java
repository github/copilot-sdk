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
 * Names the user setting to merge into, and the partial object to merge.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsUpdateParams(
    /** Top-level user-setting key to merge into, such as `extensions`. */
    @JsonProperty("key") String key,
    /** Value to store for `key`. When both the stored value and this value are objects, their fields are shallow-merged and fields this object does not name are preserved. Otherwise this value replaces the stored value. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("value") Object value,
    /** Copilot configuration directory to write the settings document in, taking precedence over the server's own `COPILOT_HOME` and default home. Omit it, or pass an empty string, to use the directory the server resolved for itself. */
    @JsonProperty("configDir") String configDir,
    /** Single-component name for the settings document at `<configDir>/settings/<subDir>.json`; path separators, `.` and `..` are rejected. Omit it, or pass an empty string, to use the main settings document. */
    @JsonProperty("subDir") String subDir
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsUpdateParams {}

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
        private String key;
        private Object value;
        private boolean $presence1;
        private String configDir;
        private String subDir;

        /**
         * Sets the {@code key} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder key(String value) {
            this.key = value;
            return this;
        }

        /**
         * Sets the {@code value} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder value(Object value) {
            this.value = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code configDir} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder configDir(String value) {
            this.configDir = value;
            return this;
        }

        /**
         * Sets the {@code subDir} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder subDir(String value) {
            this.subDir = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsUpdateParams build() {
            if (key == null) throw new IllegalStateException("Missing required property: key");
            if (!$presence1) throw new IllegalStateException("Missing required property: value");
            return new UserSettingsUpdateParams(key, value, configDir, subDir);
        }
    }
}
