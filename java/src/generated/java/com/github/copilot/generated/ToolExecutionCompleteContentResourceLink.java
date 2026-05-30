/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Resource link content block referencing an external resource
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolExecutionCompleteContentResourceLink extends ToolExecutionCompleteContent {

    @JsonProperty("type")
    private final String type = "resource_link";

    @Override
    public String getType() { return type; }

    /** Icons associated with this resource */
    @JsonProperty("icons")
    private List<ToolExecutionCompleteContentResourceLinkIcon> icons;

    /** Resource name identifier */
    @JsonProperty("name")
    private String name;

    /** Human-readable display title for the resource */
    @JsonProperty("title")
    private String title;

    /** URI identifying the resource */
    @JsonProperty("uri")
    private String uri;

    /** Human-readable description of the resource */
    @JsonProperty("description")
    private String description;

    /** MIME type of the resource content */
    @JsonProperty("mimeType")
    private String mimeType;

    /** Size of the resource in bytes */
    @JsonProperty("size")
    private Long size;

    public List<ToolExecutionCompleteContentResourceLinkIcon> getIcons() { return icons; }
    public void setIcons(List<ToolExecutionCompleteContentResourceLinkIcon> icons) { this.icons = icons; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
