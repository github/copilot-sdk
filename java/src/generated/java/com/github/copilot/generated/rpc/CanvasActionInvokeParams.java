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
 * Canvas action invocation parameters sent to the provider.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CanvasActionInvokeParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Owning provider identifier */
    @JsonProperty("extensionId") String extensionId,
    /** Provider-local canvas identifier */
    @JsonProperty("canvasId") String canvasId,
    /** Canvas instance identifier */
    @JsonProperty("instanceId") String instanceId,
    /** Action name to invoke */
    @JsonProperty("actionName") String actionName,
    /** Action input */
    @JsonProperty("input") Object input,
    /** Host context supplied by the runtime. */
    @JsonProperty("host") CanvasHostContext host,
    /** Session context supplied by the runtime. */
    @JsonProperty("session") CanvasSessionContext session
) {
}
