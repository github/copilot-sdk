/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Approval to persist for this location
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PermissionDecisionApproveForLocationApprovalCommands.class, name = "commands"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForLocationApprovalRead.class, name = "read"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForLocationApprovalWrite.class, name = "write"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForLocationApprovalMcp.class, name = "mcp"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForLocationApprovalMcpSampling.class, name = "mcp-sampling"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForLocationApprovalMemory.class, name = "memory"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForLocationApprovalCustomTool.class, name = "custom-tool"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForLocationApprovalExtensionManagement.class, name = "extension-management"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess.class, name = "extension-permission-access")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class PermissionDecisionApproveForLocationApproval {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
