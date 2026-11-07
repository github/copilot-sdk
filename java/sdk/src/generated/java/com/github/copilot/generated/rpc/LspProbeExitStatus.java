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
 * Process exit information from a startup probe.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LspProbeExitStatus(
    /** Exit code, or null when the process ended through a signal. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("code") Long code,
    /** Termination signal, or null for a normal exit. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("signal") String signal,
    /** Failure to observe the child process. */
    @JsonProperty("error") String error
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LspProbeExitStatus {}

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
        private Long code;
        private boolean $presence0;
        private String signal;
        private boolean $presence1;
        private String error;

        /**
         * Sets the {@code code} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder code(Long value) {
            this.code = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code signal} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder signal(String value) {
            this.signal = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder error(String value) {
            this.error = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LspProbeExitStatus build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: code");
            if (!$presence1) throw new IllegalStateException("Missing required property: signal");
            return new LspProbeExitStatus(code, signal, error);
        }
    }
}
