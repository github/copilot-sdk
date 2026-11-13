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
 * One discovered local skill, without its content.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillRemoveSourceEntry(
    /** Invocation name used to resolve a removal target. */
    @JsonProperty("name") String name,
    /** Original discovery source. Removal rejects unsupported or unowned sources. */
    @JsonProperty("source") String source,
    /** Discovery directory that owns the skill file. */
    @JsonProperty("baseDir") String baseDir
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillRemoveSourceEntry {}

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
        private String source;
        private String baseDir;

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
         * Sets the {@code baseDir} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder baseDir(String value) {
            this.baseDir = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillRemoveSourceEntry build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (baseDir == null) throw new IllegalStateException("Missing required property: baseDir");
            return new SkillRemoveSourceEntry(name, source, baseDir);
        }
    }
}
