/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Icon image for a resource
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteContentResourceLinkIcon(
    /** URL or path to the icon image */
    @JsonProperty("src") String src,
    /** MIME type of the icon image */
    @JsonProperty("mimeType") String mimeType,
    /** Available icon sizes (e.g., ['16x16', '32x32']) */
    @JsonProperty("sizes") List<String> sizes,
    /** Theme variant this icon is intended for */
    @JsonProperty("theme") ToolExecutionCompleteContentResourceLinkIconTheme theme
) {
}
