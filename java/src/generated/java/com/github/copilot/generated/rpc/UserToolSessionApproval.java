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
 * The approval to add as a session-scoped rule
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserToolSessionApprovalCommands.class, name = "commands"),
    @JsonSubTypes.Type(value = UserToolSessionApprovalRead.class, name = "read"),
    @JsonSubTypes.Type(value = UserToolSessionApprovalWrite.class, name = "write"),
    @JsonSubTypes.Type(value = UserToolSessionApprovalMcp.class, name = "mcp"),
    @JsonSubTypes.Type(value = UserToolSessionApprovalMemory.class, name = "memory"),
    @JsonSubTypes.Type(value = UserToolSessionApprovalCustomTool.class, name = "custom-tool"),
    @JsonSubTypes.Type(value = UserToolSessionApprovalExtensionManagement.class, name = "extension-management"),
    @JsonSubTypes.Type(value = UserToolSessionApprovalExtensionPermissionAccess.class, name = "extension-permission-access")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class UserToolSessionApproval {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
