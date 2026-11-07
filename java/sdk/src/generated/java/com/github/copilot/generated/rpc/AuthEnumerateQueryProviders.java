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
 * Variant {@code providers} of {@link AuthEnumerateQuery}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthEnumerateQueryProviders extends AuthEnumerateQuery {

    @JsonProperty("kind")
    private final String kind = "providers";

    @Override
    public String getKind() { return kind; }

    /** Whether an interactive Entra broker is available on the host; gates Entra availability in the returned list. */
    @JsonProperty("brokerAvailable")
    private Boolean brokerAvailable;

    public Boolean getBrokerAvailable() { return brokerAvailable; }
    public void setBrokerAvailable(Boolean brokerAvailable) { this.brokerAvailable = brokerAvailable; }


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
        private Boolean brokerAvailable;

        /**
         * Sets the {@code brokerAvailable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder brokerAvailable(Boolean value) {
            this.brokerAvailable = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthEnumerateQueryProviders build() {
            AuthEnumerateQueryProviders instance = new AuthEnumerateQueryProviders();
            instance.setBrokerAvailable(brokerAvailable);
            return instance;
        }
    }
}
