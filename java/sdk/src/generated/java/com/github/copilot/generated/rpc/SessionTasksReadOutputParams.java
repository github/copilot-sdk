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
 * Detached shell task to read output from, with an optional cursor and long-poll wait. waitMs defaults to zero (maximum 30000).
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionTasksReadOutputParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Identifier of a detached shell task, as returned by tasks.list */
    @JsonProperty("id") String id,
    /** Opaque cursor returned by an earlier read of the same task. Omit to read from the beginning of the shell's output. If the output no longer reaches the cursor, for example because its log was removed, the request fails; read again without a cursor. */
    @JsonProperty("cursor") String cursor,
    /** Milliseconds to wait for new output, or for the shell to finish, when the cursor is already at the end of the available output. 0 (default) returns immediately. Capped at 30000ms. */
    @JsonProperty("waitMs") Long waitMs
) {
}
