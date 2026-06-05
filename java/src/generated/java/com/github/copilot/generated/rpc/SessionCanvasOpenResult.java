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
 * Open canvas instance snapshot.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionCanvasOpenResult(
    /** Stable caller-supplied canvas instance identifier */
    @JsonProperty("instanceId") String instanceId,
    /** Owning provider identifier */
    @JsonProperty("extensionId") String extensionId,
    /** Owning extension display name, when available */
    @JsonProperty("extensionName") String extensionName,
    /** Provider-local canvas identifier */
    @JsonProperty("canvasId") String canvasId,
    /** Rendered title */
    @JsonProperty("title") String title,
    /** Provider-supplied status text */
    @JsonProperty("status") String status,
    /** URL for web-rendered canvases */
    @JsonProperty("url") String url,
    /** Input supplied when the instance was opened */
    @JsonProperty("input") Object input,
    /** Whether this snapshot came from an idempotent reopen */
    @JsonProperty("reopen") Boolean reopen,
    /** Runtime-controlled routing state for an open canvas instance. */
    @JsonProperty("availability") CanvasInstanceAvailability availability
) {
}
