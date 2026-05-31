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
 * Tool approval to persist and apply
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PermissionsLocationsAddToolApprovalDetailsCommands.class, name = "commands"),
    @JsonSubTypes.Type(value = PermissionsLocationsAddToolApprovalDetailsRead.class, name = "read"),
    @JsonSubTypes.Type(value = PermissionsLocationsAddToolApprovalDetailsWrite.class, name = "write"),
    @JsonSubTypes.Type(value = PermissionsLocationsAddToolApprovalDetailsMcp.class, name = "mcp"),
    @JsonSubTypes.Type(value = PermissionsLocationsAddToolApprovalDetailsMcpSampling.class, name = "mcp-sampling"),
    @JsonSubTypes.Type(value = PermissionsLocationsAddToolApprovalDetailsMemory.class, name = "memory"),
    @JsonSubTypes.Type(value = PermissionsLocationsAddToolApprovalDetailsCustomTool.class, name = "custom-tool"),
    @JsonSubTypes.Type(value = PermissionsLocationsAddToolApprovalDetailsExtensionManagement.class, name = "extension-management"),
    @JsonSubTypes.Type(value = PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess.class, name = "extension-permission-access")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class PermissionsLocationsAddToolApprovalDetails {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
