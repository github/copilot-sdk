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
 * Optional unstructured input hint
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SlashCommandInput(
    /** Hint to display when command input has not been provided */
    @JsonProperty("hint") String hint,
    /** Optional literal choices the input accepts, each with a human-facing description; clients may render these as selectable options */
    @JsonProperty("choices") List<SlashCommandInputChoice> choices,
    /** When true, the command requires non-empty input; clients should render the input hint as required */
    @JsonProperty("required") Boolean required,
    /** Optional completion hint for the input (e.g. 'directory' for filesystem path completion) */
    @JsonProperty("completion") SlashCommandInputCompletion completion,
    /** When true, clients should pass the full text after the command name as a single argument rather than splitting on whitespace */
    @JsonProperty("preserveMultilineInput") Boolean preserveMultilineInput
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SlashCommandInput {}

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
        private String hint;
        private List<SlashCommandInputChoice> choices;
        private Boolean required;
        private SlashCommandInputCompletion completion;
        private Boolean preserveMultilineInput;

        /**
         * Sets the {@code hint} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hint(String value) {
            this.hint = value;
            return this;
        }

        /**
         * Sets the {@code choices} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder choices(List<SlashCommandInputChoice> value) {
            this.choices = value;
            return this;
        }

        /**
         * Sets the {@code required} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder required(Boolean value) {
            this.required = value;
            return this;
        }

        /**
         * Sets the {@code completion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder completion(SlashCommandInputCompletion value) {
            this.completion = value;
            return this;
        }

        /**
         * Sets the {@code preserveMultilineInput} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder preserveMultilineInput(Boolean value) {
            this.preserveMultilineInput = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SlashCommandInput build() {
            if (hint == null) throw new IllegalStateException("Missing required property: hint");
            return new SlashCommandInput(hint, choices, required, completion, preserveMultilineInput);
        }
    }
}
