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
 * Whether this host can run one sandbox policy feature. A session whose effective policy uses an unsupported feature fails each sandboxed command with `reason`, except `filesystem_enumeration`, whose absence degrades sandboxed PowerShell instead of failing it.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxHostCapability(
    /** The policy feature, as an extensible string: ignore names you do not recognize. Known values: `network` (sandboxed commands can reach the network; on Linux this needs the tooling for Bubblewrap's private network namespace, such as slirp4netns), `network_filtering` (host rules and the sandbox proxy; on Linux this needs the same tooling as `network`; on Windows it needs Process Security Environment 1.1 host-loopback support or MXC's PSEC 1.0-only proxy-loopback compatibility capability, and a policy that uses it must also set `network.allowLocalNetwork`; compatibility applies only to an explicit identity-less runtime proxy, not general host-loopback access, and other policy restrictions still apply), `denied_paths` (native enforcement of `filesystem.deniedPaths`), `shell` (shell commands inside the sandbox), and `filesystem_enumeration` (enumerate-only filesystem grants: PowerShell's drive roots and `filesystem.enumeratePaths`; on Windows this needs Process Security Environment 1.1 filesystem enumeration support, and without it sandboxed PowerShell still runs but cannot resolve its current location, and `filesystem.enumeratePaths` entries are ignored; other platforms always report it). */
    @JsonProperty("name") String name,
    /** Whether this host can run the feature. */
    @JsonProperty("supported") Boolean supported,
    /** Human-readable reason and remedy when the feature is unsupported, such as a package to install or an OS update. Present only when `supported` is false. */
    @JsonProperty("reason") String reason
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SandboxHostCapability {}

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
        private String name;
        private Boolean supported;
        private String reason;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxHostCapability build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (supported == null) throw new IllegalStateException("Missing required property: supported");
            return new SandboxHostCapability(name, supported, reason);
        }
    }
}
