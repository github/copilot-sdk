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
 * Variant {@code loginAccounts} of {@link AuthEnumerateValue}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthEnumerateValueLoginAccounts extends AuthEnumerateValue {

    @JsonProperty("kind")
    private final String kind = "loginAccounts";

    @Override
    public String getKind() { return kind; }

    /** Cached base identities reusable by the selected login provider, active first. */
    @JsonProperty("items")
    private List<AuthLoginIdentity> items;

    public List<AuthLoginIdentity> getItems() { return items; }
    public void setItems(List<AuthLoginIdentity> items) { this.items = items; }


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
        private List<AuthLoginIdentity> items;

        /**
         * Sets the {@code items} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder items(List<AuthLoginIdentity> value) {
            this.items = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthEnumerateValueLoginAccounts build() {
            if (items == null) throw new IllegalStateException("Missing required property: items");
            AuthEnumerateValueLoginAccounts instance = new AuthEnumerateValueLoginAccounts();
            instance.setItems(items);
            return instance;
        }
    }
}
