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
 * Options for local agent discovery.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AgentRegistryListEntriesParams(
    /** Explicit Copilot home, or absent to use the home directory. */
    @JsonProperty("copilotHome") String copilotHome,
    /** The caller's home directory. */
    @JsonProperty("homeDir") String homeDir,
    /** A publisher to exclude. */
    @JsonProperty("excludePid") Long excludePid,
    /** Maximum heartbeat age in milliseconds. */
    @JsonProperty("staleMs") Double staleMs,
    /** Override the current epoch time for deterministic discovery. */
    @JsonProperty("nowMs") Double nowMs,
    /** Override process liveness checks. */
    @JsonProperty("pidAliveOverride") Boolean pidAliveOverride
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    AgentRegistryListEntriesParams {}

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
        private Long excludePid;
        private Double staleMs;
        private Double nowMs;
        private Boolean pidAliveOverride;

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
         * Sets the {@code excludePid} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder excludePid(Long value) {
            this.excludePid = value;
            return this;
        }

        /**
         * Sets the {@code staleMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder staleMs(Double value) {
            this.staleMs = value;
            return this;
        }

        /**
         * Sets the {@code nowMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder nowMs(Double value) {
            this.nowMs = value;
            return this;
        }

        /**
         * Sets the {@code pidAliveOverride} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pidAliveOverride(Boolean value) {
            this.pidAliveOverride = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentRegistryListEntriesParams build() {
            if (homeDir == null) throw new IllegalStateException("Missing required property: homeDir");
            return new AgentRegistryListEntriesParams(copilotHome, homeDir, excludePid, staleMs, nowMs, pidAliveOverride);
        }
    }
}
