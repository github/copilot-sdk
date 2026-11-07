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

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SlashCommandModelPickerDialog(
    /** Discriminator for a model-picker dialog. */
    @JsonProperty("kind") String kind,
    /** Model that should be enabled before it can be selected. */
    @JsonProperty("modelToEnable") String modelToEnable,
    /** Settings scope the picker should modify. */
    @JsonProperty("scope") String scope,
    /** Model-selection target represented by the picker. */
    @JsonProperty("target") String target
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SlashCommandModelPickerDialog {}

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
        private String kind;
        private String modelToEnable;
        private String scope;
        private String target;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(String value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code modelToEnable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelToEnable(String value) {
            this.modelToEnable = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder scope(String value) {
            this.scope = value;
            return this;
        }

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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SlashCommandModelPickerDialog build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            return new SlashCommandModelPickerDialog(kind, modelToEnable, scope, target);
        }
    }
}
