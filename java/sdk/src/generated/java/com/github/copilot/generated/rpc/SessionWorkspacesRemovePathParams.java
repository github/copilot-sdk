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
 * File or directory to remove from the session workspace files directory.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkspacesRemovePathParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Slash-separated relative path within the workspace files directory */
    @JsonProperty("path") String path,
    /** Whether to remove directory contents recursively. Defaults to false. */
    @JsonProperty("recursive") Boolean recursive,
    /** Whether a missing path should be treated as success. Defaults to false. */
    @JsonProperty("force") Boolean force
) {
}
