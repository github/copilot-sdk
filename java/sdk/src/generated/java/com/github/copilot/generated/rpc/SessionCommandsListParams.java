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
import javax.annotation.processing.Generated;

/**
 * Request parameters for the {@code session.commands.list} RPC method.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionCommandsListParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Include runtime built-in commands */
    @JsonProperty("includeBuiltins") Boolean includeBuiltins,
    /** Include enabled user-invocable skills and commands */
    @JsonProperty("includeSkills") Boolean includeSkills,
    /** Include commands registered by protocol clients, including SDK clients and extensions */
    @JsonProperty("includeClientCommands") Boolean includeClientCommands
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionCommandsListParams {}

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
        private String sessionId;
        private Boolean includeBuiltins;
        private Boolean includeSkills;
        private Boolean includeClientCommands;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code includeBuiltins} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder includeBuiltins(Boolean value) {
            this.includeBuiltins = value;
            return this;
        }

        /**
         * Sets the {@code includeSkills} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder includeSkills(Boolean value) {
            this.includeSkills = value;
            return this;
        }

        /**
         * Sets the {@code includeClientCommands} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder includeClientCommands(Boolean value) {
            this.includeClientCommands = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionCommandsListParams build() {
            return new SessionCommandsListParams(sessionId, includeBuiltins, includeSkills, includeClientCommands);
        }
    }
}
