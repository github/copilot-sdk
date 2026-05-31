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
 * Schema for the `UserToolSessionApprovalCustomTool` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UserToolSessionApprovalCustomTool extends UserToolSessionApproval {

    @JsonProperty("kind")
    private final String kind = "custom-tool";

    @Override
    public String getKind() { return kind; }

    /** Custom tool name */
    @JsonProperty("toolName")
    private String toolName;

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
}
