/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * A content block within a tool result, which may be text, terminal output, image, audio, or a resource
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ToolExecutionCompleteContentText.class, name = "text"),
    @JsonSubTypes.Type(value = ToolExecutionCompleteContentTerminal.class, name = "terminal"),
    @JsonSubTypes.Type(value = ToolExecutionCompleteContentImage.class, name = "image"),
    @JsonSubTypes.Type(value = ToolExecutionCompleteContentAudio.class, name = "audio"),
    @JsonSubTypes.Type(value = ToolExecutionCompleteContentResourceLink.class, name = "resource_link"),
    @JsonSubTypes.Type(value = ToolExecutionCompleteContentResource.class, name = "resource")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class ToolExecutionCompleteContent {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the type discriminator
     */
    public abstract String getType();
}
