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
 * Catalog source and its display order.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalModelSource(
    /** Stable source identifier. */
    @JsonProperty("id") String id,
    /** Source display name. */
    @JsonProperty("label") String label,
    /** Display order among catalog sources. */
    @JsonProperty("ordering") Double ordering
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelSource {}

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
        private Double ordering;

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
         * Sets the {@code ordering} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ordering(Double value) {
            this.ordering = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelSource build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (label == null) throw new IllegalStateException("Missing required property: label");
            if (ordering == null) throw new IllegalStateException("Missing required property: ordering");
            return new LocalModelSource(id, label, ordering);
        }
    }
}
