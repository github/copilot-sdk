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
 * Which stored location permissions were dropped because their directory is gone.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record LocationPermissionsCleanupResult(
    /** Location keys whose grants were removed. Empty when every stored location still exists. */
    @JsonProperty("removedLocationKeys") List<String> removedLocationKeys
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    LocationPermissionsCleanupResult {}

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
        private List<String> removedLocationKeys;

        /**
         * Sets the {@code removedLocationKeys} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder removedLocationKeys(List<String> value) {
            this.removedLocationKeys = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocationPermissionsCleanupResult build() {
            if (removedLocationKeys == null) throw new IllegalStateException("Missing required property: removedLocationKeys");
            return new LocationPermissionsCleanupResult(removedLocationKeys);
        }
    }
}
