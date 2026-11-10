/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Network rules to merge into the base policy.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxConfigUserPolicyNetwork(
    /** Destinations allowed through the built-in sandbox proxy. Supports hostnames, IP addresses, IPv4/IPv6 CIDRs (10.0.0.0/8, fd00::/8), and *.example.com for strict subdomains. CIDR host bits must be zero; invalid entries suggest the network address. Within each nonempty list, matching names or addresses are alternative grants; every user and managed list must permit every resolved address. Empty lists add no restriction. Host rules never override outbound or local-network toggles. An upstream proxy owns DNS: CIDR-only grants cannot authorize unresolved names, and address denials cannot constrain its resolver. */
    @JsonProperty("allowedHosts") List<String> allowedHosts,
    /** Destinations denied by the built-in sandbox proxy. Deny overrides allowedHosts, including CIDR denials against allowed names and their resolved addresses. Supports exact IPs and IPv4/IPv6 CIDRs with zero host bits. A denied domain also denies its subdomains; *.example.com matches strict subdomains, and * denies every host. With upstream-owned DNS, address denials cannot constrain remotely resolved names. */
    @JsonProperty("blockedHosts") List<String> blockedHosts,
    /** Whether outbound network destinations, including private networks, are allowed. Host-loopback access is controlled separately by allowLocalNetwork. Host rules still apply. */
    @JsonProperty("allowOutbound") Boolean allowOutbound,
    /** Whether host-local destinations, including loopback and unspecified addresses, are allowed. Private networks follow outbound and host rules. Host rules still apply. */
    @JsonProperty("allowLocalNetwork") Boolean allowLocalNetwork,
    /** HTTP(S) proxy for sandboxed traffic. This is the built-in local proxy's upstream: every sandboxed command reaches it through a loopback listener, so credentials stay in the runtime and never reach the child. On Windows the sandbox also needs local network access, because it reaches that listener over host loopback. Configure credentials in the separate username/password fields. The transient local listener URL is never persisted. */
    @JsonProperty("proxy") SandboxConfigUserPolicyNetworkProxy proxy
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SandboxConfigUserPolicyNetwork {}

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
        private List<String> allowedHosts;
        private List<String> blockedHosts;
        private Boolean allowOutbound;
        private Boolean allowLocalNetwork;
        private SandboxConfigUserPolicyNetworkProxy proxy;

        /**
         * Sets the {@code allowedHosts} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allowedHosts(List<String> value) {
            this.allowedHosts = value;
            return this;
        }

        /**
         * Sets the {@code blockedHosts} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder blockedHosts(List<String> value) {
            this.blockedHosts = value;
            return this;
        }

        /**
         * Sets the {@code allowOutbound} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allowOutbound(Boolean value) {
            this.allowOutbound = value;
            return this;
        }

        /**
         * Sets the {@code allowLocalNetwork} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allowLocalNetwork(Boolean value) {
            this.allowLocalNetwork = value;
            return this;
        }

        /**
         * Sets the {@code proxy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder proxy(SandboxConfigUserPolicyNetworkProxy value) {
            this.proxy = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxConfigUserPolicyNetwork build() {
            return new SandboxConfigUserPolicyNetwork(allowedHosts, blockedHosts, allowOutbound, allowLocalNetwork, proxy);
        }
    }
}
