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
 * Configuration for the runtime-managed remote-control singleton.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteControlConfig(
    /** Whether remote export should be enabled. */
    @JsonProperty("remote") Boolean remote,
    /** Whether the MC session may steer the local session (write mode). */
    @JsonProperty("steerable") Boolean steerable,
    /** Whether the user explicitly requested remote (vs. implicit session-sync). Controls warning surfacing for missing-repo cases. */
    @JsonProperty("explicit") Boolean explicit,
    /** When true, suppresses timeline messages on successful setup. */
    @JsonProperty("silent") Boolean silent,
    /** Existing Mission Control task ID to attach the exported session to. */
    @JsonProperty("taskId") String taskId,
    /** Reattach to an existing MC session without creating a new one. */
    @JsonProperty("existingMcSession") RemoteControlConfigExistingMcSession existingMcSession
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public RemoteControlConfig {}

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
        private Boolean remote;
        private Boolean steerable;
        private Boolean explicit;
        private Boolean silent;
        private String taskId;
        private RemoteControlConfigExistingMcSession existingMcSession;

        /**
         * Sets the {@code remote} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remote(Boolean value) {
            this.remote = value;
            return this;
        }

        /**
         * Sets the {@code steerable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder steerable(Boolean value) {
            this.steerable = value;
            return this;
        }

        /**
         * Sets the {@code explicit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder explicit(Boolean value) {
            this.explicit = value;
            return this;
        }

        /**
         * Sets the {@code silent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder silent(Boolean value) {
            this.silent = value;
            return this;
        }

        /**
         * Sets the {@code taskId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder taskId(String value) {
            this.taskId = value;
            return this;
        }

        /**
         * Sets the {@code existingMcSession} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder existingMcSession(RemoteControlConfigExistingMcSession value) {
            this.existingMcSession = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RemoteControlConfig build() {
            if (remote == null) throw new IllegalStateException("Missing required property: remote");
            if (steerable == null) throw new IllegalStateException("Missing required property: steerable");
            if (explicit == null) throw new IllegalStateException("Missing required property: explicit");
            if (silent == null) throw new IllegalStateException("Missing required property: silent");
            return new RemoteControlConfig(remote, steerable, explicit, silent, taskId, existingMcSession);
        }
    }
}
