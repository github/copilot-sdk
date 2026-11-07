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
 * Slash-command metadata with name, aliases, description, kind, input hint, execution allowance, and schedulability.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SlashCommandInfo(
    /** Canonical command name without a leading slash */
    @JsonProperty("name") String name,
    /** Canonical aliases without leading slashes */
    @JsonProperty("aliases") List<String> aliases,
    /** Human-readable command description */
    @JsonProperty("description") String description,
    /** Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command */
    @JsonProperty("kind") SlashCommandKind kind,
    /** Optional unstructured input hint */
    @JsonProperty("input") SlashCommandInput input,
    /** Whether the command may run while an agent turn is active */
    @JsonProperty("allowDuringAgentExecution") Boolean allowDuringAgentExecution,
    /** Whether the command is experimental */
    @JsonProperty("experimental") Boolean experimental,
    /** Whether the command may be the target of `/every` / `/after` schedules. Resolution happens at every tick, so only set this when the command is safe to re-invoke and produces an agent prompt. */
    @JsonProperty("schedulable") Boolean schedulable
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SlashCommandInfo {}

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
        private List<String> aliases;
        private String description;
        private SlashCommandKind kind;
        private SlashCommandInput input;
        private Boolean allowDuringAgentExecution;
        private Boolean experimental;
        private Boolean schedulable;

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
         * Sets the {@code aliases} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder aliases(List<String> value) {
            this.aliases = value;
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
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(SlashCommandKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code input} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder input(SlashCommandInput value) {
            this.input = value;
            return this;
        }

        /**
         * Sets the {@code allowDuringAgentExecution} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allowDuringAgentExecution(Boolean value) {
            this.allowDuringAgentExecution = value;
            return this;
        }

        /**
         * Sets the {@code experimental} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder experimental(Boolean value) {
            this.experimental = value;
            return this;
        }

        /**
         * Sets the {@code schedulable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder schedulable(Boolean value) {
            this.schedulable = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SlashCommandInfo build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (allowDuringAgentExecution == null) throw new IllegalStateException("Missing required property: allowDuringAgentExecution");
            return new SlashCommandInfo(name, aliases, description, kind, input, allowDuringAgentExecution, experimental, schedulable);
        }
    }
}
