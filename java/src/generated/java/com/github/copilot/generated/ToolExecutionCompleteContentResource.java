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
 * Embedded resource content block with inline text or binary data
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolExecutionCompleteContentResource extends ToolExecutionCompleteContent {

    @JsonProperty("type")
    private final String type = "resource";

    @Override
    public String getType() { return type; }

    /** The embedded resource contents, either text or base64-encoded binary */
    @JsonProperty("resource")
    private Object resource;

    public Object getResource() { return resource; }
    public void setResource(Object resource) { this.resource = resource; }
}
