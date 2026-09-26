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
 * One cursor-addressed page of retained session diagnostics.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionDiagnosticsReadResult(
    /** Retained records beginning at the requested cursor. */
    @JsonProperty("entries") List<SessionDiagnosticsReadResultEntriesItem> entries,
    /** Opaque cursor for the next independent read. */
    @JsonProperty("cursor") String cursor,
    /** Whether the requested cursor remained within the retained buffer window. */
    @JsonProperty("cursorStatus") DiagnosticCursorStatus cursorStatus,
    /** Number of records lost before this page when known. Omitted when a buffer generation change makes the count unknowable. */
    @JsonProperty("droppedCount") Long droppedCount,
    /** Whether additional retained records follow this page. */
    @JsonProperty("hasMore") Boolean hasMore
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionDiagnosticsReadResultEntriesItem(
        /** UTC RFC 3339 timestamp captured at the diagnostic source. */
        @JsonProperty("timestamp") String timestamp,
        /** Severity of this emitted diagnostic record. */
        @JsonProperty("level") DiagnosticSeverity level,
        /** Agent identifier for a subagent host. Omitted for the root agent. */
        @JsonProperty("agentId") String agentId,
        /** Human-readable diagnostic summary. */
        @JsonProperty("message") String message,
        /** Whether message or data was truncated to the record-size bound. */
        @JsonProperty("truncated") Boolean truncated,
        /** Original byte count when a known-size message or data value was truncated. */
        @JsonProperty("originalBytes") Long originalBytes,
        /** Typed MCP diagnostic detail. */
        @JsonProperty("details") McpDiagnosticDetails details,
        /** Diagnostic source identifying the typed details payload. */
        @JsonProperty("source") String source
    ) {
    }
}
