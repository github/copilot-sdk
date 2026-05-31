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
 * MCP tool invocation permission request
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionRequestMcp extends PermissionRequest {

    @JsonProperty("kind")
    private final String kind = "mcp";

    @Override
    public String getKind() { return kind; }

    /** Tool call ID that triggered this permission request */
    @JsonProperty("toolCallId")
    private String toolCallId;

    /** Name of the MCP server providing the tool */
    @JsonProperty("serverName")
    private String serverName;

    /** Internal name of the MCP tool */
    @JsonProperty("toolName")
    private String toolName;

    /** Human-readable title of the MCP tool */
    @JsonProperty("toolTitle")
    private String toolTitle;

    /** Arguments to pass to the MCP tool */
    @JsonProperty("args")
    private Object args;

    /** Whether this MCP tool is read-only (no side effects) */
    @JsonProperty("readOnly")
    private Boolean readOnly;

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getToolTitle() { return toolTitle; }
    public void setToolTitle(String toolTitle) { this.toolTitle = toolTitle; }

    public Object getArgs() { return args; }
    public void setArgs(Object args) { this.args = args; }

    public Boolean getReadOnly() { return readOnly; }
    public void setReadOnly(Boolean readOnly) { this.readOnly = readOnly; }
}
