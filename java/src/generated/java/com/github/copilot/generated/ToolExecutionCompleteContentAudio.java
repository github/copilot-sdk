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
 * Audio content block with base64-encoded data
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolExecutionCompleteContentAudio extends ToolExecutionCompleteContent {

    @JsonProperty("type")
    private final String type = "audio";

    @Override
    public String getType() { return type; }

    /** Base64-encoded audio data */
    @JsonProperty("data")
    private String data;

    /** MIME type of the audio (e.g., audio/wav, audio/mpeg) */
    @JsonProperty("mimeType")
    private String mimeType;

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}
