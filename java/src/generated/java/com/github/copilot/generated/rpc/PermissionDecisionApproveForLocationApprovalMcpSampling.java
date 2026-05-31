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
 * Schema for the `PermissionDecisionApproveForLocationApprovalMcpSampling` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionDecisionApproveForLocationApprovalMcpSampling extends PermissionDecisionApproveForLocationApproval {

    @JsonProperty("kind")
    private final String kind = "mcp-sampling";

    @Override
    public String getKind() { return kind; }

    /** MCP server name. */
    @JsonProperty("serverName")
    private String serverName;

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
}
