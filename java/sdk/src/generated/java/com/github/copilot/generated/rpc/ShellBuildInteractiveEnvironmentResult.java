/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Filtered caller environment with the runtime's Git hardening applied; never includes the runtime process environment.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShellBuildInteractiveEnvironmentResult(
    /** Environment for the host's interactive shell. */
    @JsonProperty("environment") Map<String, String> environment
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ShellBuildInteractiveEnvironmentResult {}

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
        private Map<String, String> environment;

        /**
         * Sets the {@code environment} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder environment(Map<String, String> value) {
            this.environment = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ShellBuildInteractiveEnvironmentResult build() {
            if (environment == null) throw new IllegalStateException("Missing required property: environment");
            return new ShellBuildInteractiveEnvironmentResult(environment);
        }
    }

    @Override
    public String toString() {
        return "ShellBuildInteractiveEnvironmentResult[environment=" + (environment == null ? null : "<redacted>") + "]";
    }
}
