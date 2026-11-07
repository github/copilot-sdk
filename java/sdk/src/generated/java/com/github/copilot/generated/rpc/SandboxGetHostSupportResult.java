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
 * Whether the host running this runtime can run the command sandbox. The runtime checks `supported` once per process. A capability answer can change while the process runs, for example after the user installs a missing package.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxGetHostSupportResult(
    /** Whether a process-containment backend is usable on this host: Seatbelt on macOS, Bubblewrap on Linux, or ProcessContainer on Windows. */
    @JsonProperty("supported") Boolean supported,
    /** Human-readable reason the sandbox cannot run on this host. Present only when `supported` is false. */
    @JsonProperty("reason") String reason,
    /** Sandbox policy features whose availability varies between hosts that can run the backend. Empty when `supported` is false, because no feature can run without a backend. Later runtimes can add entries; ignore an entry whose `name` you do not recognize. */
    @JsonProperty("capabilities") List<SandboxHostCapability> capabilities
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SandboxGetHostSupportResult {}

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
        private Boolean supported;
        private String reason;
        private List<SandboxHostCapability> capabilities;

        /**
         * Sets the {@code supported} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder supported(Boolean value) {
            this.supported = value;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(String value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code capabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder capabilities(List<SandboxHostCapability> value) {
            this.capabilities = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxGetHostSupportResult build() {
            if (supported == null) throw new IllegalStateException("Missing required property: supported");
            if (capabilities == null) throw new IllegalStateException("Missing required property: capabilities");
            return new SandboxGetHostSupportResult(supported, reason, capabilities);
        }
    }
}
