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
 * User-managed sandbox policy fragment merged into the auto-discovered base policy.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxConfigUserPolicy(
    /** Filesystem rules to merge into the base policy. */
    @JsonProperty("filesystem") SandboxConfigUserPolicyFilesystem filesystem,
    /** Network rules to merge into the base policy. */
    @JsonProperty("network") SandboxConfigUserPolicyNetwork network,
    /** macOS seatbelt options to merge into the base policy. */
    @JsonProperty("seatbelt") SandboxConfigUserPolicySeatbelt seatbelt,
    /** Deprecated legacy location for `seatbelt`; read only when the top-level `seatbelt` is absent. */
    @JsonProperty("experimental") SandboxConfigUserPolicyExperimental experimental
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SandboxConfigUserPolicy {}

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
        private SandboxConfigUserPolicyFilesystem filesystem;
        private SandboxConfigUserPolicyNetwork network;
        private SandboxConfigUserPolicySeatbelt seatbelt;
        private SandboxConfigUserPolicyExperimental experimental;

        /**
         * Sets the {@code filesystem} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder filesystem(SandboxConfigUserPolicyFilesystem value) {
            this.filesystem = value;
            return this;
        }

        /**
         * Sets the {@code network} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder network(SandboxConfigUserPolicyNetwork value) {
            this.network = value;
            return this;
        }

        /**
         * Sets the {@code seatbelt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder seatbelt(SandboxConfigUserPolicySeatbelt value) {
            this.seatbelt = value;
            return this;
        }

        /**
         * Sets the {@code experimental} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder experimental(SandboxConfigUserPolicyExperimental value) {
            this.experimental = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxConfigUserPolicy build() {
            return new SandboxConfigUserPolicy(filesystem, network, seatbelt, experimental);
        }
    }
}
