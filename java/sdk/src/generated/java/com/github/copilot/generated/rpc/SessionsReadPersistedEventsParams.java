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
 * Pagination options for reading an inactive or active local session's persisted event journal.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsReadPersistedEventsParams(
    /** Session ID whose persisted event journal should be read. */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque cursor returned by a previous persisted-event read. Omit on the first call. */
    @JsonProperty("cursor") String cursor,
    /** Maximum number of events to return in this batch (1–1000, default 200). */
    @JsonProperty("max") Long max,
    /** Direction to page through persisted history. Forward starts at the beginning; backward starts with the newest events. Events in each page remain chronological. */
    @JsonProperty("direction") EventsReadDirection direction
) {
}
