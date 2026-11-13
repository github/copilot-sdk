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
 * Cursor-based request for session diagnostics. The default limit is 100 (maximum 500); the default waitMs is zero (maximum 30000).
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionDiagnosticsReadParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Nonempty selection of sources to read. Each source may be listed once. */
    @JsonProperty("sources") List<DiagnosticSource> sources,
    /** Opaque cursor returned by an earlier read. Omit to start at the oldest retained record. */
    @JsonProperty("cursor") String cursor,
    /** Maximum number of records to return, from 1 through 500. Omit for 100. */
    @JsonProperty("max") Long max,
    /** Maximum time in milliseconds to wait for a new record, from 0 through 30000. */
    @JsonProperty("waitMs") Long waitMs
) {
}
