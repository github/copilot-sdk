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
 * Cost-category metadata for a CAPI model.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelPriceCategory(
    /** CAPI model identifier. */
    @JsonProperty("id") String id,
    /** Cost category assigned to the model. */
    @JsonProperty("priceCategory") ModelPickerPriceCategory priceCategory
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModelPriceCategory {}

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
        private ModelPickerPriceCategory priceCategory;

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
         * Sets the {@code priceCategory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder priceCategory(ModelPickerPriceCategory value) {
            this.priceCategory = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionModelPriceCategory build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (priceCategory == null) throw new IllegalStateException("Missing required property: priceCategory");
            return new SessionModelPriceCategory(id, priceCategory);
        }
    }
}
