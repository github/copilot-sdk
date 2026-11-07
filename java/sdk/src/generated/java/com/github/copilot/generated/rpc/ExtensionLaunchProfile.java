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
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Opaque integrator-owned process launch profile for one extension entrypoint.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtensionLaunchProfile(
    /** Executable used to launch the extension entrypoint. */
    @JsonProperty("executable") String executable,
    /** Opaque integrator-defined arguments passed to the executable. The runtime does not append the extension entrypoint. */
    @JsonProperty("args") List<String> args,
    /** Opaque integrator-defined environment variables. Runtime-owned COPILOT_SDK_PATH, SESSION_ID, and COPILOT_EXTENSION_PARENT_PID values take precedence. */
    @JsonProperty("env") Map<String, String> env
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ExtensionLaunchProfile {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String executable;
        private List<String> args;
        private Map<String, String> env;

        /**
         * Sets the {@code executable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder executable(String value) {
            this.executable = value;
            return this;
        }

        /**
         * Sets the {@code args} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder args(List<String> value) {
            this.args = value;
            return this;
        }

        /**
         * Sets the {@code env} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder env(Map<String, String> value) {
            this.env = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ExtensionLaunchProfile build() {
            if (executable == null) throw new IllegalStateException("Missing required property: executable");
            if (args == null) throw new IllegalStateException("Missing required property: args");
            if (env == null) throw new IllegalStateException("Missing required property: env");
            return new ExtensionLaunchProfile(executable, args, env);
        }
    }
}
