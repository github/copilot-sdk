/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Details of the permission being requested
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PermissionRequestShell.class, name = "shell"),
    @JsonSubTypes.Type(value = PermissionRequestWrite.class, name = "write"),
    @JsonSubTypes.Type(value = PermissionRequestRead.class, name = "read"),
    @JsonSubTypes.Type(value = PermissionRequestMcp.class, name = "mcp"),
    @JsonSubTypes.Type(value = PermissionRequestUrl.class, name = "url"),
    @JsonSubTypes.Type(value = PermissionRequestMemory.class, name = "memory"),
    @JsonSubTypes.Type(value = PermissionRequestCustomTool.class, name = "custom-tool"),
    @JsonSubTypes.Type(value = PermissionRequestHook.class, name = "hook"),
    @JsonSubTypes.Type(value = PermissionRequestExtensionManagement.class, name = "extension-management"),
    @JsonSubTypes.Type(value = PermissionRequestExtensionPermissionAccess.class, name = "extension-permission-access")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class PermissionRequest {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
