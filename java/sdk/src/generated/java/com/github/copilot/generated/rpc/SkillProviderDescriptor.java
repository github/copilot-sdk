/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Authoritative catalog metadata for one SDK-provided skill. The skill's SKILL.md text is fetched separately and lazily.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SkillProviderDescriptor(
    /** Invocation and display name. */
    @JsonProperty("name") String name,
    /** Description used in skill catalogs without fetching content. */
    @JsonProperty("description") String description,
    /** Whether users may invoke the skill directly. Defaults to true. */
    @JsonProperty("userInvocable") Boolean userInvocable,
    /** Whether model invocation is disabled. Defaults to false. */
    @JsonProperty("disableModelInvocation") Boolean disableModelInvocation,
    /** Optional freeform argument hint used by slash-command catalogs. */
    @JsonProperty("argumentHint") String argumentHint,
    /** Absolute path to the folder that holds the skill's supporting files (scripts, references, templates, assets), in the session's path conventions: the host OS for local disk, or the registered SessionFs conventions. When the skill is activated the runtime lists the folder's files for the model through the session's filesystem, with the same rules as a file-based skill's folder; it never reads or copies them. On local disk the folder becomes a read-only skill root, like a configured skill directory. It is not shown in skill catalogs. */
    @JsonProperty("directory") String directory
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SkillProviderDescriptor {}

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
        private String name;
        private String description;
        private Boolean userInvocable;
        private Boolean disableModelInvocation;
        private String argumentHint;
        private String directory;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code userInvocable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userInvocable(Boolean value) {
            this.userInvocable = value;
            return this;
        }

        /**
         * Sets the {@code disableModelInvocation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disableModelInvocation(Boolean value) {
            this.disableModelInvocation = value;
            return this;
        }

        /**
         * Sets the {@code argumentHint} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder argumentHint(String value) {
            this.argumentHint = value;
            return this;
        }

        /**
         * Sets the {@code directory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder directory(String value) {
            this.directory = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillProviderDescriptor build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            return new SkillProviderDescriptor(name, description, userInvocable, disableModelInvocation, argumentHint, directory);
        }
    }
}
