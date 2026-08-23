/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Identity of the integrating host, declared once on the `server.connect` handshake so telemetry from this connection is attributed to a single, consistent surface. All fields are optional; omit them to keep the default attribution.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectClientInfo(
    /** Name of the host editor, e.g. `"vscode"`. */
    @JsonProperty("editorName") String editorName,
    /** Version of the host editor, e.g. `"1.124.2"`. Ignored unless it looks like a version string. */
    @JsonProperty("editorVersion") String editorVersion,
    /** Name of the Copilot extension within the host, e.g. `"copilot-chat"`. */
    @JsonProperty("extensionName") String extensionName,
    /** Version of the Copilot extension within the host, e.g. `"0.54.0"`. Ignored unless it looks like a version string. */
    @JsonProperty("extensionVersion") String extensionVersion
) {
}
