/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "tool.shell_output". Live, append-only shell output. Not persisted or replayed to late subscribers. Text is decoded and redacted per chunk; chunks need not contain complete lines.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolShellOutputEvent extends SessionEvent {

    @Override
    public String getType() { return "tool.shell_output"; }

    @JsonProperty("data")
    private ToolShellOutputEventData data;

    public ToolShellOutputEventData getData() { return data; }
    public void setData(ToolShellOutputEventData data) { this.data = data; }

    /** Data payload for {@link ToolShellOutputEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolShellOutputEventData(
        /** Tool call ID that owns this shell output */
        @JsonProperty("toolCallId") String toolCallId,
        /** Output source. Omission means stdout. Terminal output has no separate stdout/stderr attribution. */
        @JsonProperty("stream") ToolShellOutputStream stream,
        /** New output to append, without synthetic shell-result markers or stream-switch separators */
        @JsonProperty("text") String text,
        /** Zero-based publication sequence across all output streams for this tool call. Not a byte offset or an OS write-order guarantee. */
        @JsonProperty("sequence") Long sequence
    ) {
    }
}
