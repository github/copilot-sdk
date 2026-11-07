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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Runtime-owned configuration inventory and configuration diagnostics.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LspListResult(
    /** Effective servers in merge order. */
    @JsonProperty("configs") List<LspServerSummary> configs,
    /** User entries, including entries disabled by project configuration. */
    @JsonProperty("userServers") List<LspServerSummary> userServers,
    /** Effective plugin entries. */
    @JsonProperty("pluginServers") List<LspServerSummary> pluginServers,
    /** Project entries, including disabled entries absent from the effective set. */
    @JsonProperty("projectServers") List<LspServerSummary> projectServers,
    /** User configuration file path. */
    @JsonProperty("userConfigPath") String userConfigPath,
    /** Nearest project configuration file path. */
    @JsonProperty("projectConfigPath") String projectConfigPath,
    /** User configuration read or validation failure. */
    @JsonProperty("userConfigError") String userConfigError,
    /** Project files rejected by the loader. */
    @JsonProperty("projectProblems") List<LspConfigurationProblem> projectProblems,
    /** Failure to inspect project configuration. */
    @JsonProperty("projectInspectionError") String projectInspectionError,
    /** Identifiers disabled through the user toggle list. */
    @JsonProperty("userDisabledServers") List<String> userDisabledServers,
    /** Configuration loader diagnostics. */
    @JsonProperty("warnings") List<String> warnings,
    /** Plugin configuration diagnostics. */
    @JsonProperty("pluginWarnings") List<String> pluginWarnings
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LspListResult {}

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
        private List<LspServerSummary> configs;
        private List<LspServerSummary> userServers;
        private List<LspServerSummary> pluginServers;
        private List<LspServerSummary> projectServers;
        private String userConfigPath;
        private String projectConfigPath;
        private String userConfigError;
        private List<LspConfigurationProblem> projectProblems;
        private String projectInspectionError;
        private List<String> userDisabledServers;
        private List<String> warnings;
        private List<String> pluginWarnings;

        /**
         * Sets the {@code configs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder configs(List<LspServerSummary> value) {
            this.configs = value;
            return this;
        }

        /**
         * Sets the {@code userServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userServers(List<LspServerSummary> value) {
            this.userServers = value;
            return this;
        }

        /**
         * Sets the {@code pluginServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pluginServers(List<LspServerSummary> value) {
            this.pluginServers = value;
            return this;
        }

        /**
         * Sets the {@code projectServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder projectServers(List<LspServerSummary> value) {
            this.projectServers = value;
            return this;
        }

        /**
         * Sets the {@code userConfigPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userConfigPath(String value) {
            this.userConfigPath = value;
            return this;
        }

        /**
         * Sets the {@code projectConfigPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder projectConfigPath(String value) {
            this.projectConfigPath = value;
            return this;
        }

        /**
         * Sets the {@code userConfigError} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userConfigError(String value) {
            this.userConfigError = value;
            return this;
        }

        /**
         * Sets the {@code projectProblems} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder projectProblems(List<LspConfigurationProblem> value) {
            this.projectProblems = value;
            return this;
        }

        /**
         * Sets the {@code projectInspectionError} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder projectInspectionError(String value) {
            this.projectInspectionError = value;
            return this;
        }

        /**
         * Sets the {@code userDisabledServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userDisabledServers(List<String> value) {
            this.userDisabledServers = value;
            return this;
        }

        /**
         * Sets the {@code warnings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warnings(List<String> value) {
            this.warnings = value;
            return this;
        }

        /**
         * Sets the {@code pluginWarnings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pluginWarnings(List<String> value) {
            this.pluginWarnings = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LspListResult build() {
            if (configs == null) throw new IllegalStateException("Missing required property: configs");
            if (userServers == null) throw new IllegalStateException("Missing required property: userServers");
            if (pluginServers == null) throw new IllegalStateException("Missing required property: pluginServers");
            if (projectServers == null) throw new IllegalStateException("Missing required property: projectServers");
            if (userConfigPath == null) throw new IllegalStateException("Missing required property: userConfigPath");
            if (projectProblems == null) throw new IllegalStateException("Missing required property: projectProblems");
            if (userDisabledServers == null) throw new IllegalStateException("Missing required property: userDisabledServers");
            if (warnings == null) throw new IllegalStateException("Missing required property: warnings");
            if (pluginWarnings == null) throw new IllegalStateException("Missing required property: pluginWarnings");
            return new LspListResult(configs, userServers, pluginServers, projectServers, userConfigPath, projectConfigPath, userConfigError, projectProblems, projectInspectionError, userDisabledServers, warnings, pluginWarnings);
        }
    }
}
