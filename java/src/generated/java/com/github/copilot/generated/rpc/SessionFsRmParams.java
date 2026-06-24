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
import javax.annotation.processing.Generated;

/**
 * Path to remove from the client-provided session filesystem, with options for recursive removal and force.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsRmParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Path using SessionFs conventions */
    @JsonProperty("path") String path,
    /** Remove directories and their contents recursively */
    @JsonProperty("recursive") Boolean recursive,
    /** Ignore errors if the path does not exist */
    @JsonProperty("force") Boolean force
) {
}
