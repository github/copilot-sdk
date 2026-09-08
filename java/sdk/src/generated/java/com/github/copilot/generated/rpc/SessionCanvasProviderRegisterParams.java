/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Internal canvas provider registration parameters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionCanvasProviderRegisterParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Connection identifier for callback routing */
    @JsonProperty("connectionId") String connectionId,
    /** Provider metadata supplied by the host */
    @JsonProperty("info") Object info,
    /** Canvas contributions supplied by the provider */
    @JsonProperty("canvases") List<Object> canvases
) {
}
