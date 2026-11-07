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
 * Result of a language-server startup probe.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionLspDiagnoseResult(
    /** Runtime selection outcome: started, disabled, missing, or unavailable. */
    @JsonProperty("status") String status,
    /** Effective server identifiers, used when the requested server is absent. */
    @JsonProperty("availableServers") List<String> availableServers,
    /** Child process identifier, or null when no process identifier exists. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("pid") Long pid,
    /** Time spent on process startup, in milliseconds. */
    @JsonProperty("spawnTimeMs") Long spawnTimeMs,
    /** Child exit observed during the startup window. */
    @JsonProperty("immediateExit") LspProbeExitStatus immediateExit,
    /** Whether the runtime confined the probe in the session sandbox. */
    @JsonProperty("sandboxed") Boolean sandboxed
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionLspDiagnoseResult {}

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
        private String status;
        private List<String> availableServers;
        private Long pid;
        private boolean $presence2;
        private Long spawnTimeMs;
        private LspProbeExitStatus immediateExit;
        private Boolean sandboxed;

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(String value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code availableServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder availableServers(List<String> value) {
            this.availableServers = value;
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
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code spawnTimeMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder spawnTimeMs(Long value) {
            this.spawnTimeMs = value;
            return this;
        }

        /**
         * Sets the {@code immediateExit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder immediateExit(LspProbeExitStatus value) {
            this.immediateExit = value;
            return this;
        }

        /**
         * Sets the {@code sandboxed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sandboxed(Boolean value) {
            this.sandboxed = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionLspDiagnoseResult build() {
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (availableServers == null) throw new IllegalStateException("Missing required property: availableServers");
            if (!$presence2) throw new IllegalStateException("Missing required property: pid");
            if (spawnTimeMs == null) throw new IllegalStateException("Missing required property: spawnTimeMs");
            if (sandboxed == null) throw new IllegalStateException("Missing required property: sandboxed");
            return new SessionLspDiagnoseResult(status, availableServers, pid, spawnTimeMs, immediateExit, sandboxed);
        }
    }
}
