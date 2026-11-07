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
 * Variant {@code providers} of {@link AuthEnumerateValue}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthEnumerateValueProviders extends AuthEnumerateValue {

    @JsonProperty("kind")
    private final String kind = "providers";

    @Override
    public String getKind() { return kind; }

    /** The providers offered for interactive login. */
    @JsonProperty("items")
    private List<ProviderDescriptor> items;

    public List<ProviderDescriptor> getItems() { return items; }
    public void setItems(List<ProviderDescriptor> items) { this.items = items; }


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
        private List<ProviderDescriptor> items;

        /**
         * Sets the {@code items} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder items(List<ProviderDescriptor> value) {
            this.items = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthEnumerateValueProviders build() {
            if (items == null) throw new IllegalStateException("Missing required property: items");
            AuthEnumerateValueProviders instance = new AuthEnumerateValueProviders();
            instance.setItems(items);
            return instance;
        }
    }
}
