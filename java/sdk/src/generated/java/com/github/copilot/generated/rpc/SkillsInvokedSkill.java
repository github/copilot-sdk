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
 * Skill invocation record with name, path, content, allowed tools, and turn number.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsInvokedSkill(
    /** Unique identifier for the skill */
    @JsonProperty("name") String name,
    /** Path to the SKILL.md file, or an empty string for an SDK-provided skill without a filesystem identity */
    @JsonProperty("path") String path,
    /** Full content of the skill file */
    @JsonProperty("content") String content,
    /** Exact invocation arguments when recorded, retained separately from the reusable skill body */
    @JsonProperty("arguments") String arguments,
    /** Tools that should be auto-approved when this skill is active, captured at invocation time */
    @JsonProperty("allowedTools") List<String> allowedTools,
    /** Whether model invocation was disabled when this skill was invoked */
    @JsonProperty("disableModelInvocation") Boolean disableModelInvocation,
    /** Turn number when the skill was invoked */
    @JsonProperty("invokedAtTurn") Long invokedAtTurn
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillsInvokedSkill {}

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
        private String path;
        private String content;
        private String arguments;
        private List<String> allowedTools;
        private Boolean disableModelInvocation;
        private Long invokedAtTurn;

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
         * Sets the {@code path} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder path(String value) {
            this.path = value;
            return this;
        }

        /**
         * Sets the {@code content} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder content(String value) {
            this.content = value;
            return this;
        }

        /**
         * Sets the {@code arguments} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder arguments(String value) {
            this.arguments = value;
            return this;
        }

        /**
         * Sets the {@code allowedTools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allowedTools(List<String> value) {
            this.allowedTools = value;
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
         * Sets the {@code invokedAtTurn} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder invokedAtTurn(Long value) {
            this.invokedAtTurn = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillsInvokedSkill build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (path == null) throw new IllegalStateException("Missing required property: path");
            if (content == null) throw new IllegalStateException("Missing required property: content");
            if (invokedAtTurn == null) throw new IllegalStateException("Missing required property: invokedAtTurn");
            return new SkillsInvokedSkill(name, path, content, arguments, allowedTools, disableModelInvocation, invokedAtTurn);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Unique identifier for the skill
     * @param path Path to the SKILL.md file, or an empty string for an SDK-provided skill without a filesystem identity
     * @param content Full content of the skill file
     * @param allowedTools Tools that should be auto-approved when this skill is active, captured at invocation time
     * @param disableModelInvocation Whether model invocation was disabled when this skill was invoked
     * @param invokedAtTurn Turn number when the skill was invoked
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillsInvokedSkill(
        String name,
        String path,
        String content,
        List<String> allowedTools,
        Boolean disableModelInvocation,
        Long invokedAtTurn
    ) {
        this(name, path, content, null, allowedTools, disableModelInvocation, invokedAtTurn);
    }
}
