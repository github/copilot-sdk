/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Extension management permission request
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionRequestExtensionManagement extends PermissionRequest {

    @JsonProperty("kind")
    private final String kind = "extension-management";

    @Override
    public String getKind() { return kind; }

    /** Tool call ID that triggered this permission request */
    @JsonProperty("toolCallId")
    private String toolCallId;

    /** The extension management operation (scaffold, reload) */
    @JsonProperty("operation")
    private String operation;

    /** Name of the extension being managed */
    @JsonProperty("extensionName")
    private String extensionName;

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getExtensionName() { return extensionName; }
    public void setExtensionName(String extensionName) { this.extensionName = extensionName; }
}
