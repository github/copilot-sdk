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
 * Semantic digest of a strictly parsed and schema-validated JSON MCP card. Both URL-backed and embedded cards are canonicalised with RFC 8785 JSON Canonicalization Scheme, encoded as UTF-8, and hashed with SHA-256.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CardDigest(
    /** Digest algorithm and canonical representation */
    @JsonProperty("algorithm") CardDigestAlgorithm algorithm,
    /** SHA-256 digest of the RFC 8785 canonical UTF-8 bytes, encoded as exactly 64 lowercase hexadecimal characters. */
    @JsonProperty("value") String value
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CardDigest {}

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
        private CardDigestAlgorithm algorithm;
        private String value;

        /**
         * Sets the {@code algorithm} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder algorithm(CardDigestAlgorithm value) {
            this.algorithm = value;
            return this;
        }

        /**
         * Sets the {@code value} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CardDigest build() {
            if (algorithm == null) throw new IllegalStateException("Missing required property: algorithm");
            if (value == null) throw new IllegalStateException("Missing required property: value");
            return new CardDigest(algorithm, value);
        }
    }
}
