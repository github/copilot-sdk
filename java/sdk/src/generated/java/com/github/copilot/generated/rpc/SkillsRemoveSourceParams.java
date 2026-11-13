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
 * Removes a user-selected source without changing verified catalog removal.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsRemoveSourceParams(
    /** Local skill name, file path, or registered discovery directory. */
    @JsonProperty("target") String target,
    /** Working directory for relative removal paths. */
    @JsonProperty("cwd") String cwd,
    /** Current discovery directories. Removal returns an updated copy. */
    @JsonProperty("skillDirectories") List<String> skillDirectories,
    /** Required for file removal, but not for directory registration removal. */
    @JsonProperty("inventory") SkillsRemoveSourceInventory inventory
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillsRemoveSourceParams {}

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
        private String target;
        private String cwd;
        private List<String> skillDirectories;
        private SkillsRemoveSourceInventory inventory;

        /**
         * Sets the {@code target} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder target(String value) {
            this.target = value;
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
         * Sets the {@code inventory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder inventory(SkillsRemoveSourceInventory value) {
            this.inventory = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillsRemoveSourceParams build() {
            if (target == null) throw new IllegalStateException("Missing required property: target");
            if (cwd == null) throw new IllegalStateException("Missing required property: cwd");
            if (skillDirectories == null) throw new IllegalStateException("Missing required property: skillDirectories");
            return new SkillsRemoveSourceParams(target, cwd, skillDirectories, inventory);
        }
    }
}
