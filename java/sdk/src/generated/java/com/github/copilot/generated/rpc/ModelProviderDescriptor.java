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
 * One model provider available to the session — the model analog of the account `ProviderDescriptor`. Opaque id/label/kind plus a stable ordering; central code never branches on kind.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderDescriptor(
    /** The neutral provider kind. */
    @JsonProperty("kind") ModelProviderKind kind,
    /** Opaque, stable provider id, stamped onto every model this provider returns. */
    @JsonProperty("id") String id,
    /** Human-readable menu label, owned by the runtime so every consumer renders identical text. */
    @JsonProperty("label") String label,
    /** Stable ordering key for presenting providers in a deterministic sequence. */
    @JsonProperty("ordering") Long ordering
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelProviderDescriptor {}

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
        private ModelProviderKind kind;
        private String id;
        private String label;
        private Long ordering;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder kind(ModelProviderKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder label(String value) {
            this.label = value;
            return this;
        }

        /**
         * Sets the {@code ordering} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder ordering(Long value) {
            this.ordering = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelProviderDescriptor build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (label == null) throw new IllegalStateException("Missing required property: label");
            if (ordering == null) throw new IllegalStateException("Missing required property: ordering");
            return new ModelProviderDescriptor(kind, id, label, ordering);
        }
    }
}
