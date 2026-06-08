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
    /** Whether outbound network traffic is allowed at all. */
    @JsonProperty("allowOutbound") Boolean allowOutbound,
    /** Whether traffic to local/loopback addresses is allowed. */
    @JsonProperty("allowLocalNetwork") Boolean allowLocalNetwork,
    /** Hosts allowed in addition to the base policy. */
    @JsonProperty("allowedHosts") List<String> allowedHosts,
    /** Hosts explicitly blocked. */
    @JsonProperty("blockedHosts") List<String> blockedHosts
) {
}
