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
 * Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsForkParams(
    /** Source session ID to fork from */
    @JsonProperty("sessionId") String sessionId,
    /** Optional event ID boundary. When provided, the fork includes only events before this ID (exclusive). When omitted, all events are included. */
    @JsonProperty("toEventId") String toEventId,
    /** Optional friendly name to assign to the forked session. */
    @JsonProperty("name") String name
) {
}
