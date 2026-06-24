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
 * File path, content to append, and optional mode for the client-provided session filesystem.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsAppendFileParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Path using SessionFs conventions */
    @JsonProperty("path") String path,
    /** Content to append */
    @JsonProperty("content") String content,
    /** Optional POSIX-style mode for newly created files */
    @JsonProperty("mode") Long mode
) {
}
