/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Policy Helper failure and recovery features implemented by this runtime. Capability presence, not absence of an optional failure, determines whether a client may rely on these contracts.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyHelperCapabilities(
    /** The runtime emits PolicyHelperFailure on shared managed-settings snapshots and applicable structured operation errors. */
    @JsonProperty("structuredFailure") Boolean structuredFailure,
    /** The runtime supports session.managedSettings.recheckPolicyHelper for a fresh scoped helper evaluation. */
    @JsonProperty("recheck") Boolean recheck
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PolicyHelperCapabilities {}

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
        private Boolean structuredFailure;
        private Boolean recheck;

        /**
         * Sets the {@code structuredFailure} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder structuredFailure(Boolean value) {
            this.structuredFailure = value;
            return this;
        }

        /**
         * Sets the {@code recheck} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder recheck(Boolean value) {
            this.recheck = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PolicyHelperCapabilities build() {
            if (structuredFailure == null) throw new IllegalStateException("Missing required property: structuredFailure");
            if (recheck == null) throw new IllegalStateException("Missing required property: recheck");
            return new PolicyHelperCapabilities(structuredFailure, recheck);
        }
    }
}
