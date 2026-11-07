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
 * Provenance of the exact validated JSON MCP card content bound privately to a completed plan and its opaque handle.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPlanProvenance(
    /** Authority associated with the validated card, without path, query, or credentials. Inert untrusted data. */
    @JsonProperty("authority") String authority,
    /** ISO 8601 timestamp at which the runtime completed strict parsing and schema validation of the card content. */
    @JsonProperty("validatedAt") String validatedAt,
    /** Semantic digest of the exact validated JSON content bound to the plan handle. */
    @JsonProperty("cardDigest") CardDigest cardDigest,
    /** JSON MCP media type the validated card was interpreted as. */
    @JsonProperty("mediaType") McpServerCardMediaType mediaType
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpPlanProvenance {}

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
        private String authority;
        private String validatedAt;
        private CardDigest cardDigest;
        private McpServerCardMediaType mediaType;

        /**
         * Sets the {@code authority} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder authority(String value) {
            this.authority = value;
            return this;
        }

        /**
         * Sets the {@code validatedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder validatedAt(String value) {
            this.validatedAt = value;
            return this;
        }

        /**
         * Sets the {@code cardDigest} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cardDigest(CardDigest value) {
            this.cardDigest = value;
            return this;
        }

        /**
         * Sets the {@code mediaType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mediaType(McpServerCardMediaType value) {
            this.mediaType = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpPlanProvenance build() {
            if (authority == null) throw new IllegalStateException("Missing required property: authority");
            if (validatedAt == null) throw new IllegalStateException("Missing required property: validatedAt");
            if (cardDigest == null) throw new IllegalStateException("Missing required property: cardDigest");
            if (mediaType == null) throw new IllegalStateException("Missing required property: mediaType");
            return new McpPlanProvenance(authority, validatedAt, cardDigest, mediaType);
        }
    }
}
