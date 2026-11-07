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
import javax.annotation.processing.Generated;

/**
 * One slash command, as typed, to test against the enforcement-block allowlist.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SandboxCheckEnforcementCommandParams(
    /** The command name as the user typed it, with or without its leading slash. */
    @JsonProperty("commandName") String commandName,
    /** The command's arguments, in order. Pass an empty array for a bare command: several entries in the allowlist admit the bare form and refuse every argument. */
    @JsonProperty("args") List<String> args
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxCheckEnforcementCommandParams {}

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
        private String commandName;
        private List<String> args;

        /**
         * Sets the {@code commandName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder commandName(String value) {
            this.commandName = value;
            return this;
        }

        /**
         * Sets the {@code args} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder args(List<String> value) {
            this.args = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxCheckEnforcementCommandParams build() {
            if (commandName == null) throw new IllegalStateException("Missing required property: commandName");
            if (args == null) throw new IllegalStateException("Missing required property: args");
            return new SandboxCheckEnforcementCommandParams(commandName, args);
        }
    }
}
