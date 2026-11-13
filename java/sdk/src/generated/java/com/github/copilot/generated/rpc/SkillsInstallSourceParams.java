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
 * Installs a user-selected skill source without catalog plan semantics.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsInstallSourceParams(
    /** HTTPS URL, local skill file, or local discovery directory. */
    @JsonProperty("source") String source,
    /** Working directory for relative source paths. */
    @JsonProperty("cwd") String cwd,
    /** Personal destination for URL and file sources. */
    @JsonProperty("personalSkillsDir") String personalSkillsDir,
    /** Project destination for URL and file sources. */
    @JsonProperty("projectSkillsDir") String projectSkillsDir,
    /** Selects project scope. Directory registration requires personal scope. */
    @JsonProperty("project") Boolean project,
    /** Current discovery directories. The result returns an updated copy. */
    @JsonProperty("skillDirectories") List<String> skillDirectories,
    /** User agent for runtime-owned HTTPS source requests. */
    @JsonProperty("userAgent") String userAgent
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillsInstallSourceParams {}

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
        private String source;
        private String cwd;
        private String personalSkillsDir;
        private String projectSkillsDir;
        private Boolean project;
        private List<String> skillDirectories;
        private String userAgent;

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code cwd} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cwd(String value) {
            this.cwd = value;
            return this;
        }

        /**
         * Sets the {@code personalSkillsDir} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder personalSkillsDir(String value) {
            this.personalSkillsDir = value;
            return this;
        }

        /**
         * Sets the {@code projectSkillsDir} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder projectSkillsDir(String value) {
            this.projectSkillsDir = value;
            return this;
        }

        /**
         * Sets the {@code project} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder project(Boolean value) {
            this.project = value;
            return this;
        }

        /**
         * Sets the {@code skillDirectories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder skillDirectories(List<String> value) {
            this.skillDirectories = value;
            return this;
        }

        /**
         * Sets the {@code userAgent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userAgent(String value) {
            this.userAgent = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillsInstallSourceParams build() {
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (cwd == null) throw new IllegalStateException("Missing required property: cwd");
            if (personalSkillsDir == null) throw new IllegalStateException("Missing required property: personalSkillsDir");
            if (projectSkillsDir == null) throw new IllegalStateException("Missing required property: projectSkillsDir");
            if (project == null) throw new IllegalStateException("Missing required property: project");
            if (skillDirectories == null) throw new IllegalStateException("Missing required property: skillDirectories");
            if (userAgent == null) throw new IllegalStateException("Missing required property: userAgent");
            return new SkillsInstallSourceParams(source, cwd, personalSkillsDir, projectSkillsDir, project, skillDirectories, userAgent);
        }
    }
}
