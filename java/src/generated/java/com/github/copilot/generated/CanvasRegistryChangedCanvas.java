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
 * Schema for the `CanvasRegistryChangedCanvas` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CanvasRegistryChangedCanvas(
    /** Owning provider identifier */
    @JsonProperty("extensionId") String extensionId,
    /** Owning extension display name, when available */
    @JsonProperty("extensionName") String extensionName,
    /** Provider-local canvas identifier */
    @JsonProperty("canvasId") String canvasId,
    /** Human-readable canvas name */
    @JsonProperty("displayName") String displayName,
    /** Short, single-sentence description shown to the agent in canvas catalogs. */
    @JsonProperty("description") String description,
    /** JSON Schema for canvas open input */
    @JsonProperty("inputSchema") Object inputSchema,
    /** Actions the agent or host may invoke */
    @JsonProperty("actions") List<CanvasRegistryChangedCanvasAction> actions
) {
}
