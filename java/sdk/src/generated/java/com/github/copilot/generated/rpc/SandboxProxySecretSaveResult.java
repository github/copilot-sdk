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
 * Where the sandbox proxy password was stored.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SandboxProxySecretSaveResult(
    /** Whether the OS keychain accepted the value. False means it was written to the on-disk fallback store instead, which is still readable by `sandbox.proxySecret.get`. */
    @JsonProperty("storedInKeychain") Boolean storedInKeychain
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxProxySecretSaveResult {}

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
        private Boolean storedInKeychain;

        /**
         * Sets the {@code storedInKeychain} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder storedInKeychain(Boolean value) {
            this.storedInKeychain = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxProxySecretSaveResult build() {
            if (storedInKeychain == null) throw new IllegalStateException("Missing required property: storedInKeychain");
            return new SandboxProxySecretSaveResult(storedInKeychain);
        }
    }
}
