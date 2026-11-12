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
 * One cursor-addressed page of retained experimental runtime lifecycle diagnostic events.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record LifecycleDiagnosticsReadResult(
    /** Retained lifecycle diagnostic events beginning at the requested cursor. */
    @JsonProperty("events") List<LifecycleDiagnosticEvent> events,
    /** Opaque cursor for the next independent lifecycle diagnostics read. */
    @JsonProperty("cursor") String cursor,
    /** Whether the requested cursor remained within the retained activation window. */
    @JsonProperty("cursorStatus") DiagnosticCursorStatus cursorStatus,
    /** Number of events lost before this page when known. Omitted when an activation change makes the count unknowable. */
    @JsonProperty("droppedCount") Long droppedCount,
    /** Whether additional retained lifecycle diagnostic events follow this page. */
    @JsonProperty("hasMore") Boolean hasMore
) {
}
