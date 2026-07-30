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
 * Browser permission metadata for an MCP Apps UI resource, including camera, microphone, geolocation, and clipboard-write.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteUIResourceMetaUIPermissions(
    /** Marker object for camera permission on an MCP Apps UI resource. */
    @JsonProperty("camera") ToolExecutionCompleteUIResourceMetaUIPermissionsCamera camera,
    /** Marker object for microphone permission on an MCP Apps UI resource. */
    @JsonProperty("microphone") ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone microphone,
    /** Marker object for geolocation permission on an MCP Apps UI resource. */
    @JsonProperty("geolocation") ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation geolocation,
    /** Marker object for clipboard-write permission on an MCP Apps UI resource. */
    @JsonProperty("clipboardWrite") ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite clipboardWrite
) {
}
