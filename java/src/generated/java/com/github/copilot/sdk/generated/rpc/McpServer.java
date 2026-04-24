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
public record McpServer(
    /** Server name (config key) */
    @JsonProperty("name") String name,
    /** Connection status: connected, failed, needs-auth, pending, disabled, or not_configured */
    @JsonProperty("status") McpServerStatus status,
    /** Configuration source: user, workspace, plugin, or builtin */
    @JsonProperty("source") McpServerSource source,
    /** Error message if the server failed to connect */
    @JsonProperty("error") String error
) {
}
