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
 * One hook to disable or re-enable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsSetHookDisabledParams(
    /** Path of the settings.json to rewrite. */
    @JsonProperty("settingsPath") String settingsPath,
    /** Durable content-hash key of the hook, as `hooks.loadResources` reports it. */
    @JsonProperty("disableKey") String disableKey,
    /** True adds the key to `disabledHooks`, false removes it. */
    @JsonProperty("disabled") Boolean disabled
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsSetHookDisabledParams {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String settingsPath;
        private String disableKey;
        private Boolean disabled;

        /**
         * Sets the {@code settingsPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder settingsPath(String value) {
            this.settingsPath = value;
            return this;
        }

        /**
         * Sets the {@code disableKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder disableKey(String value) {
            this.disableKey = value;
            return this;
        }

        /**
         * Sets the {@code disabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder disabled(Boolean value) {
            this.disabled = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsSetHookDisabledParams build() {
            if (settingsPath == null) throw new IllegalStateException("Missing required property: settingsPath");
            if (disableKey == null) throw new IllegalStateException("Missing required property: disableKey");
            if (disabled == null) throw new IllegalStateException("Missing required property: disabled");
            return new UserSettingsSetHookDisabledParams(settingsPath, disableKey, disabled);
        }
    }
}
