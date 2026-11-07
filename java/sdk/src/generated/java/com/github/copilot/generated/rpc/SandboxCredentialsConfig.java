/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Whole-value environment credential masking for sandboxed children.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxCredentialsConfig(
    /** Environment variable names and their HTTPS injection destinations. Absent variables stay absent. No real values or sentinels are stored in this map. */
    @JsonProperty("envVars") Map<String, SandboxMaskedEnvVar> envVars
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SandboxCredentialsConfig {}

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
        private Map<String, SandboxMaskedEnvVar> envVars;

        /**
         * Sets the {@code envVars} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder envVars(Map<String, SandboxMaskedEnvVar> value) {
            this.envVars = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxCredentialsConfig build() {
            if (envVars == null) throw new IllegalStateException("Missing required property: envVars");
            return new SandboxCredentialsConfig(envVars);
        }
    }
}
