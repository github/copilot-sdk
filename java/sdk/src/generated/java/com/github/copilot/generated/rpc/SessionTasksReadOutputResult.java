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
 * Output a detached shell task wrote after the request cursor, with the cursor for the next read.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionTasksReadOutputResult(
    /** Output after the request cursor, decoded as UTF-8 and redacted. At most 64 KiB per read, so a large backlog takes several reads; the text never splits a character, and text that stops before the end of the output ends at a line break when it contains one. Empty when no new output is available. Detached shells write stdout and stderr to one output, so the text does not separate them. */
    @JsonProperty("text") String text,
    /** Opaque cursor to pass to the next read of this task. Always present, even when no output was returned. */
    @JsonProperty("cursor") String cursor,
    /** Present once the shell has finished and this read reaches the end of its output. Later reads with the returned cursor return no new text and the same exit details. */
    @JsonProperty("exit") TaskShellOutputExit exit
) {
}
