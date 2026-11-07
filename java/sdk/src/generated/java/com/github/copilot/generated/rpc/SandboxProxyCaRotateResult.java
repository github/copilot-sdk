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
 * Status of the persistent certificate authority of the sandbox credential proxy.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxProxyCaRotateResult(
    /** The state of the certificate authority. */
    @JsonProperty("state") SandboxProxyCaState state,
    /** Human-readable reason for the state. On `installed` or `notInstalled`, present only when the certificate authority must be rotated, and then says why. */
    @JsonProperty("detail") String detail,
    /** Whether this process can add the certificate authority to OS trust without credentials from a different user. False where OS trust is unsupported, and on Windows when the process cannot elevate itself to write the machine trust store. When false, do not offer to set up the certificate authority. */
    @JsonProperty("canInstall") Boolean canInstall
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SandboxProxyCaRotateResult {}

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
        private SandboxProxyCaState state;
        private String detail;
        private Boolean canInstall;

        /**
         * Sets the {@code state} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder state(SandboxProxyCaState value) {
            this.state = value;
            return this;
        }

        /**
         * Sets the {@code detail} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder detail(String value) {
            this.detail = value;
            return this;
        }

        /**
         * Sets the {@code canInstall} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder canInstall(Boolean value) {
            this.canInstall = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxProxyCaRotateResult build() {
            if (state == null) throw new IllegalStateException("Missing required property: state");
            if (canInstall == null) throw new IllegalStateException("Missing required property: canInstall");
            return new SandboxProxyCaRotateResult(state, detail, canInstall);
        }
    }
}
