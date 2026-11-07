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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The overrides and host context to resolve effective runtime settings from.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsBuildRuntimeParams(
    /** Settings that take precedence over every document and environment layer, such as the values a host derived from its own command line. Keyed by setting name, with each value carried verbatim, so a host may pass keys this runtime does not recognize. Absent when the host contributes no overrides of its own. */
    @JsonProperty("overrides") Map<String, Object> overrides,
    /** The directory the settings are being resolved for. This selects the repository-scoped document, so a host resolving settings for a directory other than its own process working directory must pass that directory here. */
    @JsonProperty("workingDirectory") String workingDirectory,
    /** The environment to read settings-bearing variables from. Passed explicitly rather than read from the server process so a host can resolve settings for an environment it is about to create rather than the one it runs in. */
    @JsonProperty("environment") Map<String, String> environment,
    /** The platform to apply platform-specific defaults for, in the same spelling the host's own runtime reports. */
    @JsonProperty("platform") String platform
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsBuildRuntimeParams {}

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
        private Map<String, Object> overrides;
        private String workingDirectory;
        private Map<String, String> environment;
        private String platform;

        /**
         * Sets the {@code overrides} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder overrides(Map<String, Object> value) {
            this.overrides = value;
            return this;
        }

        /**
         * Sets the {@code workingDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder workingDirectory(String value) {
            this.workingDirectory = value;
            return this;
        }

        /**
         * Sets the {@code environment} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder environment(Map<String, String> value) {
            this.environment = value;
            return this;
        }

        /**
         * Sets the {@code platform} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder platform(String value) {
            this.platform = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsBuildRuntimeParams build() {
            if (workingDirectory == null) throw new IllegalStateException("Missing required property: workingDirectory");
            if (environment == null) throw new IllegalStateException("Missing required property: environment");
            if (platform == null) throw new IllegalStateException("Missing required property: platform");
            return new UserSettingsBuildRuntimeParams(overrides, workingDirectory, environment, platform);
        }
    }
}
