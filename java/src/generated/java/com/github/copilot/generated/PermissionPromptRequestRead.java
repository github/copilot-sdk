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
 * File read permission prompt
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionPromptRequestRead extends PermissionPromptRequest {

    @JsonProperty("kind")
    private final String kind = "read";

    @Override
    public String getKind() { return kind; }

    /** Tool call ID that triggered this permission request */
    @JsonProperty("toolCallId")
    private String toolCallId;

    /** Human-readable description of why the file is being read */
    @JsonProperty("intention")
    private String intention;

    /** Path of the file or directory being read */
    @JsonProperty("path")
    private String path;

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public String getIntention() { return intention; }
    public void setIntention(String intention) { this.intention = intention; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
