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
 * Rust-owned metadata and input schema for a built-in tool.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record BuiltinToolDescriptor(
    /** Stable name used to invoke the built-in tool. */
    @JsonProperty("name") String name,
    /** Optional human-readable title for the tool. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("title") String title,
    /** Model-facing description of the tool's behavior. */
    @JsonProperty("description") String description,
    /** JSON Schema for the tool input, or null when the tool uses a custom format. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("inputSchema") BuiltinToolInputSchema inputSchema,
    /** Optional supplemental usage instructions for the tool. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("instructions") String instructions,
    /** Optional tool category discriminator. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("type") String type,
    /** Optional custom input format used instead of a JSON Schema. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("format") BuiltinToolFormat format,
    /** Policy describing which tool metadata may be recorded without obfuscation. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("safeForTelemetry") Object safeForTelemetry,
    /** Whether the tool executes commands in a terminal. */
    @JsonProperty("isTerminal") Boolean isTerminal,
    /** Whether the tool provides a specialized intention summary. */
    @JsonProperty("hasSummariseIntention") Boolean hasSummariseIntention
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public BuiltinToolDescriptor {}

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
        private String name;
        private String title;
        private boolean $presence1;
        private String description;
        private BuiltinToolInputSchema inputSchema;
        private boolean $presence3;
        private String instructions;
        private boolean $presence4;
        private String type;
        private boolean $presence5;
        private BuiltinToolFormat format;
        private boolean $presence6;
        private Object safeForTelemetry;
        private boolean $presence7;
        private Boolean isTerminal;
        private Boolean hasSummariseIntention;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code title} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder title(String value) {
            this.title = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code inputSchema} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder inputSchema(BuiltinToolInputSchema value) {
            this.inputSchema = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code instructions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder instructions(String value) {
            this.instructions = value;
            this.$presence4 = true;
            return this;
        }

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder type(String value) {
            this.type = value;
            this.$presence5 = true;
            return this;
        }

        /**
         * Sets the {@code format} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder format(BuiltinToolFormat value) {
            this.format = value;
            this.$presence6 = true;
            return this;
        }

        /**
         * Sets the {@code safeForTelemetry} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder safeForTelemetry(Object value) {
            this.safeForTelemetry = value;
            this.$presence7 = true;
            return this;
        }

        /**
         * Sets the {@code isTerminal} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder isTerminal(Boolean value) {
            this.isTerminal = value;
            return this;
        }

        /**
         * Sets the {@code hasSummariseIntention} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hasSummariseIntention(Boolean value) {
            this.hasSummariseIntention = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public BuiltinToolDescriptor build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (!$presence1) throw new IllegalStateException("Missing required property: title");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            if (!$presence3) throw new IllegalStateException("Missing required property: inputSchema");
            if (!$presence4) throw new IllegalStateException("Missing required property: instructions");
            if (!$presence5) throw new IllegalStateException("Missing required property: type");
            if (!$presence6) throw new IllegalStateException("Missing required property: format");
            if (!$presence7) throw new IllegalStateException("Missing required property: safeForTelemetry");
            if (isTerminal == null) throw new IllegalStateException("Missing required property: isTerminal");
            if (hasSummariseIntention == null) throw new IllegalStateException("Missing required property: hasSummariseIntention");
            return new BuiltinToolDescriptor(name, title, description, inputSchema, instructions, type, format, safeForTelemetry, isTerminal, hasSummariseIntention);
        }
    }
}
