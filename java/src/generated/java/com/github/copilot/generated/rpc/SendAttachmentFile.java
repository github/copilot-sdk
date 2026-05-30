/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * File attachment
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SendAttachmentFile extends SendAttachment {

    @JsonProperty("type")
    private final String type = "file";

    @Override
    public String getType() { return type; }

    /** Absolute file path */
    @JsonProperty("path")
    private String path;

    /** User-facing display name for the attachment */
    @JsonProperty("displayName")
    private String displayName;

    /** Optional line range to scope the attachment to a specific section of the file */
    @JsonProperty("lineRange")
    private SendAttachmentFileLineRange lineRange;

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public SendAttachmentFileLineRange getLineRange() { return lineRange; }
    public void setLineRange(SendAttachmentFileLineRange lineRange) { this.lineRange = lineRange; }
}
