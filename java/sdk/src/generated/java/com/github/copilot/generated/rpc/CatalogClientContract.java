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
 * The protocol version and capability set a caller requires, supplied on every catalog request so negotiation cannot be skipped by omission.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogClientContract(
    /** SDK protocol version the caller was generated against. A caller below the runtime's minimum supported version is refused rather than served a partial result. */
    @JsonProperty("protocolVersion") Long protocolVersion,
    /** Wire features the caller requires the runtime to understand. Identifiers are bounded but extensible so a newer caller can negotiate with an older runtime. Requiring an unknown feature yields a typed refusal listing what is understood, never a partial grant. A grant does not promise that a deployment has enabled the operation; typed unavailable results report that separately. */
    @JsonProperty("requiredCapabilities") List<String> requiredCapabilities
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CatalogClientContract {}

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
        private Long protocolVersion;
        private List<String> requiredCapabilities;

        /**
         * Sets the {@code protocolVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder protocolVersion(Long value) {
            this.protocolVersion = value;
            return this;
        }

        /**
         * Sets the {@code requiredCapabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requiredCapabilities(List<String> value) {
            this.requiredCapabilities = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogClientContract build() {
            if (protocolVersion == null) throw new IllegalStateException("Missing required property: protocolVersion");
            if (requiredCapabilities == null) throw new IllegalStateException("Missing required property: requiredCapabilities");
            return new CatalogClientContract(protocolVersion, requiredCapabilities);
        }
    }
}
