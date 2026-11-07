/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Short-lived, rotating credential the caller must send on every request, in addition to `apiKey` if one is present. Omitted when the endpoint does not require one.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderSessionToken(
    /** The short-lived token value. */
    @JsonProperty("token") String token,
    /** HTTP header name the token must be sent under. */
    @JsonProperty("header") String header,
    /** The model the token is bound to, when applicable. When set, the token is only valid for requests against this model. */
    @JsonProperty("model") String model,
    /** When the token expires, if known. Callers should refresh by calling `getEndpoint` again before this time, or reactively on any 401/403 response from `baseUrl`. */
    @JsonProperty("expiresAt") OffsetDateTime expiresAt
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ProviderSessionToken {}

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
        private String token;
        private String header;
        private String model;
        private OffsetDateTime expiresAt;

        /**
         * Sets the {@code token} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder token(String value) {
            this.token = value;
            return this;
        }

        /**
         * Sets the {@code header} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder header(String value) {
            this.header = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code expiresAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expiresAt(OffsetDateTime value) {
            this.expiresAt = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ProviderSessionToken build() {
            if (token == null) throw new IllegalStateException("Missing required property: token");
            if (header == null) throw new IllegalStateException("Missing required property: header");
            return new ProviderSessionToken(token, header, model, expiresAt);
        }
    }
}
