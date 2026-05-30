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
 * Session-scoped approval to remember (tool prompts only; omitted for path/url prompts)
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PermissionDecisionApproveForSessionApprovalCommands.class, name = "commands"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForSessionApprovalRead.class, name = "read"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForSessionApprovalWrite.class, name = "write"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForSessionApprovalMcp.class, name = "mcp"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForSessionApprovalMcpSampling.class, name = "mcp-sampling"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForSessionApprovalMemory.class, name = "memory"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForSessionApprovalCustomTool.class, name = "custom-tool"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForSessionApprovalExtensionManagement.class, name = "extension-management"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess.class, name = "extension-permission-access")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class PermissionDecisionApproveForSessionApproval {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
