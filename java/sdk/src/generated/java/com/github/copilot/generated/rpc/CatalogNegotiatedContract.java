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
 * The protocol version and capability set the runtime actually honoured for a successful catalog operation.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogNegotiatedContract(
    /** Protocol version of the runtime that served the request. */
    @JsonProperty("runtimeProtocolVersion") Long runtimeProtocolVersion,
    /** Wire features the runtime understood for this operation. Includes the five original catalog capabilities and only explicitly requested supported additions, in supported order without duplicates. Capabilities that introduce new success-union variants or operations are therefore included only when explicitly required, preserving older protocol-v3 clients. Always a superset of the caller's required features, because any shortfall is a refusal instead. Operation availability remains a separate typed result. */
    @JsonProperty("grantedCapabilities") List<CatalogCapability> grantedCapabilities
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CatalogNegotiatedContract {}

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
        private Long runtimeProtocolVersion;
        private List<CatalogCapability> grantedCapabilities;

        /**
         * Sets the {@code runtimeProtocolVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder runtimeProtocolVersion(Long value) {
            this.runtimeProtocolVersion = value;
            return this;
        }

        /**
         * Sets the {@code grantedCapabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder grantedCapabilities(List<CatalogCapability> value) {
            this.grantedCapabilities = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogNegotiatedContract build() {
            if (runtimeProtocolVersion == null) throw new IllegalStateException("Missing required property: runtimeProtocolVersion");
            if (grantedCapabilities == null) throw new IllegalStateException("Missing required property: grantedCapabilities");
            return new CatalogNegotiatedContract(runtimeProtocolVersion, grantedCapabilities);
        }
    }
}
