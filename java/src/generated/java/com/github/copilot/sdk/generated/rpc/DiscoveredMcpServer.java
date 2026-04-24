/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscoveredMcpServer(
    /** Server name (config key) */
    @JsonProperty("name") String name,
    /** Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio) */
    @JsonProperty("type") DiscoveredMcpServerType type,
    /** Configuration source */
    @JsonProperty("source") DiscoveredMcpServerSource source,
    /** Whether the server is enabled (not in the disabled list) */
    @JsonProperty("enabled") Boolean enabled
) {
}
