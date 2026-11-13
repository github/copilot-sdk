/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Caller-owned inventory and storage roots for direct file removal.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsRemoveSourceInventory(
    /** Local skills discovered before removal. */
    @JsonProperty("skills") List<SkillRemoveSourceEntry> skills,
    /** Personal storage roots that the existing removal rules recognize. */
    @JsonProperty("personalSkillsDirs") List<String> personalSkillsDirs,
    /** Project storage root that the existing removal rules recognize. */
    @JsonProperty("projectSkillsDir") String projectSkillsDir
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillsRemoveSourceInventory {}

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
        private List<SkillRemoveSourceEntry> skills;
        private List<String> personalSkillsDirs;
        private String projectSkillsDir;

        /**
         * Sets the {@code skills} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder skills(List<SkillRemoveSourceEntry> value) {
            this.skills = value;
            return this;
        }

        /**
         * Sets the {@code personalSkillsDirs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder personalSkillsDirs(List<String> value) {
            this.personalSkillsDirs = value;
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillsRemoveSourceInventory build() {
            if (skills == null) throw new IllegalStateException("Missing required property: skills");
            if (personalSkillsDirs == null) throw new IllegalStateException("Missing required property: personalSkillsDirs");
            if (projectSkillsDir == null) throw new IllegalStateException("Missing required property: projectSkillsDir");
            return new SkillsRemoveSourceInventory(skills, personalSkillsDirs, projectSkillsDir);
        }
    }
}
