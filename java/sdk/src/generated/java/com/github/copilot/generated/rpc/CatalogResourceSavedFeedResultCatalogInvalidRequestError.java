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
 * The request was rejected because a bounded field fell outside its permitted range or a required field was unusable. Pagination may also be rejected by the authority after a continuation request; repeat the search without page.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogResourceSavedFeedResultCatalogInvalidRequestError extends CatalogResourceSavedFeedResult {

    @JsonProperty("kind")
    private final String kind = "invalid-request";

    @Override
    public String getKind() { return kind; }

    /** Which request field was rejected. */
    @JsonProperty("field")
    private CatalogInvalidRequestField field;

    /** Human-readable explanation, safe to surface. Never echoes the offending value, nor a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public CatalogInvalidRequestField getField() { return field; }
    public void setField(CatalogInvalidRequestField field) { this.field = field; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }


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
        private CatalogInvalidRequestField field;
        private String message;

        /**
         * Sets the {@code field} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder field(CatalogInvalidRequestField value) {
            this.field = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogResourceSavedFeedResultCatalogInvalidRequestError build() {
            if (field == null) throw new IllegalStateException("Missing required property: field");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            CatalogResourceSavedFeedResultCatalogInvalidRequestError instance = new CatalogResourceSavedFeedResultCatalogInvalidRequestError();
            instance.setField(field);
            instance.setMessage(message);
            return instance;
        }
    }
}
