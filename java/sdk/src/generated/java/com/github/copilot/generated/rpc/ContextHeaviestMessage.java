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
 * A single large message currently in context.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContextHeaviestMessage(
    /** Stable identifier for this message within the snapshot. */
    @JsonProperty("id") String id,
    /** Human-readable source label, e.g. `tool: bash` or `skill: tmux`. Presentation-only. */
    @JsonProperty("label") String label,
    /** Role of the chat message (`user`, `assistant`, or `tool`). */
    @JsonProperty("role") String role,
    /** Token count currently in context for this individual message. */
    @JsonProperty("tokens") Long tokens
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ContextHeaviestMessage {}

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
        private String id;
        private String label;
        private String role;
        private Long tokens;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code label} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder label(String value) {
            this.label = value;
            return this;
        }

        /**
         * Sets the {@code role} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder role(String value) {
            this.role = value;
            return this;
        }

        /**
         * Sets the {@code tokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tokens(Long value) {
            this.tokens = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ContextHeaviestMessage build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (label == null) throw new IllegalStateException("Missing required property: label");
            if (role == null) throw new IllegalStateException("Missing required property: role");
            if (tokens == null) throw new IllegalStateException("Missing required property: tokens");
            return new ContextHeaviestMessage(id, label, role, tokens);
        }
    }
}
