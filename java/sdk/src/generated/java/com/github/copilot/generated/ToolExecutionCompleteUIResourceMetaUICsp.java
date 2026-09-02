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
 * CSP domain allowlists for an MCP Apps UI resource, including connect, resource, frame, and base URI domains.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteUIResourceMetaUICsp(
    /** Domains the UI resource may connect to. */
    @JsonProperty("connectDomains") List<String> connectDomains,
    /** Domains from which the UI resource may load scripts, styles, images, and other resources. */
    @JsonProperty("resourceDomains") List<String> resourceDomains,
    /** Domains the UI resource may embed as nested frames. */
    @JsonProperty("frameDomains") List<String> frameDomains,
    /** Domains the UI resource may use as document base URIs. */
    @JsonProperty("baseUriDomains") List<String> baseUriDomains
) {
}
