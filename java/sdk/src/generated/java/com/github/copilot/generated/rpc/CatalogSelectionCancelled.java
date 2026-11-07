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
 * The caller cancelled the selection interaction and the retained search state was released.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogSelectionCancelled extends CatalogSelectionResult {

    @JsonProperty("kind")
    private final String kind = "cancelled";

    @Override
    public String getKind() { return kind; }

    /** The search identifier privately bound to the released selection group. */
    @JsonProperty("searchId")
    private String searchId;

    public String getSearchId() { return searchId; }
    public void setSearchId(String searchId) { this.searchId = searchId; }


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
        private String searchId;

        /**
         * Sets the {@code searchId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder searchId(String value) {
            this.searchId = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogSelectionCancelled build() {
            if (searchId == null) throw new IllegalStateException("Missing required property: searchId");
            CatalogSelectionCancelled instance = new CatalogSelectionCancelled();
            instance.setSearchId(searchId);
            return instance;
        }
    }
}
