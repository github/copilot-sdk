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
 * Canonical process-log discovery outcome; populated from spawnLiveTarget.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentRegistryLogCapture(
    /** Whether a canonical process log was discovered for this managed spawn */
    @JsonProperty("enabled") Boolean enabled,
    /** Absolute path to the managed spawn's process-<timestamp>-<pid>.log file (only set when enabled) */
    @JsonProperty("path") String path,
    /** Why no canonical process log could be opened for this managed spawn (set only when enabled is false) */
    @JsonProperty("openError") String openError,
    /** Categorized reason no canonical process log could be opened (set only when enabled is false) */
    @JsonProperty("openErrorReason") AgentRegistryLogCaptureOpenErrorReason openErrorReason
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AgentRegistryLogCapture {}

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
        private Boolean enabled;
        private String path;
        private String openError;
        private AgentRegistryLogCaptureOpenErrorReason openErrorReason;

        /**
         * Sets the {@code enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enabled(Boolean value) {
            this.enabled = value;
            return this;
        }

        /**
         * Sets the {@code path} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder path(String value) {
            this.path = value;
            return this;
        }

        /**
         * Sets the {@code openError} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder openError(String value) {
            this.openError = value;
            return this;
        }

        /**
         * Sets the {@code openErrorReason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder openErrorReason(AgentRegistryLogCaptureOpenErrorReason value) {
            this.openErrorReason = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentRegistryLogCapture build() {
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            return new AgentRegistryLogCapture(enabled, path, openError, openErrorReason);
        }
    }
}
