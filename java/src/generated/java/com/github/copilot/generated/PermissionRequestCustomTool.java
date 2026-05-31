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
 * Custom tool invocation permission request
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionRequestCustomTool extends PermissionRequest {

    @JsonProperty("kind")
    private final String kind = "custom-tool";

    @Override
    public String getKind() { return kind; }

    /** Tool call ID that triggered this permission request */
    @JsonProperty("toolCallId")
    private String toolCallId;

    /** Name of the custom tool */
    @JsonProperty("toolName")
    private String toolName;

    /** Description of what the custom tool does */
    @JsonProperty("toolDescription")
    private String toolDescription;

    /** Arguments to pass to the custom tool */
    @JsonProperty("args")
    private Object args;

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getToolDescription() { return toolDescription; }
    public void setToolDescription(String toolDescription) { this.toolDescription = toolDescription; }

    public Object getArgs() { return args; }
    public void setArgs(Object args) { this.args = args; }
}
