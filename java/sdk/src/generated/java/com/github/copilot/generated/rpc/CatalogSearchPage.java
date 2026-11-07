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
 * An explicit numbered-page request. SDK consumers treat the token as opaque. For bound search, the runtime unwraps an expiring owner-bound reference to the private authority token; only the runtime changes the authority token's targetPage. Legacy unbound navigation keeps its authority-issued token semantics. No snapshot stability is promised.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSearchPage(
    /** Opaque pagination token from an earlier response, owner-bound when session-bound search was requested. Never decode, modify or log it in an SDK consumer. Expired or foreign bound references require a fresh bound search, not a legacy retry. */
    @JsonProperty("token") String token,
    /** Requested one-based page. Must not exceed either the token's signed pageCount or the navigation window ceil(1000 / pageSize). Repeat the search without page to discover newly available pages beyond that signed pageCount. */
    @JsonProperty("number") Long number
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CatalogSearchPage {}

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
        private Long number;

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
         * Sets the {@code number} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder number(Long value) {
            this.number = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogSearchPage build() {
            if (token == null) throw new IllegalStateException("Missing required property: token");
            if (number == null) throw new IllegalStateException("Missing required property: number");
            return new CatalogSearchPage(token, number);
        }
    }
}
