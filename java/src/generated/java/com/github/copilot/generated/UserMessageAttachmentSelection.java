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
 * Code selection attachment from an editor
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UserMessageAttachmentSelection extends UserMessageAttachment {

    @JsonProperty("type")
    private final String type = "selection";

    @Override
    public String getType() { return type; }

    /** Absolute path to the file containing the selection */
    @JsonProperty("filePath")
    private String filePath;

    /** User-facing display name for the selection */
    @JsonProperty("displayName")
    private String displayName;

    /** The selected text content */
    @JsonProperty("text")
    private String text;

    /** Position range of the selection within the file */
    @JsonProperty("selection")
    private UserMessageAttachmentSelectionDetails selection;

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public UserMessageAttachmentSelectionDetails getSelection() { return selection; }
    public void setSelection(UserMessageAttachmentSelectionDetails selection) { this.selection = selection; }
}
