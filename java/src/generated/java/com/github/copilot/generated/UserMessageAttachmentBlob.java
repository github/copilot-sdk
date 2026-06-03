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
 * Blob attachment with inline base64-encoded data
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UserMessageAttachmentBlob extends UserMessageAttachment {

    @JsonProperty("type")
    private final String type = "blob";

    @Override
    public String getType() { return type; }

    /** Base64-encoded content */
    @JsonProperty("data")
    private String data;

    /** MIME type of the inline data */
    @JsonProperty("mimeType")
    private String mimeType;

    /** User-facing display name for the attachment */
    @JsonProperty("displayName")
    private String displayName;

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
