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
    /** Hosts allowed through the built-in sandbox proxy. A non-empty list denies unmatched hosts; an absent or empty list allows all hosts not blocked. Supports exact hostnames, IP addresses, and *.example.com for strict subdomains. Host rules do not override the outbound or local-network toggles. */
    @JsonProperty("allowedHosts") List<String> allowedHosts,
    /** Hosts denied by the built-in sandbox proxy. Deny rules take precedence over allowedHosts. A domain also denies all its subdomains. IP addresses match exactly; *.example.com matches strict subdomains, and * denies every host. */
    @JsonProperty("blockedHosts") List<String> blockedHosts,
    /** Whether outbound network traffic is allowed at all. */
    @JsonProperty("allowOutbound") Boolean allowOutbound,
    /** Whether traffic to local/loopback addresses is allowed. */
    @JsonProperty("allowLocalNetwork") Boolean allowLocalNetwork,
    /** HTTP(S) proxy for sandboxed traffic. With host rules, this is the built-in local proxy's upstream; credentials stay in the runtime, and Linux and macOS restrict the child to the local listener. Without host rules, Linux restricts egress to this endpoint but rejects credentials, and macOS proxying is cooperative. Windows enforcement depends on the application's networking stack. Configure credentials in the separate username/password fields. The transient local listener URL is never persisted. */
    @JsonProperty("proxy") SandboxConfigUserPolicyNetworkProxy proxy
) {
}
