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
 * Network rules to merge into the base policy.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxConfigUserPolicyNetwork(
    /** Whether outbound network traffic is allowed at all. */
    @JsonProperty("allowOutbound") Boolean allowOutbound,
    /** Whether traffic to local/loopback addresses is allowed. */
    @JsonProperty("allowLocalNetwork") Boolean allowLocalNetwork,
    /** HTTP proxy for sandboxed process traffic. Linux restricts egress to the proxy endpoint, requires that endpoint to be reachable over IPv4 (the [::] dual-stack wildcard is accepted and routed through the IPv4 gateway), and does not support proxy credentials. macOS relies on applications honoring proxy environment variables. Windows also configures a per-AppContainer WinHTTP proxy, but enforcement depends on the application's networking stack. Configure supported credentials in the separate `username` and `password` fields. A credential-free http:// loopback URL uses the localhost proxy form, while an https:// or authenticated loopback URL uses the URL form. */
    @JsonProperty("proxy") SandboxConfigUserPolicyNetworkProxy proxy
) {
}
