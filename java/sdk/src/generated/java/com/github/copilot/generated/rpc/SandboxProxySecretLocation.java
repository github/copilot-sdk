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
 * Where this runtime keeps the sandbox proxy password.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SandboxProxySecretLocation(
    /** Absolute path of the credential store holding the sandbox proxy password. The proxy password belongs to no MCP server, so it is kept in its own directory rather than in the MCP secret store. */
    @JsonProperty("configDir") String configDir,
    /** Identifier of the proxy password entry within that store. Derived from the configuration directory, so two Copilot homes on one machine never share an entry. Use it to recognize a `${secret:…}` reference in stored settings as this runtime's own proxy password. */
    @JsonProperty("secretId") String secretId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxProxySecretLocation {}

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
        private String configDir;
        private String secretId;

        /**
         * Sets the {@code configDir} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder configDir(String value) {
            this.configDir = value;
            return this;
        }

        /**
         * Sets the {@code secretId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder secretId(String value) {
            this.secretId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxProxySecretLocation build() {
            if (configDir == null) throw new IllegalStateException("Missing required property: configDir");
            if (secretId == null) throw new IllegalStateException("Missing required property: secretId");
            return new SandboxProxySecretLocation(configDir, secretId);
        }
    }
}
