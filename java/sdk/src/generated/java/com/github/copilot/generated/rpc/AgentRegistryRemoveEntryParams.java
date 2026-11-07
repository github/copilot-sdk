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
 * A local agent entry identified by its publisher.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AgentRegistryRemoveEntryParams(
    /** Explicit Copilot home, or absent to use the home directory. */
    @JsonProperty("copilotHome") String copilotHome,
    /** The caller's home directory. */
    @JsonProperty("homeDir") String homeDir,
    /** The publisher's process identifier. */
    @JsonProperty("pid") Long pid
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    AgentRegistryRemoveEntryParams {}

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
        private String copilotHome;
        private String homeDir;
        private Long pid;

        /**
         * Sets the {@code copilotHome} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder copilotHome(String value) {
            this.copilotHome = value;
            return this;
        }

        /**
         * Sets the {@code homeDir} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder homeDir(String value) {
            this.homeDir = value;
            return this;
        }

        /**
         * Sets the {@code pid} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pid(Long value) {
            this.pid = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentRegistryRemoveEntryParams build() {
            if (homeDir == null) throw new IllegalStateException("Missing required property: homeDir");
            if (pid == null) throw new IllegalStateException("Missing required property: pid");
            return new AgentRegistryRemoveEntryParams(copilotHome, homeDir, pid);
        }
    }
}
