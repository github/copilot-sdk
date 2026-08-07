/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * MCP Apps UI resource metadata for a completed tool result, including CSP, permissions, domain, and border preference.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteUIResourceMetaUI(
    /** CSP domain allowlists for an MCP Apps UI resource, including connect, resource, frame, and base URI domains. */
    @JsonProperty("csp") ToolExecutionCompleteUIResourceMetaUICsp csp,
    /** Browser permission metadata for an MCP Apps UI resource, including camera, microphone, geolocation, and clipboard-write. */
    @JsonProperty("permissions") ToolExecutionCompleteUIResourceMetaUIPermissions permissions,
    @JsonProperty("domain") String domain,
    @JsonProperty("prefersBorder") Boolean prefersBorder
) {
}
