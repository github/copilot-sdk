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
 * Derived user-facing permission prompt details for UI consumers
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PermissionPromptRequestCommands.class, name = "commands"),
    @JsonSubTypes.Type(value = PermissionPromptRequestWrite.class, name = "write"),
    @JsonSubTypes.Type(value = PermissionPromptRequestRead.class, name = "read"),
    @JsonSubTypes.Type(value = PermissionPromptRequestMcp.class, name = "mcp"),
    @JsonSubTypes.Type(value = PermissionPromptRequestUrl.class, name = "url"),
    @JsonSubTypes.Type(value = PermissionPromptRequestMemory.class, name = "memory"),
    @JsonSubTypes.Type(value = PermissionPromptRequestCustomTool.class, name = "custom-tool"),
    @JsonSubTypes.Type(value = PermissionPromptRequestPath.class, name = "path"),
    @JsonSubTypes.Type(value = PermissionPromptRequestHook.class, name = "hook"),
    @JsonSubTypes.Type(value = PermissionPromptRequestExtensionManagement.class, name = "extension-management"),
    @JsonSubTypes.Type(value = PermissionPromptRequestExtensionPermissionAccess.class, name = "extension-permission-access")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class PermissionPromptRequest {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
