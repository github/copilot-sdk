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
 * File write permission request
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionRequestWrite extends PermissionRequest {

    @JsonProperty("kind")
    private final String kind = "write";

    @Override
    public String getKind() { return kind; }

    /** Tool call ID that triggered this permission request */
    @JsonProperty("toolCallId")
    private String toolCallId;

    /** Human-readable description of the intended file change */
    @JsonProperty("intention")
    private String intention;

    /** Path of the file being written to */
    @JsonProperty("fileName")
    private String fileName;

    /** Unified diff showing the proposed changes */
    @JsonProperty("diff")
    private String diff;

    /** Complete new file contents for newly created files */
    @JsonProperty("newFileContents")
    private String newFileContents;

    /** Whether the UI can offer session-wide approval for file write operations */
    @JsonProperty("canOfferSessionApproval")
    private Boolean canOfferSessionApproval;

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public String getIntention() { return intention; }
    public void setIntention(String intention) { this.intention = intention; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getDiff() { return diff; }
    public void setDiff(String diff) { this.diff = diff; }

    public String getNewFileContents() { return newFileContents; }
    public void setNewFileContents(String newFileContents) { this.newFileContents = newFileContents; }

    public Boolean getCanOfferSessionApproval() { return canOfferSessionApproval; }
    public void setCanOfferSessionApproval(Boolean canOfferSessionApproval) { this.canOfferSessionApproval = canOfferSessionApproval; }
}
